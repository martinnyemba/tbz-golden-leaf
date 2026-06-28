package zm.co.tbz.goldenleaf.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import zm.co.tbz.goldenleaf.data.local.TrmsDatabase
import zm.co.tbz.goldenleaf.data.local.dao.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrmsDatabase {
        return Room.databaseBuilder(
            context,
            TrmsDatabase::class.java,
            "trms_db",
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSyncDao(db: TrmsDatabase): SyncDao = db.syncDao()

    @Provides
    fun provideReferenceDao(db: TrmsDatabase): ReferenceDao = db.referenceDao()

    @Provides
    fun provideGrowerDao(db: TrmsDatabase): GrowerDao = db.growerDao()

    @Provides
    fun providePermitDao(db: TrmsDatabase): PermitDao = db.permitDao()

    @Provides
    fun provideInspectionDao(db: TrmsDatabase): InspectionDao = db.inspectionDao()

    @Provides
    fun provideMarketingDao(db: TrmsDatabase): MarketingDao = db.marketingDao()

    @Provides
    fun provideUtilityDao(db: TrmsDatabase): UtilityDao = db.utilityDao()
}
