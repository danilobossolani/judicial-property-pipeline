[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$testBase = Join-Path $env:TEMP "JudicialPipelineStopTests"
$testRoot = Join-Path $testBase ([Guid]::NewGuid().ToString("N"))
$fakeBin = Join-Path $testRoot "bin"
$installation = Join-Path $testRoot "installation"
$logs = Join-Path $testRoot "logs"
$fakeDockerLog = Join-Path $testRoot "fake-docker.log"
$completed = $false

function Assert-SafeTestPath {
    param([string]$Path)

    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $safePrefix = [System.IO.Path]::GetFullPath($testBase).TrimEnd("\") + "\"
    if (-not $fullPath.StartsWith($safePrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Caminho de teste fora da área temporária permitida: $fullPath"
    }
}

try {
    Assert-SafeTestPath -Path $testRoot
    New-Item -ItemType Directory -Path $fakeBin, $installation, $logs -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot "PARAR.bat") -Destination $installation

    @(
        "@echo off",
        "echo %*>> `"%FAKE_DOCKER_LOG%`"",
        "exit /b 0"
    ) | Set-Content -LiteralPath (Join-Path $fakeBin "docker.cmd") -Encoding ASCII

    $compiler = "$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
    if (-not (Test-Path -LiteralPath $compiler)) {
        $compiler = "$env:WINDIR\Microsoft.NET\Framework\v4.0.30319\csc.exe"
    }
    if (-not (Test-Path -LiteralPath $compiler)) {
        throw "Compilador do Windows não encontrado."
    }

    $launcher = Join-Path $installation "Judicial Pipeline.exe"
    & $compiler `
        /nologo `
        /target:winexe `
        /optimize+ `
        "/out:$launcher" `
        "/win32icon:$projectRoot\installer\assets\judicial-pipeline-icon.ico" `
        /reference:System.Windows.Forms.dll `
        /reference:System.Drawing.dll `
        "$projectRoot\installer\JudicialPipelineLauncher.cs"
    if ($LASTEXITCODE -ne 0) {
        throw "O inicializador visual não compilou."
    }

    $previousPath = $env:Path
    $previousLogDirectory = $env:JUDICIAL_PIPELINE_LOG_DIR
    $previousKeepEngine = $env:JUDICIAL_PIPELINE_KEEP_ENGINE
    $previousNoDialog = $env:JUDICIAL_PIPELINE_NO_DIALOG
    $previousSkipDockerPath = $env:JUDICIAL_PIPELINE_SKIP_DOCKER_PATH
    $previousFakeDockerLog = $env:FAKE_DOCKER_LOG
    try {
        $env:Path = "$fakeBin;$previousPath"
        $env:JUDICIAL_PIPELINE_LOG_DIR = $logs
        $env:JUDICIAL_PIPELINE_KEEP_ENGINE = "1"
        $env:JUDICIAL_PIPELINE_NO_DIALOG = "1"
        $env:JUDICIAL_PIPELINE_SKIP_DOCKER_PATH = "1"
        $env:FAKE_DOCKER_LOG = $fakeDockerLog

        $process = Start-Process `
            -FilePath $launcher `
            -ArgumentList "--stop" `
            -Wait `
            -PassThru
        if ($process.ExitCode -ne 0) {
            throw "O encerramento visual retornou código $($process.ExitCode)."
        }
    } finally {
        $env:Path = $previousPath
        $env:JUDICIAL_PIPELINE_LOG_DIR = $previousLogDirectory
        $env:JUDICIAL_PIPELINE_KEEP_ENGINE = $previousKeepEngine
        $env:JUDICIAL_PIPELINE_NO_DIALOG = $previousNoDialog
        $env:JUDICIAL_PIPELINE_SKIP_DOCKER_PATH = $previousSkipDockerPath
        $env:FAKE_DOCKER_LOG = $previousFakeDockerLog
    }

    $dockerCalls = Get-Content -LiteralPath $fakeDockerLog -Encoding ASCII
    if ($dockerCalls -notcontains "info") {
        throw "O encerramento não conferiu o estado do motor local."
    }
    if ($dockerCalls -notcontains "compose stop --timeout 30") {
        throw "O encerramento não parou os componentes da aplicação."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $logs "encerramento.log"))) {
        throw "O script de encerramento não gerou o log esperado."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $logs "encerramento-launcher.log"))) {
        throw "O inicializador visual não gerou o log esperado."
    }

    Write-Output "STOP_EXPERIENCE=OK"
    Write-Output "LAUNCHER_EXIT=0"
    Write-Output "COMPOSE_STOP=True"
    Write-Output "DATA_DELETION=False"
    $completed = $true
} finally {
    if ($completed -and (Test-Path -LiteralPath $testRoot)) {
        Assert-SafeTestPath -Path $testRoot
        Remove-Item -LiteralPath $testRoot -Recurse -Force
    } elseif (-not $completed) {
        Write-Warning "Artefatos do teste preservados em: $testRoot"
    }
}
