[CmdletBinding()]
param(
    [switch]$Silencioso
)

$ErrorActionPreference = "Stop"
$minimumWslVersion = [version]"2.1.5"

function Write-Info {
    param([string]$Message)

    if (-not $Silencioso) {
        Write-Host $Message
    }
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
    try {
        if (-not $process.Start()) {
            throw "O componente do Windows não pôde ser iniciado."
        }
        $standardOutput = ""
        $standardError = ""
        if ($CaptureOutput) {
            $standardOutput = $process.StandardOutput.ReadToEnd()
            $standardError = $process.StandardError.ReadToEnd()
        }
        $process.WaitForExit()
        return [pscustomobject]@{
            ExitCode = $process.ExitCode
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
        "-Silencioso"
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

    Write-Info "Atualizando o componente do Windows..."
    $result = Invoke-NativeProcess `
        -FilePath $wsl.Source `
        -Arguments @("--update", "--web-download")
    if ($result.ExitCode -ne 0) {
        $result = Invoke-NativeProcess -FilePath $wsl.Source -Arguments @("--update")
    }

    if ($result.ExitCode -ne 0) {
        return $false
    }

    Invoke-NativeProcess -FilePath $wsl.Source -Arguments @("--shutdown") | Out-Null
    return $true
}

function Enable-WslFeatures {
    Write-Info "Ativando os componentes necessários do Windows..."

    $wslFeature = Invoke-NativeProcess `
        -FilePath "dism.exe" `
        -Arguments @(
            "/online",
            "/enable-feature",
            "/featurename:Microsoft-Windows-Subsystem-Linux",
            "/all",
            "/norestart"
        )
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
        )
    return $virtualMachineFeature.ExitCode -eq 0
}

try {
    $installedVersion = Get-WslVersion
    if ($installedVersion -and $installedVersion -ge $minimumWslVersion) {
        exit 0
    }

    if (-not (Test-Administrator)) {
        exit (Invoke-ElevatedSelf)
    }

    if (Invoke-WslUpdate) {
        $updatedVersion = Get-WslVersion
        if ($updatedVersion -and $updatedVersion -ge $minimumWslVersion) {
            exit 0
        }
    }

    if (-not (Enable-WslFeatures)) {
        throw "O Windows não conseguiu ativar os componentes necessários."
    }

    $wsl = Get-Command wsl.exe -ErrorAction SilentlyContinue
    if ($wsl) {
        $installResult = Invoke-NativeProcess `
            -FilePath $wsl.Source `
            -Arguments @("--install", "--no-distribution")
        if ($installResult.ExitCode -ne 0) {
            Write-Info "O componente será concluído após a reinicialização."
        }
    }

    exit 10
} catch {
    if (-not $Silencioso) {
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    exit 1
}
