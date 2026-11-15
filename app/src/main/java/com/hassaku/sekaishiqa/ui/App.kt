package com.hassaku.sekaishiqa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.data.Question

sealed interface Route {
    data object Home : Route
    data object Wordbooks : Route
    data object Calendar : Route
    data object Settings : Route
    data class Study(val title: String, val queue: List<Question>, val yearMode: Boolean = false) : Route
    data class BookDetail(val bookId: String) : Route
}

@Composable
fun AppRoot(store: AppStore) {
    var route by remember { mutableStateOf<Route>(Route.Home) }
    val data by store.data.collectAsState()

    val topLevel = route is Route.Home || route is Route.Wordbooks ||
        route is Route.Calendar || route is Route.Settings

    Scaffold(
        bottomBar = {
            if (topLevel) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route is Route.Home,
                        onClick = { route = Route.Home },
                        label = { Text("ホーム") },
                        icon = { Text("学") }
                    )
                    NavigationBarItem(
                        selected = route is Route.Wordbooks,
                        onClick = { route = Route.Wordbooks },
                        label = { Text("単語帳") },
                        icon = { Text("帳") }
                    )
                    NavigationBarItem(
                        selected = route is Route.Calendar,
                        onClick = { route = Route.Calendar },
                        label = { Text("記録") },
                        icon = { Text("暦") }
                    )
                    NavigationBarItem(
                        selected = route is Route.Settings,
                        onClick = { route = Route.Settings },
                        label = { Text("設定") },
                        icon = { Text("設") }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val r = route) {
                is Route.Home -> HomeScreen(
                    store = store,
                    onStudy = { title, queue, yearMode -> route = Route.Study(title, queue, yearMode) }
                )
                is Route.Wordbooks -> WordbooksScreen(
                    store = store,
                    onOpenBook = { route = Route.BookDetail(it) },
                    onStudy = { title, queue, _ -> route = Route.Study(title, queue, false) }
                )
                is Route.Calendar -> CalendarScreen(store)
                is Route.Settings -> SettingsScreen(store)
                is Route.Study -> StudyScreen(
                    store = store,
                    title = r.title,
                    queue = r.queue,
                    yearMode = r.yearMode,
                    onDone = { route = Route.Home }
                )
                is Route.BookDetail -> BookDetailScreen(
                    store = store,
                    bookId = r.bookId,
                    onBack = { route = Route.Wordbooks },
                    onStudy = { title, queue, _ -> route = Route.Study(title, queue, false) }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    store: AppStore,
    onStudy: (String, List<Question>, Boolean) -> Unit
) {
    val data by store.data.collectAsState()
    val due = remember(data) { store.dueQuestions(data) }
    val weak = remember(data) { store.questions.filter { store.isWeak(it, data) } }
    val fresh = remember(data) { store.questions.filter { !(data.progress[it.id]?.seen ?: false) } }
    val graduated = remember(data) { data.progress.values.count { it.graduated } }
    val todayLog = data.logs[store.today()]
    val chapters = remember { store.questions.map { it.chapter }.distinct() }
    val regions = remember { store.questions.map { it.region }.distinct() }
    val yearQuestions = remember { store.questions.filter { it.year != null } }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("世界史一問一答", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "連続記録 ${store.streakDays(data)}日 / 今日 ${todayLog?.answered ?: 0}問（目標 ${AppStore.DAILY_GOAL}問）" +
                    " / 卒業 ${graduated}問 / 全 ${store.questions.size}問"
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("忘れる前に、もう一度", style = MaterialTheme.typography.titleMedium)
                    Text("復習期限が来た問題: ${due.size}問")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onStudy("暗記（復習）", (due + fresh).take(30), false) },
                            enabled = due.isNotEmpty() || fresh.isNotEmpty()
                        ) { Text("暗記をはじめる") }
                        OutlinedButton(
                            onClick = { onStudy("ニガテのみ", weak.shuffled().take(30), false) },
                            enabled = weak.isNotEmpty()
                        ) { Text("ニガテのみ (${weak.size})") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onStudy("未学習", fresh.shuffled().take(30), false) },
                            enabled = fresh.isNotEmpty()
                        ) { Text("未学習 (${fresh.size})") }
                        OutlinedButton(
                            onClick = { onStudy("すべて", store.questions.shuffled().take(30), false) }
                        ) { Text("すべて") }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("切り口", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            onStudy("年号", yearQuestions.shuffled().take(30), true)
                        }) { Text("年号 (${yearQuestions.size})") }
                    }
                }
            }
        }
        item { Text("ヨコ（単元順）", style = MaterialTheme.typography.titleMedium) }
        items(chapters) { chapter ->
            val qs = store.questions.filter { it.chapter == chapter }
            ChapterRow(chapter, qs.size) { onStudy(chapter, qs, false) }
        }
        item { Text("タテ（地域別）", style = MaterialTheme.typography.titleMedium) }
        items(regions) { region ->
            val qs = store.questions.filter { it.region == region }
            ChapterRow(region, qs.size) { onStudy(region, qs.shuffled(), false) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ChapterRow(name: String, count: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name)
            Text("${count}問")
        }
    }
    HorizontalDivider()
}
