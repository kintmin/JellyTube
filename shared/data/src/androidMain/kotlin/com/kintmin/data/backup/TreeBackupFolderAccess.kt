package com.kintmin.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.kintmin.domain.backup.model.BackupFolderUnavailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * 사용자가 고른 폴더에 문서 제공자로 쓴다. [BackupFolderAccess] 의 `folderId` 는
 * `ACTION_OPEN_DOCUMENT_TREE` 가 준 tree Uri 이고, 영구 권한을 받아 둬야 다음 실행에서도 열린다.
 */
internal class TreeBackupFolderAccess(private val context: Context) : BackupFolderAccess {

    private val resolver get() = context.contentResolver

    override suspend fun write(folderId: String, fileName: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        asFolderAccess {
            val tree = Uri.parse(folderId)
            // 같은 이름으로 만들면 제공자가 번호를 붙인다. 있으면 그 파일을 비우고 다시 쓴다.
            val target = findChild(tree, fileName) ?: createChild(tree, fileName)
            resolver.openOutputStream(target, "wt")?.use { it.write(bytes) } ?: throw BackupFolderUnavailableException()
        }
        null
    }

    override suspend fun list(folderId: String): List<String> = withContext(Dispatchers.IO) {
        asFolderAccess { children(Uri.parse(folderId)).map { (_, displayName) -> displayName } }
    }

    override suspend fun read(folderId: String, fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        val target = asFolderAccess { findChild(Uri.parse(folderId), fileName) } ?: return@withContext null
        // 목록에는 있었는데 여는 사이에 지워졌다. 폴더는 열렸으므로 "없음" 이다.
        runCatching { resolver.openInputStream(target)?.use { it.readBytes() } }
            .getOrElse { if (it is FileNotFoundException) null else throw it }
    }

    override suspend fun delete(folderId: String, fileName: String) {
        withContext(Dispatchers.IO) {
            val target = asFolderAccess { findChild(Uri.parse(folderId), fileName) } ?: return@withContext
            runCatching { DocumentsContract.deleteDocument(resolver, target) }
        }
    }

    private fun createChild(tree: Uri, fileName: String): Uri {
        // gzip 으로 주면 제공자가 이름 뒤에 확장자를 또 붙인다.
        return DocumentsContract.createDocument(resolver, tree.documentUri(), OCTET_STREAM, fileName)
            ?: throw BackupFolderUnavailableException()
    }

    private fun findChild(tree: Uri, fileName: String): Uri? =
        children(tree).firstOrNull { (_, displayName) -> displayName == fileName }
            ?.let { (documentId, _) -> DocumentsContract.buildDocumentUriUsingTree(tree, documentId) }

    /** (document id, 보여 주는 이름) 쌍. */
    private fun children(tree: Uri): List<Pair<String, String>> {
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val children = mutableListOf<Pair<String, String>>()
        resolver.query(childrenUri, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0) ?: continue
                val displayName = cursor.getString(1) ?: continue
                children += documentId to displayName
            }
        } ?: throw BackupFolderUnavailableException()
        return children
    }

    private fun Uri.documentUri(): Uri =
        DocumentsContract.buildDocumentUriUsingTree(this, DocumentsContract.getTreeDocumentId(this))

    /** 권한이 풀렸거나 폴더가 사라진 것을 한 가지로 올린다. */
    private inline fun <T> asFolderAccess(block: () -> T): T = try {
        block()
    } catch (e: SecurityException) {
        throw BackupFolderUnavailableException(e)
    } catch (e: IllegalArgumentException) {
        // 제공자가 사라지면 Uri 를 해석하지 못한다.
        throw BackupFolderUnavailableException(e)
    } catch (e: FileNotFoundException) {
        throw BackupFolderUnavailableException(e)
    }

    private companion object {
        const val OCTET_STREAM = "application/octet-stream"
    }
}
