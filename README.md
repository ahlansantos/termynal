---

# TermynalR - Local Based Version (LBV)

TermynalR is a simple local terminal emulator app for Android, built with Kotlin and Jetpack Compose. It supports basic commands and importing local packages to extend functionality.

---

## Technical Documentation

### Overview

* Developed in **Kotlin** using **Jetpack Compose**.
* Provides a terminal-like interface with input and output areas.
* Supports internal commands and modular package imports (`math` and `game`).

---

### Code Structure

* **MainActivity**: Sets up the UI and enables edge-to-edge display.
* **TerminalScreen**: Composable that shows terminal output and input field.
* **interpretCommand**: Function that processes user input and returns output lines based on commands and imported packages.

---

### State Management

* `output`: List of strings representing terminal lines, stored as a mutable Compose state.
* `command`: Current text input, managed as Compose state.
* `importedPackages`: Mutable state list holding names of imported packages.

---

### User Interface

* Output is shown in a scrollable column with white text on black background.
* Input is a single-line text field with dark gray background.
* Keyboard is managed to hide after command submission.

---

### Commands

* Basic commands:

  * `ty.h` — Show help menu.
  * `ty.clear` — Clear terminal screen.
  * `ty.echo [text]` — Print text.
  * `ty.time` — Show current system time.
  * `ty.import <package>` — Import local packages (`math`, `game`).

* **math** package commands (active after `ty.import math`):

  * `math.add <num1> <num2>` — Add two numbers.
  * `math.mul <num1> <num2>` — Multiply two numbers.
  * `math.h` — Show math package help.

* **game** package commands (active after `ty.import game`):

  * `game.play` — Start simulated game.
  * `game.help` — Show game package help.
  * `game.h` — Show game package commands.

---

### Command Interpretation

* Commands are matched via simple string comparisons.
* Package commands are only available after import.
* Arguments are parsed by splitting input strings.
* Errors are handled by returning messages for unknown commands or wrong arguments.

---
