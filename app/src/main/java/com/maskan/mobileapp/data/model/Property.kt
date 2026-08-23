package com.maskan.mobileapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
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
    /**
     * ISO currency code, copied from the owning landlord's `currencyCode` at
     * write time so tenants (who can't read `landlords`) still see the right
     * currency (02-data-models.md). Absent on properties created before this
     * shipped — callers should fall back to `"USD"`.
     */
    val currency: String? = null,
) {
    // @get:Exclude on every computed property below: without it, Firestore's reflection maps
    // e.g. getPropertyType() to the same default key ("propertyType") that
    // propertyTypeRaw's @PropertyName already targets, and throws "Found conflicting
    // getters" the first time a real `properties` doc is deserialized (same fix applied to
    // Tenant/Bill/ServiceRequest's raw+computed pairs).
    @get:Exclude
    val propertyType: PropertyType?
        get() = PropertyType.fromRaw(propertyTypeRaw)

    /** Client-side building group key: shared building name (or fallback name) + address. */
    @get:Exclude
    val groupKey: String
        get() = "${buildingName ?: name}|$address"

    /** Display title within a group: prefer unit, fall back to name. */
    @get:Exclude
    val displayTitle: String
        get() = unit.ifBlank { name }

    @get:Exclude
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
