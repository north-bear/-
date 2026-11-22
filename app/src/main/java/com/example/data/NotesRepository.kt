package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NotesRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("请先使用 Google 账号登录")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "登录账号已变更，已取消跨账号操作" }
        }
        return uid
    }

    fun observeNotes(userId: String): Flow<List<TreeNote>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/notes"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("notes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TreeNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<TreeNote> { it.pinnedToTree }
                            .thenBy { if (it.treeTier in 1L..4L) it.treeTier else 99L }
                            .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveNote(
        userId: String,
        existingNoteId: String?,
        title: String,
        content: String,
        color: NoteColor,
        pinnedToTree: Boolean,
        checklistMode: Boolean,
        checklistItems: List<ChecklistItem>,
        treeTier: Long
    ): Result<String> {
        val cleanTitle = title.trim().take(MAX_TITLE_LENGTH)
        val cleanContent = content.trim().take(MAX_CONTENT_LENGTH)
        val cleanItems = checklistItems
            .take(MAX_CHECKLIST_ITEMS)
            .mapNotNull { item ->
                val text = item.text.trim().take(200)
                if (text.isEmpty() && item.itemId.isEmpty()) null
                else mapOf(
                    "itemId" to item.itemId.ifBlank { java.util.UUID.randomUUID().toString().take(12) },
                    "text" to text,
                    "checked" to item.checked
                )
            }
        val cleanTier = treeTier.coerceIn(0L, 4L)

        var docPath = "users/$userId/notes"
        return try {
            val uid = requireUserId(userId)
            val notesRef = db.collection("users").document(uid).collection("notes")
            if (existingNoteId.isNullOrBlank()) {
                val docRef = notesRef.document()
                docPath = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "content" to cleanContent,
                    "colorKey" to color.name,
                    "pinnedToTree" to pinnedToTree,
                    "checklistMode" to checklistMode,
                    "checklistItems" to cleanItems,
                    "treeTier" to cleanTier,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = notesRef.document(existingNoteId)
                docPath = docRef.path
                val payload = mapOf(
                    "title" to cleanTitle,
                    "content" to cleanContent,
                    "colorKey" to color.name,
                    "pinnedToTree" to pinnedToTree,
                    "checklistMode" to checklistMode,
                    "checklistItems" to cleanItems,
                    "treeTier" to cleanTier,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
                Result.success(existingNoteId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (existingNoteId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
            handleFirestoreError(e, op, docPath)
            Result.failure(e)
        }
    }

    suspend fun togglePinToTree(
        userId: String,
        noteId: String,
        pinnedToTree: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/notes/$noteId"
        return try {
            val uid = requireUserId(userId)
            require(noteId.isNotBlank()) { "Note ID must not be blank" }
            db.collection("users").document(uid)
                .collection("notes").document(noteId)
                .update(
                    mapOf(
                        "pinnedToTree" to pinnedToTree,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleChecklistItem(
        userId: String,
        note: TreeNote,
        itemId: String
    ): Result<Unit> {
        val docPath = "users/$userId/notes/${note.id}"
        return try {
            val uid = requireUserId(userId)
            require(note.id.isNotBlank()) { "Note ID must not be blank" }
            val updatedList = note.checklistItems
                .take(MAX_CHECKLIST_ITEMS)
                .map { item ->
                    val nextChecked = if (item.itemId == itemId) !item.checked else item.checked
                    mapOf(
                        "itemId" to item.itemId,
                        "text" to item.text.take(200),
                        "checked" to nextChecked
                    )
                }
            db.collection("users").document(uid)
                .collection("notes").document(note.id)
                .update(
                    mapOf(
                        "checklistItems" to updatedList,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        val docPath = "users/$userId/notes/$noteId"
        return try {
            val uid = requireUserId(userId)
            require(noteId.isNotBlank()) { "Note ID must not be blank" }
            db.collection("users").document(uid)
                .collection("notes").document(noteId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 120
        const val MAX_CONTENT_LENGTH = 4000
        const val MAX_CHECKLIST_ITEMS = 30
    }
}
