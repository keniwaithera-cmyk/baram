package com.barakaplaza.rentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.database.DatabaseHelper

class LoginActivity : AppCompatActivity() {

    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvTenantPortal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etPhone       = findViewById(R.id.etPhone)
        etPassword    = findViewById(R.id.etPassword)
        btnLogin      = findViewById(R.id.btnLogin)
        tvTenantPortal = findViewById(R.id.tvTenantPortal)

        btnLogin.setOnClickListener { attemptLogin() }
        tvTenantPortal.setOnClickListener {
            startActivity(Intent(this, TenantPortalActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val phone    = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter phone and password", Toast.LENGTH_SHORT).show()
            return
        }

        val db = DatabaseHelper.getInstance(this)
        if (db.validateLandlordLogin(phone, password)) {
            Toast.makeText(this, "Welcome, Landlord!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LandlordDashboardActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Invalid credentials. Try again.", Toast.LENGTH_SHORT).show()
            etPassword.setText("")
        }
    }
}
