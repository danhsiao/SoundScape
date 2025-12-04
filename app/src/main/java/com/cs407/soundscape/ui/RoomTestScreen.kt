package com.cs407.soundscape.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.soundscape.data.SessionManager
import com.cs407.soundscape.data.SoundEventViewModel
import com.cs407.soundscape.data.SoundEventViewModelFactory

@Composable
fun RoomTestScreen() {
    val sessionManager = remember { SessionManager() }
    val userId = remember { sessionManager.getUserId() }

    if (userId == null) {
        Text("Please sign in to view events")
        return
    }

    val vm: SoundEventViewModel =
        viewModel(factory = SoundEventViewModelFactory(userId))

    val events by vm.events.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text("User ID: $userId", modifier = Modifier.padding(16.dp))
        Button(onClick = { vm.addSampleEvent() }) {
            Text("Add Sample Event")
        }

        LazyColumn {
            items(events) { event ->
                Text("${event.id}: ${event.label} @ ${event.timestamp}")
            }
        }
    }
}
