package com.barakaplaza.rentmanager.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.adapters.PaymentAdapter
import com.barakaplaza.rentmanager.database.DatabaseHelper

class TenantPortalActivity : AppCompatActivity() {

    private lateinit var etPhone: EditText
    private lateinit var btnSearch: Button
    private lateinit var layoutTenantInfo: LinearLayout
    private lateinit var tvTenantName: TextView
    private lateinit var tvHouseNumber: TextView
    private lateinit var tvRentDue: TextView
    private lateinit var tvLandlordMpesa: TextView
    private lateinit var tvPaybill: TextView
    private lateinit var tvAccount: TextView
    private lateinit var recyclerPayments: RecyclerView
    private lateinit var tvNoHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tenant_portal)

        etPhone           = findViewById(R.id.etPhone)
        btnSearch         = findViewById(R.id.btnSearch)
        layoutTenantInfo  = findViewById(R.id.layoutTenantInfo)
        tvTenantName      = findViewById(R.id.tvTenantName)
        tvHouseNumber     = findViewById(R.id.tvHouseNumber)
        tvRentDue         = findViewById(R.id.tvRentDue)
        tvLandlordMpesa   = findViewById(R.id.tvLandlordMpesa)
        tvPaybill         = findViewById(R.id.tvPaybill)
        tvAccount         = findViewById(R.id.tvAccount)
        recyclerPayments  = findViewById(R.id.recyclerPayments)
        tvNoHistory       = findViewById(R.id.tvNoHistory)

        recyclerPayments.layoutManager = LinearLayoutManager(this)
        layoutTenantInfo.visibility    = View.GONE

        btnSearch.setOnClickListener { lookupTenant() }
    }

    private fun lookupTenant() {
        val phone = etPhone.text.toString().trim()
        if (phone.length < 10) { etPhone.error = "Enter a valid Kenyan phone number"; return }

        val db      = DatabaseHelper.getInstance(this)
        val tenant  = db.getTenantByPhone(phone)
            ?: db.getTenantByPhone("0${phone.removePrefix("254")}")
            ?: db.getTenantByPhone("+254${phone.removePrefix("0")}")

        if (tenant == null || !tenant.isActive) {
            Toast.makeText(this,
                "No active tenant found for this number. Contact the landlord.",
                Toast.LENGTH_LONG).show()
            layoutTenantInfo.visibility = View.GONE
            return
        }

        val house    = db.getHouseByNumber(tenant.houseNumber)
        val landlord = db.getLandlordInfo()
        val payments = db.getPaymentsByTenant(tenant.id)

        tvTenantName.text    = "👤 ${tenant.name}"
        tvHouseNumber.text   = "🏠 House: ${tenant.houseNumber}"
        tvRentDue.text       = "💰 Monthly Rent: KSh ${
            house?.let { String.format("%,.0f", it.monthlyRent) } ?: "N/A"
        }"
        tvLandlordMpesa.text = "📱 M-Pesa: ${landlord.getAsString(DatabaseHelper.COL_LANDLORD_MPESA) ?: "N/A"}"
        tvPaybill.text       = "🏦 Paybill: ${landlord.getAsString(DatabaseHelper.COL_LANDLORD_PAYBILL) ?: "N/A"}"
        tvAccount.text       = "📋 Account: ${landlord.getAsString(DatabaseHelper.COL_LANDLORD_ACCOUNT) ?: "N/A"}"

        layoutTenantInfo.visibility = View.VISIBLE

        if (payments.isEmpty()) {
            tvNoHistory.visibility    = View.VISIBLE
            recyclerPayments.visibility = View.GONE
        } else {
            tvNoHistory.visibility    = View.GONE
            recyclerPayments.visibility = View.VISIBLE
            recyclerPayments.adapter  = PaymentAdapter(payments) {}
        }
    }
}
