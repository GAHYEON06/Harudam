param(
    [ValidateSet("start", "test")][string]$Action = "test",
    [string]$NodeDirectory = ""
)
$ErrorActionPreference = "Stop"
Push-Location $PSScriptRoot
$previousPath = $env:PATH
$previousProject = $env:TEST_PROJECT_ID
try {
    if ($NodeDirectory) { $env:PATH = "$NodeDirectory;$env:PATH" }
    if ((& node --version) -notmatch '^v22\.') { throw "Use Node 22, or pass -NodeDirectory pointing to its folder." }
    $env:TEST_PROJECT_ID = "demo-harudam"
    if ($Action -eq "test") {
        & npm run test:parser --prefix functions
        if ($LASTEXITCODE -ne 0) { throw "Parser tests failed." }
        & firebase emulators:exec --project demo-harudam --only auth,firestore,functions "node functions/testbackend.js"
    } else {
        & firebase emulators:start --project demo-harudam --only auth,firestore,functions
    }
    if ($LASTEXITCODE -ne 0) { throw "Firebase emulator command failed." }
} finally {
    $env:PATH = $previousPath
    $env:TEST_PROJECT_ID = $previousProject
    Pop-Location
}
