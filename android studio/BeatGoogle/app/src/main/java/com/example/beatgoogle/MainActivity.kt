package com.example.beatgoogle

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.beatgoogle.ui.theme.BeatGoogleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Retrofit API 接口和数据结构
interface ApiService {
    @GET("search")
    suspend fun search(@Query("query") query: String): ApiResponse
}

data class ApiResponse(
    val status: String,
    val results: Map<String, String> // title -> url
)

class MainActivity : ComponentActivity() {

    // 配置 OkHttpClient，取消超时
    private val apiService: ApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS) // 设置为无限
            .readTimeout(0, TimeUnit.SECONDS) // 设置为无限
            .writeTimeout(0, TimeUnit.SECONDS) // 设置为无限
            .build()

        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/") // 模拟器访问后端
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BeatGoogleTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    SearchScreen(
                        apiService = apiService,
                        activity = this,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    fun SearchScreen(
        apiService: ApiService,
        activity: ComponentActivity,
        modifier: Modifier = Modifier
    ) {
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 搜索输入框
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Enter search query") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // 搜索按钮
            Button(
                onClick = {
                    if (query.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        results = emptyMap()
                        performSearch(
                            activity = activity,
                            apiService = apiService,
                            query = query
                        ) { response, error ->
                            isLoading = false
                            if (response != null) {
                                results = response.results
                            } else {
                                errorMessage = error
                            }
                        }
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(if (isLoading) "Searching..." else "Search")
            }

            // 加载状态或错误消息
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                // 搜索结果展示
                LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                    items(results.entries.toList(), key = { it.key }) { entry ->
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = entry.key, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = entry.value,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    private fun performSearch(
        activity: ComponentActivity,
        apiService: ApiService,
        query: String,
        onResult: (ApiResponse?, String?) -> Unit
    ) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.search(query)
                withContext(Dispatchers.Main) {
                    onResult(response, null)
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onResult(null, e.message)
                }
            }
        }
    }
}
