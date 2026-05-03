# PowerShell Automation Pipeline

This project demonstrates a modular PowerShell automation flow driven by a Python FastAPI backend. The script reads a JSON configuration file into a PowerShell object, collects user login input, optionally encrypts the password, validates the user against API credential data, retrieves JSON logs, filters them based on user-selected criteria, and stores the final result in an output file. It includes many of the common tools and functions used in PowerShell automation.

An overview of PowerShell commands can be found in `notes.md`.


## Usage

Install dependencies:

```powershell
pip install -r requirements.txt
```

Run:

```powershell
.\main.ps1

.\main.ps1 -EncryptPassword # add (simple) password encryption

.\main.ps1 -OutputFile "output\custom-logs.json" # run with custom output path
```


