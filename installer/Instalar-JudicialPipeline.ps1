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

function Test-DockerReady {
    try {
        & docker info *> $null
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Add-DockerToCurrentPath {
    $dockerBin = Join-Path $env:ProgramFiles "Docker\Docker\resources\bin"
    if (Test-Path -LiteralPath $dockerBin) {
        $pathEntries = $env:Path -split ";"
        if ($pathEntries -notcontains $dockerBin) {
            $env:Path = "$dockerBin;$env:Path"
        }
    }
}

function Install-DockerDesktop {
    Write-Step "Baixando e instalando os componentes necessários"

    $winget = Get-Command winget.exe -ErrorAction SilentlyContinue
    if ($winget) {
        $process = Start-Process -FilePath $winget.Source -ArgumentList @(
            "install",
            "--exact",
            "--id", "Docker.DockerDesktop",
            "--silent",
            "--accept-package-agreements",
            "--accept-source-agreements",
            "--disable-interactivity"
        ) -Wait -PassThru -WindowStyle Hidden

        if ($process.ExitCode -eq 0) {
            return
        }

        Write-Host "O instalador padrão não concluiu. Tentando a fonte oficial..." -ForegroundColor Yellow
    }

    $dockerInstaller = Join-Path $env:TEMP "DockerDesktopInstaller-JudicialPipeline.exe"
    $dockerUrl = "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"
    Invoke-WebRequest -Uri $dockerUrl -OutFile $dockerInstaller -UseBasicParsing

    $process = Start-Process -FilePath $dockerInstaller -ArgumentList @(
        "install",
        "--quiet",
        "--accept-license",
        "--backend=wsl-2"
    ) -Wait -PassThru -WindowStyle Hidden

    if ($process.ExitCode -ne 0) {
        throw "O Docker Desktop não foi instalado. Código: $($process.ExitCode)."
    }
}

function Start-DockerDesktop {
    if (Test-DockerReady) {
        return $true
    }

    $dockerDesktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
    if (-not (Test-Path -LiteralPath $dockerDesktop)) {
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
    $desktop = [Environment]::GetFolderPath("Desktop")
    $startMenu = Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs"
    $iconPath = Join-Path $Destino "installer\assets\judicial-pipeline-icon.ico"

    New-Shortcut `
        -Path (Join-Path $desktop "Judicial Pipeline.lnk") `
        -Target (Join-Path $Destino "INICIAR.bat") `
        -WorkingDirectory $Destino `
        -Description "Iniciar o Judicial Pipeline" `
        -Icon $iconPath

    New-Shortcut `
        -Path (Join-Path $startMenu "Judicial Pipeline.lnk") `
        -Target (Join-Path $Destino "INICIAR.bat") `
        -WorkingDirectory $Destino `
        -Description "Iniciar o Judicial Pipeline" `
        -Icon $iconPath

    New-Shortcut `
        -Path (Join-Path $startMenu "Judicial Pipeline - Ajuda.lnk") `
        -Target (Join-Path $Destino "LEIA-ME-PRIMEIRO.txt") `
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
    New-Item -ItemType Directory -Path $Destino -Force | Out-Null
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

    Start-Transcript -LiteralPath (Join-Path $Destino "instalacao.log") -Append | Out-Null
    $logIniciado = $true

    Add-DockerToCurrentPath
    if (-not (Get-Command docker.exe -ErrorAction SilentlyContinue)) {
        if ($NaoInstalarDocker) {
            throw "Docker Desktop não está instalado."
        }
        Install-DockerDesktop
        Add-DockerToCurrentPath
    }

    Install-Shortcuts

    if (-not (Start-DockerDesktop)) {
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
        & docker compose up --build -d *> $composeLog
        if ($LASTEXITCODE -ne 0) {
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
