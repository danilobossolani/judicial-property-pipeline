[CmdletBinding()]
param(
    [switch]$Silencioso,

    [ValidateRange(120, 1800)]
    [int]$MaxUpdateSeconds = 600,

    [ValidateRange(300, 3600)]
    [int]$MaxMsiDownloadSeconds = 1800,

    [switch]$ValidarDownloadOficial,

    [string]$ExportarReparoOficial
)

$ErrorActionPreference = "Stop"
$minimumWslVersion = [version]"2.1.5"
$officialWslVersion = "2.7.12.0"
$officialWslMsiFileName = "wsl.$officialWslVersion.x64.msi"
$officialWslMsiUrl = "https://github.com/microsoft/WSL/releases/download/2.7.12/$officialWslMsiFileName"
$officialWslMsiSha256 = "A460D4560215F2EFE003C136244B78EA3415D773824D7A688EA9DED36DBE9145"
$officialWslMsiSize = 258998272L
$bundledWslMsiPath = Join-Path `
    $PSScriptRoot `
    "installer\runtime\$officialWslMsiFileName"
$script:wslUpdateTimedOut = $false
$script:wslVersionExitCode = $null
$script:restartRequired = $false
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
        $script:wslVersionExitCode = $null
        return $null
    }

    $result = Invoke-NativeProcess `
        -FilePath $wsl.Source `
        -Arguments @("--version") `
        -TimeoutSeconds 30 `
        -CaptureOutput
    $script:wslVersionExitCode = $result.ExitCode
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
        "-MaxUpdateSeconds", $MaxUpdateSeconds,
        "-MaxMsiDownloadSeconds", $MaxMsiDownloadSeconds
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

    $featureNames = @(
        "Microsoft-Windows-Subsystem-Linux",
        "VirtualMachinePlatform"
    )
    foreach ($featureName in $featureNames) {
        try {
            $feature = Get-WindowsOptionalFeature `
                -Online `
                -FeatureName $featureName `
                -ErrorAction Stop
            if ($feature.State -ne "Enabled") {
                $script:restartRequired = $true
            }
        } catch {
            Write-Log -Message "Não foi possível consultar o estado de $featureName."
        }
    }

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
    if ($wslFeature.ExitCode -notin @(0, 1641, 3010)) {
        return $false
    }
    if ($wslFeature.ExitCode -in @(1641, 3010)) {
        $script:restartRequired = $true
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
    if ($virtualMachineFeature.ExitCode -in @(1641, 3010)) {
        $script:restartRequired = $true
    }
    return $virtualMachineFeature.ExitCode -in @(0, 1641, 3010)
}

function Get-FileSha256 {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToUpperInvariant()
}

function Test-OfficialWslPackage {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $false
    }

    $file = Get-Item -LiteralPath $Path
    if ($file.Length -ne $officialWslMsiSize) {
        Write-Log -Message "Pacote WSL rejeitado por tamanho incorreto: $($file.Length)."
        return $false
    }

    $actualHash = Get-FileSha256 -Path $Path
    if ($actualHash -ne $officialWslMsiSha256) {
        Write-Log -Message "Pacote WSL rejeitado por SHA-256 incorreto: $actualHash."
        return $false
    }

    return $true
}

function Invoke-DownloadWithProgress {
    param(
        [string]$Uri,
        [string]$OutputPath,
        [ValidateRange(300, 3600)]
        [int]$TimeoutSeconds
    )

    [Net.ServicePointManager]::SecurityProtocol =
        [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

    $partialPath = "$OutputPath.part"
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $request = $null
        $response = $null
        $input = $null
        $output = $null
        try {
            Remove-Item -LiteralPath $partialPath -Force -ErrorAction SilentlyContinue
            Write-Status "Baixando o reparo oficial do Windows (aprox. 247 MB)..."

            $request = [System.Net.HttpWebRequest]::Create($Uri)
            $request.AllowAutoRedirect = $true
            $request.UserAgent = "JudicialPipelineInstaller/1.1.4"
            $request.Timeout = 60000
            $request.ReadWriteTimeout = 60000
            $response = $request.GetResponse()
            $contentLength = [int64]$response.ContentLength
            $input = $response.GetResponseStream()
            $output = [System.IO.File]::Open(
                $partialPath,
                [System.IO.FileMode]::Create,
                [System.IO.FileAccess]::Write,
                [System.IO.FileShare]::None)

            $buffer = New-Object byte[] (1024 * 1024)
            $downloaded = 0L
            $nextProgress = 10
            $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
            while (($read = $input.Read($buffer, 0, $buffer.Length)) -gt 0) {
                $output.Write($buffer, 0, $read)
                $downloaded += $read

                if ($stopwatch.Elapsed.TotalSeconds -gt $TimeoutSeconds) {
                    throw "O download do reparo ultrapassou o limite automático."
                }

                if ($contentLength -gt 0) {
                    $percentage = [int][Math]::Floor(($downloaded * 100.0) / $contentLength)
                    if ($percentage -ge $nextProgress) {
                        Write-Status "Download do reparo: $percentage%"
                        $nextProgress = ([Math]::Floor($percentage / 10) + 1) * 10
                    }
                }
            }
            $stopwatch.Stop()
            $output.Flush()
            $output.Dispose()
            $output = $null
            $input.Dispose()
            $input = $null
            $response.Dispose()
            $response = $null

            Move-Item -LiteralPath $partialPath -Destination $OutputPath -Force
            Write-Log -Message "Download do pacote WSL concluído: $downloaded bytes."
            return
        } catch {
            Write-Log -Message "Tentativa $attempt de download do WSL falhou: $($_.Exception.Message)"
            if ($attempt -eq 3) {
                throw "Não foi possível baixar o reparo oficial do Windows. Verifique a internet e execute novamente."
            }
            Write-Status "O download foi interrompido. Tentando novamente..."
            Start-Sleep -Seconds (5 * $attempt)
        } finally {
            if ($output) { $output.Dispose() }
            if ($input) { $input.Dispose() }
            if ($response) { $response.Dispose() }
            Remove-Item -LiteralPath $partialPath -Force -ErrorAction SilentlyContinue
        }
    }
}

function Export-OfficialWslPackage {
    param([string]$OutputPath)

    $fullOutputPath = [System.IO.Path]::GetFullPath($OutputPath)
    $parentDirectory = [System.IO.Path]::GetDirectoryName($fullOutputPath)
    New-Item -ItemType Directory -Path $parentDirectory -Force | Out-Null

    if (Test-OfficialWslPackage -Path $fullOutputPath) {
        Write-Status "Usando o reparo oficial já baixado e validado."
        return $fullOutputPath
    }

    Remove-Item -LiteralPath $fullOutputPath -Force -ErrorAction SilentlyContinue
    Invoke-DownloadWithProgress `
        -Uri $officialWslMsiUrl `
        -OutputPath $fullOutputPath `
        -TimeoutSeconds $MaxMsiDownloadSeconds

    if (-not (Test-OfficialWslPackage -Path $fullOutputPath)) {
        throw "O reparo baixado não passou na validação de segurança."
    }

    return $fullOutputPath
}

function Install-OfficialWslPackage {
    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
    $cachedMsiPath = Join-Path $logDirectory $officialWslMsiFileName
    $msiLogPath = Join-Path $logDirectory "reparo-wsl-msi.log"
    $installationAccepted = $false
    $msiPath = $null

    try {
        if (Test-OfficialWslPackage -Path $bundledWslMsiPath) {
            $msiPath = $bundledWslMsiPath
            Write-Status "Usando o reparo oficial já incluído no instalador."
        } else {
            if (Test-Path -LiteralPath $bundledWslMsiPath) {
                Remove-Item -LiteralPath $bundledWslMsiPath -Force -ErrorAction SilentlyContinue
            }
            $msiPath = Export-OfficialWslPackage -OutputPath $cachedMsiPath
        }

        Write-Status "Instalando o reparo oficial da Microsoft. Aguarde..."
        $installResult = Invoke-NativeProcess `
            -FilePath "msiexec.exe" `
            -Arguments @(
                "/i",
                ('"' + $msiPath + '"'),
                "/qn",
                "/norestart",
                "/L*v",
                ('"' + $msiLogPath + '"')
            ) `
            -TimeoutSeconds 900
        if ($installResult.TimedOut) {
            throw "A instalação do reparo ultrapassou o limite automático."
        }
        if ($installResult.ExitCode -notin @(0, 1641, 3010)) {
            throw "O reparo oficial não pôde ser instalado. Código: $($installResult.ExitCode)."
        }
        $installationAccepted = $true
        if ($installResult.ExitCode -in @(1641, 3010)) {
            $script:restartRequired = $true
        }

        $repairedVersion = Get-WslVersion
        if (-not $repairedVersion -and -not $script:restartRequired) {
            Write-Status "Concluindo a reparação dos arquivos do Windows..."
            $repairResult = Invoke-NativeProcess `
                -FilePath "msiexec.exe" `
                -Arguments @(
                    "/fa",
                    ('"' + $msiPath + '"'),
                    "/qn",
                    "/norestart",
                    "/L*v",
                    ('"' + $msiLogPath + '"')
                ) `
                -TimeoutSeconds 900
            if ($repairResult.TimedOut) {
                throw "A reparação dos arquivos ultrapassou o limite automático."
            }
            if ($repairResult.ExitCode -notin @(0, 1641, 3010)) {
                throw "A reparação dos arquivos não foi concluída. Código: $($repairResult.ExitCode)."
            }
            if ($repairResult.ExitCode -in @(1641, 3010)) {
                $script:restartRequired = $true
            }
            $repairedVersion = Get-WslVersion
        }

        if ($repairedVersion -and $repairedVersion -ge $minimumWslVersion) {
            Write-Status "Componente do Windows reparado com sucesso."
            return $true
        }

        # Algumas correções do Windows só ficam visíveis depois do reinício,
        # mesmo quando o MSI retorna sucesso sem o código 3010.
        $script:restartRequired = $true
        Write-Info "O reparo será concluído após a reinicialização."
        return $true
    } finally {
        if ($installationAccepted -and $msiPath) {
            Remove-Item -LiteralPath $msiPath -Force -ErrorAction SilentlyContinue
        }
    }
}

function Complete-WslConfiguration {
    $wsl = Get-Command wsl.exe -ErrorAction SilentlyContinue
    if (-not $wsl) {
        return
    }

    Invoke-NativeProcess `
        -FilePath $wsl.Source `
        -Arguments @("--set-default-version", "2") `
        -TimeoutSeconds 60 | Out-Null
    Invoke-NativeProcess `
        -FilePath $wsl.Source `
        -Arguments @("--shutdown") `
        -TimeoutSeconds 60 | Out-Null
}

if (-not [string]::IsNullOrWhiteSpace($ExportarReparoOficial)) {
    try {
        $exportedPath = Export-OfficialWslPackage -OutputPath $ExportarReparoOficial
        Write-Output "OFFICIAL_WSL_EXPORT=OK"
        Write-Output $exportedPath
        exit 0
    } catch {
        Write-Log -Message ("Falha ao exportar o pacote WSL: " + $_.Exception.Message)
        Write-Host $_.Exception.Message -ForegroundColor Red
        exit 1
    }
}

if ($ValidarDownloadOficial) {
    $validationPath = Join-Path $env:TEMP (
        "JudicialPipeline-WslValidation-" + [Guid]::NewGuid().ToString("N") + ".msi")
    try {
        Export-OfficialWslPackage -OutputPath $validationPath | Out-Null
        Write-Output "OFFICIAL_WSL_DOWNLOAD=OK"
        exit 0
    } finally {
        Remove-Item -LiteralPath $validationPath -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath "$validationPath.part" -Force -ErrorAction SilentlyContinue
    }
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

    # Quando `wsl --version` falha (como em instalações legadas ou quebradas),
    # insistir em `wsl --update` costuma apenas travar. Nesse caso usamos
    # diretamente o pacote MSI oficial, fixado por versão, tamanho e SHA-256.
    if ($installedVersion) {
        if (Invoke-WslUpdate) {
            $updatedVersion = Get-WslVersion
            if ($updatedVersion -and $updatedVersion -ge $minimumWslVersion) {
                Complete-WslConfiguration
                Write-Log -Message "WSL atualizado para $updatedVersion."
                exit 0
            }
        }
    } else {
        Write-Log -Message "WSL sem versão válida. Código detectado: $script:wslVersionExitCode."
        Write-Status "A instalação atual do Windows precisa de reparação automática."
    }

    if (-not (Enable-WslFeatures)) {
        throw "O Windows não conseguiu ativar os componentes necessários."
    }

    if (-not (Install-OfficialWslPackage)) {
        throw "O Windows não conseguiu aplicar o reparo automático."
    }

    if ($script:restartRequired) {
        Write-Status "A primeira etapa foi concluída. Reinicie o computador."
        exit 10
    }

    $finalVersion = Get-WslVersion
    if (-not $finalVersion -or $finalVersion -lt $minimumWslVersion) {
        throw "O componente do Windows continuou indisponível após a reparação."
    }

    Complete-WslConfiguration
    Write-Log -Message "WSL reparado e atualizado para $finalVersion."
    exit 0
} catch {
    Write-Log -Message ("Falha: " + $_.Exception.Message)
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Log para suporte: $logPath" -ForegroundColor Yellow
    exit 1
}
