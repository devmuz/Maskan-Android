package com.maskan.mobileapp.ui.landlord.properties

import com.maskan.mobileapp.data.model.Property

data class PropertyGroup(
    val key: String,
    val buildingName: String,
    val address: String,
    val propertyType: com.maskan.mobileapp.data.model.PropertyType?,
    val flats: List<Property>,
) {
    val occupiedCount: Int get() = flats.count { it.occupied }
    val totalCount: Int get() = flats.size
    val maxCreatedAt: Long get() = flats.maxOfOrNull { it.createdAt?.time ?: 0L } ?: 0L
}

/**
 * Client-side building grouping (05-landlord-properties.md): flats sharing
 * `buildingName` + `address` are separate Firestore docs grouped only in the
 * UI. Groups sorted by most-recently-created flat first; flats within a
 * group sorted by unit name.
 */
fun groupProperties(properties: List<Property>): List<PropertyGroup> =
    properties
        .groupBy { it.groupKey }
        .map { (key, flats) ->
            val first = flats.first()
            PropertyGroup(
                key = key,
                buildingName = first.displayBuildingName,
                address = first.address,
                propertyType = flats.firstNotNullOfOrNull { it.propertyType },
                flats = flats.sortedBy { it.unit },
            )
        }
        .sortedByDescending { it.maxCreatedAt }
