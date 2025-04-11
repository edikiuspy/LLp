package com.example.llpclient.data.remote

import android.util.Log
import com.example.llpclient.data.local.AuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider




class TokenAuthenticator @Inject constructor(
    private val authManagerProvider: Provider<AuthManager>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val authManager = authManagerProvider.get()
        val currentToken = runBlocking { authManager.getAuthToken() }



        val failedToken = bearerToken(response.request)
        if (response.code != 401 || (failedToken != null && failedToken == currentToken)) {


            Log.w("TokenAuthenticator", "Giving up authentication. Code: ${response.code}. Failed token matches current: ${failedToken == currentToken}")

        }

        Log.i("TokenAuthenticator", "Authentication required (401). Attempting re-authentication via login.")

        synchronized(this) {

            val potentiallyNewToken = runBlocking { authManager.getAuthToken() }

            if (currentToken != potentiallyNewToken || (failedToken != null && failedToken != potentiallyNewToken)) {
                Log.i("TokenAuthenticator", "Token changed concurrently. Retrying with potentially new token.")

                return if (potentiallyNewToken != null) newRequestWithToken(response.request, potentiallyNewToken) else null
            }


            Log.d("TokenAuthenticator", "Calling reAuthenticate...")
            val reAuthToken = runBlocking {
                authManager.reAuthenticate()
            }

            return if (reAuthToken != null) {
                Log.i("TokenAuthenticator", "Re-authentication successful. Retrying original request with new token.")
                newRequestWithToken(response.request, reAuthToken)
            } else {
                Log.e("TokenAuthenticator", "Re-authentication failed.")


                null
            }
        }
    }

    private fun newRequestWithToken(request: Request, token: String): Request {

        return request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun bearerToken(request: Request): String? {
        val header = request.header("Authorization")
        return if (header != null && header.startsWith("Bearer ", ignoreCase = true)) {
            header.substring(7)
        } else {
            null
        }
    }
}