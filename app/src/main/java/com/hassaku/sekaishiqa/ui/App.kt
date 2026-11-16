package com.hassaku.sekaishiqa.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.data.Question

// Theme-driven palette: values come from the active Material color scheme,
// so every screen follows the light / dark-blue dark mode selection.
val PaperBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background
val PaperDot: Color
    @Composable get() = MaterialTheme.colorScheme.outline
val InkText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
val InkTextSub: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val InkSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface
val InkSurfaceHigh: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val InkOutline: Color
    @Composable get() = MaterialTheme.colorScheme.outline
val AccentBlue: Color
    @Composable get() = MaterialTheme.colorScheme.primary
val AccentPurple = Color(0xFF7C5CFC)
val AccentRed = Color(0xFFE5484D)
val AccentGreen = Color(0xFF12B76A)
val GoldPill = Color(0xFFF2B93B)

private enum class Axis { YOKO, TATE, NENGO }
private enum class StudyMode(val label: String, val short: String) {
    ALL("すべて", "すべて"), ANKI("暗記モード", "暗記"), WEAK("ニガテのみ", "ニガテ"), FRESH("未学習", "未学習")
}

private data class ScopeGroup(val name: String, val color: Color, val chapters: List<String>)

private val YokoGroups = listOf(
    ScopeGroup("古代オリエント・地中海", Color(0xFFEE8A1D), listOf("古代オリエント", "地中海諸民族", "ギリシア世界", "ヘレニズム世界", "ローマ共和国", "ローマ帝国")),
    ScopeGroup("アジアの古代帝国", Color(0xFFD94F70), listOf("中国の古代帝国", "東アジア・内陸アジアの諸国")),
    ScopeGroup("中国史", Color(0xFFE5484D), listOf("中国古代（先秦・秦漢）", "魏晋南北朝・隋唐", "宋・元・明", "明・清", "清末と近代化", "中華民国と中国共産党")),
    ScopeGroup("インド・東南アジア", Color(0xFFC74BD9), listOf("インド古代", "インド中世（デリー・スルタン朝）", "東南アジア大陸部", "東南アジア海域部", "インド近世・近代", "東南アジア近世・近代")),
    ScopeGroup("イスラーム・中央アジア", Color(0xFF12B76A), listOf("イスラームの成立とカリフ制", "イスラーム諸王朝とオスマン帝国", "中央アジア古代・中世", "中央アジア近現代")),
    ScopeGroup("中世ヨーロッパ", Color(0xFF7C5CFC), listOf("中世前期のヨーロッパ", "ビザンツ帝国とスラヴ世界", "中世後期のヨーロッパ")),
    ScopeGroup("近世ヨーロッパ", Color(0xFF2F6BFF), listOf("大航海時代", "近世ヨーロッパ")),
    ScopeGroup("市民革命と産業革命", Color(0xFF0E9BD8), listOf("市民革命", "フランス革命とナポレオン", "産業革命と資本主義", "アメリカ合衆国の形成", "19世紀のアメリカ", "20世紀のアメリカ")),
    ScopeGroup("帝国主義と世界大戦", Color(0xFF5E60CE), listOf("国民国家の形成", "帝国主義と植民地分割", "第一次世界大戦", "戦間期の世界", "第二次世界大戦", "アフリカ古代・中世", "近代アフリカ（植民地化と独立）", "先住民文明と植民地支配", "ラテンアメリカの独立と近代")),
    ScopeGroup("冷戦・現代世界", Color(0xFF7B61C9), listOf("冷戦の展開", "脱植民地化と第三世界", "現代世界（20世紀後半）", "現代世界（21世紀）", "古代・中世の日本と東アジア", "近世・近代の日本と世界")),
    ScopeGroup("文化・テーマ史", Color(0xFF0FA3A3), listOf("文化史（思想・宗教）", "文化史（文学・芸術）", "文化史（科学・学問）"))
)

private val RegionOrder = listOf(
    "西ヨーロッパ", "東ヨーロッパ・ロシア", "中東・イスラーム圏", "中央アジア",
    "中国", "インド", "東南アジア", "日本", "アフリカ", "アメリカ", "ラテンアメリカ"
)

private val RegionColors = mapOf(
    "西ヨーロッパ" to Color(0xFF2F6BFF),
    "東ヨーロッパ・ロシア" to Color(0xFF5E60CE),
    "中東・イスラーム圏" to Color(0xFF12B76A),
    "中央アジア" to Color(0xFF0FA3A3),
    "中国" to Color(0xFFE5484D),
    "インド" to Color(0xFFC74BD9),
    "東南アジア" to Color(0xFFEE8A1D),
    "日本" to Color(0xFFD94F70),
    "アフリカ" to Color(0xFFB7791F),
    "アメリカ" to Color(0xFF0E9BD8),
    "ラテンアメリカ" to Color(0xFF7C5CFC)
)

sealed interface Route {
    data object Home : Route
    data object Wordbooks : Route
    data class BookDetail(val bookId: String) : Route
    data object Calendar : Route
    data object Settings : Route
    data class Study(
        val title: String,
        val subtitle: String = "",
        val queue: List<Question>,
        val yearMode: Boolean = false
    ) : Route
}

@Composable
fun AppRoot(store: AppStore) {
    var route by remember { mutableStateOf<Route>(Route.Home) }
    var studyReturn by remember { mutableStateOf<Route>(Route.Home) }

    // System back button steps back one screen instead of leaving the app.
    BackHandler(enabled = route !is Route.Home) {
        route = when (val r = route) {
            is Route.BookDetail -> Route.Wordbooks
            is Route.Study -> studyReturn
            else -> Route.Home
        }
    }

    Scaffold(containerColor = PaperBackground) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val r = route) {
                Route.Home -> HomeScreen(
                    store = store,
                    onOpenCalendar = { route = Route.Calendar },
                    onOpenSettings = { route = Route.Settings },
                    onOpenWordbooks = { route = Route.Wordbooks },
                    onStudy = { title, subtitle, queue, yearMode ->
                        studyReturn = Route.Home
                        route = Route.Study(title, subtitle, queue, yearMode)
                    }
                )
                Route.Wordbooks -> WordbooksScreen(
                    store = store,
                    onBack = { route = Route.Home },
                    onOpenBook = { route = Route.BookDetail(it) },
                    onStudy = { title, queue, yearMode ->
                        studyReturn = Route.Wordbooks
                        route = Route.Study(title, "一問一答・my単語帳", queue, yearMode)
                    }
                )
                is Route.BookDetail -> BookDetailScreen(
                    store = store,
                    bookId = r.bookId,
                    onBack = { route = Route.Wordbooks },
                    onStudy = { title, queue, yearMode ->
                        studyReturn = Route.BookDetail(r.bookId)
                        route = Route.Study(title, "一問一答・my単語帳", queue, yearMode)
                    }
                )
                Route.Calendar -> CalendarScreen(store = store, onBack = { route = Route.Home })
                Route.Settings -> SettingsScreen(store = store, onBack = { route = Route.Home })
                is Route.Study -> StudyScreen(
                    store = store,
                    title = r.title,
                    subtitle = r.subtitle,
                    queue = r.queue.filter { it.id !in store.data.value.excludedIds },
                    yearMode = r.yearMode,
                    onDone = { route = studyReturn }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    store: AppStore,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWordbooks: () -> Unit,
    onStudy: (String, String, List<Question>, Boolean) -> Unit
) {
    val data by store.data.collectAsState()
    val total = store.questions.size
    val todayAnswered = data.logs[store.today()]?.answered ?: 0
    val streak = store.streakDays(data)
    val graduatedAll = data.progress.values.count { it.graduated }
    val seenAll = data.progress.values.count { it.seen }

    var axis by remember { mutableStateOf(Axis.YOKO) }
    var groupIdx by remember { mutableIntStateOf(0) }
    var yokoChapter by remember { mutableStateOf<String?>(null) }
    var regionIdx by remember { mutableIntStateOf(0) }
    var tateChapter by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(StudyMode.ALL) }
    var levels by remember { mutableStateOf(setOf(1, 2, 3)) }

    val group = YokoGroups[groupIdx]
    val regions = RegionOrder.filter { it in store.regions }
    val region = regions.getOrElse(regionIdx) { regions.firstOrNull() ?: "" }
    val regionColor = RegionColors[region] ?: AccentBlue
    val accent = if (axis == Axis.TATE) regionColor else group.color

    val chaptersInRegion = remember(region) {
        store.chapters.filter { ch -> store.byRegion[region]?.any { it.chapter == ch } == true }
    }
    val scopeAll: List<Question> = when (axis) {
        Axis.YOKO -> group.chapters.flatMap { store.byChapter[it].orEmpty() }
        Axis.TATE -> store.byRegion[region].orEmpty()
        Axis.NENGO -> group.chapters.flatMap { store.byChapter[it].orEmpty() }.filter { it.year != null }
    }
    val subChapters: List<String> = when (axis) {
        Axis.YOKO -> group.chapters
        Axis.TATE -> chaptersInRegion
        Axis.NENGO -> emptyList()
    }
    val selChapter = when (axis) {
        Axis.YOKO -> yokoChapter
        Axis.TATE -> tateChapter
        Axis.NENGO -> null
    }
    val scoped = if (selChapter != null) scopeAll.filter { it.chapter == selChapter } else scopeAll
    val leveled = scoped.filter { it.level in levels }

    fun progressOf(list: List<Question>): Triple<Int, Int, IntArray> {
        var seen = 0
        var grad = 0
        val streaks = IntArray(5)
        for (q in list) {
            val p = data.progress[q.id] ?: continue
            if (!p.seen) continue
            seen++
            if (p.graduated) grad++
            if (p.streak in 1..4) streaks[p.streak - 1]++
            else if (p.streak >= 5) streaks[4]++
        }
        return Triple(seen, grad, streaks)
    }
    val (seen, grad, streakCounts) = progressOf(leveled)
    val masteredPct = if (leveled.isEmpty()) 0 else grad * 100 / leveled.size

    val queue = remember(data, axis, groupIdx, regionIdx, selChapter, mode, levels) {
        val filtered = leveled.filter { q ->
            q.id !in data.excludedIds && when (mode) {
                StudyMode.ALL -> true
                StudyMode.ANKI -> store.isDue(q, data)
                StudyMode.WEAK -> store.isWeak(q, data)
                StudyMode.FRESH -> !(data.progress[q.id]?.seen ?: false)
            }
        }
        if (axis == Axis.YOKO && selChapter == null && mode == StudyMode.ALL) filtered else filtered.shuffled()
    }

    val dueQueue = remember(data) {
        store.dueQuestions(data).filter { it.id !in data.excludedIds }.shuffled()
    }

    val scopeTitle = when (axis) {
        Axis.YOKO -> "ヨコ・${group.name}・${selChapter ?: "すべて"}"
        Axis.TATE -> "タテ・$region・${selChapter ?: "すべて"}"
        Axis.NENGO -> "年号・${group.name}"
    } + if (levels.size < 3) levels.sorted().joinToString("") { "・Lv$it" } else ""
    val studyTitle = when (axis) {
        Axis.YOKO -> selChapter ?: group.name
        Axis.TATE -> selChapter ?: region
        Axis.NENGO -> group.name
    }
    val axisLabel = when (axis) { Axis.YOKO -> "ヨコ"; Axis.TATE -> "タテ"; Axis.NENGO -> "年号" }

    Box(Modifier.fillMaxSize()) {
        val dotColor = PaperDot
        Canvas(Modifier.fillMaxSize()) {
            val step = 26.dp.toPx()
            val r = 1.6.dp.toPx()
            var x = step / 2
            while (x < size.width) {
                var y = step / 2
                while (y < size.height) {
                    drawCircle(dotColor, r, Offset(x, y))
                    y += step
                }
                x += step
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("世界史 一問一答", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = InkText)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp)).background(GoldPill)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("FREE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C4300))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("一問一答 ${total}問", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
                HeaderButton(glyph = "${streak}日", label = "連続記録", onClick = onOpenCalendar)
                Spacer(Modifier.width(8.dp))
                HeaderButton(glyph = "暦", label = "カレンダー", onClick = onOpenCalendar)
                Spacer(Modifier.width(8.dp))
                HeaderButton(glyph = "⚙️", label = "設定", glyphSize = 20, onClick = onOpenSettings)
            }

            // Streak card
            Card(
                onClick = onOpenCalendar,
                colors = CardDefaults.cardColors(containerColor = InkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, InkOutline, RoundedCornerShape(16.dp))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp)).background(InkSurfaceHigh)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("記録", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("連続記録 ${streak}日", fontWeight = FontWeight.Bold, color = InkText)
                        Spacer(Modifier.weight(1f))
                        Text("›", color = InkTextSub, style = MaterialTheme.typography.headlineMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("今日のログインプレイ", style = MaterialTheme.typography.labelMedium, color = InkTextSub)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${todayAnswered}/${AppStore.DAILY_GOAL}問",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (todayAnswered >= AppStore.DAILY_GOAL) AccentGreen else InkText
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (todayAnswered.toFloat() / AppStore.DAILY_GOAL).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                        color = AccentBlue,
                        trackColor = InkSurfaceHigh
                    )
                    Text(
                        "卒業 ${graduatedAll}問 ・ 学習済み ${seenAll}問 / 全${total}問",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkTextSub
                    )
                }
            }

            // Progress card for the current selection
            Card(
                onClick = {
                    if (dueQueue.isNotEmpty()) {
                        onStudy("期限切れの復習", "一問一答・暗記", dueQueue, false)
                    }
                },
                colors = CardDefaults.cardColors(containerColor = InkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, InkOutline, RoundedCornerShape(16.dp))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            scopeTitle,
                            fontWeight = FontWeight.Bold,
                            color = InkText,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "習得 $grad/${leveled.size}・$masteredPct%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                    Text("着手 $seen/${leveled.size}", style = MaterialTheme.typography.labelSmall, color = InkTextSub)
                    LinearProgressIndicator(
                        progress = { if (leveled.isEmpty()) 0f else grad.toFloat() / leveled.size },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = accent,
                        trackColor = InkSurfaceHigh
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("正解", style = MaterialTheme.typography.labelSmall, color = InkTextSub)
                        (1..5).forEach { n ->
                            val has = streakCounts[n - 1] > 0
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (has) accent else InkSurface)
                                    .border(1.dp, if (has) accent else InkOutline, RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "${n}連",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (has) Color.White else InkTextSub
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (dueQueue.isNotEmpty()) {
                            Text(
                                "期限切れ ${dueQueue.size}問 ›",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed
                            )
                        }
                    }
                }
            }

            SectionLabel("出題軸")
            SegmentedControl(
                options = listOf("ヨコ", "タテ", "年号", "my単語帳"),
                selectedIndex = axis.ordinal,
                onSelect = {
                    if (it == 3) onOpenWordbooks() else axis = Axis.entries[it]
                }
            )

            SectionLabel("範囲")
            if (axis == Axis.TATE) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    regions.forEachIndexed { i, r ->
                        GroupPill(
                            name = r,
                            count = store.byRegion[r]?.size ?: 0,
                            color = RegionColors[r] ?: AccentBlue,
                            selected = i == regionIdx,
                            onClick = { regionIdx = i; tateChapter = null }
                        )
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YokoGroups.forEachIndexed { i, g ->
                        val count = if (axis == Axis.NENGO) {
                            g.chapters.flatMap { store.byChapter[it].orEmpty() }.count { it.year != null }
                        } else {
                            g.chapters.sumOf { store.byChapter[it]?.size ?: 0 }
                        }
                        GroupPill(
                            name = g.name,
                            count = count,
                            color = g.color,
                            selected = i == groupIdx,
                            onClick = { groupIdx = i; yokoChapter = null }
                        )
                    }
                }
            }
            if (subChapters.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = InkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, InkOutline, RoundedCornerShape(16.dp))
                ) {
                    FlowRow(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScopeChip(
                            label = "すべて",
                            count = scopeAll.size,
                            color = accent,
                            selected = selChapter == null,
                            onClick = {
                                if (axis == Axis.YOKO) yokoChapter = null else tateChapter = null
                            }
                        )
                        subChapters.forEach { ch ->
                            val count = scopeAll.count { it.chapter == ch }
                            if (count > 0) {
                                ScopeChip(
                                    label = ch,
                                    count = count,
                                    color = accent,
                                    selected = selChapter == ch,
                                    onClick = {
                                        if (axis == Axis.YOKO) yokoChapter = ch else tateChapter = ch
                                    }
                                )
                            }
                        }
                    }
                }
            }

            SectionLabel("モード")
            SegmentedControl(
                options = StudyMode.entries.map { it.label },
                selectedIndex = mode.ordinal,
                onSelect = { mode = StudyMode.entries[it] }
            )

            SectionLabel("レベル")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { lv ->
                    val selected = lv in levels
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) accent else InkSurface)
                            .border(1.dp, if (selected) accent else InkOutline, RoundedCornerShape(10.dp))
                            .clickable {
                                levels = if (selected) {
                                    if (levels.size > 1) levels - lv else levels
                                } else levels + lv
                            }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Text(
                            "Lv$lv",
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else InkTextSub
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (queue.isNotEmpty()) {
                        onStudy(studyTitle, "一問一答・$axisLabel・${mode.short}", queue, axis == Axis.NENGO)
                    }
                },
                enabled = queue.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(
                    if (axis == Axis.NENGO) "年号問題をはじめる" else "この範囲ではじめる",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                if (queue.isEmpty()) "対象の問題がありません（モード・レベルの条件を変えてください）"
                else "対象 ${queue.size}問が出題されます",
                style = MaterialTheme.typography.labelSmall,
                color = InkTextSub
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeaderButton(glyph: String, label: String, glyphSize: Int = 14, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(InkSurface)
                .border(1.dp, InkOutline, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, fontWeight = FontWeight.Bold, color = InkText, fontSize = glyphSize.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 10.sp, color = InkTextSub)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkText)
}

@Composable
private fun GroupPill(name: String, count: Int, color: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else InkSurface)
            .border(1.dp, if (selected) color else InkOutline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(8.dp).clip(CircleShape)
                .background(if (selected) Color.White else color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else InkText
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "$count",
            fontSize = 11.sp,
            color = if (selected) Color.White.copy(alpha = 0.85f) else InkTextSub
        )
    }
}

@Composable
private fun ScopeChip(label: String, count: Int, color: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else InkSurface)
            .border(1.dp, if (selected) color else InkOutline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else InkText
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "$count",
            fontSize = 11.sp,
            color = if (selected) Color.White.copy(alpha = 0.85f) else InkTextSub
        )
    }
}

@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(InkSurface)
            .border(1.dp, InkOutline, RoundedCornerShape(12.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                    .background(if (selected) AccentBlue else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) Color.White else InkText
                )
            }
        }
    }
}
