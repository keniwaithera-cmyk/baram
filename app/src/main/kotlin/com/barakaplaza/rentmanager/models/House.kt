package com.barakaplaza.rentmanager.models

data class House(
    var id: Int = 0,
    var houseNumber: String = "",
    var floor: String = "",
    var type: String = "",
    var monthlyRent: Double = 0.0,
    var isOccupied: Boolean = false,
    var description: String = ""
) {
    constructor(
        houseNumber: String,
        floor: String,
        type: String,
        monthlyRent: Double,
        description: String
    ) : this(
        id = 0,
        houseNumber = houseNumber,
        floor = floor,
        type = type,
        monthlyRent = monthlyRent,
        isOccupied = false,
        description = description
    )

    fun getDisplayName(): String = "House $houseNumber - $type ($floor)"

    fun getRentDisplay(): String = "KSh ${String.format("%,.0f", monthlyRent)}/month"
}
