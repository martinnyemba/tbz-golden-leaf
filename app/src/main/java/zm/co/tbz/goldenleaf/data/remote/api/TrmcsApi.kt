package zm.co.tbz.goldenleaf.data.remote.api

import okhttp3.MultipartBody
import retrofit2.http.*
import zm.co.tbz.goldenleaf.data.remote.dto.*

interface TrmcsApi {
    // --- Auth ---
    @POST("auth/login/")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/token/refresh/")
    suspend fun refreshToken(@Body body: TokenRefreshRequest): TokenRefreshResponse

    @POST("auth/logout/")
    suspend fun logout(): Unit

    @POST("auth/users/change-password/")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Unit

    // --- Mobile Helpers ---
    @GET("mobile/me/")
    suspend fun mobileMe(): MobileProfileResponse

    @GET("mobile/dashboard/")
    suspend fun dashboard(): DashboardResponse

    /** Inspections, permits and sales for one grower (profile tabs). */
    @GET("mobile/growers/{id}/related/")
    suspend fun growerRelated(@Path("id") growerId: String): GrowerRelatedResponse

    @GET("mobile/reference/")
    suspend fun reference(): ReferenceBundleResponse

    /** Approved grades + matrix prices for a buyer + season (drives the bale grade picker). */
    @GET("mobile/reference/price-matrix/")
    suspend fun priceMatrix(
        @Query("buyer") buyer: String,
        @Query("season") season: String,
        @Query("tobacco_type") tobaccoType: String? = null,
    ): PriceMatrixResponse

    @GET("mobile/sync/status/")
    suspend fun syncStatus(): SyncStatusResponse

    @POST("mobile/sync/bulk/")
    suspend fun syncBulk(@Body body: SyncBulkRequest): SyncBulkResponse

    @POST("mobile/device/register/")
    suspend fun registerDevice(@Body body: DeviceRegistrationRequest): Unit

    @Multipart
    @POST("mobile/growers/{id}/documents/")
    suspend fun uploadGrowerDocuments(
        @Path("id") growerId: String,
        @Part profile_photo: MultipartBody.Part? = null,
        @Part id_front: MultipartBody.Part? = null,
        @Part id_back: MultipartBody.Part? = null,
    ): Unit

    // --- Notifications ---
    @GET("notifications/my/")
    suspend fun notifications(): List<NotificationDto>

    // --- Domain (Delta Sync) ---
    @GET("growers/growers/")
    suspend fun getGrowers(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("updated_after") updatedAfter: String? = null,
        @Query("page") page: Int? = null,
    ): PagedResponse<GrowerDto>

    @GET("permits/transport-permits/")
    suspend fun getTransportPermits(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("updated_after") updatedAfter: String? = null,
        @Query("page") page: Int? = null,
    ): PagedResponse<TransportPermitDto>

    @GET("permits/group-permits/")
    suspend fun getGroupPermits(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("updated_after") updatedAfter: String? = null,
        @Query("page") page: Int? = null,
    ): PagedResponse<GroupPermitDto>

    @GET("inspectorate/inspections/")
    suspend fun getInspections(
        @Query("search") search: String? = null,
        @Query("inspection_type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("updated_after") updatedAfter: String? = null,
        @Query("page") page: Int? = null,
    ): PagedResponse<InspectionDto>

    // --- Marketing ---
    @POST("permits/verify-qr/")
    suspend fun verifyQr(@Body body: VerifyQrRequest): VerifyQrResponse

    @POST("permits/group-permits/validate-qr/")
    suspend fun validateGroupPermitQr(@Body body: GroupPermitValidateRequest): GroupPermitValidateResponse

    @POST("permits/group-permits/")
    suspend fun createGroupPermit(@Body body: kotlinx.serialization.json.JsonObject): GroupPermitCreatedResponse

    @POST("permits/group-permits/{id}/entries/")
    suspend fun addGroupPermitEntry(
        @Path("id") groupId: String,
        @Body body: kotlinx.serialization.json.JsonObject,
    ): GroupPermitEntryDto

    @POST("permits/group-permits/{id}/submit/")
    suspend fun submitGroupPermit(@Path("id") groupId: String): GroupPermitDto

    @POST("marketing/bales/bulk-create/")
    suspend fun bulkCreateBales(@Body body: BulkBaleCreateRequest): Unit
}
