package ru.pacani.fitnessdiary.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import ru.pacani.fitnessdiary.BuildConfig
import ru.pacani.fitnessdiary.data.remote.dto.ExerciseInfoDto
import ru.pacani.fitnessdiary.data.remote.dto.LanguageDto
import ru.pacani.fitnessdiary.data.remote.dto.PageDto
import ru.pacani.fitnessdiary.data.remote.dto.WgerJson

/** Публичный API wger (https://wger.de/api/v2/), ключ не нужен. */
interface WgerApi {

    @GET("language/")
    suspend fun getLanguages(@Query("limit") limit: Int = 100): PageDto<LanguageDto>

    @GET("exerciseinfo/")
    suspend fun getExercises(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageDto<ExerciseInfoDto>

    companion object {
        const val BASE_URL = "https://wger.de/api/v2/"

        fun create(enableLogging: Boolean = BuildConfig.DEBUG): WgerApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .apply {
                    // Логи запросов только в debug-сборке
                    if (enableLogging) {
                        addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                    }
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(WgerJson.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(WgerApi::class.java)
        }
    }
}
