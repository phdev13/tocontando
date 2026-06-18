param (
    [ValidateSet('DryRun', 'Apply', 'Validate')]
    [string]$Mode = 'DryRun'
)

$ErrorActionPreference = 'Stop'

$cfToken = $env:CLOUDFLARE_API_TOKEN
$hostingerToken = $env:HOSTINGER_API_TOKEN
$accountId = "f165980a213718a04990d66be1772f66"
$domain = "quantofalta.shop"

if (-not $cfToken) {
    Write-Error "CLOUDFLARE_API_TOKEN environment variable is missing."
}

$headers = @{
    "Authorization" = "Bearer $cfToken"
    "Content-Type"  = "application/json"
}

function Get-Zone {
    $response = Invoke-RestMethod -Uri "https://api.cloudflare.com/client/v4/zones?name=$domain" -Headers $headers -Method Get
    return $response.result | Select-Object -First 1
}

function Create-Zone {
    if ($Mode -eq 'DryRun' -or $Mode -eq 'Validate') {
        Write-Host "[DryRun/Validate] Would create zone $domain" -ForegroundColor Cyan
        return $null
    }
    
    $body = @{
        name = $domain
        account = @{ id = $accountId }
        type = "full"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "https://api.cloudflare.com/client/v4/zones" -Headers $headers -Method Post -Body $body
        Write-Host "Zone created successfully." -ForegroundColor Green
        return $response.result
    } catch {
        Write-Error "Failed to create zone: $_"
    }
}

function Update-HostingerNameservers($nameservers) {
    if (-not $hostingerToken) {
        Write-Warning "HOSTINGER_API_TOKEN not found. Please manually update nameservers at Hostinger to:"
        $nameservers | ForEach-Object { Write-Warning " - $_" }
        return
    }

    if ($Mode -eq 'DryRun' -or $Mode -eq 'Validate') {
        Write-Host "[DryRun/Validate] Would update Hostinger nameservers to: $($nameservers -join ', ')" -ForegroundColor Cyan
        return
    }

    # Implement Hostinger API call here if docs were available
    # Currently assuming manual update is fallback
    Write-Warning "Hostinger automated nameserver update is pending API implementation. Please manually set NS to: $($nameservers -join ', ')"
}

Write-Host "Starting domain configuration for $domain in mode: $Mode" -ForegroundColor Magenta

# 1. Zone Management
$zone = Get-Zone
if (-not $zone -and $Mode -eq 'Apply') {
    $zone = Create-Zone
} elseif (-not $zone) {
    Write-Host "Zone $domain does not exist yet." -ForegroundColor Yellow
} else {
    Write-Host "Zone $domain found. Status: $($zone.status)" -ForegroundColor Green
}

if ($zone) {
    Write-Host "Zone ID: $($zone.id)"
    Write-Host "Nameservers: $($zone.name_servers -join ', ')"
    
    # 2. Nameservers
    if ($zone.status -eq 'pending') {
        Update-HostingerNameservers $zone.name_servers
    }
}

if ($Mode -eq 'Validate') {
    # 14. Validation
    Write-Host "Validating endpoints..." -ForegroundColor Cyan
    $endpoints = @(
        "https://quantofalta.shop",
        "https://api.quantofalta.shop/health",
        "https://admin.quantofalta.shop",
        "https://share.quantofalta.shop"
    )

    foreach ($ep in $endpoints) {
        try {
            $res = Invoke-WebRequest -Uri $ep -UseBasicParsing -TimeoutSec 10
            Write-Host "[$($res.StatusCode)] $ep" -ForegroundColor Green
        } catch {
            Write-Host "[FAIL] $ep - $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host "Configuration script finished." -ForegroundColor Magenta
