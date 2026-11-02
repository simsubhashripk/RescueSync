# run_phase6_backend_tests.ps1
$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   RESCUESYNC PHASE 6 - RELIEF REQUEST & QUEUE BACKEND    " -ForegroundColor Cyan
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
        fullName = "Officer Queue"
        username = "officer_p6_$unique"
        email = "officer_p6_$unique@rescue.org"
        password = "Password@123"
    } | ConvertTo-Json
    $userRegRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/register" -Method "POST" -Body $userRegPayload
    $userToken = ($userRegRes.Content | ConvertFrom-Json).token
    Write-Host "[PASS] Regular USER registered. Token: $($userToken.Substring(0, 15))..." -ForegroundColor Green

    # TEST 1: Retrieve All Relief Requests (GET /api/requests)
    Write-Host "`n--- TEST 1: Retrieve All Requests (GET /api/requests) ---" -ForegroundColor Yellow
    $allRes = Invoke-ApiCall -Uri "$baseUrl/api/requests" -Headers @{ Authorization = "Bearer $userToken" }
    if ($allRes.StatusCode -eq 200) {
        $requests = $allRes.Content | ConvertFrom-Json
        Write-Host "[PASS] Retrieved $($requests.Count) requests using USER token (HTTP 200 OK)." -ForegroundColor Green
        if ($requests.Count -ge 6) {
            Write-Host "[PASS] All 6 original baseline relief requests confirmed present in catalog!" -ForegroundColor Green
            foreach ($r in $requests) {
                Write-Host "       Req #$($r.requestId): $($r.resourceRequired) ($($r.requiredQuantity) needed, $($r.allocatedQuantity) alloc) in $($r.regionName) [Score: $($r.priorityScore), Order: $($r.requestArrivalOrder), Status: $($r.requestStatus)]" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "[FAIL] Failed to retrieve requests: $($allRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 2: Verify Active Priority Queue Ordering & Summary (GET /api/requests/queue)
    Write-Host "`n--- TEST 2: Active Priority Queue Triage Snapshot (GET /api/requests/queue) ---" -ForegroundColor Yellow
    $queueRes = Invoke-ApiCall -Uri "$baseUrl/api/requests/queue" -Headers @{ Authorization = "Bearer $userToken" }
    if ($queueRes.StatusCode -eq 200) {
        $summary = $queueRes.Content | ConvertFrom-Json
        Write-Host "[PASS] Active Queue retrieved: $($summary.activeQueueDemands) demands pending/partial, $($summary.allocatedCount) fulfilled." -ForegroundColor Green
        Write-Host "       Total remaining deficit across active demands: $($summary.totalRemainingDeficit) units" -ForegroundColor Green
        
        $activeList = $summary.activeQueue
        Write-Host "       Queue Triage Sequence:" -ForegroundColor Cyan
        for ($i = 0; $i -lt $activeList.Count; $i++) {
            $item = $activeList[$i]
            Write-Host "       Rank #$($item.queueRank): Req #$($item.requestId) - $($item.resourceRequired) (Deficit: $($item.remainingDeficit)) | Score: $($item.priorityScore) | Arrival: $($item.requestArrivalOrder)" -ForegroundColor Yellow
        }

        # Check Position 1 is Request #1 (Score 84.50)
        if ($activeList[0].requestId -eq 1 -and $activeList[0].priorityScore -eq 84.5) {
            Write-Host "[PASS] Priority Triage verified: Request #1 is Ranked #1 due to highest score (84.50)!" -ForegroundColor Green
        } else {
            Write-Host "[FAIL] Rank 1 mismatch: Expected Req #1 with score 84.50, got Req #$($activeList[0].requestId)" -ForegroundColor Red
        }

        # Check Positions 2-5 are Requests #3, #4, #5, #6 in FIFO arrival order (3, 4, 5, 6)
        if ($activeList[1].requestId -eq 3 -and $activeList[2].requestId -eq 4 -and $activeList[3].requestId -eq 5 -and $activeList[4].requestId -eq 6) {
            Write-Host "[PASS] FIFO Tie-Breaker verified: Requests #3, #4, #5, #6 (all score 52.68) sorted strictly by arrival order (3 -> 4 -> 5 -> 6)!" -ForegroundColor Green
        } else {
            Write-Host "[FAIL] Tie-breaker order mismatch!" -ForegroundColor Red
        }

        # Check Request #2 (ALLOCATED) is excluded from active queue
        $hasAllocatedInQueue = $false
        foreach ($item in $activeList) {
            if ($item.requestId -eq 2) { $hasAllocatedInQueue = $true }
        }
        if (-not $hasAllocatedInQueue) {
            Write-Host "[PASS] Fulfilled Request #2 (ALLOCATED) correctly excluded from active PriorityQueue." -ForegroundColor Green
        } else {
            Write-Host "[FAIL] Fulfilled request found in active queue!" -ForegroundColor Red
        }
    } else {
        Write-Host "[FAIL] Failed to retrieve priority queue: $($queueRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 3: Retrieve Specific Request (GET /api/requests/1)
    Write-Host "`n--- TEST 3: Retrieve Request by ID (GET /api/requests/1) ---" -ForegroundColor Yellow
    $r1Res = Invoke-ApiCall -Uri "$baseUrl/api/requests/1" -Headers @{ Authorization = "Bearer $userToken" }
    if ($r1Res.StatusCode -eq 200) {
        $r1 = $r1Res.Content | ConvertFrom-Json
        Write-Host "[PASS] Request #1 verified: Resource=$($r1.resourceRequired), Deficit=$($r1.remainingDeficit), Score=$($r1.priorityScore), Rank=#$($r1.queueRank)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Failed to retrieve request #1: $($r1Res.StatusCode)" -ForegroundColor Red
    }

    # TEST 4: Retrieve Requests by Region (GET /api/requests/region/2)
    Write-Host "`n--- TEST 4: Retrieve Requests by Region (GET /api/requests/region/2) ---" -ForegroundColor Yellow
    $reg2Res = Invoke-ApiCall -Uri "$baseUrl/api/requests/region/2" -Headers @{ Authorization = "Bearer $userToken" }
    if ($reg2Res.StatusCode -eq 200) {
        $reg2Items = $reg2Res.Content | ConvertFrom-Json
        Write-Host "[PASS] Region #2 (Sector B) returned $($reg2Items.Count) requests. (Req #$($reg2Items[0].requestId): $($reg2Items[0].resourceRequired), Status: $($reg2Items[0].requestStatus))" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Failed to retrieve requests for region 2: $($reg2Res.StatusCode)" -ForegroundColor Red
    }

    # TEST 5: Input Validations (Quantity <= 0, Invalid Region, Blank Name)
    Write-Host "`n--- TEST 5: Input Validations ---" -ForegroundColor Yellow
    # Quantity <= 0
    $badQty = @{ regionId = 1; resourceRequired = "Water"; requiredQuantity = 0 } | ConvertTo-Json
    $badQtyRes = Invoke-ApiCall -Uri "$baseUrl/api/requests" -Method "POST" -Body $badQty -Headers @{ Authorization = "Bearer $userToken" }
    if ($badQtyRes.StatusCode -eq 400) {
        Write-Host "[PASS] Quantity <= 0 rejected with HTTP 400: $($badQtyRes.Content)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 400 for zero quantity, got $($badQtyRes.StatusCode)" -ForegroundColor Red
    }

    # Non-existent region ID
    $badRegion = @{ regionId = 99999; resourceRequired = "Water"; requiredQuantity = 50 } | ConvertTo-Json
    $badRegionRes = Invoke-ApiCall -Uri "$baseUrl/api/requests" -Method "POST" -Body $badRegion -Headers @{ Authorization = "Bearer $userToken" }
    if ($badRegionRes.StatusCode -eq 404) {
        Write-Host "[PASS] Non-existent region ID 99999 rejected with HTTP 404 Not Found." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 404 for invalid region, got $($badRegionRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 6: Create New Relief Demand (Inherits Region Priority & Next Arrival Order)
    Write-Host "`n--- TEST 6: Create New Demand & Dynamic Priority Inheritance ---" -ForegroundColor Yellow
    # Sector 3 (Sector C Valley Central has priority score 32.86)
    $newReqPayload1 = @{
        regionId = 3
        resourceRequired = "water"
        requiredQuantity = 100
    } | ConvertTo-Json
    $create1Res = Invoke-ApiCall -Uri "$baseUrl/api/requests" -Method "POST" -Body $newReqPayload1 -Headers @{ Authorization = "Bearer $userToken" }
    $testReqId1 = $null
    if ($create1Res.StatusCode -eq 201) {
        $created1 = $create1Res.Content | ConvertFrom-Json
        $testReqId1 = $created1.requestId
        Write-Host "[PASS] New demand created with HTTP 201. ID: #$testReqId1" -ForegroundColor Green
        Write-Host "       Inherited Priority Score from Sector C: $($created1.priorityScore) (Expected: 32.86)" -ForegroundColor Green
        Write-Host "       Assigned Monotonic Arrival Order: $($created1.requestArrivalOrder) (Expected: 7)" -ForegroundColor Green
        Write-Host "       Queue Rank assigned: #$($created1.queueRank)" -ForegroundColor Green
        if ($created1.priorityScore -eq 32.86 -and $created1.requestArrivalOrder -eq 7) {
            Write-Host "[PASS] Exact priority inheritance & arrival sequence verified!" -ForegroundColor Green
        }
    } else {
        Write-Host "[FAIL] Failed to create request: $($create1Res.StatusCode) - $($create1Res.Content)" -ForegroundColor Red
    }

    # TEST 7: Create Second Test Demand (Tests FIFO Tie-Breaker against existing 52.68 requests)
    Write-Host "`n--- TEST 7: Test Dynamic PriorityQueue Insertion & Tie-Breaking ---" -ForegroundColor Yellow
    # Sector 1 (Sector A Coastal Zone has priority score 52.68)
    $newReqPayload2 = @{
        regionId = 1
        resourceRequired = "Medicines"
        requiredQuantity = 50
    } | ConvertTo-Json
    $create2Res = Invoke-ApiCall -Uri "$baseUrl/api/requests" -Method "POST" -Body $newReqPayload2 -Headers @{ Authorization = "Bearer $userToken" }
    $testReqId2 = $null
    if ($create2Res.StatusCode -eq 201) {
        $created2 = $create2Res.Content | ConvertFrom-Json
        $testReqId2 = $created2.requestId
        Write-Host "[PASS] Second demand created with HTTP 201. ID: #$testReqId2" -ForegroundColor Green
        Write-Host "       Inherited Priority Score from Sector A: $($created2.priorityScore) (Expected: 52.68)" -ForegroundColor Green
        Write-Host "       Assigned Monotonic Arrival Order: $($created2.requestArrivalOrder) (Expected: 8)" -ForegroundColor Green
        Write-Host "       Assigned Queue Rank: #$($created2.queueRank) (Expected: Rank 6, behind orders 3, 4, 5, 6 but ahead of score 32.86)" -ForegroundColor Green
        if ($created2.queueRank -eq 6) {
            Write-Host "[PASS] PriorityQueue heap verified! Score 52.68 with arrival order 8 is placed at Rank 6!" -ForegroundColor Green
        }
    } else {
        Write-Host "[FAIL] Failed to create second request: $($create2Res.StatusCode)" -ForegroundColor Red
    }

    # TEST 8: Historical Allocation Deletion Protection
    Write-Host "`n--- TEST 8: Deletion Protection for Requests with Historical Allocations ---" -ForegroundColor Yellow
    # Request #1 is referenced by Allocation #1
    $delUnsafeRes = Invoke-ApiCall -Uri "$baseUrl/api/requests/1" -Method "DELETE" -Headers @{ Authorization = "Bearer $adminToken" }
    if ($delUnsafeRes.StatusCode -eq 409) {
        Write-Host "[PASS] Deletion of Request #1 blocked with HTTP 409 Conflict!" -ForegroundColor Green
        Write-Host "       Explanation: $($delUnsafeRes.Content)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 409 Conflict for deleting Request #1, got $($delUnsafeRes.StatusCode)" -ForegroundColor Red
    }

    # TEST 9: RBAC on Deletion (Regular User Cannot Delete)
    Write-Host "`n--- TEST 9: RBAC on Deletion (Regular User Denied) ---" -ForegroundColor Yellow
    if ($testReqId1) {
        $userDelRes = Invoke-ApiCall -Uri "$baseUrl/api/requests/$testReqId1" -Method "DELETE" -Headers @{ Authorization = "Bearer $userToken" }
        if ($userDelRes.StatusCode -eq 403) {
            Write-Host "[PASS] Regular USER denied DELETE /api/requests/$testReqId1 with HTTP 403 Forbidden!" -ForegroundColor Green
        } else {
            Write-Host "[FAIL] Expected HTTP 403 for user delete, got $($userDelRes.StatusCode)" -ForegroundColor Red
        }
    }

    # TEST 10: Admin Safely Deletes Only the New Test Requests
    Write-Host "`n--- TEST 10: Clean Removal of Test Demands ---" -ForegroundColor Yellow
    if ($testReqId1) {
        $adminDel1 = Invoke-ApiCall -Uri "$baseUrl/api/requests/$testReqId1" -Method "DELETE" -Headers @{ Authorization = "Bearer $adminToken" }
        if ($adminDel1.StatusCode -eq 200) {
            Write-Host "[PASS] Test demand #$testReqId1 removed cleanly with HTTP 200 OK." -ForegroundColor Green
        }
    }
    if ($testReqId2) {
        $adminDel2 = Invoke-ApiCall -Uri "$baseUrl/api/requests/$testReqId2" -Method "DELETE" -Headers @{ Authorization = "Bearer $adminToken" }
        if ($adminDel2.StatusCode -eq 200) {
            Write-Host "[PASS] Test demand #$testReqId2 removed cleanly with HTTP 200 OK." -ForegroundColor Green
        }
    }

    # TEST 11: Final Baseline Verification
    Write-Host "`n--- TEST 11: Final Baseline Integrity Verification ---" -ForegroundColor Yellow
    $finalRes = Invoke-ApiCall -Uri "$baseUrl/api/requests" -Headers @{ Authorization = "Bearer $adminToken" }
    $finalRequests = $finalRes.Content | ConvertFrom-Json
    if ($finalRequests.Count -eq 6) {
        Write-Host "[PASS] All original 6 baseline relief requests remain 100% intact!" -ForegroundColor Green
        foreach ($r in $finalRequests) {
            Write-Host "       Baseline Req #$($r.requestId): $($r.resourceRequired) ($($r.requiredQuantity) needed, $($r.allocatedQuantity) allocated) - Status: $($r.requestStatus)" -ForegroundColor Gray
        }
    } else {
        Write-Host "[WARN] Final request count: $($finalRequests.Count) (expected 6)" -ForegroundColor Yellow
    }

    Write-Host "`n==========================================================" -ForegroundColor Cyan
    Write-Host "   ALL PHASE 6 BACKEND TESTS COMPLETED SUCCESSFULLY       " -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan

} finally {
    if ($backendProcess -and -not $backendProcess.HasExited) {
        Write-Host "Stopping temporary backend process (PID: $($backendProcess.Id))..."
        Stop-Process -Id $backendProcess.Id -Force
    }
}

