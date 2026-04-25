package com.barakaplaza.rentmanager.mpesa

import android.util.Base64
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

sealed class MpesaResult<out T> {
    data class Success<T>(val data: T) : MpesaResult<T>()
    data class Error(val message: String) : MpesaResult<Nothing>()
}

object MpesaRepository {

    private const val TAG = "MpesaRepository"

    // ----------------------------------------------------------------
    // Step 1: Get access token using Consumer Key + Secret
    // ----------------------------------------------------------------
    suspend fun getAccessToken(): MpesaResult<String> {
        return try {
            val credentials = "${MpesaConfig.CONSUMER_KEY}:${MpesaConfig.CONSUMER_SECRET}"
            val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            val response = DarajaClient.apiService.getAccessToken("Basic $encoded")

            if (response.isSuccessful) {
                val token = response.body()?.access_token
                if (!token.isNullOrEmpty()) {
                    Log.d(TAG, "Access token obtained successfully")
                    MpesaResult.Success(token)
                } else {
                    MpesaResult.Error("Empty access token received")
                }
            } else {
                MpesaResult.Error("Auth failed: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting access token: ${e.message}")
            MpesaResult.Error("Network error: ${e.message ?: "Unknown error"}")
        }
    }

    // ----------------------------------------------------------------
    // Step 2: Initiate STK Push
    // phoneNumber  – customer's Kenyan number e.g. 254712345678
    // amount       – amount in KSh (integer)
    // accountRef   – e.g. "House A1"
    // description  – e.g. "Rent Payment"
    // ----------------------------------------------------------------
    suspend fun initiateStkPush(
        phoneNumber: String,
        amount: Int,
        accountRef: String,
        description: String
    ): MpesaResult<StkPushResponse> {

        // 1. Fetch token
        val tokenResult = getAccessToken()
        if (tokenResult is MpesaResult.Error) return tokenResult

        val accessToken = (tokenResult as MpesaResult.Success).data

        return try {
            val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
            val rawPassword = "${MpesaConfig.SHORTCODE}${MpesaConfig.PASSKEY}$timestamp"
            val password = Base64.encodeToString(rawPassword.toByteArray(), Base64.NO_WRAP)

            val request = StkPushRequest(
                BusinessShortCode = MpesaConfig.SHORTCODE,
                Password          = password,
                Timestamp         = timestamp,
                Amount            = amount.toString(),
                PartyA            = phoneNumber,
                PartyB            = MpesaConfig.SHORTCODE,
                PhoneNumber       = phoneNumber,
                CallBackURL       = MpesaConfig.CALLBACK_URL,
                AccountReference  = accountRef,
                TransactionDesc   = description
            )

            val response = DarajaClient.apiService.initiateStkPush(
                "Bearer $accessToken", request
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.ResponseCode == "0") {
                    Log.d(TAG, "STK Push sent: ${body.CustomerMessage}")
                    MpesaResult.Success(body)
                } else {
                    MpesaResult.Error(
                        body?.ResponseDescription ?: body?.errorMessage ?: "STK Push failed"
                    )
                }
            } else {
                MpesaResult.Error("STK Push error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "STK Push exception: ${e.message}")
            MpesaResult.Error("Network error: ${e.message ?: "Unknown error"}")
        }
    }

    // ----------------------------------------------------------------
    // Step 3: Query STK Push status
    // ----------------------------------------------------------------
    suspend fun queryStkStatus(checkoutRequestId: String): MpesaResult<StkQueryResponse> {
        val tokenResult = getAccessToken()
        if (tokenResult is MpesaResult.Error) return tokenResult
        val accessToken = (tokenResult as MpesaResult.Success).data

        return try {
            val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
            val rawPassword = "${MpesaConfig.SHORTCODE}${MpesaConfig.PASSKEY}$timestamp"
            val password = Base64.encodeToString(rawPassword.toByteArray(), Base64.NO_WRAP)

            val request = StkQueryRequest(
                BusinessShortCode  = MpesaConfig.SHORTCODE,
                Password           = password,
                Timestamp          = timestamp,
                CheckoutRequestID  = checkoutRequestId
            )

            val response = DarajaClient.apiService.queryStkStatus("Bearer $accessToken", request)

            if (response.isSuccessful) {
                MpesaResult.Success(response.body()!!)
            } else {
                MpesaResult.Error("Query failed: ${response.code()}")
            }
        } catch (e: Exception) {
            MpesaResult.Error("Query error: ${e.message ?: "Unknown"}")
        }
    }

    /** Format any Kenyan phone to 254XXXXXXXXX for Daraja */
    fun formatPhoneForDaraja(phone: String): String {
        val cleaned = phone.trim().replace("\\s+".toRegex(), "")
        return when {
            cleaned.startsWith("+254") -> cleaned.removePrefix("+")
            cleaned.startsWith("0")    -> "254${cleaned.removePrefix("0")}"
            cleaned.startsWith("254")  -> cleaned
            else                       -> "254$cleaned"
        }
    }
}
