package com.undy.dgscout.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.undy.dgscout.model.Course
import com.undy.dgscout.model.DistanceUnit
import com.undy.dgscout.model.UnitPreference
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    viewModel: CourseViewModel,
    onOpenCourse: (String) -> Unit
) {
    val context = LocalContext.current
    val courses by viewModel.courses.collectAsState()
    val unitPreference by viewModel.unitPreference.collectAsState()
    val effectiveUnit by viewModel.effectiveDistanceUnit.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showUnitsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DG Scout") },
                actions = {
                    TextButton(
                        onClick = { viewModel.exportAllData { file -> shareExportFile(context, file) } },
                        enabled = courses.isNotEmpty()
                    ) { Text("Export") }
                    TextButton(onClick = { showUnitsDialog = true }) {
                        Text(if (effectiveUnit == DistanceUnit.FEET) "Units: ft" else "Units: m")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New course")
            }
        }
    ) { innerPadding ->
        if (courses.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            ) {
                Text("No courses yet. Tap + to add the course you're scouting.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(courses, key = { it.id }) { course ->
                    CourseRow(
                        course = course,
                        onClick = { onOpenCourse(course.id) },
                        onDelete = { viewModel.deleteCourse(course.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCourseDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, holeCount ->
                viewModel.createCourse(name, holeCount)
                showCreateDialog = false
            }
        )
    }

    if (showUnitsDialog) {
        UnitsDialog(
            current = unitPreference,
            localeDefault = viewModel.localeDefaultUnit,
            onSelect = { viewModel.setUnitPreference(it) },
            onDismiss = { showUnitsDialog = false }
        )
    }
}

@Composable
private fun UnitsDialog(
    current: UnitPreference,
    localeDefault: DistanceUnit,
    onSelect: (UnitPreference) -> Unit,
    onDismiss: () -> Unit
) {
    val autoLabel = "Automatic (" + (if (localeDefault == DistanceUnit.FEET) "feet" else "metres") + ")"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Distance units") },
        text = {
            Column {
                UnitOptionRow(autoLabel, current == UnitPreference.AUTOMATIC) { onSelect(UnitPreference.AUTOMATIC) }
                UnitOptionRow("Feet", current == UnitPreference.FEET) { onSelect(UnitPreference.FEET) }
                UnitOptionRow("Metres", current == UnitPreference.METERS) { onSelect(UnitPreference.METERS) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun UnitOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun CourseRow(course: Course, onClick: () -> Unit, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val basketsSet = course.holes.count { it.basket != null }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${course.holes.size} holes · $basketsSet baskets marked · scouted ${dateFormat.format(course.createdAtEpochMs)}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { showDeleteConfirmation = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete course")
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete \"${course.name}\"?") },
            text = { Text("This removes all holes, basket positions, and notes for this course. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirmation = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CreateCourseDialog(onDismiss: () -> Unit, onCreate: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var holeCountText by remember { mutableStateOf("18") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New course") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = holeCountText,
                    onValueChange = { holeCountText = it.filter(Char::isDigit) },
                    label = { Text("Number of holes") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val holeCount = holeCountText.toIntOrNull() ?: 18
                onCreate(name, holeCount)
            }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun shareExportFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export DG Scout data"))
}
