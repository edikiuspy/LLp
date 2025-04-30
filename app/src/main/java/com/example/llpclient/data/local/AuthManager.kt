package com.example.llpclient.data.local

import android.content.Context
import android.util.Log
import com.example.llpclient.data.local.schema.UserDao
import com.example.llpclient.data.local.schema.UserEntity
import com.example.llpclient.data.remote.LibrusApiService
import com.example.llpclient.data.remote.TokenAuthenticator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLDecoder
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenAuthenticatorProvider: Provider<TokenAuthenticator>,
    private val userDao: UserDao
) {



    private val cookieJar = SessionCookieJar()
    private val tokenMutex = Mutex()
    private var currentToken: String? = null
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {


        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            loadTokenFromDatabase()
            _isLoggedIn.value = currentToken != null
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .cookieJar(cookieJar)
            .authenticator(tokenAuthenticatorProvider.get())
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val token = currentToken
                val newRequest = if (token != null && originalRequest.header("Authorization") == null && !isAuthUrl(originalRequest.url.toString())) {
                    Log.d("AuthManagerInterceptor", "Adding auth token to request: ${originalRequest.url}")
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    originalRequest
                }
                chain.proceed(newRequest)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun isAuthUrl(url: String): Boolean {
        return url.contains("OAuth/Authorization") || url.contains("refreshToken") || url.contains("loguj")
    }

    val apiService: LibrusApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.librus.pl/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibrusApiService::class.java)
    }

    val messageService: LibrusApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://wiadomosci.librus.pl/").client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibrusApiService::class.java)
    }
    val synergyService: LibrusApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://synergia.librus.pl/").client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibrusApiService::class.java)
    }
    private suspend fun loadTokenFromDatabase() {



        Log.d("AuthManager", "Attempting to load token from database...")
        val user = withContext(Dispatchers.IO) {
            try {
                userDao.getLoggedInUser()
            } catch (e: Exception) {
                Log.e("AuthManager", "Error loading user from database", e)
                null
            }
        }
        currentToken = user?.authToken
        Log.d("AuthManager", "Loaded token from DB. Token is ${if (currentToken != null) "present" else "absent"}.")

        _isLoggedIn.value = currentToken != null
    }

    suspend fun getAuthToken(): String? {
        tokenMutex.withLock {
            Log.v("AuthManager", "getAuthToken: Accessing token under lock. Current state is ${if (currentToken != null) "present" else "absent"}.")
            Log.v("AuthManager", currentToken.toString())
            return currentToken
        }
    }
    private suspend fun saveAuthToken(token: String) {

        withContext(Dispatchers.IO) {
            tokenMutex.withLock {
                val user = userDao.getLoggedInUser()
                if (user != null) {
                    userDao.insertUser(user.copy(authToken = token, lastLogin = Date().time))
                    currentToken = token
                    _isLoggedIn.value = true
                    Log.i("AuthManager", "Saved new auth token to DB and memory.")
                } else {
                    Log.e("AuthManager", "Cannot save token, no logged in user found in DB.")
                    currentToken = null
                    _isLoggedIn.value = false
                }
            }
        }
    }

    private suspend fun clearAuthToken() {

        withContext(Dispatchers.IO) {
            tokenMutex.withLock {
                currentToken = null
                _isLoggedIn.value = false
                userDao.deleteAllUsers()
                cookieJar.clearCookies()
                Log.i("AuthManager", "Cleared auth token, user data, and cookies.")
            }
        }
    }




    suspend fun reAuthenticate(): String? {
        Log.i("AuthManager", "Attempting automatic re-authentication using stored credentials (INSECURE).")
        val user = withContext(Dispatchers.IO) { userDao.getLoggedInUser() }

        if (user?.username == null || user.passwordForRelogin == null) {
            Log.e("AuthManager", "Re-authentication failed: No stored user or credentials found.")
            logout()
            return null
        }

        val username = user.username
        val password = user.passwordForRelogin

        return try {

            val initialParams = mapOf("client_id" to "46", "response_type" to "code", "scope" to "mydata")
            apiService.initializeAuth(initialParams)
            val loginParams = mapOf("client_id" to "46")
            apiService.login(params = loginParams, action = "login", login = username, pass = password)
            apiService.performLogin(loginParams)


            val newToken = cookieJar.getCookie("oauth_token")?.let {
                try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { null }
            }

            if (newToken != null) {
                Log.i("AuthManager", "Re-authentication successful. Got new token.")

                saveAuthToken(newToken)
                ensureMessageServiceAccess()
                newToken
            } else {
                Log.e("AuthManager", "Re-authentication failed: Could not extract oauth_token cookie after re-login steps.")
                logout()
                null
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Exception during re-authentication", e)
            null
        }
    }


    suspend fun login(username: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                clearAuthToken()


                val initialParams = mapOf("client_id" to "46", "response_type" to "code", "scope" to "mydata")
                apiService.initializeAuth(initialParams)
                val loginParams = mapOf("client_id" to "46")
                apiService.login(params = loginParams, action = "login", login = username, pass = password)
                apiService.performLogin(loginParams)

                val token = cookieJar.getCookie("oauth_token")?.let {
                    try { URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { null }
                }

                if (token == null) {
                    Log.e("AuthManager", "Login failed: Could not extract oauth_token cookie after Step 3.")
                    clearAuthToken()
                    return@withContext Result.failure(Exception("Failed to get auth token from cookies during login"))
                }

                Log.i("AuthManager", "Login successful, got token. Saving user and credentials (INSECURE).")

                val user = UserEntity(
                    id = username,
                    username = username,
                    authToken = token,
                    refreshToken = null,
                    lastLogin = Date().time,
                    passwordForRelogin = password
                )
                userDao.insertUser(user)

                tokenMutex.withLock {
                    currentToken = token
                    _isLoggedIn.value = true
                }
                ensureMessageServiceAccess()

                Result.success(true)

            } catch (e: Exception) {
                Log.e("AuthManager", "Login Exception: ${e.message}", e)
                clearAuthToken()
                Result.failure(e)
            }
        }
    }
    private suspend fun ensureMessageServiceAccess() {
        try {
            synergyService.visitMessages()
            val sessionCookies = cookieJar.getCookiesForDomain("synergia.librus.pl")
            Log.d("AuthManager", "Synergia cookies after visit: $sessionCookies")
            messageService.initializeMessages()
            val messageCookies = cookieJar.getCookiesForDomain("wiadomosci.librus.pl")
            Log.d("AuthManager", "Messages cookies after initialization: $messageCookies")
            Log.d("AuthManager", "All cookies after initialization:\n$cookieJar")
        } catch (e: Exception) {
            Log.e("AuthManager", "Error ensuring message service access", e)
        }
    }

    suspend fun logout() {

        clearAuthToken()
    }
}




class SessionCookieJar : CookieJar {

    private val cookieStore = HashMap<String, MutableList<Cookie>>()
    private val lock = Any()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {

            val validCookies = cookies.filter { it.expiresAt > System.currentTimeMillis() }
            if (validCookies.isNotEmpty()) {
                Log.d("SessionCookieJar", "Saving ${validCookies.size} valid cookies for ${url.host}: $validCookies")

                val hostCookies = cookieStore.getOrPut(url.host) { mutableListOf() }
                validCookies.forEach { newCookie ->
                    hostCookies.removeAll { it.name == newCookie.name }
                    hostCookies.add(newCookie)
                }
            } else {

                if (cookies.isNotEmpty()) {
                    Log.d("SessionCookieJar", "All cookies received for ${url.host} were expired.")
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(lock) {

            val hostCookies = cookieStore[url.host]
            hostCookies?.removeAll { it.expiresAt <= System.currentTimeMillis() }

            val isLibrusDomain = url.host.endsWith("librus.pl")

            val cookiesForUrl = if (isLibrusDomain) {
                val allLibrusCookies = mutableListOf<Cookie>()
                cookieStore.filterKeys { it.endsWith("librus.pl") }.values.forEach { cookies ->
                    allLibrusCookies.addAll(cookies.filter { it.matches(url) })
                }
                allLibrusCookies
            } else {
                hostCookies?.filter { it.matches(url) } ?: emptyList()
            }

            Log.d("SessionCookieJar", "Loading ${cookiesForUrl.size} cookies for ${url.host}: $cookiesForUrl")
            return cookiesForUrl
        }
    }

    fun getCookie(name: String): String? {
        synchronized(lock) {
            for (cookies in cookieStore.values) {

                cookies.removeAll { it.expiresAt <= System.currentTimeMillis() }
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
    }

    fun getCookiesForDomain(domain: String): List<Cookie> {
        synchronized(lock) {
            val cookies = cookieStore[domain] ?: return emptyList()
            cookies.removeAll { it.expiresAt <= System.currentTimeMillis() }
            return cookies.toList()
        }
    }

    fun clearCookies() {
        synchronized(lock) {
            Log.d("SessionCookieJar", "Clearing all cookies")
            cookieStore.clear()
        }
    }

    override fun toString(): String {
        synchronized(lock) {
            val sb = StringBuilder()
            cookieStore.forEach { (host, cookies) ->
                sb.append("$host: ${cookies.joinToString { "${it.name}=${it.value}" }}\n")
            }
            return sb.toString()
        }
    }

}