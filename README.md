# Fractala

## Running the API server

To run the server, use:

```ps1
sbt "project api" run
```

The application uses **PureConfig** for configuration management. You can find the default settings in: `api/src/main/resources/application.conf`.

To override settings without changing the file, you can use environment variables:

**Windows (PowerShell):**
```powershell
$env:SERVER_PORT="9000"; sbt "project api" run
```
