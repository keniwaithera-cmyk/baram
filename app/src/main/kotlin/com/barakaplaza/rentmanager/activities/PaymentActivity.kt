package com.barakaplaza.rentmanager.activities

import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.database.DatabaseHelper
import com.barakaplaza.rentmanager.models.House
import com.barakaplaza.rentmanager.models.Payment
import com.barakaplaza.rentmanager.models.Tenant
import com.barakaplaza.rentmanager.mpesa.MpesaRepository
import com.barakaplaza.rentmanager.mpesa.MpesaResult
import com.barakaplaza.rentmanager.utils.ReceiptGenerator
import com.barakaplaza.rentmanager.utils.SmsUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PaymentActivity : AppCompatActivity() {

    // Views
    private lateinit var spinnerTenant: Spinner
    private lateinit var tvHouseNo: TextView
    private lateinit var tvRentDue: TextView
    private lateinit var tvLandlordMpesa: TextView
    private lateinit var tvPaybillNo: TextView
    private lateinit var tvAccountNo: TextView
    private lateinit var etAmount: EditText
    private lateinit var etMpesaCode: EditText
    private lateinit var etNotes: EditText
    private lateinit var cardMpesa: CardView
    private lateinit var cardCash: CardView
    private lateinit var cardPaybill: CardView
    private lateinit var cardStkPush: CardView
    private lateinit var btnConfirmPayment: Button
    private lateinit var btnStkPush: Button
    private lateinit var layoutMpesaDetails: LinearLayout
    private lateinit var layoutPaybillDetails: LinearLayout
    private lateinit var layoutStkPush: LinearLayout
    private lateinit var rbMpesa: RadioButton
    private lateinit var rbCash: RadioButton
    private lateinit var rbPaybill: RadioButton
    private lateinit var rbStkPush: RadioButton

    private var tenants: List<Tenant> = emptyList()
    private var selectedTenant: Tenant? = null
    private var paymentMethod: String = Payment.METHOD_MPESA
    private var stkCheckoutRequestId: String? = null
    private var pendingPaymentId: Long = -1L

    // Auto-fill M-Pesa code from SMS
    private val mpesaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val code   = intent.getStringExtra("mpesa_code") ?: return
            val amount = intent.getDoubleExtra("amount", 0.0)
            if (code.isNotEmpty()) {
                etMpesaCode.setText(code)
                if (amount > 0) etAmount.setText(amount.toInt().toString())
                rbMpesa.isChecked = true
                paymentMethod = Payment.METHOD_MPESA
                updatePaymentUI()
                Toast.makeText(context, "M-Pesa code auto-filled: $code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        initViews()
        loadLandlordPaymentInfo()
        loadTenants()
        setupPaymentMethodToggle()
        registerReceiver(mpesaReceiver, IntentFilter("com.barakaplaza.MPESA_RECEIVED"))

        // Pre-select tenant if launched from TenantListActivity
        intent.getIntExtra("tenant_id", -1).takeIf { it > 0 }?.let { tenantId ->
            val idx = tenants.indexOfFirst { it.id == tenantId }
            if (idx >= 0) spinnerTenant.setSelection(idx)
        }

        btnConfirmPayment.setOnClickListener { confirmPayment() }
        btnStkPush.setOnClickListener { initiateStkPush() }
    }

    private fun initViews() {
        spinnerTenant        = findViewById(R.id.spinnerTenant)
        tvHouseNo            = findViewById(R.id.tvHouseNo)
        tvRentDue            = findViewById(R.id.tvRentDue)
        tvLandlordMpesa      = findViewById(R.id.tvLandlordMpesa)
        tvPaybillNo          = findViewById(R.id.tvPaybillNo)
        tvAccountNo          = findViewById(R.id.tvAccountNo)
        etAmount             = findViewById(R.id.etAmount)
        etMpesaCode          = findViewById(R.id.etMpesaCode)
        etNotes              = findViewById(R.id.etNotes)
        cardMpesa            = findViewById(R.id.cardMpesa)
        cardCash             = findViewById(R.id.cardCash)
        cardPaybill          = findViewById(R.id.cardPaybill)
        cardStkPush          = findViewById(R.id.cardStkPush)
        btnConfirmPayment    = findViewById(R.id.btnConfirmPayment)
        btnStkPush           = findViewById(R.id.btnStkPush)
        layoutMpesaDetails   = findViewById(R.id.layoutMpesaDetails)
        layoutPaybillDetails = findViewById(R.id.layoutPaybillDetails)
        layoutStkPush        = findViewById(R.id.layoutStkPush)
        rbMpesa              = findViewById(R.id.rbMpesa)
        rbCash               = findViewById(R.id.rbCash)
        rbPaybill            = findViewById(R.id.rbPaybill)
        rbStkPush            = findViewById(R.id.rbStkPush)
    }

    private fun loadLandlordPaymentInfo() {
        val landlord = DatabaseHelper.getInstance(this).getLandlordInfo()
        tvLandlordMpesa.text = "M-Pesa No: ${landlord.getAsString(DatabaseHelper.COL_LANDLORD_MPESA) ?: "N/A"}"
        tvPaybillNo.text     = "Paybill: ${landlord.getAsString(DatabaseHelper.COL_LANDLORD_PAYBILL) ?: "N/A"}"
        tvAccountNo.text     = "Account: ${landlord.getAsString(DatabaseHelper.COL_LANDLORD_ACCOUNT) ?: "N/A"}"
    }

    private fun loadTenants() {
        val db = DatabaseHelper.getInstance(this)
        tenants = db.getAllActiveTenants()

        if (tenants.isEmpty()) {
            Toast.makeText(this, "No active tenants found", Toast.LENGTH_SHORT).show()
            btnConfirmPayment.isEnabled = false
            btnStkPush.isEnabled = false
            return
        }

        val names = tenants.map { "${it.name} - House ${it.houseNumber}" }
        spinnerTenant.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, names).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerTenant.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedTenant = tenants[pos]
                updateTenantDetails()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        selectedTenant = tenants[0]
        updateTenantDetails()
    }

    private fun updateTenantDetails() {
        val tenant = selectedTenant ?: return
        val house = DatabaseHelper.getInstance(this).getHouseByNumber(tenant.houseNumber)
        tvHouseNo.text = "House: ${tenant.houseNumber}"
        house?.let {
            tvRentDue.text = "Rent Due: KSh ${String.format("%,.0f", it.monthlyRent)}"
            etAmount.setText(it.monthlyRent.toInt().toString())
        }
    }

    private fun setupPaymentMethodToggle() {
        rbMpesa.setOnClickListener   { paymentMethod = Payment.METHOD_MPESA;    updatePaymentUI() }
        rbCash.setOnClickListener    { paymentMethod = Payment.METHOD_CASH;     updatePaymentUI() }
        rbPaybill.setOnClickListener { paymentMethod = Payment.METHOD_PAYBILL;  updatePaymentUI() }
        rbStkPush.setOnClickListener { paymentMethod = Payment.METHOD_STK_PUSH; updatePaymentUI() }
        updatePaymentUI()
    }

    private fun updatePaymentUI() {
        layoutMpesaDetails.visibility   = if (paymentMethod == Payment.METHOD_MPESA)    View.VISIBLE else View.GONE
        layoutPaybillDetails.visibility = if (paymentMethod == Payment.METHOD_PAYBILL)  View.VISIBLE else View.GONE
        layoutStkPush.visibility        = if (paymentMethod == Payment.METHOD_STK_PUSH) View.VISIBLE else View.GONE
        // Show manual confirm button only for non-STK methods
        btnConfirmPayment.visibility    = if (paymentMethod == Payment.METHOD_STK_PUSH) View.GONE else View.VISIBLE

        val green  = getColor(R.color.primary_green)
        val normal = getColor(R.color.card_background)
        cardMpesa.setCardBackgroundColor(   if (paymentMethod == Payment.METHOD_MPESA)    green else normal)
        cardCash.setCardBackgroundColor(    if (paymentMethod == Payment.METHOD_CASH)     green else normal)
        cardPaybill.setCardBackgroundColor( if (paymentMethod == Payment.METHOD_PAYBILL)  green else normal)
        cardStkPush.setCardBackgroundColor( if (paymentMethod == Payment.METHOD_STK_PUSH) green else normal)
    }

    // ----------------------------------------------------------------
    // Manual payment confirmation (M-Pesa code, Cash, Paybill)
    // ----------------------------------------------------------------
    private fun confirmPayment() {
        val tenant = selectedTenant ?: run {
            Toast.makeText(this, "Please select a tenant", Toast.LENGTH_SHORT).show(); return
        }
        val amountStr = etAmount.text.toString().trim()
        if (amountStr.isEmpty()) { etAmount.error = "Enter payment amount"; return }
        val amount = amountStr.toDoubleOrNull() ?: run { etAmount.error = "Invalid amount"; return }
        if (amount <= 0) { etAmount.error = "Amount must be greater than 0"; return }

        val mpesaCode = etMpesaCode.text.toString().trim()
        if (paymentMethod == Payment.METHOD_MPESA && mpesaCode.isEmpty()) {
            etMpesaCode.error = "M-Pesa code is required"; return
        }

        val db = DatabaseHelper.getInstance(this)
        val house = db.getHouseByNumber(tenant.houseNumber)
        val rentDue = house?.monthlyRent ?: 0.0

        val confirmMsg = "Confirm Payment:\n\n" +
            "Tenant: ${tenant.name}\n" +
            "House: ${tenant.houseNumber}\n" +
            "Amount: KSh ${String.format("%,.0f", amount)}\n" +
            "Method: $paymentMethod\n" +
            (if (mpesaCode.isNotEmpty()) "M-Pesa Code: $mpesaCode\n" else "") +
            "\nRent Due: KSh ${String.format("%,.0f", rentDue)}"

        AlertDialog.Builder(this)
            .setTitle("Confirm Payment")
            .setMessage(confirmMsg)
            .setPositiveButton("Confirm") { _, _ -> processPayment(amount, mpesaCode, rentDue) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processPayment(amount: Double, mpesaCode: String, rentDue: Double) {
        val tenant = selectedTenant ?: return
        val db = DatabaseHelper.getInstance(this)
        val landlord = db.getLandlordInfo()
        val now  = Date()
        val sdf  = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(now)
        val year  = SimpleDateFormat("yyyy",  Locale.getDefault()).format(now)

        val payment = Payment(
            tenantId        = tenant.id,
            tenantName      = tenant.name,
            houseNumber     = tenant.houseNumber,
            amount          = amount,
            paymentMethod   = paymentMethod,
            paymentDate     = sdf.format(now),
            paymentMonth    = month,
            paymentYear     = year,
            referenceNumber = ReceiptGenerator.generateReferenceNumber(tenant.houseNumber),
            mpesaCode       = mpesaCode,
            status          = Payment.STATUS_CONFIRMED,
            notes           = etNotes.text.toString().trim()
        )

        val paymentId = db.addPayment(payment)
        if (paymentId > 0) {
            payment.id = paymentId.toInt()
            val receiptPath = ReceiptGenerator.generateReceipt(this, tenant, payment, rentDue)
            receiptPath?.let { db.updatePaymentReceipt(paymentId, it) }

            val landlordPhone = landlord.getAsString(DatabaseHelper.COL_LANDLORD_PHONE) ?: ""
            SmsUtils.sendPaymentConfirmationToTenant(this, tenant, payment)
            SmsUtils.sendPaymentNotificationToLandlord(this, landlordPhone, tenant, payment)

            startActivity(Intent(this, ReceiptViewActivity::class.java).apply {
                putExtra("payment_id",   paymentId.toInt())
                putExtra("receipt_path", receiptPath)
                putExtra("tenant_name",  tenant.name)
                putExtra("amount",       amount)
                putExtra("reference",    payment.referenceNumber)
            })
            finish()
        } else {
            Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    // ----------------------------------------------------------------
    // MPesa STK Push
    // ----------------------------------------------------------------
    private fun initiateStkPush() {
        val tenant = selectedTenant ?: run {
            Toast.makeText(this, "Please select a tenant", Toast.LENGTH_SHORT).show(); return
        }
        val amountStr = etAmount.text.toString().trim()
        if (amountStr.isEmpty()) { etAmount.error = "Enter payment amount"; return }
        val amount = amountStr.toIntOrNull() ?: run { etAmount.error = "Invalid amount"; return }
        if (amount <= 0) { etAmount.error = "Amount must be greater than 0"; return }

        val phoneForDaraja = MpesaRepository.formatPhoneForDaraja(tenant.phone)

        AlertDialog.Builder(this)
            .setTitle("📲 Send STK Push")
            .setMessage(
                "An M-Pesa payment request will be sent to:\n\n" +
                "Phone: ${tenant.phone}\n" +
                "Amount: KSh ${String.format("%,.0f", amount.toDouble())}\n" +
                "House: ${tenant.houseNumber}\n\n" +
                "The tenant will receive a prompt on their phone to enter their M-Pesa PIN."
            )
            .setPositiveButton("Send Request") { _, _ ->
                sendStkPush(tenant, phoneForDaraja, amount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendStkPush(tenant: Tenant, phone: String, amount: Int) {
        val progress = ProgressDialog(this).apply {
            setMessage("Sending payment request to ${tenant.name}…")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            val result = MpesaRepository.initiateStkPush(
                phoneNumber  = phone,
                amount       = amount,
                accountRef   = "House ${tenant.houseNumber}",
                description  = "Baraka Plaza Rent - ${tenant.houseNumber}"
            )
            progress.dismiss()

            when (result) {
                is MpesaResult.Success -> {
                    stkCheckoutRequestId = result.data.CheckoutRequestID
                    // Save a PENDING payment record
                    pendingPaymentId = savePendingPayment(tenant, amount.toDouble())
                    showStkWaitingDialog(tenant, amount.toDouble())
                }
                is MpesaResult.Error -> {
                    AlertDialog.Builder(this@PaymentActivity)
                        .setTitle("STK Push Failed")
                        .setMessage(
                            "${result.message}\n\n" +
                            "Tip: Make sure your Daraja credentials are set in MpesaConfig.kt"
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    /** Show a 60-second countdown dialog while tenant enters PIN */
    private fun showStkWaitingDialog(tenant: Tenant, amount: Double) {
        val messageView = TextView(this).apply {
            textSize = 16f
            setPadding(48, 32, 48, 16)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("⏳ Waiting for Payment")
            .setView(messageView)
            .setCancelable(false)
            .setNegativeButton("Cancel Payment") { _, _ ->
                updatePendingPaymentStatus(Payment.STATUS_FAILED)
                Toast.makeText(this, "Payment cancelled", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("I've Paid – Confirm") { _, _ ->
                // Manual confirm after tenant enters PIN
                confirmStkPayment(tenant, amount)
            }
            .create()

        var secondsLeft = 60
        val timer = object : CountDownTimer(60_000L, 1_000L) {
            override fun onTick(ms: Long) {
                secondsLeft = (ms / 1000).toInt()
                messageView.text =
                    "A payment request of KSh ${String.format("%,.0f", amount)} " +
                    "has been sent to ${tenant.name} (${tenant.phone}).\n\n" +
                    "Ask the tenant to check their phone and enter their M-Pesa PIN.\n\n" +
                    "⏱ ${secondsLeft}s remaining…"
            }
            override fun onFinish() {
                if (dialog.isShowing) {
                    messageView.text = "Time expired. Tap 'I've Paid' if the tenant completed payment."
                }
            }
        }

        dialog.setOnShowListener { timer.start() }
        dialog.setOnDismissListener { timer.cancel() }
        dialog.show()
    }

    /** Called when landlord taps "I've Paid – Confirm" after STK push */
    private fun confirmStkPayment(tenant: Tenant, amount: Double) {
        val db = DatabaseHelper.getInstance(this)
        val house = db.getHouseByNumber(tenant.houseNumber)
        val rentDue = house?.monthlyRent ?: 0.0

        // Check status via query if checkoutRequestId exists
        stkCheckoutRequestId?.let { checkoutId ->
            val progress = ProgressDialog(this).apply {
                setMessage("Verifying payment…")
                setCancelable(false)
                show()
            }
            lifecycleScope.launch {
                delay(2000) // small delay before querying
                val queryResult = MpesaRepository.queryStkStatus(checkoutId)
                progress.dismiss()

                when (queryResult) {
                    is MpesaResult.Success -> {
                        val resultCode = queryResult.data.ResultCode
                        if (resultCode == "0") {
                            // Safaricom confirmed success
                            finalisePayment(tenant, amount, rentDue, "STK-$checkoutId", isConfirmedByApi = true)
                        } else {
                            // Query says not paid yet – ask landlord to confirm manually
                            askManualConfirmation(tenant, amount, rentDue)
                        }
                    }
                    is MpesaResult.Error -> {
                        // Can't query – fall back to manual confirmation
                        askManualConfirmation(tenant, amount, rentDue)
                    }
                }
            }
        } ?: askManualConfirmation(tenant, amount, rentDue)
    }

    private fun askManualConfirmation(tenant: Tenant, amount: Double, rentDue: Double) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Payment Received")
            .setMessage(
                "Could not automatically verify payment.\n\n" +
                "Did ${tenant.name} successfully complete the M-Pesa payment of " +
                "KSh ${String.format("%,.0f", amount)}?"
            )
            .setPositiveButton("Yes, Confirm") { _, _ ->
                finalisePayment(tenant, amount, rentDue, "STK-MANUAL", isConfirmedByApi = false)
            }
            .setNegativeButton("No, Mark Failed") { _, _ ->
                updatePendingPaymentStatus(Payment.STATUS_FAILED)
                Toast.makeText(this, "Payment marked as failed", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun finalisePayment(tenant: Tenant, amount: Double, rentDue: Double,
                                 mpesaRef: String, isConfirmedByApi: Boolean) {
        val db = DatabaseHelper.getInstance(this)
        val landlord = db.getLandlordInfo()

        if (pendingPaymentId > 0) {
            db.updatePaymentStatus(pendingPaymentId, Payment.STATUS_CONFIRMED, mpesaRef)
        }

        // Generate receipt
        val now = Date()
        val payment = Payment(
            id              = pendingPaymentId.toInt(),
            tenantId        = tenant.id,
            tenantName      = tenant.name,
            houseNumber     = tenant.houseNumber,
            amount          = amount,
            paymentMethod   = Payment.METHOD_STK_PUSH,
            paymentDate     = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(now),
            paymentMonth    = SimpleDateFormat("MMMM", Locale.getDefault()).format(now),
            paymentYear     = SimpleDateFormat("yyyy",  Locale.getDefault()).format(now),
            referenceNumber = if (pendingPaymentId > 0)
                "BP-STK-${tenant.houseNumber}-${SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(now)}"
            else ReceiptGenerator.generateReferenceNumber(tenant.houseNumber),
            mpesaCode       = mpesaRef,
            status          = Payment.STATUS_CONFIRMED,
            notes           = "STK Push ${if (isConfirmedByApi) "(API verified)" else "(manual confirm)"}"
        )

        val receiptPath = ReceiptGenerator.generateReceipt(this, tenant, payment, rentDue)
        receiptPath?.let { if (pendingPaymentId > 0) db.updatePaymentReceipt(pendingPaymentId, it) }

        val landlordPhone = landlord.getAsString(DatabaseHelper.COL_LANDLORD_PHONE) ?: ""
        SmsUtils.sendPaymentConfirmationToTenant(this, tenant, payment)
        SmsUtils.sendPaymentNotificationToLandlord(this, landlordPhone, tenant, payment)

        Toast.makeText(this,
            "✅ Payment confirmed for ${tenant.name}!", Toast.LENGTH_LONG).show()

        startActivity(Intent(this, ReceiptViewActivity::class.java).apply {
            putExtra("payment_id",   pendingPaymentId.toInt())
            putExtra("receipt_path", receiptPath)
            putExtra("tenant_name",  tenant.name)
            putExtra("amount",       amount)
            putExtra("reference",    payment.referenceNumber)
        })
        finish()
    }

    private fun savePendingPayment(tenant: Tenant, amount: Double): Long {
        val now = Date()
        val payment = Payment(
            tenantId        = tenant.id,
            tenantName      = tenant.name,
            houseNumber     = tenant.houseNumber,
            amount          = amount,
            paymentMethod   = Payment.METHOD_STK_PUSH,
            paymentDate     = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(now),
            paymentMonth    = SimpleDateFormat("MMMM", Locale.getDefault()).format(now),
            paymentYear     = SimpleDateFormat("yyyy",  Locale.getDefault()).format(now),
            referenceNumber = ReceiptGenerator.generateReferenceNumber(tenant.houseNumber),
            status          = Payment.STATUS_PENDING,
            notes           = etNotes.text.toString().trim()
        )
        return DatabaseHelper.getInstance(this).addPayment(payment)
    }

    private fun updatePendingPaymentStatus(status: String) {
        if (pendingPaymentId > 0) {
            DatabaseHelper.getInstance(this).updatePaymentStatus(pendingPaymentId, status)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mpesaReceiver)
    }
}
