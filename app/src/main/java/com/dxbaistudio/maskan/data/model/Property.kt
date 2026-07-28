package com.dxbaistudio.maskan.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * `properties` collection. One document per rentable flat/unit (not per
 * building). Field names are load-bearing — shared with an existing Flutter
 * app and must match exactly (02-data-models.md).
 */
data class Property(
    @DocumentId
    val id: String = "",
    val landlordId: String = "",
    val name: String = "",
    val address: String = "",
    val unit: String = "",
    val monthlyRent: Double = 0.0,
    val occupied: Boolean = false,
    val propertyIdCode: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    @get:PropertyName("propertyType") @set:PropertyName("propertyType")
    var propertyTypeRaw: String? = null,
    val buildingName: String? = null,
    val electricityAccountNumber: String? = null,
    val houseTaxNumber: String? = null,
    val waterTaxNumber: String? = null,
    val photoUrl: String? = null,
) {
    val propertyType: PropertyType?
        get() = PropertyType.fromRaw(propertyTypeRaw)

    /** Client-side building group key: shared building name (or fallback name) + address. */
    val groupKey: String
        get() = "${buildingName ?: name}|$address"

    /** Display title within a group: prefer unit, fall back to name. */
    val displayTitle: String
        get() = unit.ifBlank { name }

    val displayBuildingName: String
        get() = buildingName ?: name
}

enum class PropertyType(val raw: String, val label: String, val supportsMultipleUnits: Boolean) {
    FLAT("flat", "Flat", true),
    VILLA("villa", "Villa", false),
    INDEPENDENT_HOUSE("independentHouse", "House", false),
    SHOP("shop", "Shop", false),
    ;

    companion object {
        fun fromRaw(raw: String?): PropertyType? = entries.firstOrNull { it.raw == raw }
    }
}

/**
 * `propertyIdCodes` registry collection. Document ID *is* the code itself.
 * Used purely to enforce uniqueness of tenant-login codes.
 */
data class PropertyIdCodeEntry(
    @DocumentId
    val code: String = "",
    val landlordId: String = "",
    val propertyId: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
)
