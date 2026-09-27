function Get-PostgresTarget {
    param([Parameter(Mandatory = $true)][string]$DatabaseUrl)
    $libpqUrl = $DatabaseUrl -replace '^jdbc:', ''
    $target = $null
    if (-not [Uri]::TryCreate($libpqUrl, [UriKind]::Absolute, [ref]$target) -or
        $target.Scheme -notin @('postgresql', 'postgres') -or
        [string]::IsNullOrWhiteSpace($target.Host) -or
        $target.AbsolutePath -eq '/' -or $target.UserInfo -or
        $target.Query -match '(?i)(password|user)=') {
        throw 'DATABASE_URL deve identificar explicitamente host e banco, sem credenciais na URL.'
    }
    if ($target.Query -cnotmatch '^\?sslmode=verify-full$') {
        throw 'DATABASE_URL deve usar somente sslmode=verify-full; parâmetros adicionais ou TLS mais fraco são proibidos.'
    }
    $port = if ($target.Port -lt 1) { 5432 } else { $target.Port }
    [PSCustomObject]@{
        ConnectionString = $libpqUrl
        DatabaseName = [Uri]::UnescapeDataString($target.AbsolutePath.TrimStart('/'))
        Host = $target.DnsSafeHost.TrimEnd('.').ToLowerInvariant()
        Port = $port
        SafeDescription = $target.Host + ':' + $port + $target.AbsolutePath
    }
}

function Assert-PostgresTargetMatches {
    param(
        [Parameter(Mandatory = $true)][PSObject]$Target,
        [Parameter(Mandatory = $true)][string]$ExpectedDatabaseName,
        [Parameter(Mandatory = $true)][string]$ExpectedHost,
        [Parameter(Mandatory = $true)][ValidateRange(1, 65535)][int]$ExpectedPort
    )

    if ($Target.DatabaseName -cne $ExpectedDatabaseName) {
        throw 'Nome do banco de destino não corresponde a ExpectedDatabaseName. Restore cancelado.'
    }
    if ([string]::IsNullOrWhiteSpace($ExpectedHost)) {
        throw 'ExpectedHost é obrigatório. Restore cancelado.'
    }
    $normalizedExpectedHost = $ExpectedHost.Trim().TrimEnd('.').ToLowerInvariant()
    if ($Target.Host -cne $normalizedExpectedHost -or $Target.Port -ne $ExpectedPort) {
        throw 'Servidor/porta de destino não correspondem a ExpectedHost/ExpectedPort. Restore cancelado.'
    }
}

function Remove-ExpiredPostgresBackups {
    [CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'High')]
    param(
        [Parameter(Mandatory = $true)][string]$Directory,
        [Parameter(Mandatory = $true)][datetime]$Before
    )

    $resolvedDirectory = [System.IO.Path]::GetFullPath($Directory)
    if (-not (Test-Path -LiteralPath $resolvedDirectory -PathType Container)) {
        return
    }

    $expiredBackups = Get-ChildItem -LiteralPath $resolvedDirectory -Filter 'psicogest-*.dump.age' -File |
        Where-Object { $_.LastWriteTime -lt $Before }
    foreach ($expiredBackup in $expiredBackups) {
        if ($PSCmdlet.ShouldProcess($expiredBackup.FullName, 'Excluir backup age expirado')) {
            Remove-Item -LiteralPath $expiredBackup.FullName -Force
            $expiredManifest = "$($expiredBackup.FullName).sha256"
            if (Test-Path -LiteralPath $expiredManifest -PathType Leaf) {
                Remove-Item -LiteralPath $expiredManifest -Force
            }
        }
    }
}

function Invoke-BinaryPipe {
    param(
        [Parameter(Mandatory = $true)][string]$SourceExecutable,
        [Parameter(Mandatory = $true)][string[]]$SourceArguments,
        [Parameter(Mandatory = $true)][string]$DestinationExecutable,
        [Parameter(Mandatory = $true)][string[]]$DestinationArguments,
        [hashtable]$SourceEnvironment = @{},
        [hashtable]$DestinationEnvironment = @{}
    )

    $sourceInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $sourceInfo.FileName = $SourceExecutable
    $sourceInfo.UseShellExecute = $false
    $sourceInfo.CreateNoWindow = $true
    $sourceInfo.RedirectStandardOutput = $true
    $sourceInfo.RedirectStandardError = $true
    foreach ($argument in $SourceArguments) { [void]$sourceInfo.ArgumentList.Add($argument) }
    foreach ($entry in $SourceEnvironment.GetEnumerator()) {
        $sourceInfo.Environment[$entry.Key] = [string]$entry.Value
    }

    $destinationInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $destinationInfo.FileName = $DestinationExecutable
    $destinationInfo.UseShellExecute = $false
    $destinationInfo.CreateNoWindow = $true
    $destinationInfo.RedirectStandardInput = $true
    $destinationInfo.RedirectStandardError = $true
    foreach ($argument in $DestinationArguments) { [void]$destinationInfo.ArgumentList.Add($argument) }
    foreach ($entry in $DestinationEnvironment.GetEnumerator()) {
        $destinationInfo.Environment[$entry.Key] = [string]$entry.Value
    }

    $source = [System.Diagnostics.Process]::new()
    $destination = [System.Diagnostics.Process]::new()
    $destinationStarted = $false
    $sourceStarted = $false
    try {
        $destination.StartInfo = $destinationInfo
        $destinationStarted = $destination.Start()
        if (-not $destinationStarted) { throw 'Não foi possível iniciar o processo de destino.' }
        $destinationError = $destination.StandardError.ReadToEndAsync()

        $source.StartInfo = $sourceInfo
        $sourceStarted = $source.Start()
        if (-not $sourceStarted) { throw 'Não foi possível iniciar o processo de origem.' }
        $sourceError = $source.StandardError.ReadToEndAsync()

        $null = $source.StandardOutput.BaseStream.CopyToAsync($destination.StandardInput.BaseStream).GetAwaiter().GetResult()
        $destination.StandardInput.Close()
        $source.WaitForExit()
        $destination.WaitForExit()
        $null = $sourceError.GetAwaiter().GetResult()
        $null = $destinationError.GetAwaiter().GetResult()

        if ($source.ExitCode -ne 0 -or $destination.ExitCode -ne 0) {
            throw 'O pipeline binário falhou; nenhum conteúdo de saída deve ser considerado válido.'
        }
    } finally {
        if ($sourceStarted -and -not $source.HasExited) { $source.Kill($true) }
        if ($destinationStarted -and -not $destination.HasExited) { $destination.Kill($true) }
        $source.Dispose()
        $destination.Dispose()
    }
}
