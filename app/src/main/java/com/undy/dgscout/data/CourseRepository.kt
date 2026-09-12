package com.undy.dgscout.data

import android.content.Context
import com.undy.dgscout.model.Course
import com.undy.dgscout.model.GeoPoint
import com.undy.dgscout.model.Hole
import com.undy.dgscout.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Stores each course as its own JSON file under filesDir/courses/<id>.json.
 * Everything is local-only; there is no server or account.
 */
class CourseRepository(context: Context) {

    private val appContext = context.applicationContext
    private val coursesDir: File = File(appContext.filesDir, "courses").apply { mkdirs() }

    suspend fun listCourses(): List<Course> = withContext(Dispatchers.IO) {
        coursesDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file -> runCatching { parseCourse(JSONObject(file.readText())) }.getOrNull() }
            ?.sortedByDescending { it.createdAtEpochMs }
            ?: emptyList()
    }

    suspend fun saveCourse(course: Course) = withContext(Dispatchers.IO) {
        val target = File(coursesDir, "${course.id}.json")
        val tmp = File(coursesDir, "${course.id}.json.tmp")
        tmp.writeText(courseToJson(course).toString())
        if (target.exists()) target.delete()
        tmp.renameTo(target)
        Unit
    }

    suspend fun deleteCourse(courseId: String) = withContext(Dispatchers.IO) {
        File(coursesDir, "$courseId.json").delete()
        Unit
    }

    /** Bundles every course into one JSON file under cacheDir, ready to hand to a share intent. */
    suspend fun writeExportFile(): File = withContext(Dispatchers.IO) {
        val export = JSONObject().apply {
            put("exportedAtEpochMs", System.currentTimeMillis())
            put("courses", JSONArray().apply { listCourses().forEach { put(courseToJson(it)) } })
        }
        val exportsDir = File(appContext.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(exportsDir, "dgscout-export-$stamp.json")
        file.writeText(export.toString(2))
        file
    }

    companion object {
        fun newCourse(name: String, holeCount: Int): Course {
            val now = System.currentTimeMillis()
            return Course(
                id = UUID.randomUUID().toString(),
                name = name,
                createdAtEpochMs = now,
                holes = (1..holeCount).map { Hole(number = it) }
            )
        }
    }
}

private fun geoPointToJson(point: GeoPoint): JSONObject = JSONObject().apply {
    put("latitude", point.latitude)
    put("longitude", point.longitude)
    put("accuracyMeters", point.accuracyMeters?.toDouble() ?: JSONObject.NULL)
    put("recordedAtEpochMs", point.recordedAtEpochMs)
}

private fun parseGeoPoint(obj: JSONObject?): GeoPoint? {
    if (obj == null) return null
    return GeoPoint(
        latitude = obj.getDouble("latitude"),
        longitude = obj.getDouble("longitude"),
        accuracyMeters = if (obj.isNull("accuracyMeters")) null else obj.getDouble("accuracyMeters").toFloat(),
        recordedAtEpochMs = obj.getLong("recordedAtEpochMs")
    )
}

private fun noteToJson(note: Note): JSONObject = JSONObject().apply {
    put("id", note.id)
    put("text", note.text)
    put("location", note.location?.let { geoPointToJson(it) } ?: JSONObject.NULL)
    put("createdAtEpochMs", note.createdAtEpochMs)
    put("updatedAtEpochMs", note.updatedAtEpochMs)
}

private fun parseNote(obj: JSONObject): Note = Note(
    id = obj.getString("id"),
    text = obj.getString("text"),
    location = parseGeoPoint(obj.optJSONObject("location")),
    createdAtEpochMs = obj.getLong("createdAtEpochMs"),
    updatedAtEpochMs = obj.getLong("updatedAtEpochMs")
)

private fun holeToJson(hole: Hole): JSONObject = JSONObject().apply {
    put("number", hole.number)
    put("par", hole.par ?: JSONObject.NULL)
    put("basket", hole.basket?.let { geoPointToJson(it) } ?: JSONObject.NULL)
    put("notes", JSONArray().apply { hole.notes.forEach { put(noteToJson(it)) } })
}

private fun parseHole(obj: JSONObject): Hole {
    val notesArray = obj.optJSONArray("notes") ?: JSONArray()
    val notes = (0 until notesArray.length()).map { parseNote(notesArray.getJSONObject(it)) }
    return Hole(
        number = obj.getInt("number"),
        par = if (obj.isNull("par")) null else obj.getInt("par"),
        basket = parseGeoPoint(obj.optJSONObject("basket")),
        notes = notes
    )
}

private fun courseToJson(course: Course): JSONObject = JSONObject().apply {
    put("id", course.id)
    put("name", course.name)
    put("createdAtEpochMs", course.createdAtEpochMs)
    put("holes", JSONArray().apply { course.holes.forEach { put(holeToJson(it)) } })
}

private fun parseCourse(obj: JSONObject): Course {
    val holesArray = obj.optJSONArray("holes") ?: JSONArray()
    val holes = (0 until holesArray.length()).map { parseHole(holesArray.getJSONObject(it)) }
    return Course(
        id = obj.getString("id"),
        name = obj.getString("name"),
        createdAtEpochMs = obj.getLong("createdAtEpochMs"),
        holes = holes.sortedBy { it.number }
    )
}
