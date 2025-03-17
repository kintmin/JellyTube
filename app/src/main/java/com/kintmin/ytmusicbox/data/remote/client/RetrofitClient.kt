import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kintmin.ytmusicbox.data.remote.api.YoutubeDownloadApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val retrofit = Retrofit.Builder()
        .client(client)
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: YoutubeDownloadApi = retrofit.create(YoutubeDownloadApi::class.java)
}
