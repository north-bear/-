package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChecklistItem
import com.example.data.NoteColor
import com.example.data.NotesRepository
import com.example.data.TreeNote
import com.example.data.UiState
import com.example.data.WidgetCacheStore
import com.example.data.WidgetConfig
import com.example.widget.ChristmasTreeWidgetProvider
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesViewModel(
    private val repository: NotesRepository,
    private val appContext: Context,
    private val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedColorFilter = MutableStateFlow<NoteColor?>(null)
    val selectedColorFilter: StateFlow<NoteColor?> = _selectedColorFilter.asStateFlow()

    private val _widgetConfig = MutableStateFlow(WidgetConfig())
    val widgetConfig: StateFlow<WidgetConfig> = _widgetConfig.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

    init {
        reloadWidgetConfig()
    }

    val notesState: StateFlow<UiState<List<TreeNote>>> = repository.observeNotes(currentUserId)
        .onEach { notes ->
            withContext(Dispatchers.Default) {
                WidgetCacheStore.saveNotes(appContext, signedIn = true, notes = notes)
                ChristmasTreeWidgetProvider.updateAllWidgets(appContext)
            }
        }
        .map<List<TreeNote>, UiState<List<TreeNote>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing notes", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "同步记事失败，请检查网络"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectColorFilter(color: NoteColor?) {
        _selectedColorFilter.value = if (_selectedColorFilter.value == color) null else color
    }

    fun updateWidgetConfig(transform: (WidgetConfig) -> WidgetConfig) {
        val updated = transform(_widgetConfig.value)
        _widgetConfig.value = updated
        viewModelScope.launch(Dispatchers.Default) {
            WidgetCacheStore.saveConfig(appContext, updated)
            ChristmasTreeWidgetProvider.updateAllWidgets(appContext)
        }
    }

    fun reloadWidgetConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            _widgetConfig.value = WidgetCacheStore.loadConfig(appContext)
        }
    }

    fun saveNote(
        existingNoteId: String?,
        title: String,
        content: String,
        color: NoteColor,
        pinnedToTree: Boolean,
        checklistMode: Boolean,
        checklistItems: List<ChecklistItem>,
        treeTier: Long,
        onSuccess: () -> Unit
    ) {
        val hasAnyChecklist = checklistMode && checklistItems.any { it.text.isNotBlank() }
        if (title.isBlank() && content.isBlank() && !hasAnyChecklist) {
            _toastMessages.tryEmit("请输入标题、正文或至少一项待办内容")
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveNote(
                userId = currentUserId,
                existingNoteId = existingNoteId,
                title = title,
                content = content,
                color = color,
                pinnedToTree = pinnedToTree,
                checklistMode = checklistMode,
                checklistItems = checklistItems,
                treeTier = treeTier
            )
            _isSaving.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                _toastMessages.tryEmit(err.localizedMessage ?: "保存失败，请重试")
            }
        }
    }

    fun togglePinToTree(note: TreeNote) {
        viewModelScope.launch {
            val nextPinned = !note.pinnedToTree
            val result = repository.togglePinToTree(
                userId = currentUserId,
                noteId = note.id,
                pinnedToTree = nextPinned
            )
            result.onSuccess {
                _toastMessages.tryEmit(
                    if (nextPinned) "已置顶到 2×2 樱花小组件" else "已从 2×2 小组件取消置顶"
                )
            }.onFailure { err ->
                _toastMessages.tryEmit(err.localizedMessage ?: "操作失败")
            }
        }
    }

    fun toggleChecklistItem(note: TreeNote, itemId: String) {
        viewModelScope.launch {
            repository.toggleChecklistItem(currentUserId, note, itemId)
                .onFailure { err ->
                    _toastMessages.tryEmit(err.localizedMessage ?: "更新待办状态失败")
                }
        }
    }

    fun deleteNote(noteId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteNote(currentUserId, noteId)
                .onSuccess {
                    _toastMessages.tryEmit("便利贴已删除")
                    onDeleted()
                }
                .onFailure { err ->
                    _toastMessages.tryEmit(err.localizedMessage ?: "删除失败")
                }
        }
    }

    private companion object {
        const val TAG = "NotesVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
