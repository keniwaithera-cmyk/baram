package com.barakaplaza.rentmanager.models

data class Payment(
    var id: Int = 0,
    var tenantId: Int = 0,
    var tenantName: String = "",
    var houseNumber: String = "",
    var amount: Double = 0.0,
    var paymentMethod: String = "",
    var paymentDate: String = "",
    var paymentMonth: String = "",
    var paymentYear: String = "",
    var referenceNumber: String = "",
    var mpesaCode: String = "",
    var status: String = STATUS_PENDING,
    var receiptPath: String = "",
    var notes: String = ""
) {
    companion object {
        const val METHOD_MPESA    = "M-PESA"
        const val METHOD_CASH     = "CASH"
        const val METHOD_PAYBILL  = "PAYBILL"
        const val METHOD_STK_PUSH = "STK PUSH"

        const val STATUS_PENDING   = "PENDING"
        const val STATUS_CONFIRMED = "CONFIRMED"
        const val STATUS_FAILED    = "FAILED"
    }

    fun getAmountDisplay(): String = "KSh ${String.format("%,.0f", amount)}"
}
