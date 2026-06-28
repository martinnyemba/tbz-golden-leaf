package zm.co.tbz.goldenleaf.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import zm.co.tbz.goldenleaf.data.local.preferences.UserPreferences
import zm.co.tbz.goldenleaf.data.local.entity.BarnTypeEntity
import zm.co.tbz.goldenleaf.data.local.entity.BuyerEntity
import zm.co.tbz.goldenleaf.data.local.entity.DistrictEntity
import zm.co.tbz.goldenleaf.data.local.entity.ProvinceEntity
import zm.co.tbz.goldenleaf.data.local.entity.SalesFloorEntity
import zm.co.tbz.goldenleaf.data.local.entity.SponsorEntity
import zm.co.tbz.goldenleaf.data.local.entity.TobaccoTypeEntity
import zm.co.tbz.goldenleaf.data.remote.api.TrmcsApi
import zm.co.tbz.goldenleaf.data.repository.AuthRepository
import zm.co.tbz.goldenleaf.data.repository.ReferenceRepository

@HiltWorker
class ReferenceRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: TrmcsApi,
    private val referenceRepository: ReferenceRepository,
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!authRepository.refreshSessionIfNeeded()) return Result.retry()
        return try {
            val bundle = api.reference()
            referenceRepository.upsertProvinces(
                bundle.provinces.map { ProvinceEntity(it.id, it.name, it.code) },
            )
            referenceRepository.upsertDistricts(
                bundle.districts.map { DistrictEntity(it.id, it.name, it.province_id) },
            )
            referenceRepository.upsertSponsors(
                bundle.sponsors.map { SponsorEntity(it.id, it.name) },
            )
            referenceRepository.upsertSalesFloors(
                bundle.salesfloors.map { SalesFloorEntity(it.id, it.name, it.location) },
            )
            referenceRepository.upsertBuyers(
                bundle.buyers.map { BuyerEntity(it.id, it.name) },
            )
            referenceRepository.upsertTobaccoTypes(
                bundle.tobacco_types.map { TobaccoTypeEntity(it.id, it.name, it.code) },
            )
            referenceRepository.upsertBarnTypes(
                bundle.barn_types.map { BarnTypeEntity(it.id, it.name) },
            )
            userPreferences.setReferenceVersion(bundle.version)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
