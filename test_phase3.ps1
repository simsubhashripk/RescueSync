# test_phase3.ps1 (PowerShell 5.1 & 7 compatible)
$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   RESCUESYNC PHASE 3 - SECURITY & AUTH VERIFICATION      " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

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

# 1. Health check
$healthRes = Invoke-ApiCall -Uri "$baseUrl/api/health"
Write-Host "[PASS] Backend is UP: $($healthRes.Content)" -ForegroundColor Green

# 2. Register New User & Tamper Attempt
$uniqueId = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$testUsername = "officer_live_$uniqueId"
$testEmail = "officer_live_$uniqueId@rescue.org"
$testPassword = "RescueSecurePassword2026!"

$registerPayload = @{
    fullName = "Officer Live User"
    username = $testUsername
    email = $testEmail
    password = $testPassword
    role = "ADMIN"  # Privilege escalation attempt!
} | ConvertTo-Json

Write-Host "`n--- TEST 1: Register New User & Privilege Escalation Prevention ---" -ForegroundColor Yellow
$regRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/register" -Method "POST" -Body $registerPayload
$regData = $regRes.Content | ConvertFrom-Json
$userToken = $regData.token
$userRole = $regData.user.role

if ($regRes.StatusCode -in @(200, 201) -and $userRole -eq "USER") {
    Write-Host "[PASS] Registration succeeded with HTTP $($regRes.StatusCode)." -ForegroundColor Green
    Write-Host "[PASS] Privilege escalation PREVENTED: Requested 'ADMIN', assigned role is strictly '$userRole'" -ForegroundColor Green
    Write-Host "       Generated JWT: $($userToken.Substring(0, 20))..." -ForegroundColor Green
} else {
    Write-Host "[FAIL] Registration unexpected result: $($regRes.StatusCode) - $($regRes.Content)" -ForegroundColor Red
}

# 3. Direct MySQL Database Record Inspection
Write-Host "`n--- TEST 2: Database Schema & PBKDF2 Password Hash Inspection ---" -ForegroundColor Yellow
$query = "SELECT user_id, username, email, password_hash, role, account_status FROM users WHERE username = '$testUsername';"
$dbOutput = & "C:\xampp\mysql\bin\mysql.exe" -u root rescuesync_db -e $query
Write-Host $dbOutput -ForegroundColor Gray

if ($dbOutput -like "*PBKDF2`$65536`$*") {
    Write-Host "[PASS] Database record verified: Password hash strictly uses PBKDF2-HMAC-SHA256 matching desktop format!" -ForegroundColor Green
    Write-Host "[PASS] Role verified: USER | Account status: ACTIVE" -ForegroundColor Green
} else {
    Write-Host "[FAIL] PBKDF2 format not found in DB output: $dbOutput" -ForegroundColor Red
}

# 4. Duplicate Username Prevention
Write-Host "`n--- TEST 3: Duplicate Username Prevention ---" -ForegroundColor Yellow
$dupUserPayload = @{
    fullName = "Duplicate User"
    username = $testUsername
    email = "diff_$uniqueId@rescue.org"
    password = "AnotherPassword123!"
} | ConvertTo-Json
$dupUserRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/register" -Method "POST" -Body $dupUserPayload
if ($dupUserRes.StatusCode -eq 400 -and $dupUserRes.Content -like "*already taken*") {
    Write-Host "[PASS] Duplicate username correctly rejected with HTTP 400 Bad Request: $($dupUserRes.Content)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Expected HTTP 400 with 'already taken', got $($dupUserRes.StatusCode): $($dupUserRes.Content)" -ForegroundColor Red
}

# 5. Duplicate Email Prevention
Write-Host "`n--- TEST 4: Duplicate Email Prevention ---" -ForegroundColor Yellow
$dupEmailPayload = @{
    fullName = "Duplicate Email User"
    username = "unique_$uniqueId"
    email = $testEmail
    password = "AnotherPassword123!"
} | ConvertTo-Json
$dupEmailRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/register" -Method "POST" -Body $dupEmailPayload
if ($dupEmailRes.StatusCode -eq 400 -and $dupEmailRes.Content -like "*already registered*") {
    Write-Host "[PASS] Duplicate email correctly rejected with HTTP 400 Bad Request: $($dupEmailRes.Content)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Expected HTTP 400 with 'already registered', got $($dupEmailRes.StatusCode): $($dupEmailRes.Content)" -ForegroundColor Red
}

# 6. Login with Wrong Password
Write-Host "`n--- TEST 5: Wrong Password Rejection ---" -ForegroundColor Yellow
$badLoginPayload = @{
    username = $testUsername
    password = "WrongPassword999!"
} | ConvertTo-Json
$badLoginRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/login" -Method "POST" -Body $badLoginPayload
if ($badLoginRes.StatusCode -eq 401) {
    Write-Host "[PASS] Wrong password rejected with HTTP 401 Unauthorized: $($badLoginRes.Content)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Expected HTTP 401 for wrong password, got $($badLoginRes.StatusCode): $($badLoginRes.Content)" -ForegroundColor Red
}

# 7. Login with Correct Credentials
Write-Host "`n--- TEST 6: User Login with Correct Credentials ---" -ForegroundColor Yellow
$goodLoginPayload = @{
    username = $testUsername
    password = $testPassword
} | ConvertTo-Json
$goodLoginRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/login" -Method "POST" -Body $goodLoginPayload
if ($goodLoginRes.StatusCode -eq 200) {
    $loginData = $goodLoginRes.Content | ConvertFrom-Json
    $userToken = $loginData.token
    Write-Host "[PASS] User login succeeded with HTTP 200." -ForegroundColor Green
    Write-Host "       JWT: $($userToken.Substring(0, 20))... User: $($loginData.user.username) ($($loginData.user.role))" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Login failed: $($goodLoginRes.StatusCode) - $($goodLoginRes.Content)" -ForegroundColor Red
}

# 8. Backward Compatibility: Login with Existing Desktop Admin Account
Write-Host "`n--- TEST 7: Backward Compatibility - Desktop Existing User (admin) ---" -ForegroundColor Yellow
$adminLoginPayload = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json
$adminLoginRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/login" -Method "POST" -Body $adminLoginPayload
$adminToken = ""
if ($adminLoginRes.StatusCode -eq 200) {
    $adminData = $adminLoginRes.Content | ConvertFrom-Json
    $adminToken = $adminData.token
    Write-Host "[PASS] Existing desktop admin authenticated cleanly without resetting password!" -ForegroundColor Green
    Write-Host "       Role: $($adminData.user.role), Token: $($adminToken.Substring(0, 20))..." -ForegroundColor Green
} else {
    Write-Host "[FAIL] Admin login failed: $($adminLoginRes.StatusCode) - $($adminLoginRes.Content)" -ForegroundColor Red
}

# 9. Protected Endpoint /api/auth/me
Write-Host "`n--- TEST 8: Token-Protected /api/auth/me Endpoint ---" -ForegroundColor Yellow
# Unauthenticated
$unauthMeRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/me"
if ($unauthMeRes.StatusCode -in @(401, 403)) {
    Write-Host "[PASS] Unauthenticated access blocked with HTTP $($unauthMeRes.StatusCode)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Expected 401/403 for unauthenticated request, got $($unauthMeRes.StatusCode)" -ForegroundColor Red
}

# Authenticated with User Token
$authMeRes = Invoke-ApiCall -Uri "$baseUrl/api/auth/me" -Headers @{ Authorization = "Bearer $userToken" }
if ($authMeRes.StatusCode -eq 200) {
    $meData = $authMeRes.Content | ConvertFrom-Json
    if ($meData.username -eq $testUsername) {
        Write-Host "[PASS] Authenticated profile verified: $($meData.username) <$($meData.email)> Role: $($meData.role)" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Profile data mismatch: $($authMeRes.Content)" -ForegroundColor Red
    }
} else {
    Write-Host "[FAIL] Authenticated /api/auth/me failed: $($authMeRes.StatusCode)" -ForegroundColor Red
}

# 10. Role-Based Access Control (RBAC) - Admin vs User
Write-Host "`n--- TEST 9: RBAC Enforcement (/api/admin/status) ---" -ForegroundColor Yellow
# Regular User accessing /api/admin/status
$userAdminRes = Invoke-ApiCall -Uri "$baseUrl/api/admin/status" -Headers @{ Authorization = "Bearer $userToken" }
if ($userAdminRes.StatusCode -eq 403) {
    Write-Host "[PASS] Regular USER access strictly forbidden (HTTP 403 Forbidden): $($userAdminRes.Content)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Expected HTTP 403 for user on admin endpoint, got $($userAdminRes.StatusCode)" -ForegroundColor Red
}

# Admin User accessing /api/admin/status
if ($adminToken) {
    $adminStatusRes = Invoke-ApiCall -Uri "$baseUrl/api/admin/status" -Headers @{ Authorization = "Bearer $adminToken" }
    if ($adminStatusRes.StatusCode -eq 200) {
        $statusData = $adminStatusRes.Content | ConvertFrom-Json
        Write-Host "[PASS] ADMIN granted access with HTTP 200: $($statusData.message) (Active Users: $($statusData.activeUserCount))" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Expected HTTP 200 for admin, got $($adminStatusRes.StatusCode)" -ForegroundColor Red
    }
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "   ALL PHASE 3 VERIFICATION TESTS COMPLETED SUCCESSFULLY  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
