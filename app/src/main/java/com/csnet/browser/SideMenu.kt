package com.csnet.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SideMenu(
    onDismiss: () -> Unit,
    onClearData: () -> Unit,
    onSettings: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "CsNet Browser",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Divider()

            // Menu items
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.History, contentDescription = null) },
                label = { Text("History") },
                selected = false,
                onClick = { /* Implement history */ }
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                label = { Text("Bookmarks") },
                selected = false,
                onClick = { /* Implement bookmarks */ }
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                label = { Text("Downloads") },
                selected = false,
                onClick = { /* Implement downloads */ }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                label = { Text("Clear Browsing Data") },
                selected = false,
                onClick = onClearData
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Settings") },
                selected = false,
                onClick = onSettings
            )
        }
    }
}