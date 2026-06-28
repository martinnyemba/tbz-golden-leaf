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

    @GET("mobile/reference/")
    suspend fun reference(): ReferenceBundleResponse

    @GET("mobile/sync/status/")
    suspend fun syncStatus(): SyncStatusResponse

    @POST("mobile/sync/bulk/")
    suspend fun syncBulk(@Body body: SyncBulkRequest): SyncBulkResponse

    @POST("mobile/device/register/")
    suspend fun registerDevice(@Body body: DeviceRegistrationRequest): Unit

    @Multipart
    @POST("mobile/growers/{id}/documents/")
    suspend fun uploadGrowerDocument(
        @Path("id") growerId: String,
        @Part file: MultipartBody.Part,
        @Part("document_type") documentType: String
    ): Unit

    // --- Notifications ---
    @GET("notifications/my/")
    suspend fun notifications(): List<NotificationDto>

    // --- Domain (Delta Sync) ---
    @GET("growers/growers/")
    suspend fun getGrowers(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("updated_after") updatedAfter: String? = null
    ): PagedResponse<GrowerDto>

    @GET("permits/transport-permits/")
    suspend fun getTransportPermits(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("updated_after") updatedAfter: String? = null
    ): PagedResponse<TransportPermitDto>

    @GET("inspectorate/inspections/")
    suspend fun getInspections(
        @Query("search") search: String? = null,
        @Query("inspection_type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("updated_after") updatedAfter: String? = null
    ): PagedResponse<InspectionDto>

    // --- Marketing ---
    @POST("permits/verify-qr/")
    suspend fun verifyQr(@Body body: VerifyQrRequest): VerifyQrResponse

    @POST("marketing/bales/bulk-create/")
    suspend fun bulkCreateBales(@Body body: BulkBaleCreateRequest): Unit
}
