# DAECC / Homework 01 / Team 3 / Solutions
Our group used different cloud providers, these are the results for Azure.


## Task 1
The account created uses a Microsoft Azure Student subscription and belongs to the `uibkacat.onmicrosoft.com` tenant.
The following resources/roles have been created:
- Two storage accounts, one in East Us (Virginia) and one in Germany West Central (Frankfurt), both without geo-redundancy.
- Two Azure Function App pools, also in both regions, running under a system assigned identity, i.e. registered in Azure Active Directory.
- Both app pools have the _Reader and Data Access_ role associated with their identities on both storage accounts.
- A container `download` in the Virginia storage pool, an a container `upload` in the Frankfurt storage pool.


## Task 2
Instead of using the provided code, the suggested Monte Carlo method for approximating π was implemented.
The function expects `n` as the number of iterations from the query string, and returns a JSON object including the estimated `pi` and `time` taken in ticks.
The endpoints are published under https://daeccfrankfurtpowershell.azurewebsites.net/api/PiMonteCarlo and https://daeccvirginiapowershell.azurewebsites.net/api/PiMonteCarlo, both requiring a function key.

```powershell
using namespace System.Net

param($Request)

Set-StrictMode -Version Latest
$ErrorActionPreference = [System.Management.Automation.ActionPreference]::Stop

# get and check the number of iterations
try {
    $iterations = [int]$Request.Query.n
    if ($iterations -lt 1) {
        throw
    }
}
catch {
    Push-OutputBinding -Name Response -Value ([HttpResponseContext]@{
        StatusCode = [HttpStatusCode]::BadRequest
        Body = "Invalid or missing number of iterations 'n'."
    })
    return
}

# approximate pi and measure the time
try {
    Push-OutputBinding -Name Response -Value ([HttpResponseContext]@{
        StatusCode = [HttpStatusCode]::OK
        Body = @{
            time = (Measure-Command {
                $circle_points = 0
                $square_points = 0
                while ($iterations-- -gt 0) {
                    $x = Get-Random -Minimum 0.0 -Maximum 1.0
                    $y = Get-Random -Minimum 0.0 -Maximum 1.0
                    $d = ($x * $x) + ($y * $y)
                    if ($d -le 1) { $circle_points++ }
                    $square_points++
                }
                $pi = 4 * ($circle_points / $square_points)
            }).Ticks
            pi = $pi
        }
    })
}
catch {
    Push-OutputBinding -Name Response -Value ([HttpResponseContext]@{
        StatusCode = [HttpStatusCode]::InternalServerError
        Body = "Operation failed: $_"
    })
}
```


## Task 3
The download and upload function is also implemented in PowerShell.
It expects a `filename` in the query string, which it uses to locate the source blob in the `download` container in the Virginia storage account.
It then downloads the blob to a temporary file, and uploads it to the `upload` container in the Frankfurt storage account.
The endpoints are published under https://daeccfrankfurtpowershell.azurewebsites.net/api/DownUp and https://daeccvirginiapowershell.azurewebsites.net/api/DownUp, both requiring a function key.

```powershell
using namespace System.Net

param($Request)

Set-StrictMode -Version Latest
$ErrorActionPreference = [System.Management.Automation.ActionPreference]::Stop

# get the name of the blob to down- and upload
$blobName = ""
if (-not $Request.Query.TryGetValue('filename', [ref]$blobName)) {
    Push-OutputBinding -Name Response -Value ([HttpResponseContext]@{
        StatusCode = [HttpStatusCode]::BadRequest
        Body = "Parameter 'fileName' is missing."
    })
    return
}

# create a temporary file
$tmpFile = New-TemporaryFile
try {
    Push-OutputBinding -Name Response -Value ([HttpResponseContext]@{
        StatusCode = [HttpStatusCode]::OK
        Body = @{
            source = @{
                # download from Virginia
                login = (Measure-Command { $sourceAccount = Get-AzStorageAccount -ResourceGroupName 'daecc' -Name 'daeccvirginia' }).Ticks
                locate = (Measure-Command { $sourceContainer = Get-AzStorageContainer -Name 'download' -Context $sourceAccount.Context }).Ticks
                transfer = (Measure-Command { $sourceContainer | Get-AzStorageBlobContent -Blob $blobName -Destination $tmpFile.FullName -Force }).Ticks
            }
            destination = @{
                # upload to Frankfurt
                login = (Measure-Command { $destinationAccount = Get-AzStorageAccount -ResourceGroupName 'daecc' -Name 'daeccfrankfurt' }).Ticks
                locate = (Measure-Command { $destinationContainer = Get-AzStorageContainer -Name 'upload' -Context $destinationAccount.Context }).Ticks
                transfer = (Measure-Command { $destinationContainer | Set-AzStorageBlobContent -File $tmpFile.FullName -Blob $blobName -Force }).Ticks
            }
        }
    })
}
catch {
    Push-OutputBinding -Name Response -Value ([HttpResponseContext]@{
        StatusCode = [HttpStatusCode]::InternalServerError
        Body = "Operation failed: $_"
    })
}
finally {
    $tmpFile | Remove-Item
}

```


## Task 4
In addition to parameters `n` and `k`, the region can be specified as well with `l`.

```powershell
param (
    [Parameter(Mandatory=$true)]
    [ValidateRange(1, 1000)]
    [Alias('k')]
    [int] $Invocations
,
    [ValidateRange(1, 100000)]
    [Alias('n')]
    [int] $Approximations = 1000
,
    [ValidateSet('frankfurt', 'virginia')]
    [Alias('l')]
    [string] $Location = 'frankfurt'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = [System.Management.Automation.ActionPreference]::Stop

$keys = @{
    frankfurt = '...'
    virginia  = '...'
}

for ($i = 0; $i -lt $Invocations; $i++) {
    Write-Progress -Activity PiMonteCarlo -PercentComplete (($i * 100) / $Invocations)
    $rtt = Measure-Command {
        $result = Invoke-RestMethod -Method Get -Uri "https://daecc$($Location)powershell.azurewebsites.net/api/PiMonteCarlo" -Body @{
            code = $keys[$Location]
            n    = $Approximations
        }
    }
    New-Object -TypeName psobject -Property @{
        Rtt = $rtt.TotalMilliseconds
        Cpu = [timespan]::FromTicks($result.time).TotalMilliseconds
        Pi  = $result.pi
    }
}
Write-Progress -Activity PiMonteCarlo -Completed
```


## Task 5
The script iterates over 7 pre-staged blobs:
 - One 0-byte file to check the metadata copy latency.
 - Three files with 1MB, 10MB and 100MB of random data.
 - Three files with 1MB, 10MB and 100MB of zeroes, to check if compression is used.

```powershell
param (
    [Parameter(Mandatory=$true)]
    [ValidateRange(1, 100)]
    [Alias('k')]
    [int] $Invocations
,
    [ValidateSet('frankfurt', 'virginia')]
    [Alias('l')]
    [string] $Location = 'frankfurt'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = [System.Management.Automation.ActionPreference]::Stop

$keys = @{
    frankfurt = '...'
    virginia  = '...'
}

$files = @('null.bin')
foreach ($c in '1','10','100') {
    $files += "rand_$($c)mb.bin","zero_$($c)mb.bin"
}

for ($fi = 0; $fi -lt $files.Length; $fi++) {
    $file = $files[$fi]
    for ($i = 0; $i -lt $Invocations; $i++) {
        Write-Progress -Activity DownUp -Status $file -PercentComplete (((($fi * $Invocations) + $i) * 100) / ($Invocations * $files.Length))
        $rtt = Measure-Command {
            $result = Invoke-RestMethod -Method Get -Uri "https://daecc$($Location)powershell.azurewebsites.net/api/DownUp" -Body @{
                code     = $keys[$Location]
                filename = $file
            }
        }
        New-Object -TypeName psobject -Property @{
            File = $file
            Rtt = $rtt.TotalMilliseconds
            DownGetAccount = [timespan]::FromTicks($result.source.login).TotalMilliseconds
            DownGetContainer = [timespan]::FromTicks($result.source.locate).TotalMilliseconds
            DownGetBlob = [timespan]::FromTicks($result.source.transfer).TotalMilliseconds
            UpGetAccount = [timespan]::FromTicks($result.destination.login).TotalMilliseconds
            UpGetContainer = [timespan]::FromTicks($result.destination.locate).TotalMilliseconds
            UpSetBlob = [timespan]::FromTicks($result.destination.transfer).TotalMilliseconds
        }
    }
}
Write-Progress -Activity DownUp -Completed
```


## Task 6
The first invocation was done after not using the service for some time. It therefore shows the effects of a cold start: The app pool took 18 seconds to start, and the first approximation of π took slightly longer to compute.
The latter can be due to .NET's JIT runtime and/or PowerShell's on-demand module loading.
Consecutive queries and the second invocation show little variance.

### First `k=10` invocations after long idle:

|   RTT [ms] | CPU [ms] |    Pi |
|-----------:|---------:|------:|
| 17919.4194 | 140.7371 | 3.072 |
|   173.1462 | 103.6707 | 3.136 |
|   112.1064 |  61.3000 | 3.148 |
|   136.7819 |  90.8580 | 3.216 |
|   125.1159 |  61.3611 | 3.100 |
|   123.0805 |  61.2751 | 3.124 |
|   119.1922 |  58.1713 | 3.136 |
|   122.1227 |  58.8925 | 3.240 |
|   110.0565 |  62.6334 | 3.156 |
|    99.5731 |  56.7708 | 3.240 |

### Following `k=10` invocations:

| RTT [ms] | CPU [ms] |    Pi |
|---------:|---------:|------:|
| 170.4376 |  61.2136 | 3.092 |
| 131.8380 |  71.0277 | 3.132 |
| 114.4396 |  58.1204 | 3.100 |
| 141.7529 |  75.3947 | 3.136 |
| 104.6762 |  60.6176 | 3.108 |
| 113.9564 |  57.9812 | 3.128 |
| 100.0030 |  58.1429 | 3.140 |
| 110.3582 |  56.4666 | 3.144 |
| 125.4613 |  65.6592 | 3.092 |
| 105.1429 |  61.5061 | 3.152 |


## Task 7
Since the script queries 7 different files, only two invocations per file will be executed, to not be too verbose.
Also, since we've seen the effect of app pool cold start in [task 6](#task-6) already, the first invocation will on be on a hot app pool but a cold function module, where no storage access happened prior for some time.

All times are in milliseconds. The source files are located in Virginia, the Function App and destination container reside in Frankfurt.

| File           | RTT       | Download   | Upload    |
|----------------|----------:|-----------:|----------:|
| null.bin       | 4004.6474 |   647.2386 |  577.8387 |
| null.bin       | 2253.9351 |   395.8585 |   30.3470 |
| rand_1mb.bin   | 3169.3074 |  1235.2336 |  128.0296 |
| rand_1mb.bin   | 2813.7021 |   769.3707 |  240.3956 |
| zero_1mb.bin   | 2394.1488 |   563.8923 |  128.9546 |
| zero_1mb.bin   | 3387.1495 |   572.9702 |  194.6694 |
| rand_10mb.bin  | 4185.5988 |  2014.7196 |  489.2262 |
| rand_10mb.bin  | 2960.5032 |   924.7355 |  440.7231 |
| zero_10mb.bin  | 3480.0055 |  1489.7960 |  494.8712 |
| zero_10mb.bin  | 2451.3903 |   650.9915 |  330.9256 |
| rand_100mb.bin | 5836.8281 |  2572.1387 | 1419.8062 |
| rand_100mb.bin | 6436.8495 |  2602.3837 | 1435.6903 |
| zero_100mb.bin | 5728.1196 |  2502.0036 | 1426.4695 |
| zero_100mb.bin | 6655.8517 |  2921.4875 | 1651.2230 |

We can make the following observations:

1. The first invocation shows the delay of the first access to the containers.
2. The second invocation gives us a rough baseline of the time spent with reading and writing file metadata. 
3. Files containing random bytes consistently take longer to download than files containing the same amount of zeroes, so transferring data inter-continentally uses some compression.
4. Downloading smaller files (<100MB) a second time takes less time, so there is likely some cache involved.
5. Uploading to the same region is more or less only file-size dependent, what one would expect.


## Task 8
Alas, Azure doesn't support setting memory limits on Function Apps running in the Consumption Plan.
As an alternative, both functions have been deployed to Virginia to compare the results with Frankfurt. The same idle times and startup conditions as in [task 6](#task-6) and [task 7](#task-7) have been in effect.

### Invocation of PiMonteCarlo in Virginia:

|   RTT [ms] | CPU [ms] |    Pi |
|-----------:|---------:|------:|
| 24041.0965 | 318.4519 | 3.196 |
|   377.0609 | 177.6672 | 3.092 |
|   378.1640 | 175.9320 | 3.200 |
|   304.7690 | 147.6753 | 3.104 |
|   278.1716 | 133.0282 | 3.132 |
|   301.2730 | 115.0461 | 3.168 |
|   496.2006 | 333.7812 | 3.192 |
|   700.0321 | 440.1109 | 3.116 |
|   708.4139 | 511.1308 | 3.208 |
|   305.8302 | 151.4438 | 3.080 |

Again, the effects of cold start are clearly visible. But in contrast to Frankfurt, the calculation time is more volatile and slower in general.
The reason behind that is of course hard to say, since there is no access to the host executing the function. One guess might be that Virginia, as the default East US region, has more Function Apps deployed than Germany West Central.
At least π is the same in Virginia, so sanity ✅ :)

### Invocation of DownUp in Virginia:

| File           | RTT        | Download    | Upload     |
|----------------|-----------:|------------:|-----------:|
| null.bin       | 19622.4156 |   1273.9482 |   670.2733 |
| null.bin       |  5300.6464 |     53.9868 |   330.2690 |
| rand_1mb.bin   |  4367.0755 |    185.4802 |  1787.2988 |
| rand_1mb.bin   |  5895.4258 |     82.2104 |   943.4754 |
| zero_1mb.bin   |  4395.8990 |     78.0178 |  1367.0345 |
| zero_1mb.bin   |  5819.2439 |     71.9827 |  1321.7568 |
| rand_10mb.bin  |  9749.8599 |    266.7055 |  7383.3078 |
| rand_10mb.bin  | 14075.4600 |    204.7072 | 10800.8769 |
| zero_10mb.bin  | 10976.6605 |    250.9293 |  8020.8111 |
| zero_10mb.bin  | 12667.6128 |   1216.2674 |  8745.9812 |
| rand_100mb.bin | 37393.6980 |   2789.8175 | 30831.0898 |
| rand_100mb.bin | 29496.7525 |   1954.8543 | 25313.3612 |
| zero_100mb.bin | 33179.3222 |   2676.2332 | 27273.1009 |
| zero_100mb.bin | 34599.7653 |   1910.2374 | 27573.0776 |

Just as with [PiMonteCarlo](#invocation-of-pimontecarlo-in-virginia), the time taken fluctuates more than in Frankfurt. But there are more severe differences:
1. In contrast to downloading from Virginia, uploading to Frankfurt takes more than 10 times longer.
2. Inter-continental uploading doesn't seem to use compression, unlike downloading.
3. File IO within the Virginia region is slower than file IO within the Frankfurt region, but has less latency.
