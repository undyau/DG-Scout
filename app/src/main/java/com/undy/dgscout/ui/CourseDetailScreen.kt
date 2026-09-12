package com.undy.dgscout.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.undy.dgscout.model.Hole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    viewModel: CourseViewModel,
    courseId: String,
    onOpenHole: (Int) -> Unit,
    onBack: () -> Unit
) {
    val courses by viewModel.courses.collectAsState()
    val course = courses.find { it.id == courseId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(course?.name ?: "Course") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (course == null) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)) {
                Text("Course not found.")
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            items(course.holes, key = { it.number }) { hole ->
                HoleRow(hole = hole, onClick = { onOpenHole(hole.number) })
            }
        }
    }
}

@Composable
private fun HoleRow(hole: Hole, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Hole ${hole.number}" + (hole.par?.let { " · Par $it" } ?: ""))
                val noteCount = hole.notes.size
                val noteWord = if (noteCount == 1) "note" else "notes"
                Text("$noteCount $noteWord" + if (hole.basket != null) " · basket marked" else " · basket not marked")
            }
            if (hole.basket != null) {
                Text("📍")
            }
        }
    }
}
