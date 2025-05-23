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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.rootworksgroup.termynalr.ui.theme.TermynalRTheme
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val importedPackages = remember { mutableStateListOf<String>() }
    var selectedTab by remember { mutableStateOf(TermynalTab.Terminal) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Settings state
    var darkMode by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(16) }
    var showTimeStamp by remember { mutableStateOf(false) }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }

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
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
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

    fun executeCommand(input: String) {
        if (input.isNotBlank()) {
            commandHistory = commandHistory + input
            historyIndex = -1

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
                            listOf("> $input", "Package '$pkg' successfully imported.")
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
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("TermynalR", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                NavigationDrawerItem(
                    label = { Text("Terminal") },
                    selected = selectedTab == TermynalTab.Terminal,
                    onClick = {
                        selectedTab = TermynalTab.Terminal
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = selectedTab == TermynalTab.Settings,
                    onClick = {
                        selectedTab = TermynalTab.Settings
                        scope.launch { drawerState.close() }
                    }
                )
                Divider()
                Text("Quick Actions", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelMedium)
                NavigationDrawerItem(
                    label = { Text("Request Permissions") },
                    selected = false,
                    onClick = {
                        requestStoragePermission()
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Clear Terminal") },
                    selected = false,
                    onClick = {
                        output = emptyList()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("TermySH", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (darkMode) Color(0xFF222222) else Color(0xFF6200EE)
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    TermynalTab.Terminal -> TerminalUI(
                        output = output,
                        command = command,
                        onCommandChange = {
                            command = it
                            historyIndex = -1
                        },
                        onExecuteCommand = { input ->
                            executeCommand(input)
                            command = TextFieldValue("")
                            keyboardController?.hide()
                        },
                        onHistoryUp = {
                            if (commandHistory.isNotEmpty()) {
                                val newIndex = if (historyIndex < commandHistory.size - 1) historyIndex + 1 else commandHistory.size - 1
                                historyIndex = newIndex
                                command = TextFieldValue(commandHistory[commandHistory.size - 1 - newIndex])
                            }
                        },
                        onHistoryDown = {
                            if (historyIndex > 0) {
                                historyIndex--
                                command = TextFieldValue(commandHistory[commandHistory.size - 1 - historyIndex])
                            } else {
                                historyIndex = -1
                                command = TextFieldValue("")
                            }
                        },
                        darkMode = darkMode,
                        fontSize = fontSize,
                        showTimeStamp = showTimeStamp
                    )

                    TermynalTab.Settings -> SettingsScreen(
                        darkMode = darkMode,
                        onDarkModeChange = { darkMode = it },
                        fontSize = fontSize,
                        onFontSizeChange = { fontSize = it },
                        showTimeStamp = showTimeStamp,
                        onShowTimeStampChange = { showTimeStamp = it },
                        onClearHistory = {
                            commandHistory = emptyList()
                            historyIndex = -1
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TerminalUI(
    output: List<String>,
    command: TextFieldValue,
    onCommandChange: (TextFieldValue) -> Unit,
    onExecuteCommand: (String) -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    darkMode: Boolean,
    fontSize: Int,
    showTimeStamp: Boolean
) {
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-scroll to bottom when output changes
    LaunchedEffect(output.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (darkMode) Brush.verticalGradient(listOf(Color(0xFF101010), Color(0xFF000000)))
                else Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)))
            )
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onExecuteCommand("h") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkMode) Color.DarkGray else Color(0xFF3700B3)
                )
            ) {
                Text("Help", color = Color.White)
            }
            Button(
                onClick = { onExecuteCommand("clear") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkMode) Color.DarkGray else Color(0xFF3700B3)
                )
            ) {
                Text("Clear", color = Color.White)
            }
            Button(
                onClick = { onExecuteCommand("time") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkMode) Color.DarkGray else Color(0xFF3700B3)
                )
            ) {
                Text("Time", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp)
        ) {
            output.forEachIndexed { index, line ->
                val timestamp = if (showTimeStamp) "[${LocalDateTime.now()}] " else ""
                Text(
                    text = timestamp + line,
                    color = when {
                        line.startsWith(">") -> if (darkMode) Color(0xFF76FF03) else Color(0xFF018786)
                        line.startsWith("Error") -> Color(0xFFFF5252)
                        else -> if (darkMode) Color(0xFFE0E0E0) else Color(0xFF000000)
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = with(LocalDensity.current) { fontSize.sp }
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = command.text,
            onValueChange = { onCommandChange(TextFieldValue(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (darkMode) Color(0xFF1A1A1A) else Color(0xFFFFFFFF)),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (darkMode) Color.White else Color.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = with(LocalDensity.current) { fontSize.sp }
            ),
            placeholder = {
                Text(
                    "Type a command and press enter",
                    color = if (darkMode) Color(0xFFAAAAAA) else Color(0xFF666666)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onExecuteCommand(command.text.trim())
                    onCommandChange(TextFieldValue(""))
                    keyboardController?.hide()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (darkMode) Color(0xFF333333) else Color(0xFF666666),
                focusedBorderColor = if (darkMode) Color(0xFF76FF03) else Color(0xFF018786),
                cursorColor = if (darkMode) Color.White else Color.Black
            )
        )
    }
}

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    showTimeStamp: Boolean,
    onShowTimeStampChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // Theme settings
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dark Mode")
            Switch(
                checked = darkMode,
                onCheckedChange = onDarkModeChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Font size settings
        Text("Font Size: $fontSize", style = MaterialTheme.typography.titleLarge)
        Slider(
            value = fontSize.toFloat(),
            onValueChange = { onFontSizeChange(it.toInt()) },
            valueRange = 12f..24f,
            steps = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Timestamp settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Timestamps")
            Switch(
                checked = showTimeStamp,
                onCheckedChange = onShowTimeStampChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History management
        Text("Command History", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onClearHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Command History")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App info
        Text("About", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("TermySH v0.0.6")
        Text("A Open-Sourced Local-Based Shell Terminal")
    }
}

enum class TermynalTab {
    Terminal, Settings
}

@RequiresApi(Build.VERSION_CODES.O)
private fun interpretCommand(
    input: String,
    importedPackages: List<String>,
    context: android.content.Context
): List<String> = when {
    input.equals("h", ignoreCase = true) -> listOf(
        "TermySH - Advanced Terminal Help",
        "Basic Commands:",
        "- h                  : Shows this help page",
        "- clear              : Clears the terminal screen",
        "- echo <text>        : Echoes back the provided text",
        "- time               : Shows current system time",
        "- termy-import pkg   : Imports a package (math)",
        "- termy-request-permission : Requests storage permissions",
        "",
        "Math Package:",
        "  math.add x y       : Adds two numbers",
        "  math.sub x y       : Subtracts y from x",
        "  math.mul x y       : Multiplies two numbers",
        "  math.div x y       : Divides x by y",
        "",
        "Shell Commands:",
        "  sh <command>       : Executes a shell command",
        "  su <command>       : Executes as root (needs root access)",
        "  ls [path]          : Lists directory contents",
        "  pwd                : Prints working directory",
        "  cd <dir>           : Changes directory (limited)",
        "  cat <file>         : Displays file contents",
        "  mkdir <dir>        : Creates a directory",
        "  rm <file>          : Removes a file/directory",
        "  cp <src> <dest>    : Copies files",
        "  mv <src> <dest>    : Moves/renames files",
        "  ping <host>        : Tests network connectivity",
        "",
        "System Info:",
        "  ip a / ifconfig    : Network interface information",
        "  ps                 : Displays running processes",
        "  getprop            : Shows system properties",
        "  pm list packages   : Lists installed applications",
        "",
        "Notes:",
        "- Some commands may require specific permissions",
        "- Root commands (su) only work on rooted devices",
        "- File operations without su are limited to app-accessible locations"
    )


    input.lowercase().startsWith("echo ") -> listOf(input.removePrefix("echo ").trim())

    input.equals("time", ignoreCase = true) -> listOf("Current time: ${LocalDateTime.now()}")

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

    importedPackages.contains("math") && input.lowercase().startsWith("math.sub ") -> {
        val args = input.removePrefix("math.sub ").split(" ")
        if (args.size == 2) {
            val a = args[0].toDoubleOrNull()
            val b = args[1].toDoubleOrNull()
            if (a != null && b != null) listOf("Result: ${a - b}")
            else listOf("Error: Invalid arguments for math.sub")
        } else listOf("Usage: math.sub <num1> <num2>")
    }

    importedPackages.contains("math") && input.lowercase().startsWith("math.div ") -> {
        val args = input.removePrefix("math.div ").split(" ")
        if (args.size == 2) {
            val a = args[0].toDoubleOrNull()
            val b = args[1].toDoubleOrNull()
            if (a != null && b != null) {
                if (b != 0.0) listOf("Result: ${a / b}") else listOf("Error: Division by zero")
            } else {
                listOf("Error: Invalid arguments for math.div")
            }
        } else listOf("Usage: math.div <num1> <num2>")
    }

    input.equals("su", ignoreCase = true) -> listOf("Root access required. Use 'su <command>' for root commands.")

    input.lowercase().startsWith("su ") -> {
        if (isDeviceRooted()) {
            executeRootCommand(input.removePrefix("su ").trim())
        } else {
            listOf("Error: Device is not rooted")
        }
    }

    input.lowercase().startsWith("sh ") -> {
        executeShellCommand(input.removePrefix("sh ").trim())
    }

    // Comandos Unix básicos
    input.equals("ls", ignoreCase = true) -> executeShellCommand("ls ${context.filesDir.absolutePath}")

    input.lowercase().startsWith("ls ") -> {
        val path = input.removePrefix("ls ").trim()
        executeShellCommand("ls ${if (path.startsWith("/")) path else context.filesDir.absolutePath + "/" + path}")
    }

    input.equals("pwd", ignoreCase = true) -> executeShellCommand("pwd")

    input.lowercase().startsWith("cd ") -> {
        val dir = input.removePrefix("cd ").trim()
        executeShellCommand("cd $dir && pwd")
    }

    input.lowercase().startsWith("cat ") -> {
        val file = input.removePrefix("cat ").trim()
        executeShellCommand("cat $file")
    }

    input.lowercase().startsWith("ping ") -> {
        val host = input.removePrefix("ping ").trim()
        executeShellCommand("ping -c 4 $host")
    }

    input.lowercase().startsWith("mkdir ") -> {
        val dir = input.removePrefix("mkdir ").trim()
        executeShellCommand("mkdir $dir")
    }

    input.lowercase().startsWith("rm ") -> {
        val target = input.removePrefix("rm ").trim()
        executeShellCommand("rm $target")
    }

    input.lowercase().startsWith("cp ") -> {
        val args = input.removePrefix("cp ").trim()
        executeShellCommand("cp $args")
    }

    input.lowercase().startsWith("mv ") -> {
        val args = input.removePrefix("mv ").trim()
        executeShellCommand("mv $args")
    }

    input.equals("ifconfig", ignoreCase = true) || input.equals("ip a", ignoreCase = true) -> {
        executeShellCommand("ip a")
    }

    input.equals("ps", ignoreCase = true) -> {
        executeShellCommand("ps")
    }

    input.lowercase().startsWith("grep ") -> {
        val pattern = input.removePrefix("grep ").trim()
        executeShellCommand("grep $pattern")
    }

    else -> listOf("Command not found: $input")
}

// Funções auxiliares para executar comandos
private fun executeShellCommand(command: String): List<String> {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val output = process.inputStream.bufferedReader().readLines()
        val error = process.errorStream.bufferedReader().readLines()

        process.waitFor()

        if (error.isNotEmpty()) {
            error
        } else {
            output.ifEmpty { listOf("Command executed successfully") }
        }
    } catch (e: Exception) {
        listOf("Error executing command: ${e.message}")
    }
}

private fun executeRootCommand(command: String): List<String> {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        val output = process.inputStream.bufferedReader().readLines()
        val error = process.errorStream.bufferedReader().readLines()

        process.waitFor()

        if (error.isNotEmpty()) {
            error
        } else {
            output.ifEmpty { listOf("Root command executed successfully") }
        }
    } catch (e: Exception) {
        listOf("Root access error: ${e.message}")
    }
}

private fun isDeviceRooted(): Boolean {
    return try {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root")).waitFor() == 0
    } catch (e: Exception) {
        false
    }
}


