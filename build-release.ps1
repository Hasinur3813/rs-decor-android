$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$keystorePath = if ($env:KEYSTORE_PATH) {
    $env:KEYSTORE_PATH
} else {
    Join-Path $env:USERPROFILE "rs-decor-upload.jks"
}

if (-not (Test-Path -LiteralPath $keystorePath -PathType Leaf)) {
    throw "Upload keystore not found: $keystorePath"
}

function ConvertTo-PlainText([Security.SecureString]$SecureValue) {
    $pointer = [IntPtr]::Zero
    try {
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        if ($pointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
    }
}

$storePassword = Read-Host "Enter the keystore password" -AsSecureString
$keyPassword = Read-Host "Enter the upload key password" -AsSecureString
$exitCode = 1

try {
    $env:KEYSTORE_PATH = $keystorePath
    $env:STORE_PASSWORD = ConvertTo-PlainText $storePassword
    $env:KEY_PASSWORD = ConvertTo-PlainText $keyPassword

    Push-Location $projectRoot
    try {
        & .\gradlew.bat :app:bundleRelease
        $exitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    Remove-Item Env:KEYSTORE_PATH -ErrorAction SilentlyContinue
    Remove-Item Env:STORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:KEY_PASSWORD -ErrorAction SilentlyContinue
    $storePassword.Dispose()
    $keyPassword.Dispose()
}

if ($exitCode -ne 0) {
    exit $exitCode
}

Write-Host "Signed release bundle: $projectRoot\app\build\outputs\bundle\release\app-release.aab"
