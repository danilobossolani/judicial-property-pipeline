[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Origem,

    [string]$Destino = "$env:ProgramData\JudicialPipeline",

    [ValidateRange(1024, 65535)]
    [int]$Porta = 8080,

    [string]$ProjetoCompose = "judicial-pipeline",

    [switch]$NaoInstalarDocker,
    [switch]$NaoCriarAtalhos,
    [switch]$NaoAbrirNavegador,
    [switch]$NaoAguardar
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$logIniciado = $false

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function New-RandomSecret {
    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return ([System.BitConverter]::ToString($bytes) -replace "-", "").ToLowerInvariant()
}

function Get-InstallerSetting {
    param(
        [string]$Path,
        [string]$Name
    )

    $prefix = "$Name="
    $line = Get-Content -LiteralPath $Path -Encoding UTF8 |
        Where-Object { $_.StartsWith($prefix, [System.StringComparison]::Ordinal) } |
        Select-Object -First 1

    if (-not $line) {
        throw "A configuração interna '$Name' não foi encontrada."
    }

    return $line.Substring($prefix.Length).Trim()
}

function Get-OptionalEnvironmentSetting {
    param(
        [string]$Path,
        [string]$Name
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    $prefix = "$Name="
    $line = Get-Content -LiteralPath $Path -Encoding UTF8 |
        Where-Object { $_.StartsWith($prefix, [System.StringComparison]::Ordinal) } |
        Select-Object -First 1

    if (-not $line) {
        return $null
    }

    $value = $line.Substring($prefix.Length).Trim()
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $null
    }

    return $value
}

function Test-SystemRequirements {
    if (-not [Environment]::Is64BitOperatingSystem) {
        throw "Este programa precisa do Windows de 64 bits."
    }

    $operatingSystem = Get-CimInstance Win32_OperatingSystem
    $build = [int]$operatingSystem.BuildNumber
    if ($operatingSystem.Caption -match "Windows 10" -and $build -lt 19045) {
        throw "Atualize o Windows 10 para a versão 22H2 antes de instalar."
    }
    if ($operatingSystem.Caption -match "Windows 11" -and $build -lt 22631) {
        throw "Atualize o Windows 11 para a versão 23H2 ou mais recente antes de instalar."
    }
    if ($operatingSystem.Caption -notmatch "Windows 10|Windows 11") {
        throw "O Judicial Pipeline requer Windows 10 ou Windows 11 atualizado."
    }

    $memoryBytes = [int64]$operatingSystem.TotalVisibleMemorySize * 1KB
    if ($memoryBytes -lt 8GB) {
        throw "Este computador precisa de pelo menos 8 GB de memória RAM."
    }

    # Win32_Processor.VirtualizationFirmwareEnabled pode retornar falso quando
    # o hipervisor já assumiu o recurso. O Docker faz a verificação definitiva
    # ao iniciar; bloquear aqui causaria falsos negativos em máquinas válidas.
}

function Invoke-DockerCommand {
    param(
        [string[]]$Arguments,
        [string]$OutputPath
    )

    # No Windows PowerShell 5.1, mensagens normais do Docker enviadas para
    # stderr podem virar erros terminantes quando ErrorActionPreference=Stop.
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        if ($OutputPath) {
            & docker @Arguments *> $OutputPath
        } else {
            & docker @Arguments *> $null
        }
        return $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

function Test-DockerReady {
    try {
        return (Invoke-DockerCommand -Arguments @("info")) -eq 0
    } catch {
        return $false
    }
}

function Get-DockerDesktopPath {
    $candidates = @(
        (Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"),
        (Join-Path $env:LOCALAPPDATA "Programs\DockerDesktop\Docker Desktop.exe")
    )

    return $candidates |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
}

function Add-DockerToCurrentPath {
    $candidates = @(
        (Join-Path $env:ProgramFiles "Docker\Docker\resources\bin"),
        (Join-Path $env:LOCALAPPDATA "Programs\DockerDesktop\resources\bin")
    )

    foreach ($dockerBin in $candidates) {
        if (Test-Path -LiteralPath $dockerBin) {
            $pathEntries = $env:Path -split ";"
            if ($pathEntries -notcontains $dockerBin) {
                $env:Path = "$dockerBin;$env:Path"
            }
        }
    }
}

function Invoke-DownloadWithRetry {
    param(
        [string]$Uri,
        [string]$OutputPath
    )

    [Net.ServicePointManager]::SecurityProtocol =
        [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            Invoke-WebRequest -Uri $Uri -OutFile $OutputPath -UseBasicParsing
            return
        } catch {
            if ($attempt -eq 3) {
                throw
            }
            Start-Sleep -Seconds (5 * $attempt)
        }
    }
}

function Install-DockerDesktop {
    Write-Step "Baixando e instalando os componentes necessários"

    $dockerInstaller = Join-Path $env:TEMP "DockerDesktopInstaller-JudicialPipeline.exe"
    $dockerUrl = "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"
    Invoke-DownloadWithRetry -Uri $dockerUrl -OutputPath $dockerInstaller

    try {
        $process = Start-Process -FilePath $dockerInstaller -ArgumentList @(
            "install",
            "--quiet",
            "--accept-license",
            "--backend=wsl-2",
            "--always-run-service",
            "--no-windows-containers"
        ) -Wait -PassThru -WindowStyle Hidden

        if ($process.ExitCode -ne 0) {
            throw "O componente local não foi instalado. Código: $($process.ExitCode)."
        }
    } finally {
        Remove-Item -LiteralPath $dockerInstaller -Force -ErrorAction SilentlyContinue
    }
}

function Start-DockerDesktop {
    if (Test-DockerReady) {
        return $true
    }

    $dockerDesktop = Get-DockerDesktopPath
    if (-not $dockerDesktop) {
        return $false
    }

    Write-Step "Iniciando o mecanismo local"
    Start-Process -FilePath $dockerDesktop -WindowStyle Hidden | Out-Null

    for ($attempt = 0; $attempt -lt 180; $attempt++) {
        if (Test-DockerReady) {
            return $true
        }
        Start-Sleep -Seconds 2
    }

    return $false
}

function New-Shortcut {
    param(
        [string]$Path,
        [string]$Target,
        [string]$WorkingDirectory,
        [string]$Description,
        [string]$Icon
    )

    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut($Path)
    $shortcut.TargetPath = $Target
    $shortcut.WorkingDirectory = $WorkingDirectory
    $shortcut.Description = $Description
    if ($Icon -and (Test-Path -LiteralPath $Icon)) {
        $shortcut.IconLocation = "$Icon,0"
    }
    $shortcut.Save()
}

function Install-Shortcuts {
    if ($NaoCriarAtalhos) {
        return
    }

    Write-Step "Criando o atalho na área de trabalho"
    $desktop = [Environment]::GetFolderPath("CommonDesktopDirectory")
    $startMenu = [Environment]::GetFolderPath("CommonPrograms")
    $userDesktop = [Environment]::GetFolderPath("Desktop")
    $userStartMenu = Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs"
    $iconPath = Join-Path $Destino "installer\assets\judicial-pipeline-icon.ico"
    $launcherPath = Join-Path $Destino "Judicial Pipeline.exe"
    if (-not (Test-Path -LiteralPath $launcherPath)) {
        $launcherPath = Join-Path $Destino "INICIAR.bat"
    }
    $manualPath = Join-Path $Destino "output\pdf\Manual-do-Usuario-Judicial-Pipeline.pdf"
    if (-not (Test-Path -LiteralPath $manualPath)) {
        $manualPath = Join-Path $Destino "LEIA-ME-PRIMEIRO.txt"
    }

    # Versões anteriores criavam atalhos somente para o usuário atual. Eles
    # apontavam para o arquivo BAT e podiam deixar dois ícones na área de
    # trabalho após a atualização. Removemos apenas esses atalhos legados.
    @(
        (Join-Path $userDesktop "Judicial Pipeline.lnk"),
        (Join-Path $userStartMenu "Judicial Pipeline.lnk"),
        (Join-Path $userStartMenu "Judicial Pipeline - Ajuda.lnk")
    ) | Where-Object {
        $_ -ne (Join-Path $desktop "Judicial Pipeline.lnk") -and
        $_ -ne (Join-Path $startMenu "Judicial Pipeline.lnk")
    } | ForEach-Object {
        if (Test-Path -LiteralPath $_) {
            Remove-Item -LiteralPath $_ -Force
        }
    }

    New-Shortcut `
        -Path (Join-Path $desktop "Judicial Pipeline.lnk") `
        -Target $launcherPath `
        -WorkingDirectory $Destino `
        -Description "Iniciar o Judicial Pipeline" `
        -Icon $iconPath

    New-Shortcut `
        -Path (Join-Path $startMenu "Judicial Pipeline.lnk") `
        -Target $launcherPath `
        -WorkingDirectory $Destino `
        -Description "Iniciar o Judicial Pipeline" `
        -Icon $iconPath

    New-Shortcut `
        -Path (Join-Path $startMenu "Judicial Pipeline - Manual.lnk") `
        -Target $manualPath `
        -WorkingDirectory $Destino `
        -Description "Ajuda do Judicial Pipeline" `
        -Icon $iconPath
}

function Wait-ApplicationHealth {
    param([int]$LocalPort)

    $healthUrl = "http://127.0.0.1:$LocalPort/actuator/health"
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        try {
            $health = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 5
            if ($health.StatusCode -eq 200) {
                return $true
            }
        } catch {
            # A aplicação ainda está iniciando.
        }
        Start-Sleep -Seconds 2
    }

    return $false
}

try {
    Write-Host "JUDICIAL PIPELINE" -ForegroundColor Green
    Write-Host "Instalação automática para Windows"

    New-Item -ItemType Directory -Path $Destino -Force | Out-Null
    Start-Transcript -LiteralPath (Join-Path $Destino "instalacao.log") -Append | Out-Null
    $logIniciado = $true
    Test-SystemRequirements

    if (-not (Test-Path -LiteralPath $Origem)) {
        throw "O conteúdo do instalador não foi encontrado."
    }

    $settingsFile = Join-Path $Origem "installer\installer-settings.env"
    if (-not (Test-Path -LiteralPath $settingsFile)) {
        throw "A configuração personalizada não foi encontrada dentro do instalador."
    }

    $dataJudApiKey = Get-InstallerSetting -Path $settingsFile -Name "DATAJUD_API_KEY"
    if ([string]::IsNullOrWhiteSpace($dataJudApiKey) -or $dataJudApiKey -match "troque|exemplo") {
        throw "A chave DataJud do instalador é inválida."
    }
    if ($dataJudApiKey.Contains("`r") -or $dataJudApiKey.Contains("`n")) {
        throw "A chave DataJud contém caracteres inválidos."
    }

    $environmentFile = Join-Path $Destino ".env"
    $databasePassword = Get-OptionalEnvironmentSetting `
        -Path $environmentFile `
        -Name "POSTGRES_PASSWORD"
    $internalPassword = Get-OptionalEnvironmentSetting `
        -Path $environmentFile `
        -Name "APP_SECURITY_PASSWORD"

    if (-not $databasePassword) {
        $databasePassword = New-RandomSecret
    }
    if (-not $internalPassword) {
        $internalPassword = New-RandomSecret
    }

    Write-Step "Copiando o programa"
    Get-ChildItem -LiteralPath $Origem -Force |
        Where-Object { $_.Name -notin @(".git", ".idea", "target", ".env") } |
        Copy-Item -Destination $Destino -Recurse -Force

    $copiedSettings = Join-Path $Destino "installer\installer-settings.env"
    if (Test-Path -LiteralPath $copiedSettings) {
        Remove-Item -LiteralPath $copiedSettings -Force
    }

    @(
        "COMPOSE_PROJECT_NAME=$ProjetoCompose",
        "POSTGRES_DB=judicial_pipeline",
        "POSTGRES_USER=judicial_pipeline",
        "POSTGRES_PASSWORD=$databasePassword",
        "DATAJUD_API_KEY=$dataJudApiKey",
        "APP_SECURITY_ENABLED=false",
        "APP_SECURITY_USERNAME=local",
        "APP_SECURITY_PASSWORD=$internalPassword",
        "APP_COOKIE_SECURE=false",
        "APP_PORT=$Porta"
    ) | Set-Content -LiteralPath $environmentFile -Encoding UTF8

    Add-DockerToCurrentPath
    $windowsPreparationScript = Join-Path $Destino "Preparar-Windows.ps1"
    & powershell.exe `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File $windowsPreparationScript `
        -Silencioso
    $windowsPreparationResult = $LASTEXITCODE
    if ($windowsPreparationResult -notin @(0, 10)) {
        throw "O Windows não conseguiu atualizar o componente WSL automaticamente."
    }

    if (-not (Get-Command docker.exe -ErrorAction SilentlyContinue)) {
        if ($NaoInstalarDocker) {
            throw "Docker Desktop não está instalado."
        }
        Install-DockerDesktop
        Add-DockerToCurrentPath
    }

    Install-Shortcuts

    if ($windowsPreparationResult -eq 0 -and -not (Test-DockerReady)) {
        Invoke-DockerCommand -Arguments @("desktop", "stop") | Out-Null
        Start-Sleep -Seconds 3
    }

    if ($windowsPreparationResult -eq 10 -or -not (Start-DockerDesktop)) {
        Write-Host ""
        Write-Host "A primeira etapa terminou." -ForegroundColor Green
        Write-Host "Reinicie o computador e depois clique no atalho Judicial Pipeline."
        Write-Host "Os arquivos e o atalho já estão preparados."
        Write-Host ""
        if (-not $NaoAguardar) {
            Read-Host "Pressione ENTER para fechar"
        }
        exit 0
    }

    Write-Step "Preparando o banco e a aplicação"
    Push-Location $Destino
    try {
        $composeLog = Join-Path $Destino "preparacao-docker.log"
        $composeExitCode = Invoke-DockerCommand `
            -Arguments @("compose", "up", "--build", "-d") `
            -OutputPath $composeLog
        if ($composeExitCode -ne 0) {
            throw "O ambiente local não pôde ser iniciado. Consulte preparacao-docker.log."
        }
    } finally {
        Pop-Location
    }

    Write-Step "Aguardando o Judicial Pipeline ficar pronto"
    if (-not (Wait-ApplicationHealth -LocalPort $Porta)) {
        throw "O Judicial Pipeline demorou além do esperado para responder. Use VER-STATUS.bat."
    }

    Write-Host ""
    Write-Host "Instalação concluída com sucesso." -ForegroundColor Green
    Write-Host "Nos próximos usos, clique apenas no atalho Judicial Pipeline."
    Write-Host ""

    if (-not $NaoAbrirNavegador) {
        Start-Process "http://localhost:$Porta"
    }

    if (-not $NaoAguardar) {
        Read-Host "Pressione ENTER para fechar esta janela"
    }
    exit 0
} catch {
    Write-Host ""
    Write-Host "A instalação não foi concluída." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "Se precisar de ajuda, envie o arquivo instalacao.log ao responsável técnico."
    if (-not $NaoAguardar) {
        Read-Host "Pressione ENTER para fechar"
    }
    exit 1
} finally {
    if ($logIniciado) {
        Stop-Transcript | Out-Null
    }
}
