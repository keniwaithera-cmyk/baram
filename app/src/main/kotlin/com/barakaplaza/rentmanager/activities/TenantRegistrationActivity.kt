package com.barakaplaza.rentmanager.activities

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.database.DatabaseHelper
import com.barakaplaza.rentmanager.models.House
import com.barakaplaza.rentmanager.models.Tenant
import com.barakaplaza.rentmanager.utils.SmsUtils
import java.text.SimpleDateFormat
import java.util.*

class TenantRegistrationActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etIdNumber: EditText
    private lateinit var etEmergencyContact: EditText
    private lateinit var spinnerHouse: Spinner
    private lateinit var tvRentAmount: TextView
    private lateinit var tvHouseType: TextView
    private lateinit var tvFloor: TextView
    private lateinit var btnRegister: Button
    private lateinit var btnPickDate: Button
    private lateinit var tvMoveInDate: TextView

    private var selectedMoveInDate = ""
    private var availableHouses: List<House> = emptyList()
    private var selectedHouse: House? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tenant_registration)

        initViews()
        loadAvailableHouses()
        requestSmsPermission()

        selectedMoveInDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        tvMoveInDate.text  = selectedMoveInDate

        btnPickDate.setOnClickListener { showDatePicker() }
        btnRegister.setOnClickListener { registerTenant() }
    }

    private fun initViews() {
        etFullName          = findViewById(R.id.etFullName)
        etPhone             = findViewById(R.id.etPhone)
        etEmail             = findViewById(R.id.etEmail)
        etIdNumber          = findViewById(R.id.etIdNumber)
        etEmergencyContact  = findViewById(R.id.etEmergencyContact)
        spinnerHouse        = findViewById(R.id.spinnerHouse)
        tvRentAmount        = findViewById(R.id.tvRentAmount)
        tvHouseType         = findViewById(R.id.tvHouseType)
        tvFloor             = findViewById(R.id.tvFloor)
        btnRegister         = findViewById(R.id.btnRegister)
        btnPickDate         = findViewById(R.id.btnPickDate)
        tvMoveInDate        = findViewById(R.id.tvMoveInDate)
    }

    private fun loadAvailableHouses() {
        availableHouses = DatabaseHelper.getInstance(this).getAvailableHouses()
        if (availableHouses.isEmpty()) {
            Toast.makeText(this, "No available houses at the moment", Toast.LENGTH_LONG).show()
            btnRegister.isEnabled = false
            return
        }

        val names = availableHouses.map {
            "House ${it.houseNumber} - ${it.type} | KSh ${String.format("%,.0f", it.monthlyRent)}"
        }
        spinnerHouse.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, names).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerHouse.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedHouse = availableHouses[pos]
                tvRentAmount.text = "KSh ${String.format("%,.0f", selectedHouse!!.monthlyRent)}/month"
                tvHouseType.text  = selectedHouse!!.type
                tvFloor.text      = selectedHouse!!.floor
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        selectedHouse    = availableHouses[0]
        tvRentAmount.text = "KSh ${String.format("%,.0f", selectedHouse!!.monthlyRent)}/month"
        tvHouseType.text  = selectedHouse!!.type
        tvFloor.text      = selectedHouse!!.floor
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            selectedMoveInDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year)
            tvMoveInDate.text = selectedMoveInDate
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun registerTenant() {
        val name             = etFullName.text.toString().trim()
        val phone            = etPhone.text.toString().trim()
        val email            = etEmail.text.toString().trim()
        val idNumber         = etIdNumber.text.toString().trim()
        val emergencyContact = etEmergencyContact.text.toString().trim()

        if (name.isEmpty())            { etFullName.error = "Full name is required"; return }
        if (phone.isEmpty())           { etPhone.error = "Phone number is required"; return }
        if (phone.length < 10)         { etPhone.error = "Enter a valid Kenyan phone number"; return }
        if (idNumber.isEmpty())        { etIdNumber.error = "National ID is required"; return }
        if (selectedHouse == null)     { Toast.makeText(this, "Please select a house", Toast.LENGTH_SHORT).show(); return }
        if (selectedMoveInDate.isEmpty()) { Toast.makeText(this, "Please select move-in date", Toast.LENGTH_SHORT).show(); return }

        val tenant = Tenant(name, phone, email, idNumber,
            selectedHouse!!.houseNumber, selectedMoveInDate, emergencyContact)

        val db = DatabaseHelper.getInstance(this)
        val tenantId = db.addTenant(tenant)

        if (tenantId > 0) {
            tenant.id = tenantId.toInt()
            val landlord    = db.getLandlordInfo()
            val landlordPhone = landlord.getAsString(DatabaseHelper.COL_LANDLORD_PHONE) ?: ""
            val paybill     = landlord.getAsString(DatabaseHelper.COL_LANDLORD_PAYBILL) ?: ""
            val account     = landlord.getAsString(DatabaseHelper.COL_LANDLORD_ACCOUNT) ?: ""
            val mpesaNo     = landlord.getAsString(DatabaseHelper.COL_LANDLORD_MPESA) ?: ""
            val rent        = selectedHouse!!.monthlyRent

            SmsUtils.sendRegistrationConfirmation(this, tenant, rent, paybill, account, mpesaNo)
            SmsUtils.sendNewTenantNotification(this, landlordPhone, tenant, rent)

            AlertDialog.Builder(this)
                .setTitle("✅ Registration Successful!")
                .setMessage(
                    "Tenant $name has been registered for House ${selectedHouse!!.houseNumber}.\n\n" +
                    "Monthly Rent: KSh ${String.format("%,.0f", rent)}\n" +
                    "Move-in Date: $selectedMoveInDate\n\n" +
                    "SMS notifications sent to tenant and landlord."
                )
                .setPositiveButton("OK") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } else {
            Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS),
                SmsUtils.SMS_PERMISSION_REQUEST)
        }
    }
}
