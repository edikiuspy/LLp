package com.example.llpclient

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLDecoder
import java.util.Date

class AuthRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .cookieJar(SessionCookieJar())
        .addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            Log.d("AuthRepository", "Request: ${request.method} ${request.url}")
            Log.d("AuthRepository", "Request Headers: ${request.headers}")
            Log.d("AuthRepository", "Response: ${response.code} ${response.message}")
            Log.d("AuthRepository", "Response Headers: ${response.headers}")

            response.peekBody(Long.MAX_VALUE).string().takeIf { it.isNotEmpty() }?.let { body ->
                Log.d("AuthRepository", "Response Body: $body")
            }

            response
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.librus.pl/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(LibrusApiService::class.java)
    private val cookieJar = client.cookieJar as SessionCookieJar

    suspend fun isLoggedIn(): Boolean {
        return withContext(Dispatchers.IO) {
            userDao.getLoggedInUser() != null
        }
    }

    suspend fun login(username: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val initialParams = mapOf(
                    "client_id" to "46",
                    "response_type" to "code",
                    "scope" to "mydata"
                )
                val initialAuthResponse = apiService.initializeAuth(initialParams)
                Log.d("AuthRepository", "Step 1 - Initialize Auth Response: ${initialAuthResponse.code()}")
                initialAuthResponse.body()?.string()?.let{
                    Log.d("AuthRepository", "Step 1 - Initialize Auth Response Body: $it")
                }

                val loginParams = mapOf("client_id" to "46")
                val loginResponse = apiService.login(
                    params = loginParams,
                    action = "login",
                    login = username,
                    pass = password
                )
                Log.d("AuthRepository", "Step 2 - Login Response: ${loginResponse.code()}")
                loginResponse.body()?.string()?.let{
                    Log.d("AuthRepository", "Step 2 - Login Response Body: $it")
                }

                val performLoginResponse = apiService.performLogin(loginParams)
                Log.d("AuthRepository", "Step 3 - Perform Login Response: ${performLoginResponse.code()}")
                performLoginResponse.body()?.string()?.let{
                    Log.d("AuthRepository", "Step 3 - Perform Login Response Body: $it")
                }
                val token = cookieJar.getCookie("oauth_token")?.let {
                    URLDecoder.decode(it, "UTF-8")
                } ?: return@withContext Result.failure(Exception("Failed to get auth token"))

                val response = apiService.getUserData("Bearer $token")
                Log.d("AuthRepository", "Step 4 - Get User Data Response: ${response.code()}")
                response.body()?.toString()?.let{
                    Log.d("AuthRepository", "Step 4 - Get User Data Response Body: $it")
                }

                if (response.isSuccessful) {
                    val user = UserEntity(
                        id = username,
                        username = username,
                        authToken = token,
                        refreshToken = null,
                        lastLogin = Date().time
                    )
                    userDao.insertUser(user)
                    Result.success(true)
                } else {
                    Result.failure(Exception("Authentication failed"))
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Login Exception: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            userDao.deleteAllUsers()
            cookieJar.clearCookies()
        }
    }
}

class SessionCookieJar : CookieJar {
    private val cookieStore = HashMap<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        Log.d("SessionCookieJar", "Saving cookies for ${url.host}: $cookies")
        cookieStore[url.host] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = cookieStore[url.host] ?: emptyList()
        Log.d("SessionCookieJar", "Loading cookies for ${url.host}: $cookies")
        return cookies
    }

    fun getCookie(name: String): String? {
        for (cookies in cookieStore.values) {
            for (cookie in cookies) {
                if (cookie.name == name) {
                    Log.d("SessionCookieJar", "Found cookie: $name = ${cookie.value}")
                    return cookie.value
                }
            }
        }
        Log.d("SessionCookieJar", "Cookie not found: $name")
        return null
    }

    fun clearCookies() {
        Log.d("SessionCookieJar", "Clearing all cookies")
        cookieStore.clear()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        cookieStore.forEach { entry ->
            sb.append("${entry.key}=${entry.value} ")
        }
        return sb.toString()
    }
}