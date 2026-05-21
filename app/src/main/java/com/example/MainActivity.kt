package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AlarmLog
import com.example.data.AppDatabase
import com.example.data.KeywordRule
import com.example.data.RuleRepository
import com.example.service.AlarmSoundPlayer
import com.example.service.AlarmStateManager
import com.example.ui.components.BentoCard
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberLime
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonPink
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over keyguard and wake lock screen when alarm is triggered
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current

                // Request notification permission if Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (!isGranted) {
                            Toast.makeText(context, "Notifications are required to display background Alarm alerts!", Toast.LENGTH_LONG).show()
                        }
                    }
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val database = remember { AppDatabase.getDatabase(context) }
                val repository = remember { RuleRepository(database) }
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(applicationContext as Application, repository)
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBg
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NotificationAlarmApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationAlarmApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val rules by viewModel.allRules.collectAsState()
    val logs by viewModel.allLogs.collectAsState()
    val activeAlarm by viewModel.activeAlarm.collectAsState()
    val activeRule by viewModel.activeRule.collectAsState()

    // 1. Polling check for physical system listener permission
    var isPermissionEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            isPermissionEnabled = flat != null && flat.contains(context.packageName)
            delay(1500)
        }
    }

    // 2. Local digital clock logic
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            currentTimeString = sdf.format(Date())
            delay(1000)
        }
    }

    // 3. Audio preview state
    var selectedSoundForTest by remember { mutableStateOf("Classic Sirens") }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    val audioTester = remember { AlarmSoundPlayer(context) }

    // Custom uploaded file state
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    var uploadedFileSize by remember { mutableStateOf<String?>(null) }

    // Read initial custom siren file info if exists
    LaunchedEffect(Unit) {
        val file = java.io.File(context.filesDir, "custom_user_siren.mp3")
        if (file.exists() && file.length() > 0) {
            uploadedFileName = "custom_user_siren.mp3"
            uploadedFileSize = "${String.format("%.1f", file.length() / (1024.0 * 1024.0))} MB"
        }
    }

    val mp3PickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val file = java.io.File(context.filesDir, "custom_user_siren.mp3")
                    java.io.FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                    uploadedFileName = "custom_user_siren.mp3"
                    uploadedFileSize = "${String.format("%.1f", file.length() / (1024.0 * 1024.0))} MB"
                    Toast.makeText(context, "Custom MP3 Siren Loaded! Ready for use.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error importing file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Dialog trigger
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<com.example.data.KeywordRule?>(null) }

    // Navigation and Main Content layout
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // App Bar Title + Clock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACTIVE SHIELD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan, // Bento deep purple Color
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "NotifyAlarm",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary, // Bento Obsidian Charcoal
                        letterSpacing = (-0.5).sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                // Bento Digital Clock Badge
                Surface(
                    color = Color(0xFFEADDFF), // Beautiful Bento Lavender Purple
                    shape = RoundedCornerShape(24.dp), // Fully rounded corners
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentTimeString,
                            color = CyberCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            // BENTO GRID CONTAINER (LazyColumn containing beautifully proportional cards blocks)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // BLOCK 1: Status Banner Card (Double wide grid look)
                item {
                    val statusColor = if (isPermissionEnabled) CyberLime else AlertOrange
                    val statusText = if (isPermissionEnabled) "MONITORING ACTIVE" else "PERMISSION PENDING"
                    val bentoCardColor = if (isPermissionEnabled) Color(0xFFEADDFF) else Color(0xFFFBEBE9)
                    val bentoCardBorder = if (isPermissionEnabled) Color(0xFFD0BCFF) else Color(0xFFF1C4C0)
                    val contentColor = if (isPermissionEnabled) Color(0xFF21005D) else Color(0xFF8C1D18)
                    val statusDescription = if (isPermissionEnabled) {
                        "Background notification receiver is monitoring all incoming notifications securely."
                    } else {
                        "To track system notifications in real-time, click grant permission to enable Notification Access in Android Settings."
                    }

                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = bentoCardColor,
                        borderColor = bentoCardBorder,
                        glowColor = statusColor.copy(alpha = 0.15f),
                        testTag = "system_status_card"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "System Monitor",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = contentColor,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = statusDescription,
                                    color = contentColor.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = FontFamily.SansSerif
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = contentColor.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            text = "Filters: ${rules.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        color = contentColor.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            text = "Matched Logs: ${logs.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (!isPermissionEnabled) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AlertOrange,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = RowDefaults.buttonPadding()
                                ) {
                                    Text("GRANT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }


                // BLOCK 4: Rules Engine Config Dashboard (Double-wide)
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "rules_dashboard_card"
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "FILTER AND CRITERIA RULES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Keyword Triggers",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                }

                                IconButton(
                                    onClick = { showAddRuleDialog = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(CyberCyan.copy(alpha = 0.1f), CircleShape)
                                        .border(1.dp, CyberCyan, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Filter Keyword Rule",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (rules.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No Rules Setup. Tap [+] up-top to create one.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rules.forEach { rule ->
                                        RuleItemRow(
                                            rule = rule,
                                            onToggle = { viewModel.toggleRuleEnabled(rule) },
                                            onDelete = { viewModel.deleteRule(rule) },
                                            onClick = { ruleToEdit = rule }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // BLOCK 5: History / Logs matching ledger
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "logs_ledger_card"
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Clock Logs History",
                                        tint = NeonPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MATCHED ALARMS HISTORY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonPink,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (logs.isNotEmpty()) {
                                    TextButton(
                                        onClick = { viewModel.clearAllLogs() }
                                    ) {
                                        Text("WIPE LEDGER", color = NeonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Alarm Logs Ledger",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (logs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No recorded alarm entries yet.\nTrigger a simulation or generate incoming notifications to populate logs.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    logs.take(15).forEach { log ->
                                        LogCardItem(log)
                                    }
                                }
                            }
                        }
                    }
                }

                // ROW containing simulation triggers & audio synthesizer testers
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // BLOCK 2: Simulation Card (1x1 column)
                        BentoCard(
                            modifier = Modifier.weight(1f),
                            containerColor = Color.White,
                            borderColor = DarkCardBorder,
                            testTag = "simulation_test_rig"
                        ) {
                            var testTitle by remember { mutableStateOf("SLACK MONITOR") }
                            var testText by remember { mutableStateOf("CRITICAL error: database client downtime resolved!") }
                            var dropdownExpanded by remember { mutableStateOf(false) }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "ALARM SIMULATION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                    Box {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Presets",
                                            tint = TextSecondary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { dropdownExpanded = true }
                                        )
                                        DropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false },
                                            modifier = Modifier.background(Color.White)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("MOM Emergency Preset", color = TextPrimary, fontSize = 12.sp) },
                                                onClick = {
                                                    testTitle = "Mom calling"
                                                    testText = "EMERGENCY: Please pick up immediately, mom!"
                                                    dropdownExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Server Down Preset", color = TextPrimary, fontSize = 12.sp) },
                                                onClick = {
                                                    testTitle = "Grafana Alert"
                                                    testText = "CRITICAL SERVER DOWNTIME ALERT DB CLIENT HAS FAILED!"
                                                    dropdownExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Online Resolve Preset", color = TextPrimary, fontSize = 12.sp) },
                                                onClick = {
                                                    testTitle = "Network Recover"
                                                    testText = "The main router resolved is now online."
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Instant Tester",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = testTitle,
                                    onValueChange = { testTitle = it },
                                    label = { Text("Sender Title", fontSize = 10.sp) },
                                    maxLines = 1,
                                    colors = textConfigColoredBorder(CyberCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = TextStyleCompact(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = testText,
                                    onValueChange = { testText = it },
                                    label = { Text("Message Body", fontSize = 10.sp) },
                                    maxLines = 2,
                                    colors = textConfigColoredBorder(CyberCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = TextStyleCompact(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                 Button(
                                    onClick = {
                                        isPreviewPlaying = false
                                        audioTester.stop()
                                        viewModel.simulateNotification(testTitle, testText, "TestApp")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberCyan,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Test Notification Matcher",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("FIRE ALARM RIG", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }

                        // BLOCK 3: Audio synthesizer cards (1x1 column)
                        BentoCard(
                            modifier = Modifier.weight(1f),
                            containerColor = Color(0xFFE8DEF8), // Bento Soft lavender card
                            borderColor = Color(0xFFD0BCFF),
                            testTag = "sound_synthesizer_card"
                        ) {
                            Column {
                                Text(
                                    text = "SYNTH CONTROLLER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan, // Beautiful Purple category tag
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Custom Sirens",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val sounds = listOf("Classic Sirens", "Zen Waves", "Retro Beeps", "Digital Alert", "Custom MP3 Siren", "Uploaded Siren", "System Default")
                                var labelSound by remember { mutableStateOf(sounds[0]) }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    sounds.forEach { tone ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    labelSound = tone
                                                    selectedSoundForTest = tone
                                                    if (isPreviewPlaying) {
                                                        audioTester.play(tone)
                                                    }
                                                }
                                                .padding(vertical = 1.dp)
                                        ) {
                                            RadioButton(
                                                selected = (labelSound == tone),
                                                onClick = {
                                                    labelSound = tone
                                                    selectedSoundForTest = tone
                                                    if (isPreviewPlaying) {
                                                        audioTester.play(tone)
                                                    }
                                                },
                                                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                                    selectedColor = CyberCyan,
                                                    unselectedColor = TextSecondary.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Column {
                                                Text(
                                                    text = tone,
                                                    fontSize = 10.sp,
                                                    color = if (labelSound == tone) CyberCyan else TextSecondary,
                                                    fontWeight = if (labelSound == tone) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (tone == "Uploaded Siren" && uploadedFileName != null) {
                                                    Text(
                                                        text = "Loaded ✓ ($uploadedFileSize)",
                                                    )
                                                    androidx.compose.material3.TextButton(
                                                        onClick = {
                                                            try {
                                                                 val file = java.io.File(context.filesDir, "custom_user_siren.mp3")
                                                                 if (file.exists()) {
                                                                     file.delete()
                                                                 }
                                                                 uploadedFileName = null
                                                                 uploadedFileSize = null
                                                                 if (labelSound == "Uploaded Siren") {
                                                                     labelSound = "Classic Sirens"
                                                                     selectedSoundForTest = "Classic Sirens"
                                                                 }
                                                                 Toast.makeText(context, "Siren deleted.", Toast.LENGTH_SHORT).show()
                                                            } catch (e: Exception) {
                                                                 e.printStackTrace()
                                                            }
                                                        },
                                                        modifier = Modifier.height(20.dp),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete Siren",
                                                            tint = NeonPink,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "DELETE SIREN",
                                                            fontSize = 8.sp,
                                                            color = NeonPink,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = "",
                                                        fontSize = 8.sp,
                                                        color = Color(0xFF2E7D32),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else if (tone == "Uploaded Siren" && uploadedFileName == null) {
                                                    Text(
                                                        text = "No custom file imported yet",
                                                        fontSize = 8.sp,
                                                        color = TextSecondary.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        mp3PickerLauncher.launch("audio/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberCyan,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload MP3",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uploadedFileName != null) "REPLACE MP3" else "UPLOAD MP3 SIREN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        if (isPreviewPlaying) {
                                            audioTester.stop()
                                            isPreviewPlaying = false
                                        } else {
                                            isPreviewPlaying = true
                                            audioTester.play(labelSound)
                                        }
                                    },
                                    border = BorderStroke(1.dp, if (isPreviewPlaying) NeonPink else CyberCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isPreviewPlaying) NeonPink else CyberCyan
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewPlaying) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                        contentDescription = "Preview Tone",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPreviewPlaying) "STOP PITCH" else "PREVIEW TONE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FULL ACTIVE ALARM ATTENTION-GRABBING DEUX-OVERLAY IN FOREGROUND
        AnimatedVisibility(
            visible = (activeAlarm != null),
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            val alarm = activeAlarm
            val rule = activeRule
            if (alarm != null && rule != null) {
                ActiveAlarmOverlay(
                    alarm = alarm,
                    rule = rule,
                    onSnooze = {
                        isPreviewPlaying = false
                        audioTester.stop()
                        viewModel.snoozeActiveAlarm()
                    },
                    onDismiss = {
                        isPreviewPlaying = false
                        audioTester.stop()
                        viewModel.dismissActiveAlarm()
                    }
                )
            }
        }
    }

    // Modal dialog to add a beautiful new rule configuration
    if (showAddRuleDialog) {
        AddRuleDialog(
            initialRule = null,
            onDismiss = { showAddRuleDialog = false },
            onSave = { name, keywords, isAndLogic, isExact, isCaseSensitive, sound, snooze ->
                viewModel.addRule(name, keywords, isAndLogic, isExact, isCaseSensitive, sound, snooze)
                showAddRuleDialog = false
            }
        )
    }

    // Modal dialog to edit an existing rule configuration
    if (ruleToEdit != null) {
        val currentEditTarget = ruleToEdit!!
        AddRuleDialog(
            initialRule = currentEditTarget,
            onDismiss = { ruleToEdit = null },
            onSave = { name, keywords, isAndLogic, isExact, isCaseSensitive, sound, snooze ->
                val updated = currentEditTarget.copy(
                    name = name,
                    keywordsString = keywords,
                    isAndLogic = isAndLogic,
                    isExactWord = isExact,
                    isCaseSensitive = isCaseSensitive,
                    soundType = sound,
                    snoozeDurationMinutes = snooze
                )
                viewModel.updateRule(updated)
                ruleToEdit = null
            }
        )
    }
}

@Composable
fun RuleItemRow(
    rule: KeywordRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFFF7F2FA), // Bento interior light lavender-white
        border = BorderStroke(1.dp, Color(0xFFE6E0E9)),
        shape = RoundedCornerShape(10.dp), // Beautiful 10dp rounded bento interior container
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (rule.isEnabled) CyberCyan else TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rule.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rule.isEnabled) TextPrimary else TextSecondary,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkCardBorder
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete filter",
                            tint = NeonPink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            
            // Highlight list of keywords
            val itemsList = rule.getKeywordsList()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 1.dp)
            ) {
                Text(
                    text = "Keywords: ",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                
                Spacer(modifier = Modifier.width(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsList.forEach { keyword ->
                        Surface(
                            color = Color(0xFFEADDFF), // Beautiful Light Lavender
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                        ) {
                            Text(
                                text = "# $keyword", // Prefix with stylish hashtag like Design HTML
                                fontSize = 10.sp,
                                color = CyberCyan, // Deep Purple `#6750A4`
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Pills configuration attributes (e.g. Logic operators and matching constraints)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Operator: AND vs OR
                PillSpec(
                    label = if (rule.isAndLogic) "● AND LOGIC" else "● OR COMBO",
                    bgColor = if (rule.isAndLogic) Color(0xFFEADDFF) else Color(0xFFD1E8FF),
                    textColor = if (rule.isAndLogic) CyberCyan else Color(0xFF0F4D92)
                )

                // Match Mode: EXACT vs SUBSTRING
                PillSpec(
                    label = if (rule.isExactWord) "EXACT" else "SUBSTRING",
                    bgColor = Color(0xFFF3EDF7),
                    textColor = TextSecondary
                )

                // Case: CASESENSITIVE vs IGNORECASE
                PillSpec(
                    label = if (rule.isCaseSensitive) "Aa CASE-SENS" else "Aa IGNORE",
                    bgColor = Color(0xFFF3EDF7),
                    textColor = TextSecondary
                )

                // Sound
                PillSpec(
                    label = "🔔 ${rule.soundType}",
                    bgColor = Color(0xFFE8DEF8),
                    textColor = CyberCyan
                )

                // Snooze duration
                PillSpec(
                    label = "⏳ ${rule.snoozeDurationMinutes}m",
                    bgColor = Color(0xFFF3EDF7),
                    textColor = TextSecondary
                )
            }
        }
    }
}

@Composable
fun PillSpec(label: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp) // Beautifully curved chip shape
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun LogCardItem(log: AlarmLog) {
    val statusColor = when (log.status) {
        "TRIGGERED" -> NeonPink
        "SNOOZED" -> AlertOrange
        else -> TextSecondary
    }

    Surface(
        color = Color(0xFFF7F2FA), // Matches Bento item detail styling nicely
        border = BorderStroke(1.dp, Color(0xFFE6E0E9)),
        shape = RoundedCornerShape(16.dp), // Consistency across containers
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = log.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Matched Rule: ${log.ruleName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "title: \"${log.notificationTitle}\"",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "text: \"${log.notificationText}\"",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "App: ${log.packageName.replace("com.", "")}",
                    fontSize = 10.sp,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontFamily = FontFamily.SansSerif
                )

                if (log.status == "SNOOZED") {
                    val formattedSnooze = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.snoozeUntil))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Snoozed counts: ${log.snoozeCount} | Muted Until: $formattedSnooze",
                        color = AlertOrange,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val timeLabel = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
            Text(
                text = timeLabel,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}

// THE ATTENTION-GRABBING ALARM DIAL OVERLAY IN BRIGHT METALS CHROMIUM
@Composable
fun ActiveAlarmOverlay(
    alarm: AlarmLog,
    rule: KeywordRule,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    // Pulse animation for critical background warning
    val infiniteTransition = rememberInfiniteTransition(label = "WarningGlow")
    val warningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulsing"
    )

    val scaleScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadiusScaling"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Double pulse circle layer
        Box(
            modifier = Modifier
                .size(340.dp)
                .alpha(warningAlpha)
                .border(8.dp, NeonPink, CircleShape)
                .size(380.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Active alarming match!",
                tint = NeonPink,
                modifier = Modifier
                    .size(64.dp)
                    .alpha(warningAlpha * 2)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CRITICAL ALERT TRIGGERED",
                color = NeonPink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = rule.name,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bento payload parameters card
            Surface(
                color = TextSecondary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, NeonPink),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Source Title",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Text(
                        text = alarm.notificationTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Message Message",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Text(
                        text = alarm.notificationText,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Audible alarm loop playing",
                    tint = CyberCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Synthesizer: ${rule.soundType} loop running",
                    fontSize = 11.sp,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Huge trigger dials
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SNOOZE ACTION TIMER
                Button(
                    onClick = onSnooze,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertOrange,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .testTag("snooze_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Snooze Alarm Tracker",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SNOOZE (${rule.snoozeDurationMinutes}M)",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                // SECURE DISMISS
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberLime,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .testTag("dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Dismiss active alert",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISMISS",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// DIALOG TO CONFIGURE AND ENHANCE MULTIPLE RULES ADVANCED OPTIONS
@Composable
fun AddRuleDialog(
    initialRule: com.example.data.KeywordRule? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, keywords: String, isAnd: Boolean, isExact: Boolean, isCaseSensitive: Boolean, sound: String, snooze: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var keywords by remember { mutableStateOf(initialRule?.keywordsString ?: "") }
    var isAndLogic by remember { mutableStateOf(initialRule?.isAndLogic ?: false) }
    var isExact by remember { mutableStateOf(initialRule?.isExactWord ?: false) }
    var isCaseSensitive by remember { mutableStateOf(initialRule?.isCaseSensitive ?: false) }
    var soundType by remember { mutableStateOf(initialRule?.soundType ?: "Classic Sirens") }
    var snoozeMinutes by remember { mutableStateOf((initialRule?.snoozeDurationMinutes ?: 1).toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.White, // Premium clean white dialog overlay
            shape = RoundedCornerShape(28.dp), // Bento style 28dp
            border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialRule != null) "EDIT FILTER RULE" else "NEW FILTER RULE",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close configuring dialog",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Filter Rule Name (e.g. Work Alerts)", fontFamily = FontFamily.SansSerif) },
                    colors = textConfigColoredBorder(CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords (comma separated)", fontFamily = FontFamily.SansSerif) },
                    supportingText = { Text("e.g. severe, database, error", color = TextSecondary, fontFamily = FontFamily.SansSerif) },
                    colors = textConfigColoredBorder(CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Combining Logic AND / OR select
                Text(
                    text = "Multiple Keyword Operator logic matcher:",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isAndLogic = false }
                    ) {
                        RadioButton(
                            selected = !isAndLogic,
                            onClick = { isAndLogic = false },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = CyberCyan)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("OR Logic", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.SansSerif)
                            Text("Any key match", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isAndLogic = true }
                    ) {
                        RadioButton(
                            selected = isAndLogic,
                            onClick = { isAndLogic = true },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = CyberCyan)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("AND Logic", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.SansSerif)
                            Text("All keys must match", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Enhanced toggle conditions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Match Exact Words boundary", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.SansSerif)
                            Text("Matches 'err' but not 'error'", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                        }
                        Switch(
                            checked = isExact,
                            onCheckedChange = { isExact = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(0.3f))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Case-Sensitive Match", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.SansSerif)
                            Text("Distinguish Mom vs mom", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                        }
                        Switch(
                            checked = isCaseSensitive,
                            onCheckedChange = { isCaseSensitive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(0.3f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Synthesizer Sound Selection dropdown
                Text("Select synthesized custom sound:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.SansSerif)
                Spacer(modifier = Modifier.height(6.dp))
                val soundOptions = listOf("Classic Sirens", "Zen Waves", "Retro Beeps", "Digital Alert", "Custom MP3 Siren", "Uploaded Siren", "System Default")
                var dropExpanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { dropExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = soundType, color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.SansSerif)
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "dropdown", tint = CyberCyan, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = dropExpanded,
                        onDismissRequest = { dropExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(Color.White)
                    ) {
                        soundOptions.forEach { sound ->
                            DropdownMenuItem(
                                text = { Text(text = sound, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.SansSerif) },
                                onClick = {
                                    soundType = sound
                                    dropExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Snooze duration slider
                Text(
                    text = "Snooze window timer: ${snoozeMinutes.toInt()} minutes",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Slider(
                    value = snoozeMinutes,
                    onValueChange = { snoozeMinutes = it },
                    valueRange = 1f..15f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = DarkCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank() && keywords.isNotBlank()) {
                            onSave(name, keywords, isAndLogic, isExact, isCaseSensitive, soundType, snoozeMinutes.toInt())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && keywords.isNotBlank()
                ) {
                    Text("SAVE FILTER CRITERIA", fontWeight = FontWeight.Black, fontFamily = FontFamily.SansSerif)
                }
            }
        }
    }
}

// Helper methods for visual styling and brushes
@Composable
fun borderGridBrush(color: Color): BorderStroke {
    return BorderStroke(1.dp, color)
}

@Composable
fun textConfigColoredBorder(color: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = color,
    unfocusedBorderColor = DarkCardBorder,
    focusedLabelColor = color,
    unfocusedLabelColor = TextSecondary,
    cursorColor = color
)

@Composable
fun TextStyleCompact() = androidx.compose.ui.text.TextStyle(
    color = TextPrimary,
    fontSize = 12.sp
)

// Standard components padding helpers
object RowDefaults {
    fun buttonPadding() = androidx.compose.foundation.layout.PaddingValues(
        horizontal = 14.dp,
        vertical = 4.dp
    )
}
