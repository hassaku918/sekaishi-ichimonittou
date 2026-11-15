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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
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
                    onStudy = { title, queue, yearMode -> route = Route.Study(title, queue, yearMode) },
                    onOpenWordbooks = { route = Route.Wordbooks }
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
    onStudy: (String, List<Question>, Boolean) -> Unit,
    onOpenWordbooks: () -> Unit
) {
    val data by store.data.collectAsState()
    val due = remember(data) { store.dueQuestions(data) }
    val weak = remember(data) { store.questions.filter { store.isWeak(it, data) } }
    val fresh = remember(data) { store.questions.filter { !(data.progress[it.id]?.seen ?: false) } }
    val graduated = remember(data) { data.progress.values.count { it.graduated } }
    val streak = remember(data) { store.streakDays(data) }
    val chapterStats = remember(data) { store.chapterStats(data) }
    val regionStats = remember(data) { store.regionStats(data) }
    val todayAnswered = data.logs[store.today()]?.answered ?: 0
    val chapters = store.chapters
    val regions = store.regions

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val yokoHeaderIndex = 3
    val tateHeaderIndex = yokoHeaderIndex + 1 + chapters.size

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "世界史一問一答",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text("全 ${store.questions.size}問 / 年号 ${store.yearQuestions.size}問", color = Color(0xFFC5CAE9))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("連続記録", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC5CAE9))
                            Text("${streak}日", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        }
                        Column {
                            Text("今日", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC5CAE9))
                            Text(
                                "${todayAnswered} / ${AppStore.DAILY_GOAL}問",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                        Column {
                            Text("卒業", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC5CAE9))
                            Text("${graduated}問", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (todayAnswered.toFloat() / AppStore.DAILY_GOAL).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFC107),
                        trackColor = Color(0xFF3949AB)
                    )
                    Text(
                        if (todayAnswered >= AppStore.DAILY_GOAL) "今日の目標達成。連続記録がのびます"
                        else "あと ${AppStore.DAILY_GOAL - todayAnswered}問で今日の目標達成",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC5CAE9)
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("忘れる前に、もう一度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("復習期限 ${due.size}問 ・ ニガテ ${weak.size}問 ・ 未学習 ${fresh.size}問")
                    Button(
                        onClick = { onStudy("暗記（復習）", (due + fresh).take(30), false) },
                        enabled = due.isNotEmpty() || fresh.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("暗記をはじめる") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onStudy("すべて", store.questions.shuffled().take(30), false) },
                            modifier = Modifier.weight(1f)
                        ) { Text("すべて") }
                        OutlinedButton(
                            onClick = { onStudy("ニガテのみ", weak.shuffled().take(30), false) },
                            enabled = weak.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("ニガテのみ") }
                        OutlinedButton(
                            onClick = { onStudy("未学習", fresh.shuffled().take(30), false) },
                            enabled = fresh.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("未学習") }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeCard(
                        title = "ヨコ",
                        subtitle = "単元の順に通して学ぶ",
                        color = Color(0xFF1565C0),
                        modifier = Modifier.weight(1f)
                    ) { scope.launch { listState.animateScrollToItem(yokoHeaderIndex) } }
                    ModeCard(
                        title = "タテ",
                        subtitle = "国・地域ごとにつなげて学ぶ",
                        color = Color(0xFF00838F),
                        modifier = Modifier.weight(1f)
                    ) { scope.launch { listState.animateScrollToItem(tateHeaderIndex) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeCard(
                        title = "年号",
                        subtitle = "出来事と年代をセットで（${store.yearQuestions.size}問）",
                        color = Color(0xFFEF6C00),
                        modifier = Modifier.weight(1f)
                    ) { onStudy("年号", store.yearQuestions.shuffled().take(30), true) }
                    ModeCard(
                        title = "my単語帳",
                        subtitle = "しおりで集めた自分だけの単語帳",
                        color = Color(0xFF6A1B9A),
                        modifier = Modifier.weight(1f)
                    ) { onOpenWordbooks() }
                }
            }
        }
        item {
            Text("ヨコ（単元順）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(chapters, key = { "ch-$it" }) { chapter ->
            val qs = store.byChapter[chapter].orEmpty()
            val st = chapterStats[chapter]
            ProgressRow(chapter, qs.size, st?.get(0) ?: 0, st?.get(1) ?: 0) { onStudy(chapter, qs, false) }
        }
        item {
            Text("タテ（地域別）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(regions, key = { "rg-$it" }) { region ->
            val qs = store.byRegion[region].orEmpty()
            val st = regionStats[region]
            ProgressRow(region, qs.size, st?.get(0) ?: 0, st?.get(1) ?: 0) { onStudy(region, qs.shuffled(), false) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = modifier.height(96.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = Color(0xE6FFFFFF))
        }
    }
}

@Composable
private fun ProgressRow(name: String, count: Int, seen: Int, graduated: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, fontWeight = FontWeight.Medium)
                Text("${count}問")
            }
            LinearProgressIndicator(
                progress = { if (count == 0) 0f else seen.toFloat() / count },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "学習済み ${seen}問 ・ 卒業 ${graduated}問",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
