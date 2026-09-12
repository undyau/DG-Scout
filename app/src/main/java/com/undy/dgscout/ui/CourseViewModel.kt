package com.undy.dgscout.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undy.dgscout.data.CourseRepository
import com.undy.dgscout.model.Course
import com.undy.dgscout.model.GeoPoint
import com.undy.dgscout.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CourseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CourseRepository(application)

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _courses.value = repository.listCourses() }
    }

    fun course(courseId: String): Course? = _courses.value.find { it.id == courseId }

    fun createCourse(name: String, holeCount: Int) {
        viewModelScope.launch {
            val course = CourseRepository.newCourse(name.ifBlank { "Untitled Course" }, holeCount.coerceIn(1, 36))
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
}
