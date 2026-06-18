$ErrorActionPreference = 'Stop'

$repo = Resolve-Path (Join-Path $PSScriptRoot '..')
$tmpRoot = Join-Path $env:TEMP 'qf-backend-worker-tests'

if (Test-Path $tmpRoot) {
  Remove-Item -LiteralPath $tmpRoot -Recurse -Force
}

robocopy $repo $tmpRoot /E /XD node_modules .wrangler | Out-Null
Copy-Item -Recurse -Force (Join-Path $repo 'node_modules') (Join-Path $tmpRoot 'node_modules')

Push-Location $tmpRoot
try {
  npm test
} finally {
  Pop-Location
}
