package zm.co.tbz.goldenleaf.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import zm.co.tbz.goldenleaf.data.repository.AuthRepository

@HiltWorker
class SessionRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val ok = authRepository.refreshSessionIfNeeded()
        return if (ok) Result.success() else Result.retry()
    }
}
