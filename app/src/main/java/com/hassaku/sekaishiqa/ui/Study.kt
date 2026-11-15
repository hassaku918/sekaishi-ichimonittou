package com.hassaku.sekaishiqa.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.data.Grade
import com.hassaku.sekaishiqa.data.Question

private val StudyBlue = Color(0xFF2F6BFF)
private val StudyGreen = Color(0xFF12A150)
private val StudyGray = Color(0xFF5B6472)
private val StudyPillGreen = Color(0xFF0FA968)

@Composable
fun StudyScreen(
    store: AppStore,
    title: String,
    subtitle: String,
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onDone),
                contentAlignment = Alignment.Center
            ) {
                Text("×", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.size(10.dp))
            Text(
                if (queue.isEmpty()) "" else "${minOf(index + 1, queue.size)}/${queue.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        if (queue.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { (index.toFloat() / queue.size).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                color = StudyBlue,
                trackColor = Color(0xFFE3E6F0)
            )
        }
        Spacer(Modifier.height(12.dp))

        if (queue.isEmpty()) {
            Text("対象の問題がありません。")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDone) { Text("ホームへ戻る") }
            return
        }
        if (index >= queue.size) {
            Text("セッション完了", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("自信アリ ${known} / まあまあ ${unsure} / 自信なし ${unknown}")
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color(0xFFE0E0E6), RoundedCornerShape(16.dp))
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
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    StreakHistory(store = store, questionId = q.id)
                }
                Spacer(Modifier.weight(1f))
                if (yearMode) {
                    Text(
                        "『${q.a}』に関わる出来事が起きた年は？",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (revealed) Color(0xFF9AA0AA) else Color(0xFF111318),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (revealed) {
                        Spacer(Modifier.height(20.dp))
                        DashedDividerWithLabel()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            formatYear(q.year),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (q.note.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                q.note,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Text(
                        q.q,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (revealed) Color(0xFF9AA0AA) else Color(0xFF111318),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (revealed) {
                        Spacer(Modifier.height(20.dp))
                        DashedDividerWithLabel()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            q.a,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (q.year != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                formatYear(q.year),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (q.note.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                q.note,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${q.chapter} ・ ${q.region} ・ Lv${q.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    if (!revealed) {
                        Text(
                            "タップで答えを表示",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, Color(0xFFD5D7DD), RoundedCornerShape(50))
                            .clickable { showBookmark = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("しおり", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        if (revealed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { applyGrade(Grade.UNKNOWN) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudyGray)
                ) { Text("自信なし", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { applyGrade(Grade.UNSURE) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBlue)
                ) { Text("まあまあ", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { applyGrade(Grade.KNOWN) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudyGreen)
                ) { Text("自信アリ", fontWeight = FontWeight.Bold) }
            }
        } else {
            Button(
                onClick = { revealed = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF11131A))
            ) { Text("答えを見る", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Text(
                "答えを見たあと、右＝自信アリ / 左＝自信なし / 下＝まあまあ にスワイプでも判定できます",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showBookmark && index < queue.size) {
        BookmarkDialog(store = store, question = queue[index], onDismiss = { showBookmark = false })
    }
}

@Composable
private fun DashedDividerWithLabel() {
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                color = Color(0xFFD5D7DD),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "こたえ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StreakHistory(store: AppStore, questionId: String) {
    val data by store.data.collectAsState()
    val p = data.progress[questionId] ?: return
    if (!p.seen) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (p.streak >= 1) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(StudyPillGreen)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    "${p.streak}連続正解",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        val blue = minOf(p.correctCount, 5)
        val red = minOf(p.wrongCount, 5 - blue)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { i ->
                val c = when {
                    i < blue -> StudyBlue
                    i < blue + red -> Color(0xFFE5484D)
                    else -> Color(0xFFD8DAE0)
                }
                Box(Modifier.size(9.dp).clip(CircleShape).background(c))
            }
        }
        Text(
            "${p.correctCount + p.wrongCount}回",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (p.graduated) {
            Text(
                "卒業",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = StudyPillGreen
            )
        }
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
