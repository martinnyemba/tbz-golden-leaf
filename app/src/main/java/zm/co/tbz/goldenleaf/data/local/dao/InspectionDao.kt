package zm.co.tbz.goldenleaf.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.entity.InspectionEntity
import zm.co.tbz.goldenleaf.data.local.entity.InspectionReportEntity
import zm.co.tbz.goldenleaf.data.local.entity.ValidationEntity

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspections ORDER BY scheduled_date DESC")
    fun observeInspections(): Flow<List<InspectionEntity>>

    @Upsert
    suspend fun upsertInspections(inspections: List<InspectionEntity>)

    @Query("SELECT * FROM inspection_reports ORDER BY created_at DESC")
    fun observeReports(): Flow<List<InspectionReportEntity>>

    @Upsert
    suspend fun upsertReport(report: InspectionReportEntity)

    @Delete
    suspend fun deleteReport(report: InspectionReportEntity): Int

    @Query("SELECT * FROM validations ORDER BY updated_at_local DESC")
    fun observeValidations(): Flow<List<ValidationEntity>>

    @Upsert
    suspend fun upsertValidations(validations: List<ValidationEntity>)
}
