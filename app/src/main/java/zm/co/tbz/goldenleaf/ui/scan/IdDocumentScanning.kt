package zm.co.tbz.goldenleaf.ui.scan

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared ID document pick → scan → persist pipeline for registration flows. */
object IdDocumentScanning {

    /** Only NRC front/back picks run through OpenCV; profile and farm photos stay as-is. */
    fun shouldScan(target: String): Boolean = target == TARGET_ID_FRONT || target == TARGET_ID_BACK

    const val TARGET_ID_FRONT = "front"
    const val TARGET_ID_BACK = "back"
    const val TARGET_PROFILE = "profile"

    /**
     * Loads [uri], optionally runs [DocumentScanner.scanDocument], saves to app storage,
     * and returns a `file://` or content URI string. Falls back to the original [uri] on failure.
     */
    suspend fun processPick(context: Context, uri: Uri, target: String): String {
        if (!shouldScan(target)) return uri.toString()
        return withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = ScannedImageStore.loadBitmap(context, uri) ?: return@runCatching null
                val scanned = DocumentScanner.scanDocument(bitmap)
                ScannedImageStore.save(context, scanned, target).toString()
            }.getOrNull() ?: uri.toString()
        }
    }
}
