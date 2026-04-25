package com.barakaplaza.rentmanager.mpesa

// =====================================================================
// REPLACE these values with your actual Safaricom Daraja API credentials
// Register at: https://developer.safaricom.co.ke/
// =====================================================================
object MpesaConfig {
    // Your Daraja API Consumer Key
    const val CONSUMER_KEY    = "t3qkaGqEaUABpgsJr9eenFFOkqpAAWoRmlkQ1b2Wgc17ibij"
    // Your Daraja API Consumer Secret
    const val CONSUMER_SECRET = "Q32siYmwgAoxZTosXbYTRgqS2usntUNKORKufvUkNwxbLrB15Kk8Yvvrmk6w07zh"
    // Your M-Pesa shortcode (Paybill or Till Number)
    const val SHORTCODE       = "174379"               // Safaricom sandbox default
    // Your Lipa Na M-Pesa Online Passkey
    const val PASSKEY         = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919"
    // Callback URL – must be a publicly accessible HTTPS URL
    const val CALLBACK_URL    = "https://your-ngrok-url.ngrok.io/mpesa/callback/"

    // Use sandbox for testing, live for production
    const val BASE_URL        = "https://sandbox.safaricom.co.ke/"
    // const val BASE_URL = "https://api.safaricom.co.ke/"   // production
}

// --- Request / Response data classes ---

data class MpesaAccessTokenResponse(
    val access_token: String,
    val expires_in: String
)

data class StkPushRequest(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val TransactionType: String = "CustomerPayBillOnline",
    val Amount: String,
    val PartyA: String,          // Customer phone number
    val PartyB: String,          // Shortcode
    val PhoneNumber: String,     // Customer phone number
    val CallBackURL: String,
    val AccountReference: String,
    val TransactionDesc: String
)

data class StkPushResponse(
    val MerchantRequestID: String?,
    val CheckoutRequestID: String?,
    val ResponseCode: String?,
    val ResponseDescription: String?,
    val CustomerMessage: String?,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

data class StkQueryRequest(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val CheckoutRequestID: String
)

data class StkQueryResponse(
    val ResponseCode: String?,
    val ResponseDescription: String?,
    val MerchantRequestID: String?,
    val CheckoutRequestID: String?,
    val ResultCode: String?,
    val ResultDesc: String?
)
