package com.barakaplaza.rentmanager.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.adapters.TenantAdapter
import com.barakaplaza.rentmanager.database.DatabaseHelper
import com.barakaplaza.rentmanager.models.Tenant

class TenantListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvTotalCount: TextView
    private lateinit var adapter: TenantAdapter

    private var allTenants: List<Tenant> = emptyList()
    private val filteredTenants = mutableListOf<Tenant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tenant_list)

        recyclerView  = findViewById(R.id.recyclerTenants)
        etSearch      = findViewById(R.id.etSearch)
        tvTotalCount  = findViewById(R.id.tvTotalCount)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadTenants()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filterTenants(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddTenant)
            .setOnClickListener { startActivity(Intent(this, TenantRegistrationActivity::class.java)) }
    }

    override fun onResume() { super.onResume(); loadTenants() }

    private fun loadTenants() {
        allTenants = DatabaseHelper.getInstance(this).getAllTenants()
        filteredTenants.clear()
        filteredTenants.addAll(allTenants)
        tvTotalCount.text = "Total Tenants: ${allTenants.size}"

        adapter = TenantAdapter(
            filteredTenants,
            onPayClick = { tenant ->
                startActivity(Intent(this, PaymentActivity::class.java)
                    .putExtra("tenant_id", tenant.id))
            },
            onHistoryClick = { tenant ->
                startActivity(Intent(this, PaymentHistoryActivity::class.java)
                    .putExtra("tenant_id", tenant.id))
            }
        )
        recyclerView.adapter = adapter
    }

    private fun filterTenants(query: String) {
        filteredTenants.clear()
        if (query.isEmpty()) {
            filteredTenants.addAll(allTenants)
        } else {
            val lq = query.lowercase()
            allTenants.filterTo(filteredTenants) {
                it.name.lowercase().contains(lq) ||
                it.houseNumber.lowercase().contains(lq) ||
                it.phone.contains(lq)
            }
        }
        adapter.notifyDataSetChanged()
    }
}
