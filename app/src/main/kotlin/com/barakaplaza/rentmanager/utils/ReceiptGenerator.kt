package com.barakaplaza.rentmanager.utils

import android.content.Context
import android.util.Log
import com.barakaplaza.rentmanager.models.Payment
import com.barakaplaza.rentmanager.models.Tenant
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReceiptGenerator {

    private const val TAG = "ReceiptGenerator"

    fun generateReceipt(context: Context, tenant: Tenant, payment: Payment, rentAmount: Double): String? {
        return try {
            val receiptsDir = File(context.filesDir, "receipts").also { it.mkdirs() }
            val fileName = "receipt_${payment.referenceNumber}_${System.currentTimeMillis()}.txt"
            val receiptFile = File(receiptsDir, fileName)
            FileOutputStream(receiptFile).use { it.write(buildReceiptContent(tenant, payment, rentAmount).toByteArray()) }
            Log.d(TAG, "Receipt saved: ${receiptFile.absolutePath}")
            receiptFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error generating receipt: ${e.message}")
            null
        }
    }

    private fun buildReceiptContent(tenant: Tenant, payment: Payment, rentAmount: Double): String {
        val sep  = "================================================\n"
        val thin = "------------------------------------------------\n"
        val balance = payment.amount - rentAmount
        val mpesaLine = if (payment.mpesaCode.isNotEmpty()) "M-Pesa:   ${payment.mpesaCode}\n" else ""
        val notesLine = if (payment.notes.isNotEmpty()) "Notes:    ${payment.notes}\n" else ""
        val balanceLine = if (balance >= 0)
            "Overpaid:     KSh ${String.format("%,.0f", balance)}\n"
        else
            "Balance Due:  KSh ${String.format("%,.0f", Math.abs(balance))}\n"

        return buildString {
            append(sep)
            append("           BARAKA PLAZA\n")
            append("       RENT PAYMENT RECEIPT\n")
            append(sep)
            append("\nReceipt No: ${payment.referenceNumber}\n")
            append("Date: ${payment.paymentDate}\n\n")
            append(thin)
            append("TENANT DETAILS\n")
            append(thin)
            append("Name:     ${tenant.name}\n")
            append("Phone:    ${tenant.phone}\n")
            append("ID No:    ${tenant.idNumber}\n")
            append("House No: ${tenant.houseNumber}\n\n")
            append(thin)
            append("PAYMENT DETAILS\n")
            append(thin)
            append("Period:   ${payment.paymentMonth} ${payment.paymentYear}\n")
            append("Amount:   KSh ${String.format("%,.0f", payment.amount)}\n")
            append("Method:   ${payment.paymentMethod}\n")
            append(mpesaLine)
            append("Status:   ${payment.status}\n")
            append(notesLine)
            append("\n")
            append(thin)
            append("Monthly Rent: KSh ${String.format("%,.0f", rentAmount)}\n")
            append(balanceLine)
            append("\n")
            append(sep)
            append("  Thank you for your payment!\n")
            append("  BARAKA PLAZA Management\n")
            append("  Next rent due: 5th of next month\n")
            append(sep)
            append("\nGenerated: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
        }
    }

    fun readReceipt(path: String): String? =
        try { File(path).takeIf { it.exists() }?.readText() } catch (e: Exception) { null }

    fun generateReferenceNumber(houseNumber: String): String {
        val ts = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(Date())
        return "BP-${houseNumber.uppercase()}-$ts"
    }
}
