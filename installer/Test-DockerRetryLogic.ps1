[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$installerScript = Join-Path $PSScriptRoot "Instalar-JudicialPipeline.ps1"
$tokens = $null
$parseErrors = $null
$syntaxTree = [System.Management.Automation.Language.Parser]::ParseFile(
    $installerScript,
    [ref]$tokens,
    [ref]$parseErrors)

if ($parseErrors.Count -gt 0) {
    throw ($parseErrors | Out-String)
}

$requiredFunctions = @(
    "Test-TransientDockerPreparationFailure",
    "Start-ApplicationEnvironment"
)

foreach ($functionName in $requiredFunctions) {
    $definition = $syntaxTree.Find(
        {
            param($node)
            $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
                $node.Name -eq $functionName
        },
        $true)

    if (-not $definition) {
        throw "A função obrigatória '$functionName' não foi encontrada."
    }

    . ([scriptblock]::Create($definition.Extent.Text))
}

$testRoot = Join-Path $env:TEMP (
    "JudicialPipelineDockerRetryTests-" + [Guid]::NewGuid().ToString("N"))
$transientLog = Join-Path $testRoot "transient.log"
$permanentLog = Join-Path $testRoot "permanent.log"

try {
    New-Item -ItemType Directory -Path $testRoot -Force | Out-Null

    $script:dockerAttempt = 0
    function Invoke-DockerCommand {
        param(
            [string[]]$Arguments,
            [string]$OutputPath,
            [switch]$AppendOutput
        )

        $script:dockerAttempt++
        if ($script:dockerAttempt -lt 3) {
            "short read: unexpected EOF" | Add-Content -LiteralPath $OutputPath -Encoding UTF8
            return 1
        }

        "ambiente iniciado" | Add-Content -LiteralPath $OutputPath -Encoding UTF8
        return 0
    }

    function Start-Sleep {
        param([int]$Seconds)
        # O teste valida a repetição sem introduzir esperas reais.
    }

    if (-not (Start-ApplicationEnvironment -LogPath $transientLog)) {
        throw "A falha transitória não foi recuperada."
    }
    if ($script:dockerAttempt -ne 3) {
        throw "Era esperado recuperar na terceira tentativa; foram feitas $script:dockerAttempt."
    }
    if ((Select-String -LiteralPath $transientLog -Pattern "===== Tentativa").Count -ne 3) {
        throw "O log não preservou as três tentativas."
    }

    $script:dockerAttempt = 0
    function Invoke-DockerCommand {
        param(
            [string[]]$Arguments,
            [string]$OutputPath,
            [switch]$AppendOutput
        )

        $script:dockerAttempt++
        "Dockerfile inválido" | Add-Content -LiteralPath $OutputPath -Encoding UTF8
        return 1
    }

    if (Start-ApplicationEnvironment -LogPath $permanentLog) {
        throw "Uma falha permanente foi tratada incorretamente como sucesso."
    }
    if ($script:dockerAttempt -ne 1) {
        throw "Uma falha permanente não deve ser repetida automaticamente."
    }

    Write-Output "DOCKER_RETRY_LOGIC=OK"
    Write-Output "TRANSIENT_ATTEMPTS=3"
    Write-Output "PERMANENT_ATTEMPTS=1"
} finally {
    if (Test-Path -LiteralPath $testRoot) {
        Remove-Item -LiteralPath $testRoot -Recurse -Force
    }
}
