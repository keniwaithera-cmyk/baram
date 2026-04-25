package com.barakaplaza.rentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.database.DatabaseHelper
import com.barakaplaza.rentmanager.utils.SmsUtils
import java.text.SimpleDateFormat
import java.util.*

class LandlordDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvTotalTenants: TextView
    private lateinit var tvVacantHouses: TextView
    private lateinit var tvMonthlyCollection: TextView
    private lateinit var tvCurrentMonth: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landlord_dashboard)

        tvWelcome           = findViewById(R.id.tvWelcome)
        tvTotalTenants      = findViewById(R.id.tvTotalTenants)
        tvVacantHouses      = findViewById(R.id.tvVacantHouses)
        tvMonthlyCollection = findViewById(R.id.tvMonthlyCollection)
        tvCurrentMonth      = findViewById(R.id.tvCurrentMonth)

        loadDashboardStats()

        fun card(id: Int) = findViewById<CardView>(id)
        card(R.id.cardRegister).setOnClickListener { startActivity(Intent(this, TenantRegistrationActivity::class.java)) }
        card(R.id.cardTenants).setOnClickListener  { startActivity(Intent(this, TenantListActivity::class.java)) }
        card(R.id.cardPayments).setOnClickListener { startActivity(Intent(this, PaymentActivity::class.java)) }
        card(R.id.cardHistory).setOnClickListener  { startActivity(Intent(this, PaymentHistoryActivity::class.java)) }
        card(R.id.cardHouses).setOnClickListener   { startActivity(Intent(this, HouseManagementActivity::class.java)) }
        card(R.id.cardReminders).setOnClickListener { sendManualReminders() }

        findViewById<android.widget.Button>(R.id.btnLogout).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardStats()
    }

    private fun loadDashboardStats() {
        val db       = DatabaseHelper.getInstance(this)
        val landlord = db.getLandlordInfo()
        val name     = landlord.getAsString(DatabaseHelper.COL_LANDLORD_NAME) ?: "Manager"
        tvWelcome.text = "Welcome, $name"

        val now          = Date()
        val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(now)
        val currentYear  = SimpleDateFormat("yyyy",  Locale.getDefault()).format(now)
        val collected    = db.getTotalCollectedThisMonth(currentMonth, currentYear)

        tvTotalTenants.text      = db.getTotalActiveTenants().toString()
        tvVacantHouses.text      = db.getVacantHousesCount().toString()
        tvMonthlyCollection.text = "KSh ${String.format("%,.0f", collected)}"
        tvCurrentMonth.text      = "$currentMonth $currentYear Collections"
    }

    private fun sendManualReminders() {
        AlertDialog.Builder(this)
            .setTitle("Send Reminders")
            .setMessage("Send rent reminder SMS to all active tenants?")
            .setPositiveButton("Send") { _, _ ->
                val db       = DatabaseHelper.getInstance(this)
                val landlord = db.getLandlordInfo()
                val tenants  = db.getAllActiveTenants()
                val deadline = "5th ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}"
                var count = 0
                tenants.forEach { tenant ->
                    val house = db.getHouseByNumber(tenant.houseNumber) ?: return@forEach
                    SmsUtils.sendMonthlyReminder(this, tenant, house.monthlyRent, deadline)
                    count++
                }
                Toast.makeText(this, "Reminders sent to $count tenants", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
