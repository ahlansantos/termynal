package com.rootworksgroup.termynalr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.rootworksgroup.termynalr.ui.theme.TermynalRTheme
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TermynalRTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TerminalScreen()
                }
            }
        }
    }
}

@Composable
fun TerminalScreen() {
    var output by remember { mutableStateOf(listOf("TermynalR LBV - Local Based Version")) }
    var command by remember { mutableStateOf(TextFieldValue("")) }
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // IMPORTED PACKAGES
    val importedPackages = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 32.dp, bottom = 16.dp, start = 12.dp, end = 12.dp)
    ) {
        // EXIT RESULTS
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            output.forEach { line ->
                Text(
                    text = line,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ENRTRY CAMP AND REUSLTS
        TextField(
            value = command.text,
            onValueChange = { command = TextFieldValue(it) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(8.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            placeholder = { Text("Enter command...", color = Color.LightGray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val input = command.text.trim()
                    output = when {
                        input.equals("ty.clear", ignoreCase = true) -> listOf("TermynalR - Simple Terminal")
                        input.lowercase().startsWith("ty.import ") -> {
                            val pkg = input.substringAfter("ty.import ").trim().lowercase()
                            if (pkg in listOf("math", "game")) {
                                if (!importedPackages.contains(pkg)) {
                                    importedPackages.add(pkg)
                                    listOf("Package '$pkg' imported successfully.")
                                } else {
                                    listOf("Package '$pkg' is already imported.")
                                }
                            } else {
                                listOf("Unknown package: '$pkg'")
                            }
                        }
                        else -> output + listOf("> $input") + interpretCommand(input, importedPackages)
                    }
                    command = TextFieldValue("")
                    keyboardController?.hide()
                }
            )
        )
    }
}

fun interpretCommand(input: String, importedPackages: List<String>): List<String> {
    val lowerInput = input.lowercase()

    return when {
        lowerInput == "ty.h" -> listOf(
            "Available commands:",
            "- ty.h          : Show available commands",
            "- ty.clear      : Clear the screen",
            "- ty.echo       : echo [text] → Print text",
            "- ty.time       : Show current time",
            "Termynal Extra Available commands:",
            "- ty.import <package> : Import available (local only) Packages",
            "Available Imports:",
            "- math : SKP (Simple Kernel Package) - Adds MATH functions to the Kernel of Termynal.",
            "- game : AGEP (Advanced Game Engine Package) - Simple Termynal Game Package (Being tested)."
        )

        // SPECIFIC PKG COMMANDS (LOCAL PKGS)
        lowerInput == "math.h" && importedPackages.contains("math") -> listOf(
            "Math Package commands:",
            "- math.add <num1> <num2> : Add two numbers",
            "- math.mul <num1> <num2> : Multiply two numbers",
            "- math.h                : Show this help message"
        )

        lowerInput == "game.h" && importedPackages.contains("game") -> listOf(
            "Game Package commands:",
            "- game.play  : Start the game (simulated)",
            "- game.help  : Show the game help message",
            "- game.h     : Show this help message"
        )

        lowerInput.startsWith("ty.echo ") -> listOf(input.removePrefix("ty.echo "))

        lowerInput == "ty.time" -> listOf("Current time: ${LocalDateTime.now()}")

        // MATH
        importedPackages.contains("math") && lowerInput.startsWith("math.add ") -> {
            val args = lowerInput.removePrefix("math.add ").split(" ")
            if (args.size == 2) {
                val a = args[0].toDoubleOrNull()
                val b = args[1].toDoubleOrNull()
                if (a != null && b != null) {
                    listOf("Result: ${a + b}")
                } else {
                    listOf("Invalid arguments for math.add. Usage: math.add <num1> <num2>")
                }
            } else {
                listOf("Invalid number of arguments for math.add. Usage: math.add <num1> <num2>")
            }
        }

        importedPackages.contains("math") && lowerInput.startsWith("math.mul ") -> {
            val args = lowerInput.removePrefix("math.mul ").split(" ")
            if (args.size == 2) {
                val a = args[0].toDoubleOrNull()
                val b = args[1].toDoubleOrNull()
                if (a != null && b != null) {
                    listOf("Result: ${a * b}")
                } else {
                    listOf("Invalid arguments for math.mul. Usage: math.mul <num1> <num2>")
                }
            } else {
                listOf("Invalid number of arguments for math.mul. Usage: math.mul <num1> <num2>")
            }
        }

        // GAME
        importedPackages.contains("game") && lowerInput == "game.play" -> listOf("Game starting... (simulated)")

        importedPackages.contains("game") && lowerInput == "game.help" -> listOf(
            "Game Package commands:",
            "- game.play  : Start the game (simulated)",
            "- game.help  : Show this help message"
        )

        input.isBlank() -> emptyList()

        else -> listOf("Unknown command: $input")
    }
}
