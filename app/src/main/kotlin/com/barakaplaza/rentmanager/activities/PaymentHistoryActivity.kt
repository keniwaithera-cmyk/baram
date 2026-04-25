package com.barakaplaza.rentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.adapters.PaymentAdapter
import com.barakaplaza.rentmanager.database.DatabaseHelper
import com.barakaplaza.rentmanager.models.Payment
import java.text.SimpleDateFormat
import java.util.*

class PaymentHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvTotal: TextView
    private lateinit var tvNoPayments: TextView
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerYear: Spinner

    private val months = listOf(
        "All","January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )
    private val years = (2023..2030).map { it.toString() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_history)

        recyclerView  = findViewById(R.id.recyclerPayments)
        tvTotal       = findViewById(R.id.tvTotalCollection)
        tvNoPayments  = findViewById(R.id.tvNoPayments)
        spinnerMonth  = findViewById(R.id.spinnerMonth)
        spinnerYear   = findViewById(R.id.spinnerYear)

        recyclerView.layoutManager = LinearLayoutManager(this)

        spinnerMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerYear.adapter  = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Default to current month/year
        val now = Date()
        val curMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(now)
        val curYear  = SimpleDateFormat("yyyy",  Locale.getDefault()).format(now)
        spinnerMonth.setSelection(months.indexOf(curMonth).coerceAtLeast(0))
        spinnerYear.setSelection(years.indexOf(curYear).coerceAtLeast(0))

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = loadPayments()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerMonth.onItemSelectedListener = filterListener
        spinnerYear.onItemSelectedListener  = filterListener

        // If launched from TenantListActivity to show one tenant's history
        val tenantId = intent.getIntExtra("tenant_id", -1)
        if (tenantId > 0) loadPaymentsForTenant(tenantId) else loadPayments()
    }

    private fun loadPayments() {
        val db      = DatabaseHelper.getInstance(this)
        val month   = spinnerMonth.selectedItem as String
        val year    = spinnerYear.selectedItem as String
        val payments = if (month == "All") db.getAllPayments()
                       else db.getPaymentsByMonth(month, year)

        showPayments(payments)
        val total = payments.filter { it.status == Payment.STATUS_CONFIRMED }.sumOf { it.amount }
        tvTotal.text = "Total Collected: KSh ${String.format("%,.0f", total)}"
    }

    private fun loadPaymentsForTenant(tenantId: Int) {
        spinnerMonth.visibility = View.GONE
        spinnerYear.visibility  = View.GONE
        val payments = DatabaseHelper.getInstance(this).getPaymentsByTenant(tenantId)
        showPayments(payments)
        val total = payments.sumOf { it.amount }
        tvTotal.text = "Total Paid: KSh ${String.format("%,.0f", total)}"
    }

    private fun showPayments(payments: List<Payment>) {
        if (payments.isEmpty()) {
            tvNoPayments.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoPayments.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter = PaymentAdapter(payments) { payment ->
                startActivity(Intent(this, ReceiptViewActivity::class.java).apply {
                    putExtra("receipt_path", payment.receiptPath)
                    putExtra("tenant_name",  payment.tenantName)
                    putExtra("amount",       payment.amount)
                    putExtra("reference",    payment.referenceNumber)
                })
            }
        }
    }
}
