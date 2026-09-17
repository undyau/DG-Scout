package com.undy.dgscout.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undy.dgscout.data.CourseRepository
import com.undy.dgscout.model.Course
import com.undy.dgscout.location.deviceDefaultDistanceUnit
import com.undy.dgscout.model.DistanceUnit
import com.undy.dgscout.model.GeoPoint
import com.undy.dgscout.model.Note
import com.undy.dgscout.model.UnitPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CourseRepository(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    val localeDefaultUnit: DistanceUnit = deviceDefaultDistanceUnit(application)

    private val _unitPreference = MutableStateFlow(loadUnitPreference())
    val unitPreference: StateFlow<UnitPreference> = _unitPreference.asStateFlow()

    val effectiveDistanceUnit: StateFlow<DistanceUnit> = _unitPreference
        .map { it.resolve(localeDefaultUnit) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _unitPreference.value.resolve(localeDefaultUnit))

    private val _attachLocationToNotes = MutableStateFlow(prefs.getBoolean(KEY_ATTACH_LOCATION_TO_NOTES, true))
    val attachLocationToNotes: StateFlow<Boolean> = _attachLocationToNotes.asStateFlow()

    init {
        refresh()
    }

    fun setUnitPreference(preference: UnitPreference) {
        _unitPreference.value = preference
        prefs.edit().putString(KEY_UNIT_PREFERENCE, preference.name).apply()
    }

    private fun loadUnitPreference(): UnitPreference {
        val stored = prefs.getString(KEY_UNIT_PREFERENCE, null) ?: return UnitPreference.AUTOMATIC
        return runCatching { UnitPreference.valueOf(stored) }.getOrDefault(UnitPreference.AUTOMATIC)
    }

    fun setAttachLocationToNotes(attach: Boolean) {
        _attachLocationToNotes.value = attach
        prefs.edit().putBoolean(KEY_ATTACH_LOCATION_TO_NOTES, attach).apply()
    }

    fun refresh() {
        viewModelScope.launch { _courses.value = repository.listCourses() }
    }

    fun course(courseId: String): Course? = _courses.value.find { it.id == courseId }

    fun createCourse(name: String, holeCount: Int) {
        viewModelScope.launch {
            val course = CourseRepository.newCourse(name.trim().ifBlank { "Untitled Course" }, holeCount.coerceIn(1, 36))
            repository.saveCourse(course)
            refresh()
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
            refresh()
        }
    }

    fun exportAllData(onReady: (File) -> Unit) {
        viewModelScope.launch {
            onReady(repository.writeExportFile())
        }
    }

    fun importFromJson(jsonText: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val existingNames = _courses.value.map { it.name }
                repository.importCourses(jsonText, existingNames).size
            }
            refresh()
            onResult(result)
        }
    }

    private fun updateCourse(courseId: String, transform: (Course) -> Course) {
        val current = _courses.value.find { it.id == courseId } ?: return
        val updated = transform(current)
        _courses.value = _courses.value.map { if (it.id == courseId) updated else it }
        viewModelScope.launch { repository.saveCourse(updated) }
    }

    fun setPar(courseId: String, holeNumber: Int, par: Int?) = updateCourse(courseId) { course ->
        course.copy(holes = course.holes.map { if (it.number == holeNumber) it.copy(par = par) else it })
    }

    fun setBasket(courseId: String, holeNumber: Int, geoPoint: GeoPoint) = updateCourse(courseId) { course ->
        course.copy(holes = course.holes.map { if (it.number == holeNumber) it.copy(basket = geoPoint) else it })
    }

    fun clearBasket(courseId: String, holeNumber: Int) = updateCourse(courseId) { course ->
        course.copy(holes = course.holes.map { if (it.number == holeNumber) it.copy(basket = null) else it })
    }

    fun addNote(courseId: String, holeNumber: Int, text: String, geoPoint: GeoPoint?) {
        if (text.isBlank()) return
        updateCourse(courseId) { course ->
            val now = System.currentTimeMillis()
            val note = Note(
                id = UUID.randomUUID().toString(),
                text = text.trim(),
                location = geoPoint,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            course.copy(holes = course.holes.map { if (it.number == holeNumber) it.copy(notes = it.notes + note) else it })
        }
    }

    fun updateNoteText(courseId: String, holeNumber: Int, noteId: String, text: String) {
        if (text.isBlank()) return
        updateCourse(courseId) { course ->
            course.copy(holes = course.holes.map { hole ->
                if (hole.number != holeNumber) hole
                else hole.copy(notes = hole.notes.map { note ->
                    if (note.id == noteId) note.copy(text = text.trim(), updatedAtEpochMs = System.currentTimeMillis()) else note
                })
            })
        }
    }

    fun deleteNote(courseId: String, holeNumber: Int, noteId: String) = updateCourse(courseId) { course ->
        course.copy(holes = course.holes.map { hole ->
            if (hole.number != holeNumber) hole else hole.copy(notes = hole.notes.filter { it.id != noteId })
        })
    }

    companion object {
        private const val PREFS_NAME = "dgscout_settings"
        private const val KEY_UNIT_PREFERENCE = "unit_preference"
        private const val KEY_ATTACH_LOCATION_TO_NOTES = "attach_location_to_notes"
    }
}
