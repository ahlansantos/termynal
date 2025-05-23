# TermySH - A Powerful Local-Based Shell Terminal for Android  

*(Formerly TermynalR LBV)*  

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org) 
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5.0-brightgreen)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](https://opensource.org/licenses/Apache-2.0)
[![Version](https://img.shields.io/badge/Version-0.0.6-important)]()

TermySH is an open-source Android terminal emulator focusing on secure, local command execution with native Android APIs.

---

## Key Features

- **Pure Kotlin** - No native dependencies
- **Math Package** - Advanced arithmetic operations
- **Shell Integration** - Safe `sh` command execution
- **Modern UI** - Jetpack Compose with Material3
- **Permission Control** - Granular access management

---

## ⚠ Deprecated Features (v0.0.6+)
- `shell` package - Replaced by native `sh` command
- `files` package - Use Android Storage Access Framework instead

---

## Command Reference

### Core Commands
| Command | Description |
|---------|-------------|
| `h` | Show help |
| `clear` | Clear terminal |
| `echo <text>` | Print text |
| `time` | System time |

### Math Package
```bash
math.add 5 3       # → 8
math.div 10 3      # → 3.333...
```

### Shell Execution
```bash
sh ls -l           # List files (sandboxed)
sh ping google.com  # Network test
su cmd             # Root commands (requires root)
```

### Permission Control
```bash
termy-request-permission  # Manage storage access
```

---

## Current Package System

| Package | Status | Description |
|---------|--------|-------------|
| `math`  | ✅ Active | Arithmetic operations |
| `shell` | ❌ Deprecated | Use native `sh` instead |
| `files` | ❌ Deprecated | Use Android Storage API |

---

## Security Notes
- `sh` commands run in app sandbox
- `su` requires rooted device
- All file access uses Android permission system

---

## Development
```bash
./gradlew assembleDebug
