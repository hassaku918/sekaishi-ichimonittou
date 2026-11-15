package com.hassaku.sekaishiqa.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.data.Grade
import com.hassaku.sekaishiqa.data.Question

@Composable
fun StudyScreen(
    store: AppStore,
    title: String,
    queue: List<Question>,
    yearMode: Boolean,
    onDone: () -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var known by remember { mutableIntStateOf(0) }
    var unsure by remember { mutableIntStateOf(0) }
    var unknown by remember { mutableIntStateOf(0) }
    var showBookmark by remember { mutableStateOf(false) }
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onDone) { Text("やめる") }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(if (index < queue.size) "${index + 1} / ${queue.size}" else "")
        }
        Spacer(Modifier.height(8.dp))

        if (queue.isEmpty()) {
            Text("対象の問題がありません。")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDone) { Text("ホームへ戻る") }
            return
        }
        if (index >= queue.size) {
            Text("セッション完了", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("分かる ${known} / まあまあ ${unsure} / 分からない ${unknown}")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDone) { Text("ホームへ戻る") }
            return
        }

        val q = queue[index]

        fun applyGrade(g: Grade) {
            store.grade(q.id, g)
            when (g) {
                Grade.KNOWN -> known++
                Grade.UNSURE -> unsure++
                Grade.UNKNOWN -> unknown++
            }
            revealed = false
            dragX = 0f
            dragY = 0f
            index++
        }

        Card(
            onClick = { revealed = true },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer { translationX = dragX; translationY = dragY }
                .pointerInput(q.id, revealed) {
                    detectDragGestures(
                        onDragEnd = {
                            if (revealed) {
                                when {
                                    dragX > 160f -> applyGrade(Grade.KNOWN)
                                    dragX < -160f -> applyGrade(Grade.UNKNOWN)
                                    dragY > 160f -> applyGrade(Grade.UNSURE)
                                }
                            }
                            dragX = 0f
                            dragY = 0f
                        },
                        onDragCancel = { dragX = 0f; dragY = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragX += dragAmount.x
                            dragY += dragAmount.y
                        }
                    )
                }
        ) {
            Column(
                Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${q.chapter} / ${q.region} / レベル${q.level}",
                    style = MaterialTheme.typography.labelMedium
                )
                if (yearMode) {
                    Text("『${q.a}』に関わる出来事が起きた年は？", style = MaterialTheme.typography.titleLarge)
                    if (revealed) {
                        Text(formatYear(q.year), style = MaterialTheme.typography.headlineMedium)
                        Text(q.note)
                    } else {
                        Text("タップで年を表示")
                    }
                } else {
                    Text(q.q, style = MaterialTheme.typography.titleLarge)
                    if (revealed) {
                        Text(q.a, style = MaterialTheme.typography.headlineMedium)
                        if (q.year != null) Text("年: ${formatYear(q.year)}")
                        if (q.note.isNotBlank()) Text(q.note)
                    } else {
                        Text("タップで答えを表示")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { showBookmark = true }) { Text("しおり（単語帳へ）") }
            if (!revealed) {
                Button(onClick = { revealed = true }) { Text("答えを見る") }
            }
        }
        if (revealed) {
            Spacer(Modifier.height(8.dp))
            Text("右スワイプ＝分かる / 左＝分からない / 下＝まあまあ でも判定できます")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { applyGrade(Grade.UNKNOWN) }, Modifier.weight(1f)) { Text("分からない") }
                OutlinedButton(onClick = { applyGrade(Grade.UNSURE) }, Modifier.weight(1f)) { Text("まあまあ") }
                Button(onClick = { applyGrade(Grade.KNOWN) }, Modifier.weight(1f)) { Text("分かる") }
            }
        }
    }

    if (showBookmark && index < queue.size) {
        BookmarkDialog(store = store, question = queue[index], onDismiss = { showBookmark = false })
    }
}

@Composable
private fun BookmarkDialog(store: AppStore, question: Question, onDismiss: () -> Unit) {
    val data by store.data.collectAsState()
    val q = question
    var newName by remember { mutableStateOf("") }
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("my単語帳に入れる") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (data.wordbooks.isEmpty()) {
                        Text("単語帳がまだありません。下で作成できます。")
                    }
                    data.wordbooks.forEach { book ->
                        Row(Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = q.id in book.questionIds,
                                onCheckedChange = { store.toggleInWordbook(book.id, q.id) }
                            )
                            Text(book.name, Modifier.padding(top = 12.dp))
                        }
                    }
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("新しい単語帳の名前") },
                        singleLine = true
                    )
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            store.createWordbook(newName)
                            newName = ""
                        }
                    }) { Text("単語帳を作成") }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("閉じる") }
            }
        )
}

fun formatYear(year: Int?): String = when {
    year == null -> "年不明"
    year < 0 -> "紀元前${-year}年"
    else -> "${year}年"
}
