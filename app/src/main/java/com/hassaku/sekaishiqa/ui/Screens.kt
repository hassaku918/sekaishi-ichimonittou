package com.hassaku.sekaishiqa.ui

import android.content.Intent
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
import androidx.compose.material3.FilterChip
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

// ---------------- Essays ----------------

@Composable
fun EssayListScreen(store: AppStore, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val data by store.data.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("戻る") }
            Text("論述", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(store.essays) { essay ->
                Card(Modifier.fillMaxWidth(), onClick = { onOpen(essay.id) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(essay.theme, style = MaterialTheme.typography.titleSmall)
                        Text(essay.prompt)
                        val score = data.essayScores[essay.id]
                        if (score != null) Text("前回の自己採点: ${gradeLabel(score)}")
                    }
                }
            }
        }
    }
}

@Composable
fun EssayDetailScreen(store: AppStore, essayId: String, onBack: () -> Unit) {
    val essay = store.essays.firstOrNull { it.id == essayId }
    var showAnswer by remember { mutableStateOf(false) }
    var tapped by remember { mutableStateOf<Set<String>>(emptySet()) }
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("戻る") }
        if (essay == null) {
            Text("論述が見つかりません。")
            return
        }
        Text(essay.theme, style = MaterialTheme.typography.titleLarge)
        Text(essay.prompt)
        if (!showAnswer) {
            Button(onClick = { showAnswer = true }) { Text("模範解答を見る") }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("模範解答", style = MaterialTheme.typography.titleSmall)
                    Text(essay.answer)
                    Text("採点キーワード（書けていたらタップ）", style = MaterialTheme.typography.titleSmall)
                    essay.keywords.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { kw ->
                                FilterChip(
                                    selected = kw in tapped,
                                    onClick = { tapped = if (kw in tapped) tapped - kw else tapped + kw },
                                    label = { Text(kw) }
                                )
                            }
                        }
                    }
                    val ratio = if (essay.keywords.isEmpty()) 0
                        else tapped.size * 100 / essay.keywords.size
                    Text("キーワード達成率: ${ratio}%（${tapped.size}/${essay.keywords.size}）")
                }
            }
            Text("自己採点")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { store.recordEssay(essay.id, Grade.UNKNOWN); onBack() }) { Text("できない") }
                OutlinedButton(onClick = { store.recordEssay(essay.id, Grade.UNSURE); onBack() }) { Text("まあまあ") }
                Button(onClick = { store.recordEssay(essay.id, Grade.KNOWN); onBack() }) { Text("できた") }
            }
        }
    }
}

private fun gradeLabel(name: String): String = when (name) {
    Grade.KNOWN.name -> "できた"
    Grade.UNSURE.name -> "まあまあ"
    else -> "できない"
}

// ---------------- Wordbooks ----------------

@Composable
fun WordbooksScreen(
    store: AppStore,
    onOpenBook: (String) -> Unit,
    onStudy: (String, List<Question>, Boolean) -> Unit
) {
    val data by store.data.collectAsState()
    var newName by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Card(Modifier.fillMaxWidth(), onClick = { onOpenBook(book.id) }) {
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
fun CalendarScreen(store: AppStore) {
    val data by store.data.collectAsState()
    val today = LocalDate.now()
    val totalAnswered = data.logs.values.sumOf { it.answered }
    val totalCorrect = data.logs.values.sumOf { it.correct }
    val graduated = data.progress.values.count { it.graduated }
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("学習記録", style = MaterialTheme.typography.headlineSmall)
        Text(
            "連続記録 ${store.streakDays(data)}日（1日${AppStore.DAILY_GOAL}問で継続）" +
                " / 累計 ${totalAnswered}問 / 正解 ${totalCorrect} / 卒業 ${graduated}問"
        )
        Text("直近35日", style = MaterialTheme.typography.titleMedium)
        val start = today.minusDays(34)
        (0 until 5).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { dow ->
                    val day = start.plusDays((week * 7 + dow).toLong())
                    val log = data.logs[day.toEpochDay()]
                    val mark = when {
                        log == null || log.answered == 0 -> "・"
                        log.answered >= AppStore.DAILY_GOAL -> "◎"
                        else -> "○"
                    }
                    Column(Modifier.weight(1f)) {
                        Text(mark, style = MaterialTheme.typography.titleMedium)
                        Text("${day.monthValue}/${day.dayOfMonth}", style = MaterialTheme.typography.labelSmall)
                        Text("${log?.answered ?: 0}問", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Text("◎ = 目標達成 / ○ = 学習あり / ・ = なし")
    }
}

// ---------------- Settings ----------------

@Composable
fun SettingsScreen(store: AppStore) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("設定", style = MaterialTheme.typography.headlineSmall)
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
