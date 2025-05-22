@file:OptIn(ExperimentalMaterial3Api::class)

package com.rootworksgroup.termynalr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
    var output by remember { mutableStateOf(listOf("TermySH - Local Based Advanced Terminal", "Use 'h' for help")) }
    var command by remember { mutableStateOf(TextFieldValue("")) }
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val importedPackages = remember { mutableStateListOf<String>() }

    val storagePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        output += if (it) listOf("> File permission granted") else listOf("> File permission denied")
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            output += listOf("> Full access granted")
        } else {
            output += listOf("> Full access denied")
        }
    }

    fun requestStoragePermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager() -> {
                output += listOf("> Full access already granted")
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent().apply {
                        action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                        data = "package:${context.packageName}".toUri()
                    }
                    manageStorageLauncher.launch(intent)
                }
            }
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                output += listOf("> Permission already granted")
            }
            else -> {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF101010), Color(0xFF000000))))
            .padding(12.dp)
    ) {
        TopAppBar(
            title = { Text("TermySH", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF222222))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp)
        ) {
            output.forEach { line ->
                AnimatedVisibility(visible = true) {
                    Text(
                        text = line,
                        color = when {
                            line.startsWith(">") -> Color(0xFF76FF03)
                            line.startsWith("Error") -> Color(0xFFFF5252)
                            else -> Color(0xFFE0E0E0)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = command.text,
            onValueChange = { command = TextFieldValue(it) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A)),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontFamily = FontFamily.Monospace
            ),
            placeholder = { Text("Type a command and press enter", color = Color(0xFFAAAAAA)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val input = command.text.trim()
                    val result = when {
                        input.equals("clear", ignoreCase = true) -> {
                            output = emptyList(); emptyList()
                        }
                        input.equals("termy-request-permission", ignoreCase = true) -> {
                            requestStoragePermission(); emptyList()
                        }
                        input.lowercase().startsWith("termy-import ") -> {
                            val pkg = input.substringAfter("termy-import ").trim().lowercase()
                            if (pkg in listOf("math", "shell", "files")) {
                                if (!importedPackages.contains(pkg)) {
                                    importedPackages.add(pkg)
                                    listOf("> $input", "Package '$pkg' successfully imported into the local environment.")
                                } else {
                                    listOf("> $input", "Package '$pkg' is already imported.")
                                }
                            } else {
                                listOf("> $input", "Unknown package: '$pkg'")
                            }
                        }
                        else -> listOf("> $input") + interpretCommand(input, importedPackages, context)
                    }

                    output = output + result
                    command = TextFieldValue("")
                    keyboardController?.hide()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF333333),
                focusedBorderColor = Color(0xFF76FF03),
                cursorColor = Color.White
            )
        )
    }
}



@RequiresApi(Build.VERSION_CODES.O)
fun interpretCommand(input: String, importedPackages: List<String>, context: android.content.Context): List<String> {
    return when {
        input.equals("h", ignoreCase = true) -> listOf(
            "Avaliable Commands:",
            "- h          : Shows this Page",
            "- clear      : Clears the Terminal",
            "- termy-import pkg: Imports a local package (math, shell, files)",
            "- termy-request-permission : Asks for Files / Permission Access",
            "- echo       : Echo things",
            "- time       : Shows system time",
            "",
            "math Package:",
            "  math.add x y  : Add 2 Numbers",
            "  math.mul x y  : Multiply 2 Numbers",
            "",
            "shell Package:",
            "  ls, pwd, mkdir : Unix System Commands",
            "",
            "files Package:",
            "  files.list dir : Shows archives inside a dir",
            "  files.read path: Reads archives"
        )

        input.lowercase().startsWith("echo ") -> listOf(input.removePrefix("echo "))

        input.equals("time", ignoreCase = true) -> listOf("Date: ${LocalDateTime.now()}")

        importedPackages.contains("math") && input.lowercase().startsWith("math.add ") -> {
            val args = input.removePrefix("math.add ").split(" ")
            if (args.size == 2) {
                val a = args[0].toDoubleOrNull()
                val b = args[1].toDoubleOrNull()
                if (a != null && b != null) listOf("Result: ${a + b}")
                else listOf("Error: Invalid arguments for math.add")
            } else listOf("Usage: math.add <num1> <num2>")
        }

        importedPackages.contains("math") && input.lowercase().startsWith("math.mul ") -> {
            val args = input.removePrefix("math.mul ").split(" ")
            if (args.size == 2) {
                val a = args[0].toDoubleOrNull()
                val b = args[1].toDoubleOrNull()
                if (a != null && b != null) listOf("Result: ${a * b}")
                else listOf("Error: Invalid arguments for math.mul")
            } else listOf("Usage: math.mul <num1> <num2>")
        }

        importedPackages.contains("shell") && input.isNotBlank() -> {
            executeShellCommand(input, context)
        }


        else -> listOf("Error: Unknown command: '$input'", "Use 'h' for help")
    }
}

fun executeShellCommand(command: String, context: android.content.Context): List<String> {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val output = process.inputStream.bufferedReader().readLines()
        val error = process.errorStream.bufferedReader().readLines()
        process.waitFor()

        if (error.isNotEmpty()) {
            listOf("Error:") + error
        } else {
            output
        }
    } catch (e: Exception) {
        listOf("Error while executing command: ${e.message}")
    }
}
