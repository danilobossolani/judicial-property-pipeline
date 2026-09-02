[CmdletBinding()]
param(
    [switch]$Silencioso,

    [ValidateRange(120, 1800)]
    [int]$MaxUpdateSeconds = 600
)

$ErrorActionPreference = "Stop"
$minimumWslVersion = [version]"2.1.5"
$script:wslUpdateTimedOut = $false
$logDirectory = if ($env:LOCALAPPDATA) {
    Join-Path $env:LOCALAPPDATA "JudicialPipeline"
} else {
    Join-Path $env:TEMP "JudicialPipeline"
}
$logPath = Join-Path $logDirectory "preparacao-windows.log"

function Write-Log {
    param([string]$Message)

    try {
        New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
        $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
        Add-Content -LiteralPath $logPath -Value $line -Encoding UTF8
    } catch {
        # A preparação não deve falhar apenas porque o log está indisponível.
    }
}

function Write-Info {
    param([string]$Message)

    Write-Log -Message $Message
    if (-not $Silencioso) {
        Write-Host $Message
    }
}

function Write-Status {
    param([string]$Message)

    Write-Log -Message $Message
    Write-Host $Message -ForegroundColor Cyan
}

function Test-Administrator {
    $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object System.Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([System.Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Invoke-NativeProcess {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [ValidateRange(5, 1800)]
        [int]$TimeoutSeconds = 60,
        [switch]$CaptureOutput
    )

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $FilePath
    $startInfo.Arguments = $Arguments -join " "
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $CaptureOutput
    $startInfo.RedirectStandardError = $CaptureOutput

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    $standardOutputTask = $null
    $standardErrorTask = $null
    $timedOut = $false
    try {
        Write-Log -Message ("Executando: {0} {1}" -f $FilePath, ($Arguments -join " "))
        if (-not $process.Start()) {
            throw "O componente do Windows não pôde ser iniciado."
        }

        if ($CaptureOutput) {
            $standardOutputTask = $process.StandardOutput.ReadToEndAsync()
            $standardErrorTask = $process.StandardError.ReadToEndAsync()
        }

        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            $timedOut = $true
            try {
                $process.Kill()
            } catch {
                # O processo pode ter terminado entre a espera e o encerramento.
            }
            $process.WaitForExit()
        }

        $standardOutput = ""
        $standardError = ""
        if ($CaptureOutput) {
            $standardOutput = $standardOutputTask.GetAwaiter().GetResult()
            $standardError = $standardErrorTask.GetAwaiter().GetResult()
        }

        $exitCode = if ($timedOut) { 124 } else { $process.ExitCode }
        $timeoutDescription = if ($timedOut) { " (tempo excedido)" } else { "" }
        Write-Log -Message ("Resultado: código {0}{1}" -f $exitCode, $timeoutDescription)
        return [pscustomobject]@{
            ExitCode = $exitCode
            TimedOut = $timedOut
            Output = (($standardOutput + "`n" + $standardError) -replace "`0", "")
        }
    } finally {
        $process.Dispose()
    }
}

function Get-WslVersion {
    $wsl = Get-Command wsl.exe -ErrorAction SilentlyContinue
    if (-not $wsl) {
        return $null
    }

    $result = Invoke-NativeProcess `
        -FilePath $wsl.Source `
        -Arguments @("--version") `
        -TimeoutSeconds 30 `
        -CaptureOutput
    if ($result.ExitCode -ne 0) {
        return $null
    }

    $match = [regex]::Match($result.Output, "\d+\.\d+\.\d+(?:\.\d+)?")
    if (-not $match.Success) {
        return $null
    }

    try {
        return [version]$match.Value
    } catch {
        return $null
    }
}

function Invoke-ElevatedSelf {
    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", ('"' + $PSCommandPath + '"'),
        "-Silencioso",
        "-MaxUpdateSeconds", $MaxUpdateSeconds
    )

    try {
        $process = Start-Process `
            -FilePath "powershell.exe" `
            -ArgumentList $arguments `
            -Verb RunAs `
            -Wait `
            -PassThru
        return $process.ExitCode
    } catch {
        throw "A atualização precisa da autorização de administrador do Windows."
    }
}

function Invoke-WslUpdate {
    $wsl = Get-Command wsl.exe -ErrorAction SilentlyContinue
    if (-not $wsl) {
        return $false
    }

    $primaryTimeout = [Math]::Max(60, [Math]::Floor($MaxUpdateSeconds * 0.7))
    $fallbackTimeout = [Math]::Max(60, $MaxUpdateSeconds - $primaryTimeout)

    Write-Status "Atualizando o componente do Windows. Aguarde; esta etapa possui limite automático."
    $result = Invoke-NativeProcess `
        -FilePath $wsl.Source `
        -Arguments @("--update", "--web-download") `
        -TimeoutSeconds $primaryTimeout
    if ($result.ExitCode -ne 0) {
        if ($result.TimedOut) {
            $script:wslUpdateTimedOut = $true
            Write-Status "A primeira tentativa demorou demais. Tentando a alternativa automática..."
        }
        $result = Invoke-NativeProcess `
            -FilePath $wsl.Source `
            -Arguments @("--update") `
            -TimeoutSeconds $fallbackTimeout
    }

    if ($result.ExitCode -ne 0) {
        if ($result.TimedOut) {
            $script:wslUpdateTimedOut = $true
        }
        return $false
    }

    Invoke-NativeProcess `
        -FilePath $wsl.Source `
        -Arguments @("--shutdown") `
        -TimeoutSeconds 60 | Out-Null
    Write-Status "Atualização do Windows concluída."
    return $true
}

function Enable-WslFeatures {
    Write-Status "Ativando os componentes necessários do Windows..."

    $wslFeature = Invoke-NativeProcess `
        -FilePath "dism.exe" `
        -Arguments @(
            "/online",
            "/enable-feature",
            "/featurename:Microsoft-Windows-Subsystem-Linux",
            "/all",
            "/norestart"
        ) `
        -TimeoutSeconds 300
    if ($wslFeature.ExitCode -ne 0) {
        return $false
    }

    $virtualMachineFeature = Invoke-NativeProcess `
        -FilePath "dism.exe" `
        -Arguments @(
            "/online",
            "/enable-feature",
            "/featurename:VirtualMachinePlatform",
            "/all",
            "/norestart"
        ) `
        -TimeoutSeconds 300
    return $virtualMachineFeature.ExitCode -eq 0
}

try {
    Write-Log -Message "Início da preparação do Windows."
    $installedVersion = Get-WslVersion
    if ($installedVersion -and $installedVersion -ge $minimumWslVersion) {
        Write-Log -Message "WSL já está atualizado: $installedVersion."
        exit 0
    }

    if (-not (Test-Administrator)) {
        exit (Invoke-ElevatedSelf)
    }

    Write-Host ""
    Write-Host "JUDICIAL PIPELINE - PREPARAÇÃO DO WINDOWS" -ForegroundColor Green
    Write-Host "Não feche esta janela. O processo será encerrado automaticamente se exceder o limite."
    Write-Host ""

    if (Invoke-WslUpdate) {
        $updatedVersion = Get-WslVersion
        if ($updatedVersion -and $updatedVersion -ge $minimumWslVersion) {
            Write-Log -Message "WSL atualizado para $updatedVersion."
            exit 0
        }
    }

    if ($script:wslUpdateTimedOut) {
        throw "A atualização demorou além do limite. Verifique a internet e execute novamente o instalador."
    }

    if (-not (Enable-WslFeatures)) {
        throw "O Windows não conseguiu ativar os componentes necessários."
    }

    $wsl = Get-Command wsl.exe -ErrorAction SilentlyContinue
    if ($wsl) {
        Write-Status "Concluindo a instalação do componente do Windows..."
        $installResult = Invoke-NativeProcess `
            -FilePath $wsl.Source `
            -Arguments @("--install", "--no-distribution") `
            -TimeoutSeconds $MaxUpdateSeconds
        if ($installResult.TimedOut) {
            throw "A instalação do componente demorou além do limite. Verifique a internet e tente novamente."
        }
        if ($installResult.ExitCode -ne 0) {
            Write-Info "O componente será concluído após a reinicialização."
        }
    }

    Write-Status "A primeira etapa foi concluída. Reinicie o computador."
    exit 10
} catch {
    Write-Log -Message ("Falha: " + $_.Exception.Message)
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Log para suporte: $logPath" -ForegroundColor Yellow
    exit 1
}
