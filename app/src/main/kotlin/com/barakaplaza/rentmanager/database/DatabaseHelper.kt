package com.barakaplaza.rentmanager.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.barakaplaza.rentmanager.models.House
import com.barakaplaza.rentmanager.models.Payment
import com.barakaplaza.rentmanager.models.Tenant

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME    = "BarakaPlaza.db"
        private const val DATABASE_VERSION = 1

        // Tables
        const val TABLE_TENANTS  = "tenants"
        const val TABLE_HOUSES   = "houses"
        const val TABLE_PAYMENTS = "payments"
        const val TABLE_LANDLORD = "landlord"

        // Tenant columns
        const val COL_TENANT_ID                = "id"
        const val COL_TENANT_NAME              = "name"
        const val COL_TENANT_PHONE             = "phone"
        const val COL_TENANT_EMAIL             = "email"
        const val COL_TENANT_ID_NUMBER         = "id_number"
        const val COL_TENANT_HOUSE_NUMBER      = "house_number"
        const val COL_TENANT_MOVE_IN_DATE      = "move_in_date"
        const val COL_TENANT_EMERGENCY_CONTACT = "emergency_contact"
        const val COL_TENANT_ACTIVE            = "is_active"
        const val COL_TENANT_BALANCE           = "balance"

        // House columns
        const val COL_HOUSE_ID          = "id"
        const val COL_HOUSE_NUMBER      = "house_number"
        const val COL_HOUSE_FLOOR       = "floor"
        const val COL_HOUSE_TYPE        = "type"
        const val COL_HOUSE_RENT        = "monthly_rent"
        const val COL_HOUSE_OCCUPIED    = "is_occupied"
        const val COL_HOUSE_DESCRIPTION = "description"

        // Payment columns
        const val COL_PAY_ID           = "id"
        const val COL_PAY_TENANT_ID    = "tenant_id"
        const val COL_PAY_TENANT_NAME  = "tenant_name"
        const val COL_PAY_HOUSE_NUMBER = "house_number"
        const val COL_PAY_AMOUNT       = "amount"
        const val COL_PAY_METHOD       = "payment_method"
        const val COL_PAY_DATE         = "payment_date"
        const val COL_PAY_MONTH        = "payment_month"
        const val COL_PAY_YEAR         = "payment_year"
        const val COL_PAY_REFERENCE    = "reference_number"
        const val COL_PAY_MPESA_CODE   = "mpesa_code"
        const val COL_PAY_STATUS       = "status"
        const val COL_PAY_RECEIPT_PATH = "receipt_path"
        const val COL_PAY_NOTES        = "notes"

        // Landlord columns
        const val COL_LANDLORD_ID       = "id"
        const val COL_LANDLORD_NAME     = "name"
        const val COL_LANDLORD_PHONE    = "phone"
        const val COL_LANDLORD_EMAIL    = "email"
        const val COL_LANDLORD_PASSWORD = "password"
        const val COL_LANDLORD_PAYBILL  = "paybill_number"
        const val COL_LANDLORD_ACCOUNT  = "account_number"
        const val COL_LANDLORD_MPESA    = "mpesa_number"

        @Volatile private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
    }

    // ----------------------------------------------------------------
    // Schema creation
    // ----------------------------------------------------------------
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_TENANTS (
                $COL_TENANT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TENANT_NAME TEXT NOT NULL,
                $COL_TENANT_PHONE TEXT NOT NULL,
                $COL_TENANT_EMAIL TEXT,
                $COL_TENANT_ID_NUMBER TEXT NOT NULL,
                $COL_TENANT_HOUSE_NUMBER TEXT NOT NULL,
                $COL_TENANT_MOVE_IN_DATE TEXT NOT NULL,
                $COL_TENANT_EMERGENCY_CONTACT TEXT,
                $COL_TENANT_ACTIVE INTEGER DEFAULT 1,
                $COL_TENANT_BALANCE REAL DEFAULT 0
            )""".trimIndent()
        )
        db.execSQL("""
            CREATE TABLE $TABLE_HOUSES (
                $COL_HOUSE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_HOUSE_NUMBER TEXT NOT NULL UNIQUE,
                $COL_HOUSE_FLOOR TEXT,
                $COL_HOUSE_TYPE TEXT,
                $COL_HOUSE_RENT REAL NOT NULL,
                $COL_HOUSE_OCCUPIED INTEGER DEFAULT 0,
                $COL_HOUSE_DESCRIPTION TEXT
            )""".trimIndent()
        )
        db.execSQL("""
            CREATE TABLE $TABLE_PAYMENTS (
                $COL_PAY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PAY_TENANT_ID INTEGER,
                $COL_PAY_TENANT_NAME TEXT,
                $COL_PAY_HOUSE_NUMBER TEXT,
                $COL_PAY_AMOUNT REAL NOT NULL,
                $COL_PAY_METHOD TEXT NOT NULL,
                $COL_PAY_DATE TEXT NOT NULL,
                $COL_PAY_MONTH TEXT,
                $COL_PAY_YEAR TEXT,
                $COL_PAY_REFERENCE TEXT,
                $COL_PAY_MPESA_CODE TEXT,
                $COL_PAY_STATUS TEXT DEFAULT 'PENDING',
                $COL_PAY_RECEIPT_PATH TEXT,
                $COL_PAY_NOTES TEXT
            )""".trimIndent()
        )
        db.execSQL("""
            CREATE TABLE $TABLE_LANDLORD (
                $COL_LANDLORD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LANDLORD_NAME TEXT NOT NULL,
                $COL_LANDLORD_PHONE TEXT NOT NULL,
                $COL_LANDLORD_EMAIL TEXT,
                $COL_LANDLORD_PASSWORD TEXT NOT NULL,
                $COL_LANDLORD_PAYBILL TEXT,
                $COL_LANDLORD_ACCOUNT TEXT,
                $COL_LANDLORD_MPESA TEXT
            )""".trimIndent()
        )
        insertDefaultLandlord(db)
        insertDefaultHouses(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf(TABLE_TENANTS, TABLE_HOUSES, TABLE_PAYMENTS, TABLE_LANDLORD)
            .forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }

    // ----------------------------------------------------------------
    // Seed data
    // ----------------------------------------------------------------
    private fun insertDefaultLandlord(db: SQLiteDatabase) {
        db.insert(TABLE_LANDLORD, null, ContentValues().apply {
            put(COL_LANDLORD_NAME, "Baraka Plaza Manager")
            put(COL_LANDLORD_PHONE, "0726352340")
            put(COL_LANDLORD_EMAIL, "manager@barakaplaza.co.ke")
            put(COL_LANDLORD_PASSWORD, "admin1234")
            put(COL_LANDLORD_PAYBILL, "247247")
            put(COL_LANDLORD_ACCOUNT, "BARAKA")
            put(COL_LANDLORD_MPESA, "0726352338")
        })
    }

    private fun insertDefaultHouses(db: SQLiteDatabase) {
        val houses = listOf(
            arrayOf("A1","Ground Floor","Bedsitter","5000"),
            arrayOf("A2","Ground Floor","Bedsitter","5000"),
            arrayOf("A3","Ground Floor","1 Bedroom","8000"),
            arrayOf("B1","1st Floor","1 Bedroom","8000"),
            arrayOf("B2","1st Floor","2 Bedroom","12000"),
            arrayOf("B3","1st Floor","2 Bedroom","12000"),
            arrayOf("C1","2nd Floor","2 Bedroom","13000"),
            arrayOf("C2","2nd Floor","3 Bedroom","18000"),
            arrayOf("C3","2nd Floor","3 Bedroom","18000"),
            arrayOf("D1","3rd Floor","Penthouse","25000")
        )
        houses.forEach { h ->
            db.insert(TABLE_HOUSES, null, ContentValues().apply {
                put(COL_HOUSE_NUMBER, h[0])
                put(COL_HOUSE_FLOOR, h[1])
                put(COL_HOUSE_TYPE, h[2])
                put(COL_HOUSE_RENT, h[3].toDouble())
                put(COL_HOUSE_OCCUPIED, 0)
            })
        }
    }

    // ----------------------------------------------------------------
    // Tenant operations
    // ----------------------------------------------------------------
    fun addTenant(tenant: Tenant): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_TENANT_NAME, tenant.name)
            put(COL_TENANT_PHONE, tenant.phone)
            put(COL_TENANT_EMAIL, tenant.email)
            put(COL_TENANT_ID_NUMBER, tenant.idNumber)
            put(COL_TENANT_HOUSE_NUMBER, tenant.houseNumber)
            put(COL_TENANT_MOVE_IN_DATE, tenant.moveInDate)
            put(COL_TENANT_EMERGENCY_CONTACT, tenant.emergencyContact)
            put(COL_TENANT_ACTIVE, 1)
            put(COL_TENANT_BALANCE, 0)
        }
        val id = db.insert(TABLE_TENANTS, null, cv)
        if (id > 0) {
            db.update(TABLE_HOUSES, ContentValues().apply { put(COL_HOUSE_OCCUPIED, 1) },
                "$COL_HOUSE_NUMBER=?", arrayOf(tenant.houseNumber))
        }
        return id
    }

    fun getAllActiveTenants(): List<Tenant> =
        query(TABLE_TENANTS, "$COL_TENANT_ACTIVE=1", orderBy = COL_TENANT_NAME) { cursorToTenant(it) }

    fun getAllTenants(): List<Tenant> =
        query(TABLE_TENANTS, null, orderBy = COL_TENANT_NAME) { cursorToTenant(it) }

    fun getTenantById(id: Int): Tenant? =
        querySingle(TABLE_TENANTS, "$COL_TENANT_ID=?", arrayOf(id.toString())) { cursorToTenant(it) }

    fun getTenantByPhone(phone: String): Tenant? =
        querySingle(TABLE_TENANTS, "$COL_TENANT_PHONE=?", arrayOf(phone)) { cursorToTenant(it) }

    fun updateTenant(tenant: Tenant): Boolean {
        val cv = ContentValues().apply {
            put(COL_TENANT_NAME, tenant.name)
            put(COL_TENANT_PHONE, tenant.phone)
            put(COL_TENANT_EMAIL, tenant.email)
            put(COL_TENANT_ID_NUMBER, tenant.idNumber)
            put(COL_TENANT_EMERGENCY_CONTACT, tenant.emergencyContact)
        }
        return writableDatabase.update(TABLE_TENANTS, cv,
            "$COL_TENANT_ID=?", arrayOf(tenant.id.toString())) > 0
    }

    fun deactivateTenant(tenantId: Int, houseNumber: String): Boolean {
        val rows = writableDatabase.update(TABLE_TENANTS,
            ContentValues().apply { put(COL_TENANT_ACTIVE, 0) },
            "$COL_TENANT_ID=?", arrayOf(tenantId.toString()))
        if (rows > 0) {
            writableDatabase.update(TABLE_HOUSES,
                ContentValues().apply { put(COL_HOUSE_OCCUPIED, 0) },
                "$COL_HOUSE_NUMBER=?", arrayOf(houseNumber))
        }
        return rows > 0
    }

    fun getTotalActiveTenants(): Int = countWhere(TABLE_TENANTS, "$COL_TENANT_ACTIVE=1")

    private fun cursorToTenant(c: Cursor) = Tenant(
        id               = c.getInt(c.getColumnIndexOrThrow(COL_TENANT_ID)),
        name             = c.getString(c.getColumnIndexOrThrow(COL_TENANT_NAME)) ?: "",
        phone            = c.getString(c.getColumnIndexOrThrow(COL_TENANT_PHONE)) ?: "",
        email            = c.getString(c.getColumnIndexOrThrow(COL_TENANT_EMAIL)) ?: "",
        idNumber         = c.getString(c.getColumnIndexOrThrow(COL_TENANT_ID_NUMBER)) ?: "",
        houseNumber      = c.getString(c.getColumnIndexOrThrow(COL_TENANT_HOUSE_NUMBER)) ?: "",
        moveInDate       = c.getString(c.getColumnIndexOrThrow(COL_TENANT_MOVE_IN_DATE)) ?: "",
        emergencyContact = c.getString(c.getColumnIndexOrThrow(COL_TENANT_EMERGENCY_CONTACT)) ?: "",
        isActive         = c.getInt(c.getColumnIndexOrThrow(COL_TENANT_ACTIVE)) == 1,
        balance          = c.getDouble(c.getColumnIndexOrThrow(COL_TENANT_BALANCE))
    )

    // ----------------------------------------------------------------
    // House operations
    // ----------------------------------------------------------------
    fun getAllHouses(): List<House> =
        query(TABLE_HOUSES, null, orderBy = COL_HOUSE_NUMBER) { cursorToHouse(it) }

    fun getAvailableHouses(): List<House> =
        query(TABLE_HOUSES, "$COL_HOUSE_OCCUPIED=0", orderBy = COL_HOUSE_NUMBER) { cursorToHouse(it) }

    fun getHouseByNumber(number: String): House? =
        querySingle(TABLE_HOUSES, "$COL_HOUSE_NUMBER=?", arrayOf(number)) { cursorToHouse(it) }

    fun addHouse(house: House): Long {
        val cv = ContentValues().apply {
            put(COL_HOUSE_NUMBER, house.houseNumber)
            put(COL_HOUSE_FLOOR, house.floor)
            put(COL_HOUSE_TYPE, house.type)
            put(COL_HOUSE_RENT, house.monthlyRent)
            put(COL_HOUSE_OCCUPIED, 0)
            put(COL_HOUSE_DESCRIPTION, house.description)
        }
        return writableDatabase.insert(TABLE_HOUSES, null, cv)
    }

    fun updateHouseRent(houseNumber: String, newRent: Double): Boolean =
        writableDatabase.update(TABLE_HOUSES,
            ContentValues().apply { put(COL_HOUSE_RENT, newRent) },
            "$COL_HOUSE_NUMBER=?", arrayOf(houseNumber)) > 0

    fun getVacantHousesCount(): Int = countWhere(TABLE_HOUSES, "$COL_HOUSE_OCCUPIED=0")

    private fun cursorToHouse(c: Cursor) = House(
        id          = c.getInt(c.getColumnIndexOrThrow(COL_HOUSE_ID)),
        houseNumber = c.getString(c.getColumnIndexOrThrow(COL_HOUSE_NUMBER)) ?: "",
        floor       = c.getString(c.getColumnIndexOrThrow(COL_HOUSE_FLOOR)) ?: "",
        type        = c.getString(c.getColumnIndexOrThrow(COL_HOUSE_TYPE)) ?: "",
        monthlyRent = c.getDouble(c.getColumnIndexOrThrow(COL_HOUSE_RENT)),
        isOccupied  = c.getInt(c.getColumnIndexOrThrow(COL_HOUSE_OCCUPIED)) == 1,
        description = c.getString(c.getColumnIndexOrThrow(COL_HOUSE_DESCRIPTION)) ?: ""
    )

    // ----------------------------------------------------------------
    // Payment operations
    // ----------------------------------------------------------------
    fun addPayment(payment: Payment): Long {
        val cv = ContentValues().apply {
            put(COL_PAY_TENANT_ID, payment.tenantId)
            put(COL_PAY_TENANT_NAME, payment.tenantName)
            put(COL_PAY_HOUSE_NUMBER, payment.houseNumber)
            put(COL_PAY_AMOUNT, payment.amount)
            put(COL_PAY_METHOD, payment.paymentMethod)
            put(COL_PAY_DATE, payment.paymentDate)
            put(COL_PAY_MONTH, payment.paymentMonth)
            put(COL_PAY_YEAR, payment.paymentYear)
            put(COL_PAY_REFERENCE, payment.referenceNumber)
            put(COL_PAY_MPESA_CODE, payment.mpesaCode)
            put(COL_PAY_STATUS, payment.status)
            put(COL_PAY_RECEIPT_PATH, payment.receiptPath)
            put(COL_PAY_NOTES, payment.notes)
        }
        return writableDatabase.insert(TABLE_PAYMENTS, null, cv)
    }

    fun updatePaymentReceipt(paymentId: Long, receiptPath: String): Boolean =
        writableDatabase.update(TABLE_PAYMENTS,
            ContentValues().apply {
                put(COL_PAY_RECEIPT_PATH, receiptPath)
                put(COL_PAY_STATUS, "CONFIRMED")
            },
            "$COL_PAY_ID=?", arrayOf(paymentId.toString())) > 0

    fun updatePaymentStatus(paymentId: Long, status: String, mpesaCode: String = ""): Boolean =
        writableDatabase.update(TABLE_PAYMENTS,
            ContentValues().apply {
                put(COL_PAY_STATUS, status)
                if (mpesaCode.isNotEmpty()) put(COL_PAY_MPESA_CODE, mpesaCode)
            },
            "$COL_PAY_ID=?", arrayOf(paymentId.toString())) > 0

    fun getAllPayments(): List<Payment> =
        query(TABLE_PAYMENTS, null, orderBy = "$COL_PAY_DATE DESC") { cursorToPayment(it) }

    fun getPaymentsByTenant(tenantId: Int): List<Payment> =
        query(TABLE_PAYMENTS, "$COL_PAY_TENANT_ID=$tenantId",
            orderBy = "$COL_PAY_DATE DESC") { cursorToPayment(it) }

    fun getPaymentsByMonth(month: String, year: String): List<Payment> =
        query(TABLE_PAYMENTS, "$COL_PAY_MONTH='$month' AND $COL_PAY_YEAR='$year'",
            orderBy = "$COL_PAY_DATE DESC") { cursorToPayment(it) }

    fun getTotalCollectedThisMonth(month: String, year: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($COL_PAY_AMOUNT) FROM $TABLE_PAYMENTS " +
                "WHERE $COL_PAY_MONTH=? AND $COL_PAY_YEAR=? AND $COL_PAY_STATUS='CONFIRMED'",
            arrayOf(month, year)
        )
        val total = if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0
        cursor.close()
        return total
    }

    private fun cursorToPayment(c: Cursor) = Payment(
        id             = c.getInt(c.getColumnIndexOrThrow(COL_PAY_ID)),
        tenantId       = c.getInt(c.getColumnIndexOrThrow(COL_PAY_TENANT_ID)),
        tenantName     = c.getString(c.getColumnIndexOrThrow(COL_PAY_TENANT_NAME)) ?: "",
        houseNumber    = c.getString(c.getColumnIndexOrThrow(COL_PAY_HOUSE_NUMBER)) ?: "",
        amount         = c.getDouble(c.getColumnIndexOrThrow(COL_PAY_AMOUNT)),
        paymentMethod  = c.getString(c.getColumnIndexOrThrow(COL_PAY_METHOD)) ?: "",
        paymentDate    = c.getString(c.getColumnIndexOrThrow(COL_PAY_DATE)) ?: "",
        paymentMonth   = c.getString(c.getColumnIndexOrThrow(COL_PAY_MONTH)) ?: "",
        paymentYear    = c.getString(c.getColumnIndexOrThrow(COL_PAY_YEAR)) ?: "",
        referenceNumber = c.getString(c.getColumnIndexOrThrow(COL_PAY_REFERENCE)) ?: "",
        mpesaCode      = c.getString(c.getColumnIndexOrThrow(COL_PAY_MPESA_CODE)) ?: "",
        status         = c.getString(c.getColumnIndexOrThrow(COL_PAY_STATUS)) ?: "",
        receiptPath    = c.getString(c.getColumnIndexOrThrow(COL_PAY_RECEIPT_PATH)) ?: "",
        notes          = c.getString(c.getColumnIndexOrThrow(COL_PAY_NOTES)) ?: ""
    )

    // ----------------------------------------------------------------
    // Landlord operations
    // ----------------------------------------------------------------
    fun getLandlordInfo(): ContentValues {
        val db = readableDatabase
        val cursor = db.query(TABLE_LANDLORD, null, null, null, null, null, null)
        val cv = ContentValues()
        if (cursor.moveToFirst()) {
            cv.put(COL_LANDLORD_NAME,    cursor.getString(cursor.getColumnIndexOrThrow(COL_LANDLORD_NAME)))
            cv.put(COL_LANDLORD_PHONE,   cursor.getString(cursor.getColumnIndexOrThrow(COL_LANDLORD_PHONE)))
            cv.put(COL_LANDLORD_EMAIL,   cursor.getString(cursor.getColumnIndexOrThrow(COL_LANDLORD_EMAIL)))
            cv.put(COL_LANDLORD_PAYBILL, cursor.getString(cursor.getColumnIndexOrThrow(COL_LANDLORD_PAYBILL)))
            cv.put(COL_LANDLORD_ACCOUNT, cursor.getString(cursor.getColumnIndexOrThrow(COL_LANDLORD_ACCOUNT)))
            cv.put(COL_LANDLORD_MPESA,   cursor.getString(cursor.getColumnIndexOrThrow(COL_LANDLORD_MPESA)))
        }
        cursor.close()
        return cv
    }

    fun validateLandlordLogin(phone: String, password: String): Boolean {
        val cursor = readableDatabase.query(
            TABLE_LANDLORD, null,
            "$COL_LANDLORD_PHONE=? AND $COL_LANDLORD_PASSWORD=?",
            arrayOf(phone, password), null, null, null
        )
        val valid = cursor.count > 0
        cursor.close()
        return valid
    }

    fun updateLandlordInfo(name: String, phone: String, email: String,
                           paybill: String, account: String, mpesa: String): Boolean {
        val cv = ContentValues().apply {
            put(COL_LANDLORD_NAME, name)
            put(COL_LANDLORD_PHONE, phone)
            put(COL_LANDLORD_EMAIL, email)
            put(COL_LANDLORD_PAYBILL, paybill)
            put(COL_LANDLORD_ACCOUNT, account)
            put(COL_LANDLORD_MPESA, mpesa)
        }
        return writableDatabase.update(TABLE_LANDLORD, cv, null, null) > 0
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------
    private fun <T> query(
        table: String, selection: String?, orderBy: String? = null,
        mapper: (Cursor) -> T
    ): List<T> {
        val list = mutableListOf<T>()
        val cursor = readableDatabase.query(table, null, selection, null, null, null, orderBy)
        cursor.use { if (it.moveToFirst()) do { list.add(mapper(it)) } while (it.moveToNext()) }
        return list
    }

    private fun <T> querySingle(
        table: String, selection: String, args: Array<String>,
        mapper: (Cursor) -> T
    ): T? {
        val cursor = readableDatabase.query(table, null, selection, args, null, null, null)
        return cursor.use { if (it.moveToFirst()) mapper(it) else null }
    }

    private fun countWhere(table: String, where: String): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $table WHERE $where", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }
}
