package com.barakaplaza.rentmanager.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barakaplaza.rentmanager.R
import com.barakaplaza.rentmanager.adapters.HouseAdapter
import com.barakaplaza.rentmanager.database.DatabaseHelper
import com.barakaplaza.rentmanager.models.House

class HouseManagementActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvVacant: TextView
    private lateinit var tvOccupied: TextView
    private lateinit var tvTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_house_management)

        recyclerView = findViewById(R.id.recyclerHouses)
        tvVacant     = findViewById(R.id.tvVacant)
        tvOccupied   = findViewById(R.id.tvOccupied)
        tvTotal      = findViewById(R.id.tvTotal)

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadHouses()

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddHouse)
            .setOnClickListener { showAddHouseDialog() }
    }

    override fun onResume() { super.onResume(); loadHouses() }

    private fun loadHouses() {
        val db     = DatabaseHelper.getInstance(this)
        val houses = db.getAllHouses()
        val vacant   = houses.count { !it.isOccupied }
        val occupied = houses.count { it.isOccupied }

        tvTotal.text    = "Total: ${houses.size}"
        tvVacant.text   = "🟢 Vacant: $vacant"
        tvOccupied.text = "🔴 Occupied: $occupied"

        recyclerView.adapter = HouseAdapter(houses) { house -> showEditRentDialog(house) }
    }

    private fun showEditRentDialog(house: House) {
        val etRent = EditText(this).apply {
            hint = "New monthly rent (KSh)"
            setText(house.monthlyRent.toInt().toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(48, 32, 48, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Rent – House ${house.houseNumber}")
            .setMessage("Current rent: KSh ${String.format("%,.0f", house.monthlyRent)}/month\n\nEnter new rent:")
            .setView(etRent)
            .setPositiveButton("Update") { _, _ ->
                val newRent = etRent.text.toString().trim().toDoubleOrNull()
                if (newRent != null && newRent > 0) {
                    if (DatabaseHelper.getInstance(this).updateHouseRent(house.houseNumber, newRent)) {
                        Toast.makeText(this, "Rent updated to KSh ${String.format("%,.0f", newRent)}", Toast.LENGTH_SHORT).show()
                        loadHouses()
                    } else {
                        Toast.makeText(this, "Failed to update rent", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddHouseDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val etNumber = EditText(this).apply { hint = "House Number (e.g. D2)" }
        val etFloor  = EditText(this).apply { hint = "Floor (e.g. 3rd Floor)" }
        val etType   = EditText(this).apply { hint = "Type (e.g. 2 Bedroom)" }
        val etRent   = EditText(this).apply {
            hint = "Monthly Rent (KSh)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(etNumber); layout.addView(etFloor)
        layout.addView(etType);   layout.addView(etRent)

        AlertDialog.Builder(this)
            .setTitle("Add New House")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val number = etNumber.text.toString().trim()
                val floor  = etFloor.text.toString().trim()
                val type   = etType.text.toString().trim()
                val rent   = etRent.text.toString().trim().toDoubleOrNull()

                when {
                    number.isEmpty() -> Toast.makeText(this, "House number required", Toast.LENGTH_SHORT).show()
                    floor.isEmpty()  -> Toast.makeText(this, "Floor required", Toast.LENGTH_SHORT).show()
                    type.isEmpty()   -> Toast.makeText(this, "Type required", Toast.LENGTH_SHORT).show()
                    rent == null || rent <= 0 -> Toast.makeText(this, "Enter valid rent", Toast.LENGTH_SHORT).show()
                    else -> {
                        val house = House(number, floor, type, rent, "")
                        val id = DatabaseHelper.getInstance(this).addHouse(house)
                        if (id > 0) {
                            Toast.makeText(this, "House $number added", Toast.LENGTH_SHORT).show()
                            loadHouses()
                        } else {
                            Toast.makeText(this, "Failed – house number may already exist", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
