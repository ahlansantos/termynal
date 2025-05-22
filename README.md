# TermySH - A Open-Sourced Local-Based Shell Terminal

*(Formerly TermynalR LBV)*

TermySH is a simple, open-source local-based shell terminal emulator app for Android. It supports its own command language, Termy, and local packages that extend the terminal’s functionality without relying on external dependencies.

---

## Technical Documentation

### Overview

* Developed entirely in **Kotlin** with **Jetpack Compose**.
* Provides a terminal interface with input/output areas.
* Supports internal commands and modular local packages such as `math`, `shell`, and `files`.
* Designed to be self-contained: all packages and command executions happen locally within the app.

---

### Code Structure

* **MainActivity**: Initializes the UI, sets edge-to-edge layout, and manages keyboard behavior.
* **TerminalScreen**: Composable showing terminal output and input field, handling command input.
* **interpretCommand**: Core function parsing and processing user input commands, including package-specific commands.

---

### State Management

* `output`: Mutable Compose state holding terminal lines (output history).
* `command`: Compose state for current input line.
* `importedPackages`: Mutable list storing imported package names, enabling package-specific commands.

---

### User Interface

* Terminal output displayed in a vertically scrollable column with white text on a black background for contrast.
* Single-line input field with dark gray background for ease of typing and clear visibility.
* Keyboard automatically hides upon command submission to improve UX.

---

### Commands

* **Common commands:**

  * `h` — Displays the help page
  * `clear` — Clears the terminal
  * `echo [text]` — Echo things
  * `time` — Shows system time
  * `termy-import <package>` — Imports a local package (`math`, `shell`, `files`)
  * `termy-request-permission` — Asks for file / full acess permissions
* **math** package commands (active after `termy-import math`):

  * `math.add <num1> <num2>` — Adds two numbers.
  * `math.mul <num1> <num2>` — Multiplies two numbers.

* **shell** package commands (active after `termy-import shell`):

  * Supports basic Unix-shell commands, enabling execution of typical Linux/Unix commands similar to Termux.
  * `ls` — Lists archives
  * `mkdir` — Create dirs
  * And much more!


* **files** package commands (active after `ty.import files`):

  * Allows file reading and writing inside the local Android filesystem sandbox.
  * `files.list dir` — Shows archives inside a dir
  * `files.read path` — Read archives

---

### Command Interpretation

* Commands are parsed via string matching and argument splitting.
* Package commands are only accessible after explicit import to maintain modularity.
* Error handling returns clear messages on unknown commands or invalid argument count.

---

## Main Available Packages

* ➔ **math** — Adds basic math functions (multiply, add).
* ➔ **shell** — Adds Unix-shell commands, allowing execution of most Linux/Unix commands, similar to Termux.
* ➔ **files** — Enables reading and writing files within the local Android filesystem sandbox.

---
