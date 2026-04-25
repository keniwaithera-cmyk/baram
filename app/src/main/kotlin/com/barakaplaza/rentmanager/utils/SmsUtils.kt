package com.barakaplaza.rentmanager.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.barakaplaza.rentmanager.models.Payment
import com.barakaplaza.rentmanager.models.Tenant

object SmsUtils {

    private const val TAG = "SmsUtils"
    const val SMS_PERMISSION_REQUEST = 101

    fun hasSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED

    fun requestSmsPermission(activity: Activity) =
        ActivityCompat.requestPermissions(activity,
            arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST)

    fun sendPaymentConfirmationToTenant(context: Context, tenant: Tenant, payment: Payment) {
        val msg = """
            Dear ${tenant.name},
            Your rent payment of KSh ${String.format("%,.0f", payment.amount)} for House ${tenant.houseNumber} has been received.
            Ref: ${payment.referenceNumber}
            Method: ${payment.paymentMethod}
            Date: ${payment.paymentDate}
            Thank you - BARAKA PLAZA Management
        """.trimIndent()
        sendSms(context, tenant.phone, msg)
    }

    fun sendPaymentNotificationToLandlord(context: Context, landlordPhone: String,
                                           tenant: Tenant, payment: Payment) {
        val mpesaLine = if (payment.mpesaCode.isNotEmpty()) "M-Pesa Code: ${payment.mpesaCode}\n" else ""
        val msg = """
            BARAKA PLAZA PAYMENT ALERT
            Tenant: ${tenant.name}
            House: ${tenant.houseNumber}
            Amount: KSh ${String.format("%,.0f", payment.amount)}
            Method: ${payment.paymentMethod}
            ${mpesaLine}Ref: ${payment.referenceNumber}
            Date: ${payment.paymentDate}
        """.trimIndent()
        sendSms(context, landlordPhone, msg)
    }

    fun sendNewTenantNotification(context: Context, landlordPhone: String,
                                   tenant: Tenant, rentAmount: Double) {
        val msg = """
            NEW TENANT REGISTERED - BARAKA PLAZA
            Name: ${tenant.name}
            Phone: ${tenant.phone}
            House: ${tenant.houseNumber}
            Monthly Rent: KSh ${String.format("%,.0f", rentAmount)}
            Move-in: ${tenant.moveInDate}
            ID: ${tenant.idNumber}
        """.trimIndent()
        sendSms(context, landlordPhone, msg)
    }

    fun sendRegistrationConfirmation(context: Context, tenant: Tenant, rentAmount: Double,
                                      paybill: String, account: String, mpesaNo: String) {
        val msg = """
            Welcome to BARAKA PLAZA!
            Dear ${tenant.name}, you have been registered.
            House: ${tenant.houseNumber}
            Monthly Rent: KSh ${String.format("%,.0f", rentAmount)}
            
            Payment Options:
            1. M-PESA: Send to $mpesaNo
            2. Paybill: $paybill Account: $account
            Rent due on 5th every month.
            - BARAKA PLAZA Management
        """.trimIndent()
        sendSms(context, tenant.phone, msg)
    }

    fun sendMonthlyReminder(context: Context, tenant: Tenant, rentAmount: Double, deadline: String) {
        val msg = """
            RENT REMINDER - BARAKA PLAZA
            Dear ${tenant.name},
            Your rent of KSh ${String.format("%,.0f", rentAmount)} for House ${tenant.houseNumber} is due by $deadline.
            Please pay on time to avoid penalties.
            - BARAKA PLAZA Management
        """.trimIndent()
        sendSms(context, tenant.phone, msg)
    }

    fun sendOverdueReminder(context: Context, tenant: Tenant, rentAmount: Double, daysOverdue: Int) {
        val msg = """
            OVERDUE RENT NOTICE - BARAKA PLAZA
            Dear ${tenant.name},
            Your rent of KSh ${String.format("%,.0f", rentAmount)} for House ${tenant.houseNumber} is $daysOverdue days overdue.
            Please pay IMMEDIATELY to avoid legal action.
            - BARAKA PLAZA Management
        """.trimIndent()
        sendSms(context, tenant.phone, msg)
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        if (!hasSmsPermission(context)) {
            Log.w(TAG, "SMS permission not granted")
            Toast.makeText(context, "SMS permission required to send notifications", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val formatted = formatKenyanPhone(phoneNumber)
            val smsManager = SmsManager.getDefault()
            if (message.length > 160) {
                smsManager.sendMultipartTextMessage(formatted, null,
                    smsManager.divideMessage(message), null, null)
            } else {
                smsManager.sendTextMessage(formatted, null, message, null, null)
            }
            Log.d(TAG, "SMS sent to: $formatted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}")
            Toast.makeText(context, "Failed to send SMS notification", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatKenyanPhone(phone: String): String {
        val cleaned = phone.trim().replace("\\s+".toRegex(), "")
        return when {
            cleaned.startsWith("+254") -> cleaned
            cleaned.startsWith("0")    -> "+254${cleaned.removePrefix("0")}"
            cleaned.startsWith("254")  -> "+$cleaned"
            else                       -> cleaned
        }
    }
}
