package zm.co.tbz.goldenleaf.data.repository

import kotlinx.coroutines.flow.Flow
import zm.co.tbz.goldenleaf.data.local.dao.InspectionDao
import zm.co.tbz.goldenleaf.data.local.entity.InspectionEntity
import zm.co.tbz.goldenleaf.data.local.entity.InspectionReportEntity
import zm.co.tbz.goldenleaf.data.local.entity.ValidationEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspectionRepository @Inject constructor(
    private val inspectionDao: InspectionDao
) {
    fun observeInspections(): Flow<List<InspectionEntity>> = inspectionDao.observeInspections()
    suspend fun upsertInspections(inspections: List<InspectionEntity>) = inspectionDao.upsertInspections(inspections)

    fun observeReports(): Flow<List<InspectionReportEntity>> = inspectionDao.observeReports()
    suspend fun upsertReport(report: InspectionReportEntity) = inspectionDao.upsertReport(report)
    suspend fun deleteReport(report: InspectionReportEntity) = inspectionDao.deleteReport(report)

    fun observeValidations(): Flow<List<ValidationEntity>> = inspectionDao.observeValidations()
    suspend fun upsertValidations(validations: List<ValidationEntity>) = inspectionDao.upsertValidations(validations)
}
