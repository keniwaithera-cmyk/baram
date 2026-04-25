package com.barakaplaza.rentmanager.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return

        for (pdu in pdus) {
            val sms = SmsMessage.createFromPdu(pdu as ByteArray)
            val sender = sms.displayOriginatingAddress ?: continue
            val body = sms.messageBody

            if (sender.equals("MPESA", ignoreCase = true) ||
                sender.equals("M-PESA", ignoreCase = true)) {
                Log.d(TAG, "M-Pesa SMS received: $body")
                parseMpesaConfirmation(context, body)
            }
        }
    }

    private fun parseMpesaConfirmation(context: Context, body: String) {
        try {
            var mpesaCode: String? = null
            var amount = 0.0

            val parts = body.split(" ")
            if (parts.isNotEmpty()) mpesaCode = parts[0]

            if (body.contains("Ksh", ignoreCase = true)) {
                val kshIndex = body.lowercase().indexOf("ksh")
                if (kshIndex >= 0) {
                    val afterKsh = body.substring(kshIndex + 3).trim()
                    val amountStr = afterKsh.split(" ")[0].replace(",", "")
                    amount = amountStr.toDoubleOrNull() ?: 0.0
                }
            }

            context.sendBroadcast(Intent("com.barakaplaza.MPESA_RECEIVED").apply {
                putExtra("mpesa_code", mpesaCode)
                putExtra("amount", amount)
                putExtra("full_message", body)
            })

            Log.d(TAG, "M-Pesa parsed – Code: $mpesaCode, Amount: $amount")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing M-Pesa SMS: ${e.message}")
        }
    }
}
