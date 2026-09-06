[CmdletBinding()]
param(
    [ValidateRange(1024, 65535)]
    [int]$Porta = 18081
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$testId = [Guid]::NewGuid().ToString("N")
$testBase = [System.IO.Path]::GetFullPath(
    (Join-Path $env:TEMP "JudicialPipelineClientTests"))
$testRoot = [System.IO.Path]::GetFullPath(
    (Join-Path $testBase $testId))
$source = Join-Path $testRoot "source"
$destination = Join-Path $testRoot "installed"
$localAppData = Join-Path $testRoot "localappdata"
$projectName = "jp-smoke-" + $testId.Substring(0, 12)
$completed = $false

function Assert-SafeTestPath {
    param([string]$Path)

    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $safePrefix = $testBase.TrimEnd("\") + "\"
    if (-not $fullPath.StartsWith($safePrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Caminho de teste fora da área temporária permitida: $fullPath"
    }
}

function Get-EnvironmentValue {
    param(
        [string]$Path,
        [string]$Name
    )

    $prefix = "$Name="
    $line = Get-Content -LiteralPath $Path -Encoding UTF8 |
        Where-Object { $_.StartsWith($prefix, [System.StringComparison]::Ordinal) } |
        Select-Object -First 1
    if (-not $line) {
        throw "Configuração ausente no teste: $Name"
    }
    return $line.Substring($prefix.Length).Trim()
}

try {
    Assert-SafeTestPath -Path $testRoot
    New-Item -ItemType Directory -Path $source -Force | Out-Null
    New-Item -ItemType Directory -Path $localAppData -Force | Out-Null

    & robocopy.exe $projectRoot $source /E `
        /XD .git .idea target dist `
        /XF .env *.log `
        /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -gt 7) {
        throw "Não foi possível preparar a cópia temporária. Código: $LASTEXITCODE."
    }

    @(
        "services:",
        "  db:",
        "    volumes: !reset []",
        "    tmpfs:",
        "      - /var/lib/postgresql/data"
    ) | Set-Content -LiteralPath "$source\compose.smoke.yaml" -Encoding UTF8
    $env:COMPOSE_FILE = "compose.yaml;compose.smoke.yaml"

    $compiler = "$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
    if (-not (Test-Path -LiteralPath $compiler)) {
        $compiler = "$env:WINDIR\Microsoft.NET\Framework\v4.0.30319\csc.exe"
    }
    if (-not (Test-Path -LiteralPath $compiler)) {
        throw "Compilador do Windows não encontrado."
    }

    & $compiler `
        /nologo `
        /target:winexe `
        /optimize+ `
        "/out:$source\Judicial Pipeline.exe" `
        "/win32icon:$source\installer\assets\judicial-pipeline-icon.ico" `
        /reference:System.Windows.Forms.dll `
        /reference:System.Drawing.dll `
        "$source\installer\JudicialPipelineLauncher.cs"
    if ($LASTEXITCODE -ne 0) {
        throw "O inicializador visual não compilou no teste."
    }

    "DATAJUD_API_KEY=client-install-smoke-test-key" |
        Set-Content `
            -LiteralPath "$source\installer\installer-settings.env" `
            -Encoding UTF8

    $installer = Join-Path $source "installer\Instalar-JudicialPipeline.ps1"
    & powershell.exe `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File $installer `
        -Origem $source `
        -Destino $destination `
        -Porta $Porta `
        -ProjetoCompose $projectName `
        -NaoInstalarDocker `
        -NaoCriarAtalhos `
        -NaoAbrirNavegador `
        -NaoAguardar
    if ($LASTEXITCODE -ne 0) {
        throw "A instalação limpa falhou. Código: $LASTEXITCODE."
    }

    $environmentFile = Join-Path $destination ".env"
    $firstPassword = Get-EnvironmentValue -Path $environmentFile -Name "POSTGRES_PASSWORD"
    if ((Get-EnvironmentValue -Path $environmentFile -Name "APP_SECURITY_ENABLED") -ne "false") {
        throw "O login local não foi desativado."
    }
    if ((Get-EnvironmentValue -Path $environmentFile -Name "APP_PORT") -ne "$Porta") {
        throw "A porta de teste não foi configurada."
    }
    if (Test-Path -LiteralPath "$destination\installer\installer-settings.env") {
        throw "A configuração temporária permaneceu na instalação."
    }

    $health = Invoke-WebRequest `
        -Uri "http://127.0.0.1:$Porta/actuator/health" `
        -UseBasicParsing `
        -TimeoutSec 15
    if ($health.StatusCode -ne 200) {
        throw "O health check não retornou 200."
    }

    $page = Invoke-WebRequest `
        -Uri "http://127.0.0.1:$Porta/" `
        -UseBasicParsing `
        -TimeoutSec 15
    if ($page.StatusCode -ne 200 -or $page.Content -notmatch "Judicial Pipeline") {
        throw "O painel não abriu corretamente."
    }
    if ($page.Content -match "form-login|Entrar no sistema") {
        throw "A tela de login apareceu na instalação local."
    }

    $previousLogDirectory = $env:JUDICIAL_PIPELINE_LOG_DIR
    $previousNoBrowser = $env:JUDICIAL_PIPELINE_NO_BROWSER
    try {
        $env:JUDICIAL_PIPELINE_LOG_DIR = Join-Path $localAppData "JudicialPipeline"
        $env:JUDICIAL_PIPELINE_NO_BROWSER = "1"
        $launcher = Join-Path $destination "Judicial Pipeline.exe"
        $launcherProcess = Start-Process `
            -FilePath $launcher `
            -Wait `
            -PassThru
        if ($launcherProcess.ExitCode -ne 0) {
            throw "O inicializador visual falhou. Código: $($launcherProcess.ExitCode)."
        }
    } finally {
        $env:JUDICIAL_PIPELINE_LOG_DIR = $previousLogDirectory
        $env:JUDICIAL_PIPELINE_NO_BROWSER = $previousNoBrowser
    }

    if (-not (Test-Path -LiteralPath "$localAppData\JudicialPipeline\inicializacao.log")) {
        throw "O inicializador visual não gerou o log esperado."
    }

    $previousKeepEngine = $env:JUDICIAL_PIPELINE_KEEP_ENGINE
    $previousNoDialog = $env:JUDICIAL_PIPELINE_NO_DIALOG
    try {
        $env:JUDICIAL_PIPELINE_KEEP_ENGINE = "1"
        $env:JUDICIAL_PIPELINE_NO_DIALOG = "1"
        $stopProcess = Start-Process `
            -FilePath $launcher `
            -ArgumentList "--stop" `
            -Wait `
            -PassThru
        if ($stopProcess.ExitCode -ne 0) {
            throw "O encerramento visual falhou. Código: $($stopProcess.ExitCode)."
        }
    } finally {
        $env:JUDICIAL_PIPELINE_KEEP_ENGINE = $previousKeepEngine
        $env:JUDICIAL_PIPELINE_NO_DIALOG = $previousNoDialog
    }

    if (-not (Test-Path -LiteralPath "$localAppData\JudicialPipeline\encerramento.log")) {
        throw "O encerramento visual não gerou o log esperado."
    }
    if (-not (Test-Path -LiteralPath "$localAppData\JudicialPipeline\encerramento-launcher.log")) {
        throw "O inicializador não registrou o encerramento esperado."
    }

    $applicationStopped = $false
    try {
        Invoke-WebRequest `
            -Uri "http://127.0.0.1:$Porta/actuator/health" `
            -UseBasicParsing `
            -TimeoutSec 3 | Out-Null
    } catch {
        $applicationStopped = $true
    }
    if (-not $applicationStopped) {
        throw "A aplicação continuou respondendo após o encerramento."
    }

    & powershell.exe `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File $installer `
        -Origem $source `
        -Destino $destination `
        -Porta $Porta `
        -ProjetoCompose $projectName `
        -NaoInstalarDocker `
        -NaoCriarAtalhos `
        -NaoAbrirNavegador `
        -NaoAguardar
    if ($LASTEXITCODE -ne 0) {
        throw "A reinstalação falhou. Código: $LASTEXITCODE."
    }

    $secondPassword = Get-EnvironmentValue -Path $environmentFile -Name "POSTGRES_PASSWORD"
    if ($firstPassword -ne $secondPassword) {
        throw "A reinstalação alterou a senha do banco existente."
    }

    Write-Output "CLIENT_INSTALL_SMOKE=OK"
    Write-Output "HEALTH_STATUS=$($health.StatusCode)"
    Write-Output "LOGIN_DISABLED=True"
    Write-Output "LAUNCHER_LOG=True"
    Write-Output "STOPPER_LOG=True"
    Write-Output "APPLICATION_STOPPED=True"
    Write-Output "PASSWORD_PRESERVED=True"
    $completed = $true
} finally {
    if (Test-Path -LiteralPath (Join-Path $destination "compose.yaml")) {
        Push-Location $destination
        try {
            # O Docker envia mensagens normais de progresso para stderr. No
            # Windows PowerShell 5.1 isso pode virar NativeCommandError quando
            # ErrorActionPreference=Stop, mesmo com código de saída zero.
            $previousPreference = $ErrorActionPreference
            try {
                $ErrorActionPreference = "Continue"
                & docker compose -p $projectName down --remove-orphans *> $null
                $cleanupExitCode = $LASTEXITCODE
            } finally {
                $ErrorActionPreference = $previousPreference
            }
            if ($cleanupExitCode -ne 0) {
                Write-Warning "A limpeza do ambiente Docker de teste retornou código $cleanupExitCode."
            }
        } finally {
            Pop-Location
        }
    }

    if ($completed -and (Test-Path -LiteralPath $testRoot)) {
        Assert-SafeTestPath -Path $testRoot
        Remove-Item -LiteralPath $testRoot -Recurse -Force
    } elseif (-not $completed) {
        Write-Warning "Artefatos do teste preservados em: $testRoot"
    }
}
