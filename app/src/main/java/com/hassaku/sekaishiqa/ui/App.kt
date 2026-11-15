package com.hassaku.sekaishiqa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.data.Question

// Palette matched to the original app's black home theme.
val InkBackground = Color(0xFF0C0C11)
val InkSurface = Color(0xFF1B1B23)
val InkSurfaceHigh = Color(0xFF26262F)
val InkOutline = Color(0xFF3A3A44)
val InkTextSub = Color(0xFF9E9EA7)
val AccentPurple = Color(0xFF6C63FF)
val AccentBlue = Color(0xFF8F8BFF)
val AccentRed = Color(0xFFE5484D)
val AccentGreen = Color(0xFF12B76A)

sealed interface Route {
    data object Home : Route
    data object Wordbooks : Route
    data object Calendar : Route
    data object Settings : Route
    data class Study(
        val title: String,
        val queue: List<Question>,
        val yearMode: Boolean = false,
        val subtitle: String = ""
    ) : Route
    data class BookDetail(val bookId: String) : Route
}

private val StudyLightColors = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF2F6BFF),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111318),
    onSurface = Color(0xFF111318),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceVariant = Color(0xFFF0F0F3),
    outline = Color(0xFFE0E0E6)
)

@Composable
fun AppRoot(store: AppStore) {
    var route by remember { mutableStateOf<Route>(Route.Home) }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val r = route) {
                is Route.Home -> HomeScreen(
                    store = store,
                    onStudy = { title, queue, yearMode, subtitle ->
                        route = Route.Study(title, queue, yearMode, subtitle)
                    },
                    onOpenWordbooks = { route = Route.Wordbooks },
                    onOpenCalendar = { route = Route.Calendar },
                    onOpenSettings = { route = Route.Settings }
                )
                is Route.Wordbooks -> WordbooksScreen(
                    store = store,
                    onBack = { route = Route.Home },
                    onOpenBook = { route = Route.BookDetail(it) },
                    onStudy = { title, queue, _ -> route = Route.Study(title, queue, false, "一問一答・my単語帳") }
                )
                is Route.Calendar -> CalendarScreen(store, onBack = { route = Route.Home })
                is Route.Settings -> SettingsScreen(store, onBack = { route = Route.Home })
                is Route.Study -> androidx.compose.material3.MaterialTheme(colorScheme = StudyLightColors) {
                    androidx.compose.material3.Surface(color = Color.White) {
                        StudyScreen(
                            store = store,
                            title = r.title,
                            subtitle = r.subtitle,
                            queue = r.queue,
                            yearMode = r.yearMode,
                            onDone = { route = Route.Home }
                        )
                    }
                }
                is Route.BookDetail -> BookDetailScreen(
                    store = store,
                    bookId = r.bookId,
                    onBack = { route = Route.Wordbooks },
                    onStudy = { title, queue, _ -> route = Route.Study(title, queue, false, "一問一答・my単語帳") }
                )
            }
        }
    }
}

private enum class Axis { YOKO, TATE, NENGO }
private enum class StudyMode { ALL, ANKI, WEAK, FRESH }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    store: AppStore,
    onStudy: (String, List<Question>, Boolean, String) -> Unit,
    onOpenWordbooks: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val data by store.data.collectAsState()
    val streak = remember(data) { store.streakDays(data) }
    val graduated = remember(data) { data.progress.values.count { it.graduated } }
    val seenTotal = remember(data) { data.progress.values.count { it.seen } }
    val due = remember(data) { store.dueQuestions(data) }
    val streakCounts = remember(data) {
        (1..5).map { n -> data.progress.values.count { p -> if (n == 5) p.streak >= 5 else p.streak == n } }
    }
    val todayAnswered = data.logs[store.today()]?.answered ?: 0
    val total = store.questions.size

    var axis by remember { mutableStateOf(Axis.YOKO) }
    var selChapter by remember { mutableStateOf<String?>(null) }
    var selRegion by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(StudyMode.ALL) }
    var levels by remember { mutableStateOf(setOf(1, 2, 3)) }

    fun buildQueue(): List<Question> {
        val scope = when (axis) {
            Axis.YOKO -> selChapter?.let { store.byChapter[it].orEmpty() }
                ?: store.chapters.flatMap { store.byChapter[it].orEmpty() }
            Axis.TATE -> selRegion?.let { store.byRegion[it].orEmpty() }
                ?: store.regions.flatMap { store.byRegion[it].orEmpty() }
            Axis.NENGO -> store.yearQuestions
        }
        var qs = scope.filter { it.level in levels }
        qs = when (mode) {
            StudyMode.ALL -> qs
            StudyMode.ANKI -> qs.filter { store.isDue(it, data) }
            StudyMode.WEAK -> qs.filter { store.isWeak(it, data) }
            StudyMode.FRESH -> qs.filter { !(data.progress[it.id]?.seen ?: false) }
        }
        return if (axis == Axis.YOKO && mode == StudyMode.ALL) qs else qs.shuffled()
    }

    val queue = remember(data, axis, selChapter, selRegion, mode, levels) { buildQueue() }

    fun startStudy() {
        if (queue.isEmpty()) return
        val axisLabel = when (axis) { Axis.YOKO -> "ヨコ"; Axis.TATE -> "タテ"; Axis.NENGO -> "年号" }
        val modeLabel = when (mode) {
            StudyMode.ALL -> "すべて"; StudyMode.ANKI -> "暗記"
            StudyMode.WEAK -> "ニガテのみ"; StudyMode.FRESH -> "未学習"
        }
        val title = when (axis) {
            Axis.YOKO -> selChapter ?: "全体"
            Axis.TATE -> selRegion ?: "全体"
            Axis.NENGO -> "年号"
        }
        onStudy(title, queue, axis == Axis.NENGO, "一問一答・$axisLabel・$modeLabel")
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(2.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "世界史 一問一答",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Pill("全機能フリー", AccentPurple)
                        Text("一問一答 ${total}問", style = MaterialTheme.typography.labelMedium, color = InkTextSub)
                    }
                }
                HeaderButton(top = "${streak}日", label = "連続記録", onClick = onOpenCalendar)
                Spacer(Modifier.width(8.dp))
                HeaderButton(top = "暦", label = "カレンダー", onClick = onOpenCalendar)
                Spacer(Modifier.width(8.dp))
                HeaderButton(top = "設", label = "設定", onClick = onOpenSettings)
            }
        }
        item {
            Card(
                onClick = onOpenCalendar,
                colors = CardDefaults.cardColors(containerColor = InkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill("連続記録", AccentPurple)
                        Spacer(Modifier.weight(1f))
                        Text("${streak}日", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(" ›", color = InkTextSub, style = MaterialTheme.typography.headlineMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("今日のログインプレイ", style = MaterialTheme.typography.labelMedium, color = InkTextSub)
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (todayAnswered >= AppStore.DAILY_GOAL) "完了"
                            else "あと ${AppStore.DAILY_GOAL - todayAnswered}問",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (todayAnswered >= AppStore.DAILY_GOAL) AccentGreen else Color.White
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (todayAnswered.toFloat() / AppStore.DAILY_GOAL).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = InkSurfaceHigh
                    )
                    Text(
                        "卒業 ${graduated}問 ・ 学習済み ${seenTotal}問 / 全 ${total}問",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkTextSub
                    )
                }
            }
        }
        item {
            Card(
                onClick = {
                    if (due.isNotEmpty()) {
                        onStudy("全体", due.shuffled(), false, "一問一答・全体・暗記")
                    }
                },
                colors = CardDefaults.cardColors(containerColor = InkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("全体の進捗", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "習得 ${graduated}/${total} ・ ${if (total == 0) 0 else graduated * 100 / total}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue
                        )
                    }
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else graduated.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentPurple,
                        trackColor = InkSurfaceHigh
                    )
                    Text("着手 ${seenTotal}/${total}", style = MaterialTheme.typography.labelSmall, color = InkTextSub)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("正解", style = MaterialTheme.typography.labelMedium, color = InkTextSub)
                        streakCounts.forEachIndexed { i, c ->
                            StreakPill(n = i + 1, count = c, highlighted = i == 0)
                        }
                    }
                    Text(
                        if (due.isNotEmpty()) "タップで暗記の復習（期限 ${due.size}問）"
                        else "復習期限の問題はありません",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkTextSub
                    )
                }
            }
        }
        item {
            SectionLabel("出題軸")
            Spacer(Modifier.height(6.dp))
            SegmentedControl(
                options = listOf("ヨコ", "タテ", "年号", "my単語帳"),
                selectedIndex = axis.ordinal,
                onSelect = { i ->
                    when (i) {
                        0 -> axis = Axis.YOKO
                        1 -> axis = Axis.TATE
                        2 -> axis = Axis.NENGO
                        3 -> onOpenWordbooks()
                    }
                }
            )
        }
        item {
            SectionLabel("範囲")
            Spacer(Modifier.height(6.dp))
            when (axis) {
                Axis.YOKO -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScopeChip("すべて", total, selChapter == null, AccentRed) { selChapter = null }
                    store.chapters.forEach { ch ->
                        ScopeChip(ch, store.byChapter[ch].orEmpty().size, selChapter == ch, AccentRed) { selChapter = ch }
                    }
                }
                Axis.TATE -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScopeChip("すべて", total, selRegion == null, AccentPurple) { selRegion = null }
                    store.regions.forEach { rg ->
                        ScopeChip(rg, store.byRegion[rg].orEmpty().size, selRegion == rg, AccentPurple) { selRegion = rg }
                    }
                }
                Axis.NENGO -> Card(
                    colors = CardDefaults.cardColors(containerColor = InkSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "年号 ${store.yearQuestions.size}問。出来事と年代をセットで覚えます。",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            SectionLabel("モード")
            Spacer(Modifier.height(6.dp))
            SegmentedControl(
                options = listOf("すべて", "暗記", "ニガテのみ", "未学習"),
                selectedIndex = mode.ordinal,
                onSelect = { mode = StudyMode.entries[it] }
            )
        }
        item {
            SectionLabel("レベル")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { lv ->
                    LevelPill(lv, lv in levels) {
                        levels = if (lv in levels) {
                            if (levels.size == 1) levels else levels - lv
                        } else levels + lv
                    }
                }
            }
        }
        item {
            Button(
                onClick = { startStudy() },
                enabled = queue.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    disabledContainerColor = InkSurfaceHigh
                )
            ) {
                Text(
                    if (queue.isNotEmpty()) "学習をはじめる（${queue.size}問）" else "対象の問題がありません",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "ヨコ＝単元の順 ・ タテ＝国・地域ごと ・ 年号＝出来事と年代",
                style = MaterialTheme.typography.labelSmall,
                color = InkTextSub
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun HeaderButton(top: String, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(InkSurface)
            .border(1.dp, InkOutline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(top, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkTextSub)
    }
}

@Composable
private fun StreakPill(n: Int, count: Int, highlighted: Boolean) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (highlighted) AccentPurple else InkSurfaceHigh)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            "${n}連 ${count}",
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) Color.White else InkTextSub
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFD6D6DE)
    )
}

@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InkSurfaceHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) Color.Black else InkTextSub
                )
            }
        }
    }
}

@Composable
private fun ScopeChip(name: String, count: Int, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent else InkSurface)
            .border(1.dp, if (selected) accent else InkOutline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = Color.White
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color(0xE6FFFFFF) else InkTextSub
        )
    }
}

@Composable
private fun LevelPill(level: Int, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) AccentRed else InkSurface)
            .border(1.dp, if (on) AccentRed else InkOutline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Lv$level",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (on) Color.White else InkTextSub
        )
    }
}
