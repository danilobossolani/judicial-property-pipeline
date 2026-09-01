[CmdletBinding()]
param(
    [string]$DataJudApiKey = $env:DATAJUD_API_KEY,

    [string]$OutputPath = (Join-Path $PSScriptRoot "..\dist\Judicial-Pipeline-Instalador.exe")
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$outputFullPath = [System.IO.Path]::GetFullPath($OutputPath)
$temporaryDirectory = Join-Path $env:TEMP ("judicial-pipeline-installer-build-" + [Guid]::NewGuid().ToString("N"))

if ([string]::IsNullOrWhiteSpace($DataJudApiKey)) {
    throw "Defina DATAJUD_API_KEY no ambiente antes de gerar o instalador personalizado."
}
if ($DataJudApiKey.Contains("`r") -or $DataJudApiKey.Contains("`n")) {
    throw "DATAJUD_API_KEY contém caracteres inválidos."
}

try {
    New-Item -ItemType Directory -Path $temporaryDirectory -Force | Out-Null
    New-Item -ItemType Directory -Path ([System.IO.Path]::GetDirectoryName($outputFullPath)) -Force | Out-Null

    $compilerCandidates = @(
        "$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\csc.exe",
        "$env:WINDIR\Microsoft.NET\Framework\v4.0.30319\csc.exe"
    )
    $compiler = $compilerCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $compiler) {
        throw "O compilador do Windows não foi encontrado."
    }

    $iconPath = Join-Path $PSScriptRoot "assets\judicial-pipeline-icon.ico"
    if (-not (Test-Path -LiteralPath $iconPath)) {
        throw "O ícone do instalador não foi encontrado."
    }

    $launcherPath = Join-Path $temporaryDirectory "Judicial Pipeline.exe"
    $launcherSource = Join-Path $PSScriptRoot "JudicialPipelineLauncher.cs"
    & $compiler `
        /nologo `
        /target:winexe `
        /optimize+ `
        "/out:$launcherPath" `
        "/win32icon:$iconPath" `
        /reference:System.Windows.Forms.dll `
        /reference:System.Drawing.dll `
        $launcherSource

    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $launcherPath)) {
        throw "O inicializador visual não foi gerado."
    }

    $payloadPath = Join-Path $temporaryDirectory "payload.zip"
    Push-Location $projectRoot
    try {
        & git archive --format=zip --prefix=Judicial-Pipeline/ --output=$payloadPath HEAD
        if ($LASTEXITCODE -ne 0) {
            throw "Não foi possível montar o conteúdo versionado do instalador."
        }
    } finally {
        Pop-Location
    }

    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::Open($payloadPath, [System.IO.Compression.ZipArchiveMode]::Update)
    try {
        $entry = $archive.CreateEntry(
            "Judicial-Pipeline/installer/installer-settings.env",
            [System.IO.Compression.CompressionLevel]::Optimal)
        $stream = $entry.Open()
        $writer = New-Object System.IO.StreamWriter($stream, (New-Object System.Text.UTF8Encoding($false)))
        try {
            $writer.WriteLine("DATAJUD_API_KEY=$DataJudApiKey")
        } finally {
            $writer.Dispose()
        }

        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $archive,
            $launcherPath,
            "Judicial-Pipeline/Judicial Pipeline.exe",
            [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    } finally {
        $archive.Dispose()
    }

    $bootstrapperSource = Join-Path $PSScriptRoot "Bootstrapper.cs"
    & $compiler `
        /nologo `
        /target:winexe `
        /optimize+ `
        "/out:$outputFullPath" `
        "/win32icon:$iconPath" `
        "/resource:$payloadPath,JudicialPipeline.Payload.zip" `
        /reference:System.Windows.Forms.dll `
        /reference:System.IO.Compression.dll `
        /reference:System.IO.Compression.FileSystem.dll `
        $bootstrapperSource

    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $outputFullPath)) {
        throw "O executável do instalador não foi gerado."
    }

    $hash = Get-FileHash -LiteralPath $outputFullPath -Algorithm SHA256
    $hashPath = "$outputFullPath.sha256.txt"
    "$($hash.Hash)  $([System.IO.Path]::GetFileName($outputFullPath))" |
        Set-Content -LiteralPath $hashPath -Encoding ASCII

    [pscustomobject]@{
        Installer = $outputFullPath
        SizeBytes = (Get-Item -LiteralPath $outputFullPath).Length
        Sha256 = $hash.Hash
        HashFile = $hashPath
    }
} finally {
    if (Test-Path -LiteralPath $temporaryDirectory) {
        Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force
    }
}
