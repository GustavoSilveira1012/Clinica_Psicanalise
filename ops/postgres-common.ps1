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
    [PSCustomObject]@{
        ConnectionString = $libpqUrl
        DatabaseName = [Uri]::UnescapeDataString($target.AbsolutePath.TrimStart('/'))
        SafeDescription = $target.Host + ':' + $target.Port + $target.AbsolutePath
    }
}
