# rebuild.ps1
Write-Host "Cleaning old builds..."
./gradlew clean

Write-Host "Building project..."
./gradlew build

Write-Host "Installing distribution..."
./gradlew installDist

Write-Host "Done! Run your app from:"
Write-Host "   build/install/queuectl/bin/queuectl"
