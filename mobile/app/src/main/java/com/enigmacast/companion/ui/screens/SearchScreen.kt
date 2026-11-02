package com.enigmacast.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.enigmacast.companion.protocol.SearchItem
import com.enigmacast.companion.ui.AppViewModel
import com.enigmacast.companion.ui.components.EmptyState
import com.enigmacast.companion.ui.components.SectionLabel
import com.enigmacast.companion.ui.components.Text
import com.enigmacast.companion.ui.components.TopBar
import com.enigmacast.companion.ui.theme.Ink

/** Search ([scope] "all") or the Torrents tab ([scope] "torrents"): the TV searches, results open on the TV. */
@Composable
fun SearchScreen(vm: AppViewModel, scope: String) {
    val torrents = scope == SCOPE_TORRENTS
    val query = vm.searchQueries[scope].orEmpty()
    val result = vm.searchResults[scope]
    Column(Modifier.fillMaxSize()) {
        TopBar(title = if (torrents) "Torrents" else "Zoeken")
        OutlinedTextField(
            value = query,
            onValueChange = { vm.search(it, scope) },
            singleLine = true,
            enabled = vm.canSearch,
            placeholder = { Text(if (torrents) "Zoek films en series" else "Zoek films, series, personen of zenders") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        when {
            !vm.canSearch -> EmptyState("⌕", "Zoeken", "Verbind eerst met de tv om te zoeken.")
            vm.searchingScope == scope -> Text("Zoeken…", color = Ink.Muted, modifier = Modifier.padding(20.dp))
            query.trim().length < 2 || result == null ->
                Text("Kies een resultaat om het op de tv te openen.", color = Ink.Subtle, modifier = Modifier.padding(20.dp))
            result.sections.isEmpty() -> Text("Geen resultaten", color = Ink.Muted, modifier = Modifier.padding(20.dp))
            else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                result.sections.forEach { section ->
                    item(key = "s-${section.id}") {
                        SectionLabel(sectionTitle(section.id), Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp))
                    }
                    items(section.items, key = { "${section.id}-${it.key}" }) { item ->
                        ResultRow(item) { vm.openSearchResult(item.key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(item: SearchItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(Ink.Card)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 42.dp, height = 60.dp).clip(RoundedCornerShape(8.dp)).background(Ink.Canvas)) {
            if (item.image.isNotBlank()) {
                AsyncImage(model = item.image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Ink.Text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.subtitle == "director") {
                Text("Regisseur", color = Ink.Subtle, style = MaterialTheme.typography.bodySmall)
            } else if (item.subtitle.isNotBlank()) {
                Text(item.subtitle, color = Ink.Subtle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text("▶", color = Ink.Accent)
    }
}

private fun sectionTitle(id: String): String = when (id) {
    "now" -> "Nu op tv"
    "channels" -> "Zenders"
    "later" -> "Straks"
    "recordings" -> "Opnames"
    "films" -> "Films"
    "series" -> "Series"
    "iptv" -> "IPTV-zenders"
    "people" -> "Personen"
    "torrents" -> "Torrents"
    else -> id
}

const val SCOPE_ALL = "all"
const val SCOPE_TORRENTS = "torrents"
