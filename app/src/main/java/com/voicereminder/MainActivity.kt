package com.voicereminder

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.currentBackStackEntryAsState
import com.voicereminder.ui.AppBottomNavigation
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.voicereminder.voice.VoiceActivationOverlay
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.voicereminder.notification.NotificationHelper
import com.voicereminder.ui.DashboardScreen
import com.voicereminder.ui.ConversationHistoryScreen
import com.voicereminder.ui.DocumentAIViewScreen
import com.voicereminder.ui.DocumentsScreen
import com.voicereminder.ui.MemoryScreen
import com.voicereminder.ui.NotesScreen
import com.voicereminder.ui.ReminderDetailScreen
import com.voicereminder.ui.RemindersScreen
import com.voicereminder.ui.ResearchScreen
import com.voicereminder.ui.SettingsScreen
import com.voicereminder.ui.TasksScreen
import com.voicereminder.ui.theme.VoiceReminderTheme
import com.voicereminder.viewmodel.ReminderViewModel

// ============================================================
// Application class
// ============================================================

class VoiceReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Create notification channel on app start (safe to call repeatedly)
        NotificationHelper.createNotificationChannel(this)
        com.voicereminder.network.AuthManager.init(this)
    }
}

// ============================================================
// Navigation routes
// ============================================================

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val REMINDERS = "reminders"
    const val TASKS = "tasks"
    const val NOTES = "notes"
    const val DOCUMENTS = "documents"
    const val DOCUMENT_AI = "document_ai"
    const val RESEARCH = "research"
    const val MEMORY = "memory"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val WORKSPACE = "workspace"
    const val REMINDER_DETAIL = "reminder_detail"
}

// ============================================================
// MainActivity
// ============================================================

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoiceReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    VoiceReminderAppUI()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.voicereminder.voice.VoiceSessionManager.isAppInForeground = true
    }

    override fun onPause() {
        super.onPause()
        com.voicereminder.voice.VoiceSessionManager.isAppInForeground = false
    }
}

// ============================================================
// Root composable with permission handling + navigation
// ============================================================

@Composable
private fun VoiceReminderAppUI() {
    val navController = rememberNavController()
    val viewModel: ReminderViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val allReminders by viewModel.allReminders.collectAsState()

    // ── Permission handling ─────────────────────────────────
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasMicPermission = granted
            if (granted) viewModel.onMicPermissionGranted()
        },
    )

    // ── Overlay permission handling ────────────────────────
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Request permission immediately on first launch, and start engine if already granted
    LaunchedEffect(Unit) {
        if (hasMicPermission) {
            viewModel.onMicPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Navigation ──────────────────────────────────────────
    val startDest = if (com.voicereminder.network.AuthManager.accessToken == null) Routes.LOGIN else Routes.HOME

    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(Routes.HOME, Routes.REMINDERS, Routes.TASKS, Routes.NOTES, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigateToHome = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    },
                    onNavigateToReminders = { navController.navigate(Routes.REMINDERS) { popUpTo(Routes.HOME) } },
                    onNavigateToTasks = { navController.navigate(Routes.TASKS) { popUpTo(Routes.HOME) } },
                    onNavigateToNotes = { navController.navigate(Routes.NOTES) { popUpTo(Routes.HOME) } },
                    onNavigateToMore = { navController.navigate(Routes.SETTINGS) { popUpTo(Routes.HOME) } }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

        NavHost(navController = navController, startDestination = startDest) {

        composable(Routes.LOGIN) {
            com.voicereminder.ui.LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { /* TODO: Register Screen */ }
            )
        }

        composable(Routes.HOME) {
            DashboardScreen(
                uiState = uiState,
                reminders = allReminders,
                onToggleReminder = { viewModel.updateReminder(it.copy(isCompleted = !it.isCompleted)) },
                onStartRecording = {
                    if (hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = { viewModel.stopRecordingAndProcess() },
                onCancelRecording = { viewModel.cancelRecording() },
                onDismiss = { viewModel.dismissState() },
                onNavigateToReminders = { navController.navigate(Routes.REMINDERS) },
                onNavigateToTasks = { navController.navigate(Routes.TASKS) },
                onNavigateToNotes = { navController.navigate(Routes.NOTES) },
                onNavigateToDocuments = { navController.navigate(Routes.DOCUMENTS) },
                onNavigateToResearch = { navController.navigate(Routes.RESEARCH) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToMemory = { navController.navigate(Routes.MEMORY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onSubmitText = { text -> viewModel.submitManualText(text) },
                hasOverlayPermission = hasOverlayPermission,
                onRequestOverlayPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            )
        }

        composable(Routes.REMINDERS) {
            RemindersScreen(
                reminders = allReminders,
                uiState = uiState,
                onOpenDetail = { reminder -> navController.navigate("${Routes.REMINDER_DETAIL}/${reminder.id}") },
                onComplete = { viewModel.markReminderCompleted(it) },
                onSnooze = { viewModel.snoozeReminder(it) },
                onReschedule = { viewModel.rescheduleReminder(it) },
                onDelete = { viewModel.deleteReminder(it) },
                onStartRecording = {
                    if (hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = { viewModel.stopRecordingAndProcess() },
                onCancelRecording = { viewModel.cancelRecording() },
                onDismissState = { viewModel.dismissState() },
                onAttachFile = { },
                onCaptureDocument = { },
                onOpenCamera = { },
                onSubmitText = { text -> viewModel.submitManualText(text) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.TASKS) {
            TasksScreen(
                tasks = allReminders,
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onOpenDetail = { reminder -> navController.navigate("${Routes.REMINDER_DETAIL}/${reminder.id}") },
                onComplete = { viewModel.markReminderCompleted(it) },
                onSnooze = { viewModel.snoozeReminder(it) },
                onReschedule = { viewModel.rescheduleReminder(it) },
                onDelete = { viewModel.deleteReminder(it) },
                onStartRecording = {
                    if (hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = { viewModel.stopRecordingAndProcess() },
                onCancelRecording = { viewModel.cancelRecording() },
                onDismissState = { viewModel.dismissState() },
                onSubmitText = { text -> viewModel.submitManualText(text) },
            )
        }

        composable(Routes.NOTES) {
            NotesScreen(
                notes = allReminders,
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onOpenDetail = { note -> navController.navigate("${Routes.REMINDER_DETAIL}/${note.id}") },
                onDelete = { viewModel.deleteReminder(it) },
                onStartRecording = {
                    if (hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = { viewModel.stopRecordingAndProcess() },
                onCancelRecording = { viewModel.cancelRecording() },
                onDismissState = { viewModel.dismissState() },
                onSubmitText = { text -> viewModel.submitManualText(text) },
            )
        }

        composable(Routes.DOCUMENTS) {
            DocumentsScreen(
                onBack = { navController.popBackStack() },
                onOpenDocument = { name -> navController.navigate("${Routes.DOCUMENT_AI}/${name}") },
                onStartRecording = {
                    if (hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = { viewModel.stopRecordingAndProcess() },
                onCancelRecording = { viewModel.cancelRecording() },
                onDismissState = { viewModel.dismissState() },
                onAttachFile = { },
                onCaptureDocument = { },
                onOpenCamera = { },
                onSubmitText = { text -> viewModel.submitManualText(text) },
            )
        }

        composable(
            route = "${Routes.DOCUMENT_AI}/{name}",
            arguments = listOf(navArgument("name") { type = NavType.StringType }),
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Document"
            DocumentAIViewScreen(
                documentName = name,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.RESEARCH) {
            ResearchScreen(
                onBack = { navController.popBackStack() },
                onStartRecording = {
                    if (hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = { viewModel.stopRecordingAndProcess() },
                onCancelRecording = { viewModel.cancelRecording() },
                onDismissState = { viewModel.dismissState() },
                onAttachFile = { },
                onCaptureDocument = { },
                onOpenCamera = { },
                onSubmitText = { text -> viewModel.submitManualText(text) },
            )
        }

        composable(Routes.MEMORY) {
            MemoryScreen(
                onBack = { navController.popBackStack() },
                onStartRecording = {
                    if (hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = { viewModel.stopRecordingAndProcess() },
                onCancelRecording = { viewModel.cancelRecording() },
                onDismissState = { viewModel.dismissState() },
                onAttachFile = { },
                onCaptureDocument = { },
                onOpenCamera = { },
                onSubmitText = { text -> viewModel.submitManualText(text) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.HISTORY) {
            ConversationHistoryScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onContinueInteraction = { draft -> viewModel.submitManualText(draft) },
                onStartRecording = {
                    if (hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = { viewModel.stopRecordingAndProcess() },
                onCancelRecording = { viewModel.cancelRecording() },
                onDismissState = { viewModel.dismissState() },
                onAttachFile = { },
                onCaptureDocument = { },
                onOpenCamera = { },
                onSubmitText = { text -> viewModel.submitManualText(text) },
            )
        }

        composable(
            route = "${Routes.REMINDER_DETAIL}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString("id")
            val reminder = allReminders.firstOrNull { it.id == reminderId }
            if (reminder != null) {
                ReminderDetailScreen(
                    reminder = reminder,
                    onBack = { navController.popBackStack() },
                    onSave = { viewModel.updateReminder(it) },
                    onDelete = { viewModel.deleteReminder(it) },
                )
            }
        }

        composable(Routes.WORKSPACE) {
            com.voicereminder.ui.WorkspaceScreen(
                uiState = uiState,
                onStartRecording = {
                    if (hasMicPermission) {
                        viewModel.startRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecording = { viewModel.stopRecordingAndProcess() },
                onCancelRecording = { viewModel.cancelRecording() },
                onDismissState = { viewModel.dismissState() },
                onAttachFile = { },
                onCaptureDocument = { },
                onOpenCamera = { },
                onSubmitText = { text -> viewModel.submitManualText(text) },
                onBack = { navController.popBackStack() },
            )
        }
    }

        val voiceUiState by viewModel.voiceUiState.collectAsState()
        VoiceActivationOverlay(uiState = voiceUiState, onCancel = { viewModel.cancelRecording() })
    }
    }
}
