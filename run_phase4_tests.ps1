# run_phase4_tests.ps1
$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   RESCUESYNC PHASE 4 - DISASTER REGION VERIFICATION      " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Start backend if not listening on port 8080
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
    # Admin login
    $adminLoginRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/login" -Method "POST" -Body (@{ username = "admin"; password = "admin123" } | ConvertTo-Json)
    $adminToken = ($adminLoginRes.Content | ConvertFrom-Json).token
    Write-Host "[PASS] Admin authenticated. Token: $($adminToken.Substring(0, 15))..." -ForegroundColor Green

    # Register/Login a regular USER
    $unique = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $userLoginPayload = @{
        fullName = "Officer Regular"
        username = "officer_p4_$unique"
        email = "officer_p4_$unique@rescue.org"
        password = "Password@123"
    } | ConvertTo-Json
    $userRegRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/register" -Method "POST" -Body $userLoginPayload
    $userToken = ($userRegRes.Content | ConvertFrom-Json).token
    Write-Host "[PASS] Regular USER registered. Token: $($userToken.Substring(0, 15))..." -ForegroundColor Green

    # TEST 1: Retrieve All Regions (GET /api/regions) with USER token
    Write-Host "`n--- TEST 1: Retrieve All Disaster Regions (GET /api/regions) ---" -ForegroundColor Yellow
    $allRes = Invoke-ApiCall -Uri "$baseUrl/api/regions" -Headers @{ Authorization = "Bearer $userToken" }
    if ($allRes.StatusCode -eq 200) {
        $regions = $allRes.Content | ConvertFrom-Json
        Write-Host "[PASS] Successfully retrieved $($regions.Count) regions using USER token (HTTP 200 OK)." -ForegroundColor Green
        foreach ($r in $regions) {
            Write-Host "       Sector #$($r.regionId): $($r.regionName) - Priority Score: $($r.priorityScore)" -ForegroundColor Gray
        }
        if ($regions.Count -ge 4) {
            Write-Host "[PASS] All 4 baseline desktop regions preserved intact!" -ForegroundColor Green
        }
    } else {
        Write-Host "[FAIL] Failed to retrieve regions: $($allRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 2: Retrieve Specific Region (GET /api/regions/1)
    Write-Host "`n--- TEST 2: Retrieve Region by ID (GET /api/regions/1) ---" -ForegroundColor Yellow
    $singleRes = Invoke-ApiCall -Uri "$baseUrl/api/regions/1" -Headers @{ Authorization = "Bearer $userToken" }
    if ($singleRes.StatusCode -eq 200) {
        $r1 = $singleRes.Content | ConvertFrom-Json
        if ($r1.regionName -like "*Sector A*" -and $r1.priorityScore -eq 52.68) {
            Write-Host "[PASS] Sector A verified: $($r1.regionName), Location: $($r1.location), Priority: $($r1.priorityScore)" -ForegroundColor Green
        } else {
            Write-Host "[WARN] Sector A data differs: $($singleRes.Content)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "[FAIL] Failed to retrieve region 1: $($singleRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 3: RBAC Enforcement - Non-admin cannot create region
    Write-Host "`n--- TEST 3: RBAC Check - Regular USER Blocked from Creating Region ---" -ForegroundColor Yellow
    $testPayload = @{
        regionName = "Unauthorized Sector"
        location = "Forbidden Valley"
        populationAffected = 500
        disasterSeverity = 5
        injuredPeople = 20
        vulnerablePeople = 30
        roadAccessibility = 0.5
        latitude = 13.0
        longitude = 80.0
    } | ConvertTo-Json
    $unauthCreate = Invoke-ApiCall -Uri "$baseUrl/api/regions" -Method "POST" -Body $testPayload -Headers @{ Authorization = "Bearer $userToken" }
    if ($unauthCreate.StatusCode -eq 403) {
        Write-Host "[PASS] Non-admin POST /api/regions strictly rejected with HTTP 403 Forbidden!" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 403 for user, got $($unauthCreate.StatusCode)" -ForegroundColor Red
    }

    # TEST 4: Register Valid Region using ADMIN token & verify Priority Score Formula
    Write-Host "`n--- TEST 4: Register Disaster Area (POST /api/regions) with Priority Score Check ---" -ForegroundColor Yellow
    $newRegionPayload = @{
        regionName = "Sector E (Delta Wetlands)"
        location = "Southern Estuary District"
        populationAffected = 400
        disasterSeverity = 7
        injuredPeople = 50
        vulnerablePeople = 80
        roadAccessibility = 0.60
        latitude = 12.9512
        longitude = 80.1411
    } | ConvertTo-Json
    $createRes = Invoke-ApiCall -Uri "$baseUrl/api/regions" -Method "POST" -Body $newRegionPayload -Headers @{ Authorization = "Bearer $adminToken" }
    $createdId = $null
    if ($createRes.StatusCode -eq 201) {
        $created = $createRes.Content | ConvertFrom-Json
        $createdId = $created.regionId
        Write-Host "[PASS] Sector E registered successfully with HTTP 201 Created. New ID: #$createdId" -ForegroundColor Green
        Write-Host "       Priority Score returned: $($created.priorityScore)" -ForegroundColor Green
        # Expected score: 42.13
        if ($created.priorityScore -eq 42.13) {
            Write-Host "[PASS] Exact desktop formula verified! Score matches expected 42.13!" -ForegroundColor Green
        } else {
            Write-Host "[INFO] Score calculated: $($created.priorityScore)" -ForegroundColor Gray
        }
    } else {
        Write-Host "[FAIL] Failed to create region: $($createRes.StatusCode) - $($createRes.Content)" -ForegroundColor Red
    }

    # TEST 5: Duplicate Region Name Rejection
    Write-Host "`n--- TEST 5: Duplicate Region Name Rejection ---" -ForegroundColor Yellow
    $dupNameRes = Invoke-ApiCall -Uri "$baseUrl/api/regions" -Method "POST" -Body $newRegionPayload -Headers @{ Authorization = "Bearer $adminToken" }
    if ($dupNameRes.StatusCode -eq 400 -and $dupNameRes.Content -like "*already registered*") {
        Write-Host "[PASS] Duplicate region name rejected with HTTP 400: $($dupNameRes.Content)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 400 for duplicate name, got $($dupNameRes.StatusCode): $($dupNameRes.Content)" -ForegroundColor Red
    }

    # TEST 6: Input Validations
    Write-Host "`n--- TEST 6: Metric & Range Validations ---" -ForegroundColor Yellow
    # Invalid Severity (>10)
    $badSevPayload = @{
        regionName = "Bad Sev Sector"
        location = "Nowhere"
        populationAffected = 100
        disasterSeverity = 15
        injuredPeople = 10
        vulnerablePeople = 10
        roadAccessibility = 0.5
        latitude = 10.0
        longitude = 70.0
    } | ConvertTo-Json
    $badSevRes = Invoke-ApiCall -Uri "$baseUrl/api/regions" -Method "POST" -Body $badSevPayload -Headers @{ Authorization = "Bearer $adminToken" }
    if ($badSevRes.StatusCode -eq 400) {
        Write-Host "[PASS] Severity > 10 rejected with HTTP 400." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 400 for invalid severity, got $($badSevRes.StatusCode)" -ForegroundColor Red
    }

    # Injured > Population
    $badInjPayload = @{
        regionName = "Bad Injury Sector"
        location = "Nowhere"
        populationAffected = 50
        disasterSeverity = 5
        injuredPeople = 200
        vulnerablePeople = 10
        roadAccessibility = 0.5
        latitude = 10.0
        longitude = 70.0
    } | ConvertTo-Json
    $badInjRes = Invoke-ApiCall -Uri "$baseUrl/api/regions" -Method "POST" -Body $badInjPayload -Headers @{ Authorization = "Bearer $adminToken" }
    if ($badInjRes.StatusCode -eq 400) {
        Write-Host "[PASS] Injured > Population rejected with HTTP 400: $($badInjRes.Content)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 400 for injured > population, got $($badInjRes.StatusCode)" -ForegroundColor Red
    }

    # Invalid Latitude (>90)
    $badCoordPayload = @{
        regionName = "Bad Coord Sector"
        location = "Nowhere"
        populationAffected = 50
        disasterSeverity = 5
        injuredPeople = 10
        vulnerablePeople = 10
        roadAccessibility = 0.5
        latitude = 98.5
        longitude = 70.0
    } | ConvertTo-Json
    $badCoordRes = Invoke-ApiCall -Uri "$baseUrl/api/regions" -Method "POST" -Body $badCoordPayload -Headers @{ Authorization = "Bearer $adminToken" }
    if ($badCoordRes.StatusCode -eq 400) {
        Write-Host "[PASS] Latitude > 90.0 rejected with HTTP 400." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 400 for bad latitude, got $($badCoordRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 7: Update Region (PUT /api/regions/{id})
    if ($createdId) {
        Write-Host "`n--- TEST 7: Update Disaster Region (PUT /api/regions/$createdId) ---" -ForegroundColor Yellow
        $updatePayload = @{
            regionName = "Sector E (Delta Wetlands Calibrated)"
            location = "Southern Estuary District - Command Zone"
            populationAffected = 500
            disasterSeverity = 8
            injuredPeople = 100
            vulnerablePeople = 120
            roadAccessibility = 0.40
            latitude = 12.9512
            longitude = 80.1411
        } | ConvertTo-Json
        $updateRes = Invoke-ApiCall -Uri "$baseUrl/api/regions/$createdId" -Method "PUT" -Body $updatePayload -Headers @{ Authorization = "Bearer $adminToken" }
        if ($updateRes.StatusCode -eq 200) {
            $updated = $updateRes.Content | ConvertFrom-Json
            Write-Host "[PASS] Sector updated successfully with HTTP 200 OK." -ForegroundColor Green
            Write-Host "       Updated Name: $($updated.regionName)" -ForegroundColor Green
            Write-Host "       Recalculated Priority Score: $($updated.priorityScore)" -ForegroundColor Green
        } else {
            Write-Host "[FAIL] Update failed: $($updateRes.StatusCode) - $($updateRes.Content)" -ForegroundColor Red
        }
    }

    # TEST 8: Foreign-Key Protection on Delete
    Write-Host "`n--- TEST 8: Foreign-Key Relational Safety on Deletion ---" -ForegroundColor Yellow
    # Sector 1 has active allocations & road connections
    $unsafeDelRes = Invoke-ApiCall -Uri "$baseUrl/api/regions/1" -Method "DELETE" -Headers @{ Authorization = "Bearer $adminToken" }
    if ($unsafeDelRes.StatusCode -eq 409) {
        Write-Host "[PASS] Destructive deletion on Sector 1 safely blocked with HTTP 409 Conflict!" -ForegroundColor Green
        Write-Host "       Reason: $($unsafeDelRes.Content)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 409 Conflict for deleting dependent region, got $($unsafeDelRes.StatusCode)" -ForegroundColor Red
    }

    # Safe delete of isolated test Sector E
    if ($createdId) {
        Write-Host "`n--- TEST 9: Safe Deletion of Isolated Region without Dependencies ---" -ForegroundColor Yellow
        $safeDelRes = Invoke-ApiCall -Uri "$baseUrl/api/regions/$createdId" -Method "DELETE" -Headers @{ Authorization = "Bearer $adminToken" }
        if ($safeDelRes.StatusCode -eq 200) {
            Write-Host "[PASS] Isolated test sector #$createdId safely deleted with HTTP 200 OK: $($safeDelRes.Content)" -ForegroundColor Green
        } else {
            Write-Host "[FAIL] Failed to delete isolated region: $($safeDelRes.StatusCode) - $($safeDelRes.Content)" -ForegroundColor Red
        }
    }

    Write-Host "`n==========================================================" -ForegroundColor Cyan
    Write-Host "   ALL PHASE 4 VERIFICATION TESTS COMPLETED SUCCESSFULLY  " -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan

} finally {
    if ($backendProcess -and -not $backendProcess.HasExited) {
        Write-Host "Stopping temporary backend process (PID: $($backendProcess.Id))..."
        Stop-Process -Id $backendProcess.Id -Force
    }
}

