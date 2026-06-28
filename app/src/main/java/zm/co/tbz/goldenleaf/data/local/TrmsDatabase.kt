package zm.co.tbz.goldenleaf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import zm.co.tbz.goldenleaf.data.local.dao.*
import zm.co.tbz.goldenleaf.data.local.entity.*

@Database(
    entities = [
        SyncCursorEntity::class,
        OfflineQueueEntity::class,
        ProvinceEntity::class,
        DistrictEntity::class,
        SponsorEntity::class,
        SalesFloorEntity::class,
        BuyerEntity::class,
        TobaccoTypeEntity::class,
        BarnTypeEntity::class,
        GrowerEntity::class,
        GrowerRegistrationEntity::class,
        GrowerEditEntity::class,
        TransportPermitEntity::class,
        GroupPermitEntity::class,
        PermitRequestEntity::class,
        InspectionEntity::class,
        InspectionReportEntity::class,
        ValidationEntity::class,
        BaleEntity::class,
        PendingSaleEntity::class,
        NotificationEntity::class,
        SyncAuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TrmsDatabase : RoomDatabase() {
    abstract fun syncDao(): SyncDao
    abstract fun referenceDao(): ReferenceDao
    abstract fun growerDao(): GrowerDao
    abstract fun permitDao(): PermitDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun marketingDao(): MarketingDao
    abstract fun utilityDao(): UtilityDao
}
