package com.voicereminder.network

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ============================================================
// Response models
// ============================================================

@JsonClass(generateAdapter = true)
data class MobileCommandResponse(
    val success: Boolean,
    val transcript: String? = null,
    val intent: String? = null,
    val task: String? = null,
    @Json(name = "date_expression") val dateExpression: String? = null,
    @Json(name = "time_expression") val timeExpression: String? = null,
    @Json(name = "scheduled_at") val scheduledAt: String? = null,
    @Json(name = "reply_text") val replyText: String = "",
    val confidence: Float = 1.0f,
    val error: String? = null,
    @Json(name = "action_type") val actionType: String? = null,
    @Json(name = "action_target") val actionTarget: String? = null,
    @Json(name = "document_title") val documentTitle: String? = null,
    @Json(name = "document_content") val documentContent: String? = null,
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val status: String,
    val service: String,
)

// ============================================================
// Auth Models
// ============================================================

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String
)

// ============================================================
// Workspace Models
// ============================================================

@JsonClass(generateAdapter = true)
data class ReportResponse(
    val id: String,
    @Json(name = "content_markdown") val contentMarkdown: String,
    @Json(name = "updated_at") val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class ProjectResponse(
    val id: String,
    val title: String,
    val status: String,
    val reports: List<ReportResponse>
)

@JsonClass(generateAdapter = true)
data class MemoryResponse(
    val id: String,
    val category: String,
    val content: String,
    @Json(name = "is_permanent") val isPermanent: Boolean
)

@JsonClass(generateAdapter = true)
data class WorkflowStepResponse(
    val id: String,
    val description: String,
    val status: String,
    @Json(name = "requires_approval") val requiresApproval: Boolean
)

@JsonClass(generateAdapter = true)
data class WorkflowResponse(
    val id: String,
    val goal: String,
    val status: String,
    val steps: List<WorkflowStepResponse>
)

// ============================================================
// Retrofit API interface
// ============================================================

@JsonClass(generateAdapter = true)
data class MobileTextCommandRequest(
    val transcript: String,
    val timezone: String = "UTC",
    @Json(name = "model_size") val modelSize: String = "1.7B"
)

interface VoiceboxApi {

    @Multipart
    @POST("api/mobile/command")
    suspend fun processVoiceCommand(
        @Part audio: MultipartBody.Part,
        @Part("timezone") timezone: RequestBody,
        @Part("model_size") modelSize: RequestBody,
    ): Response<MobileCommandResponse>

    @POST("api/mobile/command/text")
    suspend fun processTextCommand(
        @Body request: MobileTextCommandRequest
    ): Response<MobileCommandResponse>

    @GET("api/mobile/health")
    suspend fun healthCheck(): Response<HealthResponse>

    /** Login — uses OAuth2 form encoding (username/password fields). */
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<TokenResponse>

    /** Register a new user. */
    @POST("auth/register")
    suspend fun register(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("api/mobile/projects")
    suspend fun getProjects(): Response<List<ProjectResponse>>

    @GET("api/mobile/memories")
    suspend fun getMemories(): Response<List<MemoryResponse>>

    @GET("api/mobile/workflows")
    suspend fun getWorkflows(): Response<List<WorkflowResponse>>

    @POST("api/mobile/workflows/{id}/approve")
    suspend fun approveWorkflow(@Path("id") workflowId: String): Response<Map<String, Any>>

    // ============================================================
    // Zero-Memory Lightweight Backend Endpoints
    // ============================================================
    
    @POST("generate_reminder")
    @Streaming
    suspend fun generateReminder(
        @Body request: ReminderRequest
    ): Response<okhttp3.ResponseBody>
}

@JsonClass(generateAdapter = true)
data class ReminderRequest(
    val title: String,
    val description: String,
    val language: String = "Hinglish"
)

// ============================================================
// Singleton Retrofit client — resettable when base URL changes
// ============================================================

object VoiceboxApiClient {

    @Volatile
    private var instance: VoiceboxApi? = null

    fun get(): VoiceboxApi = instance ?: synchronized(this) {
        instance ?: buildApi().also { instance = it }
    }

    /** Force a rebuild of the Retrofit client (call when BASE_URL changes). */
    fun reset() {
        synchronized(this) { instance = null }
    }

    private fun buildApi(): VoiceboxApi {
        val baseUrl = AuthManager.baseUrl
        Log.d("VoiceboxApiClient", "Building Retrofit with baseUrl=$baseUrl")

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = okhttp3.Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            val token = AuthManager.accessToken
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(VoiceboxApi::class.java)
    }
}
