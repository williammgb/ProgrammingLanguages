### Usage
Running a `.ps1` file (if it is in current directory):
```powershell
.\script_name.ps1
```

Get system information
```powershell
Get-ComputerInfo
```

Find commands based on keys
```powershell
Get-Command *process*
```

Get overview of what you can do with an object
```powershell
Get-Member
```

Note: PowerShell is case insensitive for the main part.
## Input
Pass arguments to script
```powershell
param($name)
Write-Output "Hello $name"
# run
.\script_name.ps1 Peter
```

## Output
Print output to terminal
```powershell
Write-Output "Hello, PowerShell!"
# or
echo "Hello again"
```

## Files and folders
Print current directory
```powershell
Get-Location
```

Change directory
```powershell
Set-Location .. # goes 1 directory back
Set-Location folder_name
# or 
cd ..
```
Show all files in current directory
```powershell
Get-ChildItem
# or
ls
# or (to show hidden items)
Get-ChildItem -Force
```

Create a file
```powershell
New-Item testfile.txt
```

Write to file (overwrites each time)
```powershell
"Hello file" > testfile.txt
# or
"Hello file" | Out-File testfile.txt
# to append
"Additional data" | Out-File testfile.txt -Append
```

Read file
```powershell
Get-Content testfile.txt
```

Erase file content
```powershell
Clear-Content testfile.txt
```

Create a folder
```powershell
New-Item testfolder -ItemType Directory
```

Copy/move file/folder
```powershell
Copy-Item testfile.txt testfolder
# or 
Move-Item
```

Remove file/folder
```powershell
Remove-Item .\testfolder\testfile.txt
```

Rename file/folder
```powershell
Rename-Item testfile.txt test.txt
```

Check file/folder existence
```powershell
Test-Path path # True or False
```

## Loops and conditionals
For-loop
```powershell
for ($i=1; $i -le 5; $i++) {
    Write-Output "Number $i"
}
```

If-statement
```powershell
$number = 10 # variable assignment

if ($number -gt 5) {
    Write-Output "Greater than 5"
}
```

## Objects and variables
Variable assignment
```powershell
$name = "Bob"
$age = 25
```

Creating an object
```powershell
$user = [PSCustomObject]@{
    Name = "Bob"
    Age = 25
    Role = "Admin"
}

Write-Output $user.Name
$user.Role = "User" # overwriting properties
```

## Arrays
Creating arrays
```powershell
$numbers = 1,2,3,4,5
# or
$names = @("A", "B", "C")

$names[1] # B
$names.Count # 3
```

Adding items to arrays
```powershell
$names = $names + "D"
# or
$names += "D"
# note: this is fine for small arrays but inefficient for large ones.
# because arrays are fixed size, so you need to create a new array.
```

Array operations
```powershell
$names | Sort-Object
$names | Select-Object -First 2
$names | Select-Object -Last 1
```



## System data
```powershell
Get-Date
# Thursday, April 23, 2026 8:07:41 PM
Get-Date -displayHint time
# 8:07:41 PM
```


## Processes
Print all running processes
```powershell
Get-Process
```
Properties
- Handles: Number of open references the process has to system resources (files, keys, threads,etc.)
- NPM: Memory the process is using that cannot be swapped to the disk and must stay in RAM.
- PM: Memory that can be moved to disk when not actively used.
- WS: Amount of physical RAM currently being used by the process.
- CPU: Total processor time the process has consumed since it started.


Filter processes ($_ is the object, so individual process in this case)
```powershell
Get-Process | Where-Object {$_.CPU -gt 10}
```

Select properties of an object
```powershell
Get-Process | Select-Object Name, CPU
```
Sort
```powershell
Get-Process | Sort-Object CPU -Descending
```

Select first 5
```powershell
Get-Process | Sort-Object CPU -Descending | Select-Object -First 5
```

## Automation
```powershell
Export-Csv # save object as csv
Import-Csv # read csv into object
Restart-Service
```

JSON
```powershell
$data = Get-Content "testfile.json" | Convert-From-Json # read JSON file

$data | ConvertTo-Json | Out-File "testfile.json" # write JSON file
```

.SYNOPSIS is short description of what a function does. Helps other people understand the function.
```powershell
function Say-Hello {
<#
.SYNOPSIS
Says hello to user.
#>
    "Hello!"
}

Get-Help Say-Hello # prints "Says hello to user."
```