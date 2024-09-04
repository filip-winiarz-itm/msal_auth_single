package com.example.msal_auth

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication.ISingleAccountApplicationCreatedListener
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel

class Msal(context: Context, internal var activity: FlutterActivity?) {
    internal val applicationContext = context

    lateinit var clientApplication: ISingleAccountPublicClientApplication
    var currentAccount: IAccount? = null

    fun setActivity(activity: FlutterActivity) {
        this.activity = activity
    }

    internal fun isClientInitialized(): Boolean = ::clientApplication.isInitialized

    internal fun getApplicationCreatedListener(result: MethodChannel.Result): ISingleAccountApplicationCreatedListener {

        return object : ISingleAccountApplicationCreatedListener {
            override fun onCreated(application: ISingleAccountPublicClientApplication) {
                clientApplication = application
                result.success(true)
            }

            override fun onError(exception: MsalException?) {
                result.error("AUTH_ERROR", exception?.message, null)
            }
        }
    }

    internal fun getAuthCallback(result: MethodChannel.Result): AuthenticationCallback {
        return object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                Handler(Looper.getMainLooper()).post {
                    val accountMap = mutableMapOf<String, Any?>()
                    authenticationResult.account.claims?.let { accountMap.putAll(it) }
                    accountMap["access_token"] = authenticationResult.accessToken
                    accountMap["exp"] = authenticationResult.expiresOn.time
                    result.success(Gson().toJson(accountMap))
                }
            }

            override fun onError(exception: MsalException) {
                Handler(Looper.getMainLooper()).post {
                    result.error(
                        "AUTH_ERROR",
                        "Authentication failed ${exception.message}",
                        null
                    )
                }
            }

            override fun onCancel() {
                Handler(Looper.getMainLooper()).post {
                    result.error(
                        "USER_CANCELED",
                        "User has cancelled the login process",
                        null
                    )
                }
            }
        }
    }

    /**
     * Callback used in for silent acquireToken calls.
     */
    internal fun getAuthSilentCallback(result: MethodChannel.Result): SilentAuthenticationCallback {
        return object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                Handler(Looper.getMainLooper()).post {
                    val accountMap = mutableMapOf<String, Any?>()
                    authenticationResult.account.claims?.let { accountMap.putAll(it) }
                    accountMap["access_token"] = authenticationResult.accessToken
                    accountMap["exp"] = authenticationResult.expiresOn.time
                    result.success(Gson().toJson(accountMap))
                }
            }

            override fun onError(exception: MsalException) {
                when (exception) {
                    is MsalUiRequiredException -> {
                        result.error("UI_REQUIRED", exception.message, null)
                    }

                    else -> {
                        result.error("AUTH_ERROR", exception.message, null)
                    }
                }
            }
        }
    }

    /**
     * Load currently signed-in accounts, if there's any.
     */
    internal fun loadAccounts(result: MethodChannel.Result) {
        clientApplication.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {

            override fun onAccountLoaded(account: IAccount?) {
                currentAccount = account
                result.success(true)
            }

            override fun onError(exception: MsalException) {
                result.error(
                    "AUTH_ERROR",
                    "No account is available to acquire token silently for",
                    null
                )
            }

            override fun onAccountChanged(old: IAccount?, new: IAccount?) {

            }
        })
    }
}

