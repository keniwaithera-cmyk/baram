package com.barakaplaza.rentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.utils.ReceiptGenerator

class ReceiptViewActivity : AppCompatActivity() {

    private lateinit var tvReceiptContent: TextView
    private lateinit var btnShare: Button
    private lateinit var btnDone: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_view)

        tvReceiptContent = findViewById(R.id.tvReceiptContent)
        btnShare         = findViewById(R.id.btnShare)
        btnDone          = findViewById(R.id.btnDone)

        val receiptPath = intent.getStringExtra("receipt_path") ?: ""
        val tenantName  = intent.getStringExtra("tenant_name") ?: "Tenant"
        val amount      = intent.getDoubleExtra("amount", 0.0)
        val reference   = intent.getStringExtra("reference") ?: ""

        if (receiptPath.isNotEmpty()) {
            val content = ReceiptGenerator.readReceipt(receiptPath)
            tvReceiptContent.text = content ?: buildFallbackReceipt(tenantName, amount, reference)
        } else {
            tvReceiptContent.text = buildFallbackReceipt(tenantName, amount, reference)
        }

        btnShare.setOnClickListener { shareReceipt(tenantName, reference) }
        btnDone.setOnClickListener  { finish() }
    }

    private fun buildFallbackReceipt(name: String, amount: Double, ref: String) =
        """
        ====================================
               BARAKA PLAZA
            RENT PAYMENT RECEIPT
        ====================================
        
        Tenant: $name
        Amount: KSh ${String.format("%,.0f", amount)}
        Reference: $ref
        Status: CONFIRMED
        
        Thank you for your payment!
        - BARAKA PLAZA Management
        ====================================
        """.trimIndent()

    private fun shareReceipt(tenantName: String, reference: String) {
        val content = tvReceiptContent.text.toString()
        if (content.isEmpty()) { Toast.makeText(this, "No receipt to share", Toast.LENGTH_SHORT).show(); return }
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type    = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Baraka Plaza Receipt – $tenantName")
                putExtra(Intent.EXTRA_TEXT, content)
            }, "Share Receipt via"
        ))
    }
}
