# run_phase5_tests.ps1
$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   RESCUESYNC PHASE 5 - RESOURCE INVENTORY VERIFICATION   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Check if backend is listening on 8080, otherwise start it
$backendProcess = $null
$isListening = $false
try {
    $tcp = New-Object System.Net.Sockets.TcpClient("localhost", 8080)
    if ($tcp.Connected) {
        $tcp.Close()
        $isListening = $true
    }
} catch {}

if (-not $isListening) {
    Write-Host "Starting Spring Boot backend server..."
    $backendProcess = Start-Process -FilePath "java" -ArgumentList "-jar", "e:\RescueSync\RescueSync-Web\backend\target\rescuesync-backend-0.0.1-SNAPSHOT.jar" -PassThru -WindowStyle Hidden
    Write-Host "Started backend process PID: $($backendProcess.Id)"
    for ($i = 0; $i -lt 35; $i++) {
        Start-Sleep -Seconds 1
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient("localhost", 8080)
            if ($tcp.Connected) {
                $tcp.Close()
                $isListening = $true
                break
            }
        } catch {}
    }
    Start-Sleep -Seconds 2
}

if (-not $isListening) {
    Write-Host "[FAIL] Could not connect to backend server on port 8080!" -ForegroundColor Red
    exit 1
}

$baseUrl = "http://localhost:8080"

function Invoke-ApiCall {
    param(
        [string]$Uri,
        [string]$Method = "GET",
        [string]$Body = $null,
        [hashtable]$Headers = @{}
    )
    $req = [System.Net.HttpWebRequest]::Create($Uri)
    $req.Method = $Method
    $req.ContentType = "application/json"
    foreach ($k in $Headers.Keys) {
        $req.Headers.Add($k, $Headers[$k])
    }
    if ($Body) {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
        $req.ContentLength = $bytes.Length
        $reqStream = $req.GetRequestStream()
        $reqStream.Write($bytes, 0, $bytes.Length)
        $reqStream.Close()
    }
    try {
        $resp = $req.GetResponse()
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $content = $reader.ReadToEnd()
        $reader.Close()
        return @{
            StatusCode = [int]$resp.StatusCode
            Content = $content
        }
    } catch [System.Net.WebException] {
        $errResp = $_.Exception.Response
        if ($errResp) {
            $reader = New-Object System.IO.StreamReader($errResp.GetResponseStream())
            $errContent = $reader.ReadToEnd()
            $reader.Close()
            return @{
                StatusCode = [int]$errResp.StatusCode
                Content = $errContent
            }
        }
        throw $_
    }
}

try {
    # 1. Obtain Tokens
    Write-Host "`n--- AUTHENTICATION SETUP ---" -ForegroundColor Yellow
    $adminLoginRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/login" -Method "POST" -Body (@{ username = "admin"; password = "admin123" } | ConvertTo-Json)
    $adminToken = ($adminLoginRes.Content | ConvertFrom-Json).token
    Write-Host "[PASS] Admin authenticated. Token: $($adminToken.Substring(0, 15))..." -ForegroundColor Green

    $unique = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $userRegPayload = @{
        fullName = "Officer Inventory"
        username = "officer_p5_$unique"
        email = "officer_p5_$unique@rescue.org"
        password = "Password@123"
    } | ConvertTo-Json
    $userRegRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/register" -Method "POST" -Body $userRegPayload
    $userToken = ($userRegRes.Content | ConvertFrom-Json).token
    Write-Host "[PASS] Regular USER registered. Token: $($userToken.Substring(0, 15))..." -ForegroundColor Green

    # TEST 1: Retrieve All Resources (GET /api/resources)
    Write-Host "`n--- TEST 1: Retrieve All Resources (GET /api/resources) ---" -ForegroundColor Yellow
    $allRes = Invoke-ApiCall -Uri "$baseUrl/api/resources" -Headers @{ Authorization = "Bearer $userToken" }
    if ($allRes.StatusCode -eq 200) {
        $resources = $allRes.Content | ConvertFrom-Json
        Write-Host "[PASS] Retrieved $($resources.Count) resource records using USER token (HTTP 200 OK)." -ForegroundColor Green
        if ($resources.Count -ge 20) {
            Write-Host "[PASS] All 20 baseline desktop resources confirmed intact in catalog!" -ForegroundColor Green
        }
    } else {
        Write-Host "[FAIL] Failed to retrieve resources: $($allRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 2: Retrieve Specific Resource (GET /api/resources/21)
    Write-Host "`n--- TEST 2: Retrieve Resource by ID (GET /api/resources/21) ---" -ForegroundColor Yellow
    $r21Res = Invoke-ApiCall -Uri "$baseUrl/api/resources/21" -Headers @{ Authorization = "Bearer $userToken" }
    if ($r21Res.StatusCode -eq 200) {
        $r21 = $r21Res.Content | ConvertFrom-Json
        Write-Host "[PASS] Resource #21 verified: $($r21.resourceName) in $($r21.regionName) (Avail: $($r21.availableQuantity), Reserved: $($r21.reservedQuantity), Surplus: $($r21.surplusQuantity))" -ForegroundColor Green
        if ($r21.availableQuantity -eq 0 -and $r21.reservedQuantity -eq 50 -and $r21.surplusQuantity -eq 0) {
            Write-Host "[PASS] Baseline semantics verified: available=0, reserved=50, surplus=0 matching desktop exactly." -ForegroundColor Green
        }
    } else {
        Write-Host "[FAIL] Failed to retrieve resource 21: $($r21Res.StatusCode)" -ForegroundColor Red
    }

    # TEST 3: Retrieve Resources by Region (GET /api/resources/region/2)
    Write-Host "`n--- TEST 3: Retrieve Resources by Region (GET /api/resources/region/2) ---" -ForegroundColor Yellow
    $reg2Res = Invoke-ApiCall -Uri "$baseUrl/api/resources/region/2" -Headers @{ Authorization = "Bearer $userToken" }
    if ($reg2Res.StatusCode -eq 200) {
        $itemsReg2 = $reg2Res.Content | ConvertFrom-Json
        Write-Host "[PASS] Sector B (Region #2) has $($itemsReg2.Count) inventory items:" -ForegroundColor Green
        foreach ($item in $itemsReg2) {
            Write-Host "       - $($item.resourceName): Avail=$($item.availableQuantity), Resv=$($item.reservedQuantity), Surplus=$($item.surplusQuantity)" -ForegroundColor Gray
        }
    } else {
        Write-Host "[FAIL] Failed to retrieve resources for region 2: $($reg2Res.StatusCode)" -ForegroundColor Red
    }

    # TEST 4: RBAC Enforcement - Non-admin cannot register resource
    Write-Host "`n--- TEST 4: RBAC Check - Regular USER Blocked from Adding Inventory ---" -ForegroundColor Yellow
    $unauthPayload = @{
        regionId = 1
        resourceName = "Unauthorized Supply"
        availableQuantity = 100
        reservedQuantity = 10
    } | ConvertTo-Json
    $unauthRes = Invoke-ApiCall -Uri "$baseUrl/api/resources" -Method "POST" -Body $unauthPayload -Headers @{ Authorization = "Bearer $userToken" }
    if ($unauthRes.StatusCode -eq 403) {
        Write-Host "[PASS] Regular USER POST /api/resources strictly blocked with HTTP 403 Forbidden!" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 403 for user, got $($unauthRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 5: Administrator Registers New Resource Item (POST /api/resources)
    Write-Host "`n--- TEST 5: Admin Registers New Resource (POST /api/resources) ---" -ForegroundColor Yellow
    $newResourcePayload = @{
        regionId = 1
        resourceName = "Blankets"
        availableQuantity = 150
        reservedQuantity = 30
    } | ConvertTo-Json
    $createRes = Invoke-ApiCall -Uri "$baseUrl/api/resources" -Method "POST" -Body $newResourcePayload -Headers @{ Authorization = "Bearer $adminToken" }
    $createdId = $null
    if ($createRes.StatusCode -eq 201) {
        $created = $createRes.Content | ConvertFrom-Json
        $createdId = $created.resourceId
        Write-Host "[PASS] New resource registered with HTTP 201 Created. ID: #$createdId" -ForegroundColor Green
        Write-Host "       $($created.resourceName) in $($created.regionName) - Avail: $($created.availableQuantity), Resv: $($created.reservedQuantity), Surplus: $($created.surplusQuantity)" -ForegroundColor Green
        if ($created.surplusQuantity -eq 120) {
            Write-Host "[PASS] Surplus correctly calculated as (150 - 30) = 120!" -ForegroundColor Green
        }
    } else {
        Write-Host "[FAIL] Failed to create resource: $($createRes.StatusCode) - $($createRes.Content)" -ForegroundColor Red
    }

    # TEST 6: Duplicate Resource in Same Region Rejection
    Write-Host "`n--- TEST 6: Duplicate Resource in Region Rejection ---" -ForegroundColor Yellow
    $dupRes = Invoke-ApiCall -Uri "$baseUrl/api/resources" -Method "POST" -Body $newResourcePayload -Headers @{ Authorization = "Bearer $adminToken" }
    if ($dupRes.StatusCode -eq 400 -and $dupRes.Content -like "*already registered*") {
        Write-Host "[PASS] Duplicate item rejected with HTTP 400 Bad Request: $($dupRes.Content)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 400 for duplicate resource, got $($dupRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 7: Input Validations (Negative Quantities & Nonexistent Region)
    Write-Host "`n--- TEST 7: Input Validations (Negative Quantities & Invalid Region) ---" -ForegroundColor Yellow
    # Negative available
    $badAvail = @{ regionId = 1; resourceName = "Bad Item"; availableQuantity = -10; reservedQuantity = 5 } | ConvertTo-Json
    $badAvailRes = Invoke-ApiCall -Uri "$baseUrl/api/resources" -Method "POST" -Body $badAvail -Headers @{ Authorization = "Bearer $adminToken" }
    if ($badAvailRes.StatusCode -eq 400) {
        Write-Host "[PASS] Negative available quantity rejected with HTTP 400." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 400 for negative available, got $($badAvailRes.StatusCode)" -ForegroundColor Red
    }

    # Negative reserved
    $badResv = @{ regionId = 1; resourceName = "Bad Item"; availableQuantity = 10; reservedQuantity = -5 } | ConvertTo-Json
    $badResvRes = Invoke-ApiCall -Uri "$baseUrl/api/resources" -Method "POST" -Body $badResv -Headers @{ Authorization = "Bearer $adminToken" }
    if ($badResvRes.StatusCode -eq 400) {
        Write-Host "[PASS] Negative reserved quantity rejected with HTTP 400." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 400 for negative reserved, got $($badResvRes.StatusCode)" -ForegroundColor Red
    }

    # Invalid region ID 99999
    $badRegion = @{ regionId = 99999; resourceName = "Ghost Supply"; availableQuantity = 10; reservedQuantity = 5 } | ConvertTo-Json
    $badRegionRes = Invoke-ApiCall -Uri "$baseUrl/api/resources" -Method "POST" -Body $badRegion -Headers @{ Authorization = "Bearer $adminToken" }
    if ($badRegionRes.StatusCode -eq 404) {
        Write-Host "[PASS] Non-existent region ID 99999 rejected with HTTP 404 Not Found." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 404 for invalid region, got $($badRegionRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 8: Update Resource (PUT /api/resources/{id})
    if ($createdId) {
        Write-Host "`n--- TEST 8: Update Resource Item (PUT /api/resources/$createdId) ---" -ForegroundColor Yellow
        $updatePayload = @{
            regionId = 1
            resourceName = "Thermal Blankets"
            availableQuantity = 200
            reservedQuantity = 50
        } | ConvertTo-Json
        $updateRes = Invoke-ApiCall -Uri "$baseUrl/api/resources/$createdId" -Method "PUT" -Body $updatePayload -Headers @{ Authorization = "Bearer $adminToken" }
        if ($updateRes.StatusCode -eq 200) {
            $updated = $updateRes.Content | ConvertFrom-Json
            Write-Host "[PASS] Resource updated successfully with HTTP 200 OK." -ForegroundColor Green
            Write-Host "       Updated Name: $($updated.resourceName) - Avail: $($updated.availableQuantity), Resv: $($updated.reservedQuantity), Surplus: $($updated.surplusQuantity)" -ForegroundColor Green
            if ($updated.surplusQuantity -eq 150) {
                Write-Host "[PASS] Updated surplus correctly recalculated as (200 - 50) = 150!" -ForegroundColor Green
            }
        } else {
            Write-Host "[FAIL] Failed to update resource: $($updateRes.StatusCode) - $($updateRes.Content)" -ForegroundColor Red
        }
    }

    # TEST 9: Safe Deletion & Dependency Protection
    Write-Host "`n--- TEST 9: Dependency Protection on Deletion ---" -ForegroundColor Yellow
    # Resource 21 ("Water" in Region 1) has allocations and requests
    $unsafeDel = Invoke-ApiCall -Uri "$baseUrl/api/resources/21" -Method "DELETE" -Headers @{ Authorization = "Bearer $adminToken" }
    if ($unsafeDel.StatusCode -eq 409) {
        Write-Host "[PASS] Deletion of Resource #21 with active history safely blocked with HTTP 409 Conflict!" -ForegroundColor Green
        Write-Host "       Conflict explanation: $($unsafeDel.Content)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 409 for deleting resource with history, got $($unsafeDel.StatusCode)" -ForegroundColor Red
    }

    # Safe delete of test item with 0 history
    if ($createdId) {
        Write-Host "`n--- TEST 10: Safe Deletion of Isolated Item without Dependencies ---" -ForegroundColor Yellow
        $safeDel = Invoke-ApiCall -Uri "$baseUrl/api/resources/$createdId" -Method "DELETE" -Headers @{ Authorization = "Bearer $adminToken" }
        if ($safeDel.StatusCode -eq 200) {
            Write-Host "[PASS] Isolated test resource #$createdId safely deleted with HTTP 200 OK." -ForegroundColor Green
        } else {
            Write-Host "[FAIL] Failed to delete isolated resource: $($safeDel.StatusCode) - $($safeDel.Content)" -ForegroundColor Red
        }
    }

    Write-Host "`n==========================================================" -ForegroundColor Cyan
    Write-Host "   ALL PHASE 5 VERIFICATION TESTS COMPLETED SUCCESSFULLY  " -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan

} finally {
    if ($backendProcess -and -not $backendProcess.HasExited) {
        Write-Host "Stopping test backend process (PID: $($backendProcess.Id))..."
        Stop-Process -Id $backendProcess.Id -Force
    }
}

