package com.undy.dgscout.ui

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.undy.dgscout.model.DistanceUnit
import com.undy.dgscout.model.GeoPoint
import com.undy.dgscout.model.Note
import com.undy.dgscout.location.combinedAccuracy
import com.undy.dgscout.location.distanceInMeters
import com.undy.dgscout.location.formatAccuracy
import com.undy.dgscout.location.formatDistance
import com.undy.dgscout.location.getAccurateLocation
import com.undy.dgscout.location.hasLocationPermission
import com.undy.dgscout.location.isFresh
import com.undy.dgscout.location.locationUpdates
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleDetailScreen(
    viewModel: CourseViewModel,
    courseId: String,
    holeNumber: Int,
    onBack: () -> Unit,
    onNavigateToHole: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val courses by viewModel.courses.collectAsState()
    val course = courses.find { it.id == courseId }
    val hole = course?.holes?.find { it.number == holeNumber }
    val unit by viewModel.effectiveDistanceUnit.collectAsState()
    val attachLocationToNotes by viewModel.attachLocationToNotes.collectAsState()

    val holeNumbers = remember(course) { course?.holes?.map { it.number }?.sorted() ?: emptyList() }
    val currentIndex = holeNumbers.indexOf(holeNumber)
    // Previous/next wrap around at the ends of the course; with a single hole there is nowhere to go.
    val canCycle = currentIndex >= 0 && holeNumbers.size > 1
    val previousHoleNumber = if (canCycle) holeNumbers[(currentIndex - 1 + holeNumbers.size) % holeNumbers.size] else null
    val nextHoleNumber = if (canCycle) holeNumbers[(currentIndex + 1) % holeNumbers.size] else null

    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isFetchingBasket by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.any { it }
        if (hasPermission) pendingAction?.invoke() else statusMessage = "Location permission is needed to record positions."
        pendingAction = null
    }

    fun runWithLocationPermission(action: () -> Unit) {
        if (hasLocationPermission(context)) {
            hasPermission = true
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // Ask for permission up front so distance-to-basket can start updating right away.
    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context)) {
            runWithLocationPermission {}
        }
    }

    // Keep the GPS running while this hole is open, so it can settle to a tight fix and the
    // distance to the basket and to any located notes stays current.
    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        locationUpdates(context).collect { currentLocation = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hole $holeNumber") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { previousHoleNumber?.let(onNavigateToHole) },
                    enabled = previousHoleNumber != null
                ) { Text("Previous") }
                OutlinedButton(
                    onClick = { nextHoleNumber?.let(onNavigateToHole) },
                    enabled = nextHoleNumber != null
                ) { Text("Next") }
            }
        }
    ) { innerPadding ->
        if (hole == null) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)) {
                Text("Hole not found.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            statusMessage?.let {
                Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            ParRow(
                par = hole.par,
                onParChange = { viewModel.setPar(courseId, holeNumber, it) }
            )

            BasketCard(
                basket = hole.basket,
                currentLocation = currentLocation,
                unit = unit,
                isFetching = isFetchingBasket,
                onSetBasket = {
                    runWithLocationPermission {
                        isFetchingBasket = true
                        scope.launch {
                            val location = getAccurateLocation(context)
                            isFetchingBasket = false
                            if (location != null) {
                                viewModel.setBasket(
                                    courseId, holeNumber,
                                    GeoPoint(location.latitude, location.longitude, location.accuracy, System.currentTimeMillis())
                                )
                            } else {
                                statusMessage = "Couldn't get a GPS fix. Try again in the open."
                            }
                        }
                    }
                },
                onClearBasket = { viewModel.clearBasket(courseId, holeNumber) }
            )

            NotesSection(
                modifier = Modifier.weight(1f),
                notes = hole.notes,
                currentLocation = currentLocation,
                unit = unit,
                hasPermission = hasPermission,
                attachLocation = attachLocationToNotes,
                onAttachLocationChange = viewModel::setAttachLocationToNotes,
                onAddNote = { text, attachLocation ->
                    if (attachLocation) {
                        runWithLocationPermission {
                            scope.launch {
                                // The live stream has usually settled by now; only wait if it hasn't
                                // produced a recent fix yet.
                                val location = currentLocation?.takeIf(::isFresh)
                                    ?: getAccurateLocation(context, timeoutMs = 5_000L)
                                val geoPoint = location?.let {
                                    GeoPoint(it.latitude, it.longitude, it.accuracy, System.currentTimeMillis())
                                }
                                viewModel.addNote(courseId, holeNumber, text, geoPoint)
                            }
                        }
                    } else {
                        viewModel.addNote(courseId, holeNumber, text, null)
                    }
                },
                onUpdateNote = { noteId, text -> viewModel.updateNoteText(courseId, holeNumber, noteId, text) },
                onDeleteNote = { noteId -> viewModel.deleteNote(courseId, holeNumber, noteId) }
            )
        }
    }
}

@Composable
private fun ParRow(par: Int?, onParChange: (Int?) -> Unit) {
    var text by remember(par) { mutableStateOf(par?.toString() ?: "") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Par")
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it.filter(Char::isDigit)
                onParChange(text.toIntOrNull())
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.width(80.dp)
        )
    }
}

@Composable
private fun BasketCard(
    basket: GeoPoint?,
    currentLocation: Location?,
    unit: DistanceUnit,
    isFetching: Boolean,
    onSetBasket: () -> Unit,
    onClearBasket: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Basket")

            if (basket == null) {
                Text("No basket position recorded yet.")
            } else {
                Text("Recorded ${timeFormat.format(basket.recordedAtEpochMs)}")

                val distanceText = if (currentLocation != null) {
                    val meters = distanceInMeters(currentLocation.latitude, currentLocation.longitude, basket.latitude, basket.longitude)
                    val accuracy = combinedAccuracy(basket.accuracyMeters, currentLocation.accuracy)
                    val accuracySuffix = accuracy?.let { " (${formatAccuracy(it, unit)})" } ?: ""
                    "${formatDistance(meters, unit)} to basket$accuracySuffix"
                } else {
                    val recordedSuffix = basket.accuracyMeters?.let { " (recorded to within ${formatAccuracy(it, unit)})" } ?: ""
                    "Waiting for GPS fix to measure distance…$recordedSuffix"
                }
                Text(distanceText, modifier = Modifier.padding(top = 4.dp))
            }

            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSetBasket, enabled = !isFetching) {
                    if (isFetching) {
                        CircularProgressIndicator(modifier = Modifier.width(16.dp))
                    } else {
                        Text(if (basket == null) "Set Basket Here" else "Update Basket")
                    }
                }
                if (basket != null) {
                    OutlinedButton(onClick = onClearBasket) { Text("Clear") }
                }
            }
        }
    }
}

@Composable
private fun NotesSection(
    notes: List<Note>,
    currentLocation: Location?,
    unit: DistanceUnit,
    hasPermission: Boolean,
    attachLocation: Boolean,
    onAttachLocationChange: (Boolean) -> Unit,
    onAddNote: (String, Boolean) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isComposing by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }
    var editingNoteId by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Notes")
            if (!isComposing) {
                TextButton(onClick = { isComposing = true }) { Text("Add Note") }
            }
        }

        if (isComposing) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(checked = attachLocation, onCheckedChange = onAttachLocationChange, enabled = hasPermission)
                        Text("Attach my current location")
                    }
                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            onAddNote(draftText, attachLocation)
                            draftText = ""
                            isComposing = false
                        }, enabled = draftText.isNotBlank()) { Text("Save") }
                        TextButton(onClick = { isComposing = false; draftText = "" }) { Text("Cancel") }
                    }
                }
            }
        }

        val sortedNotes = notes.sortedByDescending { it.createdAtEpochMs }
        val timeFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(sortedNotes, key = { it.id }) { note ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (editingNoteId == note.id) {
                            OutlinedTextField(
                                value = editingText,
                                onValueChange = { editingText = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    onUpdateNote(note.id, editingText)
                                    editingNoteId = null
                                }, enabled = editingText.isNotBlank()) { Text("Save") }
                                TextButton(onClick = { editingNoteId = null }) { Text("Cancel") }
                            }
                        } else {
                            Text(note.text)
                            val locationSuffix = note.location?.let { location ->
                                if (currentLocation != null) {
                                    val meters = distanceInMeters(
                                        currentLocation.latitude, currentLocation.longitude,
                                        location.latitude, location.longitude
                                    )
                                    val accuracy = combinedAccuracy(location.accuracyMeters, currentLocation.accuracy)
                                    val accuracySuffix = accuracy?.let { " (${formatAccuracy(it, unit)})" } ?: ""
                                    " · ${formatDistance(meters, unit)} away$accuracySuffix"
                                } else {
                                    location.accuracyMeters?.let { " · recorded to within ${formatAccuracy(it, unit)}" } ?: ""
                                }
                            } ?: ""
                            Text(
                                "${timeFormat.format(note.updatedAtEpochMs)}$locationSuffix",
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Row(modifier = Modifier.padding(top = 4.dp)) {
                                IconButton(onClick = { editingNoteId = note.id; editingText = note.text }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit note")
                                }
                                IconButton(onClick = { onDeleteNote(note.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
