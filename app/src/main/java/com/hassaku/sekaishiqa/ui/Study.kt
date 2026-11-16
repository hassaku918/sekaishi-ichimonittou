package com.hassaku.sekaishiqa.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassaku.sekaishiqa.data.AppStore
import com.hassaku.sekaishiqa.data.Grade
import com.hassaku.sekaishiqa.data.Progress
import com.hassaku.sekaishiqa.data.Question
import kotlin.math.abs

private val OverlayGreen = Color(0xFF79C57E)
private val OverlayGray = Color(0xFF9AA0A8)
private val OverlayBlue = Color(0xFF7FA8DC)

private data class UndoEntry(val questionId: String, val prev: Progress?, val grade: Grade)

private fun levelLabel(level: Int): Pair<String, Color> = when (level) {
    1 -> "Lv1 共通テスト向け基礎" to Color(0xFF12B76A)
    2 -> "Lv2 私大向け応用" to Color(0xFF2F6BFF)
    else -> "Lv3 早慶向け難題" to Color(0xFFF79009)
}

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
    val undoStack = remember { mutableStateListOf<UndoEntry>() }

    fun applyGrade(q: Question, g: Grade) {
        val prev = store.data.value.progress[q.id]
        store.grade(q.id, g)
        undoStack.add(UndoEntry(q.id, prev, g))
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

    fun undo() {
        val e = undoStack.removeLastOrNull() ?: return
        store.undoGrade(e.questionId, e.prev, e.grade)
        when (e.grade) {
            Grade.KNOWN -> known--
            Grade.UNSURE -> unsure--
            Grade.UNKNOWN -> unknown--
        }
        index--
        revealed = true
        dragX = 0f
        dragY = 0f
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkText)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 12.sp, color = InkTextSub)
                }
            }
            CircleButton(glyph = "↩", enabled = undoStack.isNotEmpty(), onClick = { undo() })
            Spacer(Modifier.size(8.dp))
            CircleButton(glyph = "×", enabled = true, onClick = onDone)
            Spacer(Modifier.size(10.dp))
            Text(
                if (queue.isEmpty()) "" else "${minOf(index + 1, queue.size)}/${queue.size}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = InkText
            )
        }
        Spacer(Modifier.height(10.dp))
        if (queue.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { (index.toFloat() / queue.size).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                color = AccentBlue,
                trackColor = InkSurfaceHigh
            )
        }
        Spacer(Modifier.height(12.dp))

        if (queue.isEmpty()) {
            Text("対象の問題がありません。", color = InkText)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) { Text("ホームへ戻る") }
            return
        }
        if (index >= queue.size) {
            Column(
                Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("セッション完了", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InkText)
                Text(
                    "合っていた $known / まあまあ $unsure / 間違っていた $unknown",
                    color = InkTextSub
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) { Text("ホームへ戻る", fontWeight = FontWeight.Bold) }
                if (undoStack.isNotEmpty()) {
                    TextButton(onClick = { undo() }) { Text("最後の判定をもどす") }
                }
            }
            return
        }

        val q = queue[index]
        val questionFade by animateFloatAsState(
            targetValue = if (revealed) 1f else 0f,
            animationSpec = tween(220),
            label = "questionFade"
        )
        val questionColor = lerp(InkText, InkTextSub, questionFade)

        // Direction overlay (like the original: the whole area tints while swiping)
        val dragMag = maxOf(abs(dragX), abs(dragY))
        val overlay = when {
            dragX > 40f && abs(dragX) >= abs(dragY) -> Triple(OverlayGreen, "✓", "合っていた")
            dragX < -40f && abs(dragX) >= abs(dragY) -> Triple(OverlayGray, "✕", "間違っていた")
            dragY > 40f -> Triple(OverlayBlue, "〜", "まあまあ")
            else -> null
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (overlay != null) {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                        .background(overlay.first.copy(alpha = (dragMag / 260f).coerceIn(0f, 0.92f))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(84.dp).clip(CircleShape)
                                .border(3.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(overlay.second, fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(overlay.third, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = InkSurface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.5.dp, InkText.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .graphicsLayer {
                        translationX = dragX
                        translationY = dragY
                        rotationZ = (dragX / 30f).coerceIn(-7f, 7f)
                    }
                    .pointerInput(index) {
                        detectDragGestures(
                            onDragEnd = {
                                when {
                                    abs(dragX) >= abs(dragY) && dragX > 170f -> applyGrade(q, Grade.KNOWN)
                                    abs(dragX) >= abs(dragY) && dragX < -170f -> applyGrade(q, Grade.UNKNOWN)
                                    dragY > 170f -> applyGrade(q, Grade.UNSURE)
                                }
                                dragX = 0f
                                dragY = 0f
                            },
                            onDragCancel = { dragX = 0f; dragY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragX += dragAmount.x
                                dragY += dragAmount.y
                                if (!revealed && maxOf(abs(dragX), abs(dragY)) > 70f) revealed = true
                            }
                        )
                    }
            ) {
                Column(Modifier.fillMaxSize().padding(18.dp)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        StatusPill(store = store, questionId = q.id)
                    }
                    Spacer(Modifier.weight(2f))
                    val questionText = if (yearMode) "『${q.a}』に関わる出来事が起きた年は？" else q.q
                    val answerText = if (yearMode) formatYear(q.year) else q.a
                    Text(
                        questionText,
                        fontSize = 21.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = questionColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedContent(
                        targetState = revealed,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 })
                                .togetherWith(fadeOut(tween(120)))
                                .using(SizeTransform(clip = false))
                        },
                        label = "answerReveal"
                    ) { shown ->
                        if (shown) {
                            val dividerColor = InkOutline
                            Column {
                                Spacer(Modifier.height(26.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                                            drawLine(
                                                color = dividerColor,
                                                start = Offset(0f, 0f),
                                                end = Offset(size.width, 0f),
                                                strokeWidth = 2f,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("こたえ", fontSize = 11.sp, color = InkTextSub)
                                    }
                                    Spacer(Modifier.size(10.dp))
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(50))
                                            .border(1.dp, InkOutline, RoundedCornerShape(50))
                                            .clickable { showBookmark = true }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("しおり", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkText)
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    answerText,
                                    fontSize = 25.sp,
                                    lineHeight = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = InkText,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (!yearMode && q.year != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        formatYear(q.year),
                                        textAlign = TextAlign.Center,
                                        color = InkTextSub,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (q.note.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        q.note,
                                        textAlign = TextAlign.Center,
                                        color = InkTextSub,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.height(0.dp))
                        }
                    }
                    Spacer(Modifier.weight(3f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val (lvText, lvColor) = levelLabel(q.level)
                        Box(Modifier.size(8.dp).clip(CircleShape).background(lvColor))
                        Spacer(Modifier.size(6.dp))
                        Text(lvText, fontSize = 12.sp, color = InkTextSub)
                        Spacer(Modifier.weight(1f))
                        Text(q.chapter, fontSize = 11.sp, color = InkTextSub)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            if (revealed) "右へ＝合っていた ・ 左へ＝間違っていた ・ 下へ＝まあまあ"
            else "スワイプすると答えが出ます。そのまま離すと判定",
            fontSize = 11.sp,
            color = InkTextSub,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showBookmark && index < queue.size) {
        BookmarkDialog(store = store, question = queue[index], onDismiss = { showBookmark = false })
    }
}

@Composable
private fun CircleButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(InkSurfaceHigh)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            glyph,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) InkText else InkTextSub.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun StatusPill(store: AppStore, questionId: String) {
    val data by store.data.collectAsState()
    val p = data.progress[questionId]
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val (label, color) = when {
            p == null || !p.seen -> "新規" to Color(0xFF7C86A0)
            p.streak >= 1 -> "${p.streak}連続正解" to AccentBlue
            p.lastGrade == Grade.UNKNOWN.name -> "前回不正解" to Color(0xFFF79009)
            else -> "復習" to Color(0xFF7C86A0)
        }
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(color)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        val blueDots = if (p != null && p.seen) minOf(p.streak, 5) else 0
        val redDot = p != null && p.seen && p.streak == 0 && p.lastGrade == Grade.UNKNOWN.name
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { i ->
                val c = when {
                    i >= 5 - blueDots && blueDots > 0 -> AccentBlue
                    i == 4 && redDot -> AccentRed
                    else -> InkOutline
                }
                Box(Modifier.size(8.dp).clip(CircleShape).background(c))
            }
        }
        if (p != null && p.seen) {
            val attempts = p.correctCount + p.wrongCount
            if (attempts > 0) {
                Text("${attempts}回", fontSize = 11.sp, color = InkTextSub)
            }
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
