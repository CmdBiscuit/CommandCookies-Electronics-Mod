param(
    [ValidateSet('main', 'retro')][string]$Mode = 'main',
    [switch]$CheckOnly
)
$ErrorActionPreference = 'Stop'
try {
    $projectRoot = $PSScriptRoot
    $wrapper = Join-Path $projectRoot 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $wrapper)) { throw 'Keep this launcher inside the CEM-Standalone project folder.' }

    $candidates = [Collections.Generic.List[string]]::new()
    if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($javaCommand) { $candidates.Add((Split-Path -Parent (Split-Path -Parent $javaCommand.Source))) }
    $searchRoots = @(
        (Join-Path $env:USERPROFILE '.jdks'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Java'),
        (Join-Path $env:ProgramFiles 'Microsoft'),
        (Join-Path $env:USERPROFILE '.mcreator\gradle\jdks')
    )
    foreach ($directory in $searchRoots) {
        if (Test-Path -LiteralPath $directory) {
            foreach ($jdk in Get-ChildItem -LiteralPath $directory -Directory -ErrorAction SilentlyContinue) { $candidates.Add($jdk.FullName) }
        }
    }
    $selected = $null
    foreach ($candidate in $candidates | Select-Object -Unique) {
        $release = Join-Path $candidate 'release'
        if ((Test-Path -LiteralPath (Join-Path $candidate 'bin\java.exe')) -and (Test-Path -LiteralPath (Join-Path $candidate 'bin\javac.exe')) -and (Test-Path -LiteralPath $release)) {
            $metadata = [IO.File]::ReadAllText($release)
            if ($metadata -match '(?m)^JAVA_VERSION="21(?:\.|[+"]|$)') { $selected = $candidate; break }
        }
    }
    if (-not $selected) { throw 'Java 21 JDK was not found. Install a Java 21 JDK, then run this launcher again.' }
    $env:JAVA_HOME = $selected

    # Reuse an existing local dependency cache when available; it is optional.
    $existingCache = Join-Path $env:USERPROFILE '.mcreator\gradle'
    if (-not $env:GRADLE_USER_HOME -and (Test-Path -LiteralPath (Join-Path $existingCache 'wrapper\dists'))) { $env:GRADLE_USER_HOME = $existingCache }
    $task = if ($Mode -eq 'retro') { ':retro-expansion:runClient' } else { ':main:runClient' }
    Write-Host "Project: $projectRoot"
    Write-Host "Java 21: $selected"
    Write-Host $(if ($Mode -eq 'retro') { 'Starting Minecraft with the main mod and Retro Expansion.' } else { 'Starting Minecraft with the main mod.' })
    if ($CheckOnly) {
        Write-Host "Launch setup verified: $task"
        exit 0
    }
    Set-Location -LiteralPath $projectRoot
    & $wrapper --no-daemon $task
    exit $LASTEXITCODE
} catch {
    Write-Host "Could not start Minecraft: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
