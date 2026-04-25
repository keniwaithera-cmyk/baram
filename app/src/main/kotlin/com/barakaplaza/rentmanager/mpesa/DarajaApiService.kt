package com.barakaplaza.rentmanager.mpesa

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface DarajaApiService {

    /** Step 1 – Get OAuth access token */
    @GET("oauth/v1/generate?grant_type=client_credentials")
    suspend fun getAccessToken(
        @Header("Authorization") credentials: String
    ): Response<MpesaAccessTokenResponse>

    /** Step 2 – Initiate STK Push */
    @POST("mpesa/stkpush/v1/processrequest")
    suspend fun initiateStkPush(
        @Header("Authorization") authorization: String,
        @Body request: StkPushRequest
    ): Response<StkPushResponse>

    /** Step 3 – Query STK Push status */
    @POST("mpesa/stkpushquery/v1/query")
    suspend fun queryStkStatus(
        @Header("Authorization") authorization: String,
        @Body request: StkQueryRequest
    ): Response<StkQueryResponse>
}

object DarajaClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: DarajaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(MpesaConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DarajaApiService::class.java)
    }
}
