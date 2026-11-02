# run_tests_with_server.ps1
$proc = Start-Process -FilePath "java" -ArgumentList "-jar", "e:\RescueSync\RescueSync-Web\backend\target\rescuesync-backend-0.0.1-SNAPSHOT.jar" -PassThru -WindowStyle Hidden
Write-Host "Started backend process with PID: $($proc.Id)"

try {
    $ready = $false
    for ($i = 0; $i -lt 35; $i++) {
        Start-Sleep -Seconds 1
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient("localhost", 8080)
            if ($tcp.Connected) {
                $tcp.Close()
                $ready = $true
                break
            }
        } catch {
            # continue waiting
        }
    }

    if ($ready) {
        Write-Host "Backend is UP and responsive on port 8080! Waiting 2 seconds for warm-up..."
        Start-Sleep -Seconds 2
        & "e:\RescueSync\RescueSync-Web\test_phase3.ps1"
    } else {
        Write-Host "Backend failed to become responsive within 35 seconds." -ForegroundColor Red
    }
} finally {
    if ($proc -and -not $proc.HasExited) {
        Write-Host "Stopping test backend process (PID: $($proc.Id))..."
        Stop-Process -Id $proc.Id -Force
    }
}
