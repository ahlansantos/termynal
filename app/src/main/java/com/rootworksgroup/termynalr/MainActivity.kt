@file:OptIn(ExperimentalMaterial3Api::class)

package com.rootworksgroup.termynalr

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.rootworksgroup.termynalr.ui.theme.TermynalRTheme
import java.io.File
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
                    TerminalScreen(applicationContext.filesDir.absolutePath)
                }
            }
        }
    }
}

@Composable
fun TerminalScreen(filesDir: String) {
    var output by remember { mutableStateOf(listOf("TermynalR LBV - Local Based Version")) }
    var command by remember { mutableStateOf(TextFieldValue("")) }
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val importedPackages = remember { mutableStateListOf<String>() }

    // Estados para modo editor nano
    var editorMode by remember { mutableStateOf(false) }
    var editorFileName by remember { mutableStateOf("") }
    var editorFileContent by remember { mutableStateOf("") }

    if (editorMode) {
        NanoEditor(
            filename = editorFileName,
            initialText = editorFileContent,
            onSave = { content ->
                saveToFile(filesDir, editorFileName, content)
                output = output + listOf("Saved '$editorFileName'")
                editorMode = false
            },
            onExit = {
                output = output + listOf("Exited nano without saving '$editorFileName'")
                editorMode = false
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF111111), Color.Black)))
                .padding(12.dp)
        ) {
            TopAppBar(
                title = { Text("TermynalR", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF222222))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(8.dp)
            ) {
                output.forEach { line ->
                    Text(
                        text = line,
                        color = if (line.startsWith(">")) Color(0xFF76FF03) else Color.White,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            OutlinedTextField(
                value = command.text,
                onValueChange = { command = TextFieldValue(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1C), shape = MaterialTheme.shapes.medium),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                ),
                placeholder = { Text("Execute a command...", color = Color.LightGray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val input = command.text.trim()
                        val result = when {
                            input.equals("ty.clear", ignoreCase = true) -> listOf("TermynalR - Simple Terminal")

                            input.lowercase().startsWith("ty.import ") -> {
                                val pkg = input.substringAfter("ty.import ").trim().lowercase()
                                if (pkg in listOf("math", "game")) {
                                    if (!importedPackages.contains(pkg)) {
                                        importedPackages.add(pkg)
                                        listOf("> $input", "Package '$pkg' imported successfully.")
                                    } else {
                                        listOf("> $input", "Package '$pkg' is already imported.")
                                    }
                                } else {
                                    listOf("> $input", "Unknown package: '$pkg'")
                                }
                            }

                            else -> {
                                val res = interpretCommand(input, importedPackages)
                                if (res.size == 1 && res[0].startsWith("[[nano_open:")) {
                                    val file = res[0].removePrefix("[[nano_open:").removeSuffix("]]")
                                    editorFileName = file
                                    editorFileContent = loadFromFile(filesDir, file)
                                    editorMode = true
                                    emptyList()
                                } else {
                                    listOf("> $input") + res
                                }
                            }
                        }

                        output = output + result
                        command = TextFieldValue("")
                        keyboardController?.hide()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedBorderColor = Color(0xFF76FF03),
                    cursorColor = Color.White
                )
            )
        }
    }
}

@Composable
fun NanoEditor(
    filename: String,
    initialText: String,
    onSave: (String) -> Unit,
    onExit: () -> Unit
) {
    var text by remember { mutableStateOf(TextFieldValue(initialText)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        Text(
            text = "nano: $filename",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontFamily = FontFamily.Monospace
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF76FF03),
                unfocusedBorderColor = Color(0xFF444444),
                cursorColor = Color.White
            ),
            placeholder = { Text("Editing...", color = Color.DarkGray) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onSave(text.text) }) {
                Text("Save")
            }
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Exit")
            }
        }
    }
}

fun saveToFile(filesDir: String, filename: String, content: String) {
    try {
        val file = File(filesDir, filename)
        file.writeText(content)
    } catch (e: Exception) {
        // Tratar erros de I/O se necessário
    }
}

fun loadFromFile(filesDir: String, filename: String): String {
    return try {
        val file = File(filesDir, filename)
        if (file.exists()) file.readText() else ""
    } catch (e: Exception) {
        ""
    }
}

@RequiresApi(Build.VERSION_CODES.O)
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
            "- kernel.import <package> : Import available (local only) Packages into the LBKernel.",
            "Available Imports:",
            "- math : Add MATH functions to the LBKernel.",
            "- game : Simple Game (Testing)",
            "- nano <filename> : Open nano text editor"
        )

        lowerInput == "math.h" && importedPackages.contains("math") -> listOf(
            "Math Package commands:",
            "- math.add <num1> <num2> : Add two numbers",
            "- math.mul <num1> <num2> : Multiply two numbers",
            "- math.h                 : Show this help message"
        )

        lowerInput == "game.h" && importedPackages.contains("game") -> listOf(
            "Game Package commands:",
            "- game.play  : Start the game (simulated)",
            "- game.help  : Show the game help message",
            "- game.h     : Show this help message"
        )

        lowerInput.startsWith("ty.echo ") -> listOf(input.removePrefix("ty.echo "))

        lowerInput == "ty.time" -> listOf("Current time: ${LocalDateTime.now()}")

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

        importedPackages.contains("game") && lowerInput == "game.play" -> listOf("Game starting... (simulated)")

        importedPackages.contains("game") && lowerInput == "game.help" -> listOf(
            "Game Package commands:",
            "- game.play  : Start the game (simulated)",
            "- game.help  : Show this help message"
        )

        lowerInput.startsWith("nano ") -> {
            val filename = input.substringAfter("nano ").trim()
            if (filename.isNotEmpty()) {
                listOf("[[nano_open:$filename]]")
            } else {
                listOf("Usage: nano <filename>")
            }
        }

        input.isBlank() -> emptyList()

        else -> listOf("Unknown command: $input")
    }
}
