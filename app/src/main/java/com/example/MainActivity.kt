package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.GoogleAuthHelper
import com.example.data.NotesRepository
import com.example.data.TreeNote
import com.example.data.UiState
import com.example.ui.NotesViewModel
import com.example.ui.screens.NoteEditorSheet
import com.example.ui.screens.NotesGridScreen
import com.example.ui.screens.TreeStudioScreen
import com.example.ui.screens.WidgetSettingsScreen
import com.example.ui.theme.PineNoteTheme
import com.example.widget.ChristmasTreeWidgetProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppTab(val label: String) {
    TREE("樱花微件"),
    NOTES("全部记事"),
    WIDGET("小组件")
}

private data class WidgetLaunchRequest(
    val action: String? = null,
    val noteId: String? = null,
    val nonce: Long = System.nanoTime()
)

class MainActivity : ComponentActivity() {
    private val launchRequestState = mutableStateOf<WidgetLaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        parseWidgetIntent(intent)
        setContent {
            PineNoteTheme {
                AppContent(
                    launchRequest = launchRequestState.value,
                    onConsumeLaunchRequest = { launchRequestState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseWidgetIntent(intent)
    }

    private fun parseWidgetIntent(intent: Intent?) {
        val action = intent?.getStringExtra(ChristmasTreeWidgetProvider.EXTRA_WIDGET_ACTION)
        val noteId = intent?.getStringExtra(ChristmasTreeWidgetProvider.EXTRA_NOTE_ID)
        if (action != null || noteId != null) {
            launchRequestState.value = WidgetLaunchRequest(action = action, noteId = noteId)
        }
    }
}

@Composable
private fun AppContent(
    launchRequest: WidgetLaunchRequest?,
    onConsumeLaunchRequest: () -> Unit
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            PineNoteMainScreen(
                currentUserId = user.uid,
                userEmail = user.email,
                launchRequest = launchRequest,
                onConsumeLaunchRequest = onConsumeLaunchRequest
            )
        }
    }
}

@Composable
private fun PineNoteMainScreen(
    currentUserId: String,
    userEmail: String?,
    launchRequest: WidgetLaunchRequest?,
    onConsumeLaunchRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: NotesViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NotesViewModel(
                    repository = NotesRepository(db),
                    appContext = app.applicationContext,
                    currentUserId = currentUserId
                )
            }
        }
    )

    val notesState by viewModel.notesState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedColor by viewModel.selectedColorFilter.collectAsStateWithLifecycle()
    val widgetConfig by viewModel.widgetConfig.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AppTab.TREE) }
    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<TreeNote?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.reloadWidgetConfig()
        viewModel.toastMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(launchRequest, notesState) {
        val req = launchRequest ?: return@LaunchedEffect
        when (req.action) {
            ChristmasTreeWidgetProvider.ACTION_QUICK_ADD -> {
                editingNote = null
                showEditor = true
                onConsumeLaunchRequest()
            }
            ChristmasTreeWidgetProvider.ACTION_OPEN_NOTE -> {
                val successState = notesState as? UiState.Success
                if (successState != null) {
                    val matched = successState.data.firstOrNull { it.id == req.noteId }
                    if (matched != null) {
                        editingNote = matched
                        showEditor = true
                    }
                    onConsumeLaunchRequest()
                }
            }
            else -> onConsumeLaunchRequest()
        }
    }

    val openNewNote = {
        editingNote = null
        showEditor = true
    }

    val openEditNote: (TreeNote) -> Unit = { note ->
        editingNote = note
        showEditor = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                if (currentTab != AppTab.WIDGET) {
                    FloatingActionButton(
                        onClick = openNewNote,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .let { if (useRail) it.navigationBarsPadding() else it }
                            .testTag("fab_create_note")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "新建便利贴"
                        )
                    }
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.statusBarsPadding()
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .statusBarsPadding()
                ) {
                    when (currentTab) {
                        AppTab.TREE -> TreeStudioScreen(
                            notesState = notesState,
                            widgetConfig = widgetConfig,
                            onCreateNote = openNewNote,
                            onEditNote = openEditNote,
                            onTogglePin = viewModel::togglePinToTree,
                            onUpdateConfig = viewModel::updateWidgetConfig
                        )
                        AppTab.NOTES -> NotesGridScreen(
                            notesState = notesState,
                            searchQuery = searchQuery,
                            selectedColor = selectedColor,
                            onSearchChange = viewModel::updateSearchQuery,
                            onColorSelect = viewModel::selectColorFilter,
                            onCreateNote = openNewNote,
                            onEditNote = openEditNote,
                            onTogglePin = viewModel::togglePinToTree,
                            onToggleChecklistItem = viewModel::toggleChecklistItem
                        )
                        AppTab.WIDGET -> WidgetSettingsScreen(
                            userEmail = userEmail,
                            widgetConfig = widgetConfig,
                            onUpdateConfig = viewModel::updateWidgetConfig,
                            onSignOut = {
                                GoogleAuthHelper.signOut(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        NoteEditorSheet(
            editingNote = editingNote,
            isSaving = isSaving,
            onDismiss = { showEditor = false },
            onSave = { existingId, title, content, color, pinnedToTree, checklistMode, items, tier ->
                viewModel.saveNote(
                    existingNoteId = existingId,
                    title = title,
                    content = content,
                    color = color,
                    pinnedToTree = pinnedToTree,
                    checklistMode = checklistMode,
                    checklistItems = items,
                    treeTier = tier,
                    onSuccess = { showEditor = false }
                )
            },
            onDelete = { noteId ->
                viewModel.deleteNote(noteId) { showEditor = false }
            }
        )
    }
}

private fun tabIcon(tab: AppTab, selected: Boolean) = when (tab) {
    AppTab.TREE -> if (selected) Icons.Rounded.LocalFlorist else Icons.Outlined.LocalFlorist
    AppTab.NOTES -> if (selected) Icons.Rounded.Description else Icons.Outlined.Description
    AppTab.WIDGET -> if (selected) Icons.Rounded.Widgets else Icons.Outlined.Widgets
}
