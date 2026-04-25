package com.barakaplaza.rentmanager.models

data class Tenant(
    var id: Int = 0,
    var name: String = "",
    var phone: String = "",
    var email: String = "",
    var idNumber: String = "",
    var houseNumber: String = "",
    var moveInDate: String = "",
    var emergencyContact: String = "",
    var isActive: Boolean = true,
    var balance: Double = 0.0
) {
    constructor(
        name: String,
        phone: String,
        email: String,
        idNumber: String,
        houseNumber: String,
        moveInDate: String,
        emergencyContact: String
    ) : this(
        id = 0,
        name = name,
        phone = phone,
        email = email,
        idNumber = idNumber,
        houseNumber = houseNumber,
        moveInDate = moveInDate,
        emergencyContact = emergencyContact,
        isActive = true,
        balance = 0.0
    )
}
