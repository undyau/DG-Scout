package com.undy.dgscout.model

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val recordedAtEpochMs: Long
)

data class Note(
    val id: String,
    val text: String,
    val location: GeoPoint?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

data class Hole(
    val number: Int,
    val par: Int? = null,
    val basket: GeoPoint? = null,
    val notes: List<Note> = emptyList()
)

data class Course(
    val id: String,
    val name: String,
    val createdAtEpochMs: Long,
    val holes: List<Hole>
)
