package com.example.volatiledemandforecasting.data.imports

import com.example.volatiledemandforecasting.core.network.apiCall
import com.example.volatiledemandforecasting.data.cache.ResponseCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

object ImportStatus {
    const val UPLOADED = "UPLOADED"
    const val NEEDS_ATTENTION = "NEEDS_ATTENTION"
    const val READY = "READY"
    const val IMPORTED = "IMPORTED"
    const val FAILED = "FAILED"

    val filterable = listOf(UPLOADED, NEEDS_ATTENTION, READY, IMPORTED, FAILED)
}

object OverlapPolicy {
    const val SKIP = "SKIP"
    const val REPLACE = "REPLACE"
}

/** The canonical fields a CSV column can be mapped to (api.md, "Map and validate"). */
object ImportField {
    const val SALES_DATE = "salesDate"
    const val UNITS_SOLD = "unitsSold"
    const val PRODUCT_ID = "productId"
    const val PRODUCT_NAME = "productName"
    const val STORE_ID = "storeId"
    const val UNIT_PRICE = "unitPrice"
    const val CATEGORY = "category"
    const val UNIT_OF_MEASURE = "unitOfMeasure"
    const val STORE_CLOSED = "storeClosed"

    /** Shown in this order. */
    val all = listOf(SALES_DATE, UNITS_SOLD, PRODUCT_ID, PRODUCT_NAME, UNIT_PRICE, CATEGORY, UNIT_OF_MEASURE, STORE_ID, STORE_CLOSED)
    val required = setOf(SALES_DATE, UNITS_SOLD)
}

@Serializable
data class SuggestedColumnDto(val column: String, val confidence: String = "HIGH") {
    /** `CHECK` entries are highlighted so the user confirms them. */
    val needsCheck: Boolean get() = confidence == "CHECK"
}

@Serializable
data class DetectedDto(
    val delimiter: String? = null,
    val preview: List<JsonObject> = emptyList(),
    val suggestedMapping: Map<String, SuggestedColumnDto> = emptyMap(),
    val dateFormats: List<String> = emptyList(),
    val suggestedDateFormat: String? = null,
    val dateFormatAmbiguous: Boolean = false,
    val unusedColumns: List<String> = emptyList(),
)

@Serializable
data class DateRangeDto(val from: String? = null, val to: String? = null)

@Serializable
data class ImportSummaryDto(
    val rowsRead: Int = 0,
    val rowsAccepted: Int = 0,
    val rowsRejected: Int = 0,
    val rowsWithWarnings: Int = 0,
    val rowsSummedAsDuplicates: Int = 0,
    val dailyRecords: Int = 0,
    val zeroFilledDays: Int = 0,
    val closedDays: Int = 0,
    val dateRange: DateRangeDto? = null,
    val products: Int = 0,
    val newProducts: Int = 0,
    val stores: List<String> = emptyList(),
    val overlappingDays: Int = 0,
    val errorsByCode: Map<String, Int> = emptyMap(),
    val warningsByCode: Map<String, Int> = emptyMap(),
    val issuesTruncated: Boolean = false,
    val unusedColumns: List<String> = emptyList(),
)

@Serializable
data class ImportResultDto(
    val overlapPolicy: String? = null,
    val recordsInserted: Int = 0,
    val recordsReplaced: Int = 0,
    val overlapsSkipped: Int = 0,
    val productsCreated: Int = 0,
    val demandClassesUpdated: Int = 0,
    val recordsDeleted: Int? = null,
)

@Serializable
data class ImportDto(
    val id: String,
    val storeId: String,
    val filename: String,
    val sizeBytes: Long = 0,
    val status: String,
    val createdAt: String? = null,
    val validatedAt: String? = null,
    val importedAt: String? = null,
    val columns: List<String> = emptyList(),
    val detected: DetectedDto? = null,
    val mapping: JsonObject? = null,
    val missingDaysAreZero: Boolean? = null,
    val summary: ImportSummaryDto? = null,
    val result: ImportResultDto? = null,
)

@Serializable
data class ImportPageDto(val page: Int, val pageSize: Int, val total: Int, val items: List<ImportDto>)

@Serializable
data class MappingRequest(
    val columns: Map<String, String>,
    /** Only needed when the file's dates are ambiguous. */
    val dateFormat: String? = null,
    val missingDaysAreZero: Boolean,
)

@Serializable
data class ConfirmRequest(val overlapPolicy: String)

@Serializable
data class PreviewRowDto(
    val storeId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val date: String? = null,
    val unitsSold: Double? = null,
    val unitPrice: Double? = null,
    val category: String? = null,
    val sourceRows: Int? = null,
    val zeroFilled: Boolean = false,
)

@Serializable
data class ValidationDto(
    val importId: String,
    val status: String,
    val summary: ImportSummaryDto? = null,
    val preview: List<PreviewRowDto> = emptyList(),
)

@Serializable
data class ImportIssueDto(
    val severity: String,
    val code: String,
    /** The line in the file, where the header is line 1. */
    val rowNumber: Int? = null,
    val column: String? = null,
    val value: String? = null,
    val message: String = "",
)

@Serializable
data class ImportIssuePageDto(val page: Int = 1, val pageSize: Int = 0, val total: Int = 0, val items: List<ImportIssueDto> = emptyList())

interface ImportApi {
    @Multipart
    @POST("v1/stores/{storeId}/imports")
    suspend fun upload(
        @Path("storeId") storeId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Part file: MultipartBody.Part,
    ): ImportDto

    @PUT("v1/imports/{importId}/mapping")
    suspend fun mapping(@Path("importId") importId: String, @Body request: MappingRequest): ImportDto

    @GET("v1/imports/{importId}/validation")
    suspend fun validation(@Path("importId") importId: String): ValidationDto

    @GET("v1/imports/{importId}/errors")
    suspend fun issues(
        @Path("importId") importId: String,
        @Query("severity") severity: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): ImportIssuePageDto

    @POST("v1/imports/{importId}/confirm")
    suspend fun confirm(@Path("importId") importId: String, @Body request: ConfirmRequest): ImportDto

    @GET("v1/imports/{importId}")
    suspend fun import(@Path("importId") importId: String): ImportDto

    @DELETE("v1/imports/{importId}")
    suspend fun delete(@Path("importId") importId: String): ImportDto

    @GET("v1/stores/{storeId}/imports")
    suspend fun list(
        @Path("storeId") storeId: String,
        @Query("status") status: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): ImportPageDto
}

class ImportRepository(private val api: ImportApi, private val json: Json, private val cache: ResponseCache? = null) {

    /** [idempotencyKey] is one per user action and is reused when that same action is retried. */
    suspend fun upload(storeId: String, idempotencyKey: String, filename: String, bytes: ByteArray): ImportDto {
        val body = bytes.toRequestBody("text/csv".toMediaType())
        val part = MultipartBody.Part.createFormData("file", filename, body)
        return apiCall(json) { api.upload(storeId, idempotencyKey, part) }
    }

    suspend fun mapping(importId: String, request: MappingRequest): ImportDto = apiCall(json) { api.mapping(importId, request) }
    suspend fun validation(importId: String): ValidationDto = apiCall(json) { api.validation(importId) }
    suspend fun issues(importId: String, severity: String?, page: Int = 1, pageSize: Int = 50): ImportIssuePageDto =
        apiCall(json) { api.issues(importId, severity, page, pageSize) }

    suspend fun confirm(importId: String, overlapPolicy: String): ImportDto =
        apiCall(json) { api.confirm(importId, ConfirmRequest(overlapPolicy)) }

    suspend fun delete(importId: String): ImportDto = apiCall(json) { api.delete(importId) }

    suspend fun import(importId: String): ImportDto = apiCall(json) { api.import(importId) }

    suspend fun list(storeId: String, status: String?, page: Int, pageSize: Int): ImportPageDto =
        cache?.load("imports:$storeId:$status:$page:$pageSize", ImportPageDto.serializer()) { apiCall(json) { api.list(storeId, status, page, pageSize) } }
            ?: apiCall(json) { api.list(storeId, status, page, pageSize) }
}
