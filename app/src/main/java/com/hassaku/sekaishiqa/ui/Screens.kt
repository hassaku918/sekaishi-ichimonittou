package com.hassaku.sekaishiqa.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.time.YearMonth
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.data.Grade
import com.hassaku.sekaishiqa.data.Question
import java.time.LocalDate

// ---------------- Wordbooks ----------------

@Composable
fun WordbooksScreen(
    store: AppStore,
    onBack: () -> Unit,
    onOpenBook: (String) -> Unit,
    onStudy: (String, List<Question>, Boolean) -> Unit
) {
    val data by store.data.collectAsState()
    var newName by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("‹ 戻る") }
        Text("my単語帳", style = MaterialTheme.typography.headlineSmall)
        Text("単語帳はいくつでも作れます。学習カードの「しおり」から問題を追加します。")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("新しい単語帳の名前") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (newName.isNotBlank()) {
                    store.createWordbook(newName)
                    newName = ""
                }
            }) { Text("作成") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(data.wordbooks) { book ->
                Card(onClick = { onOpenBook(book.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(book.name, style = MaterialTheme.typography.titleSmall)
                        Text("${book.questionIds.size}問")
                    }
                }
            }
        }
    }
}

@Composable
fun BookDetailScreen(
    store: AppStore,
    bookId: String,
    onBack: () -> Unit,
    onStudy: (String, List<Question>, Boolean) -> Unit
) {
    val data by store.data.collectAsState()
    val book = data.wordbooks.firstOrNull { it.id == bookId }
    var renameText by remember { mutableStateOf("") }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("戻る") }
        if (book == null) {
            Text("単語帳が見つかりません。")
            return
        }
        Text(book.name, style = MaterialTheme.typography.headlineSmall)
        val qs = book.questionIds.mapNotNull { store.questionsById[it] }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onStudy(book.name, qs.shuffled(), false) },
                enabled = qs.isNotEmpty()
            ) { Text("この単語帳で学習") }
            OutlinedButton(onClick = { renameText = book.name; showRename = true }) { Text("名前変更") }
            OutlinedButton(onClick = { showDelete = true }) { Text("削除") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(qs) { q ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(q.a, style = MaterialTheme.typography.titleSmall)
                            Text(q.q)
                        }
                        TextButton(onClick = { store.toggleInWordbook(book.id, q.id) }) { Text("外す") }
                    }
                }
            }
        }
    }
    if (showRename && book != null) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("名前変更") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) store.renameWordbook(book.id, renameText)
                    showRename = false
                }) { Text("変更") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("中止") } }
        )
    }
    if (showDelete && book != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("単語帳を削除") },
            text = { Text("「${book.name}」を削除します。問題自体は消えません。") },
            confirmButton = {
                TextButton(onClick = {
                    store.deleteWordbook(book.id)
                    showDelete = false
                    onBack()
                }) { Text("削除") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("中止") } }
        )
    }
}

// ---------------- Calendar / records ----------------

@Composable
fun CalendarScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsState()
    var tab by remember { mutableStateOf(0) }
    val totalAnswered = data.logs.values.sumOf { it.answered }
    val freshCount = store.questions.count { !(data.progress[it.id]?.seen ?: false) }
    val graduated = data.progress.values.count { it.graduated }
    val ym = YearMonth.now()
    val first = ym.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val daysInMonth = ym.lengthOfMonth()

    fun metric(day: LocalDate): Int {
        val log = data.logs[day.toEpochDay()] ?: return 0
        return when (tab) {
            0 -> log.answered
            1 -> log.correct
            else -> log.answered - log.correct
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(InkSurface)
                    .border(1.dp, InkOutline, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("今まで解いた総問題数", style = MaterialTheme.typography.labelSmall, color = InkTextSub)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$totalAnswered", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(" 問", style = MaterialTheme.typography.labelMedium, color = InkTextSub)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("未学習", style = MaterialTheme.typography.labelSmall, color = InkTextSub)
                Text("${freshCount}問", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        SegmentedControl(
            options = listOf("総問題数", "覚えた問題", "苦手な問題"),
            selectedIndex = tab,
            onSelect = { tab = it }
        )
        Text(
            "連続記録 ${store.streakDays(data)}日（1日${AppStore.DAILY_GOAL}問で継続） / 卒業 ${graduated}問",
            style = MaterialTheme.typography.labelMedium,
            color = InkTextSub
        )
        Text("${ym.year}年 ${ym.monthValue}月", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth()) {
            listOf("月", "火", "水", "木", "金", "土", "日").forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkTextSub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        val cells = offset + daysInMonth
        val weeks = (cells + 6) / 7
        (0 until weeks).forEach { w ->
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { d ->
                    val dayNum = w * 7 + d - offset + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..daysInMonth) {
                            val date = ym.atDay(dayNum)
                            DayRing(day = dayNum, count = metric(date), isToday = date == LocalDate.now())
                        }
                    }
                }
            }
        }
        Text(
            "リングは1日100問を満量として表示しています",
            style = MaterialTheme.typography.labelSmall,
            color = InkTextSub
        )
    }
}

@Composable
private fun DayRing(day: Int, count: Int, isToday: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$day",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) AccentBlue else InkTextSub
        )
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(44.dp)) {
                val stroke = 4.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(
                    color = InkSurfaceHigh,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                if (count > 0) {
                    drawArc(
                        color = AccentBlue,
                        startAngle = -90f,
                        sweepAngle = 360f * minOf(count / 100f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Text("$count", fontSize = 10.sp, color = InkText)
        }
    }
}

// ---------------- Settings ----------------

@Composable
fun SettingsScreen(store: AppStore, onBack: () -> Unit) {
    val data by store.data.collectAsState()
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹ 戻る") }
        Text("設定", style = MaterialTheme.typography.headlineSmall)
        Text("テーマ", style = MaterialTheme.typography.titleMedium)
        SegmentedControl(
            options = listOf("ライト", "ダーク"),
            selectedIndex = if (data.darkMode) 1 else 0,
            onSelect = { store.setDarkMode(it == 1) }
        )
        Text("会員・課金・広告はありません。全ての機能を無料で使えます。学習データは端末内にのみ保存されます。")
        Button(onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "世界史一問一答 学習データ")
                putExtra(Intent.EXTRA_TEXT, store.exportJson())
            }
            context.startActivity(Intent.createChooser(intent, "学習データを書き出す"))
        }) { Text("学習データを書き出す（共有）") }
        OutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            label = { Text("読み込むJSONを貼り付け") },
            modifier = Modifier.fillMaxWidth().height(140.dp)
        )
        OutlinedButton(onClick = {
            message = if (importText.isNotBlank() && store.importJson(importText)) {
                importText = ""
                "読み込みました。"
            } else {
                "読み込みに失敗しました。JSONを確認してください。"
            }
        }) { Text("学習データを読み込む") }
        if (message.isNotBlank()) Text(message)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showReset = true }) { Text("全データをリセット") }
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("全データをリセット") },
            text = { Text("学習記録・単語帳を全て消します。元に戻せません。") },
            confirmButton = {
                TextButton(onClick = { store.resetAll(); showReset = false }) { Text("リセット") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("中止") } }
        )
    }
}
