package zm.co.tbz.goldenleaf.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import zm.co.tbz.goldenleaf.data.repository.AuthRepository
import zm.co.tbz.goldenleaf.data.repository.ReferenceRepository

@HiltWorker
class ReferenceRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val referenceRepository: ReferenceRepository,
    private val authRepository: AuthRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        authRepository.refreshSessionIfNeeded()
        return if (referenceRepository.refreshReference()) Result.success() else Result.retry()
    }
}
