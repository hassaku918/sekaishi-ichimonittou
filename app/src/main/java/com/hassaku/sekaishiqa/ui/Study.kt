package com.hassaku.sekaishiqa.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OverlayGreen = Color(0xFF79C57E)
private val OverlayGray = Color(0xFF9AA0A8)
private val OverlayBlue = Color(0xFF7FA8DC)

private data class UndoEntry(val questionId: String, val prev: Progress?, val grade: Grade)

private fun levelLabel(level: Int): Pair<String, Color> = when (level) {
    1 -> "Lv1 共通テスト向け基礎" to Color(0xFF12B76A)
    2 -> "Lv2 私大向け応用" to Color(0xFF2F6BFF)
    else -> "Lv3 早慶向け難題" to Color(0xFFF79009)
}

private fun overlayFor(grade: Grade): Triple<Color, String, String> = when (grade) {
    Grade.KNOWN -> Triple(OverlayGreen, "✓", "合っていた")
    Grade.UNKNOWN -> Triple(OverlayGray, "✕", "間違っていた")
    Grade.UNSURE -> Triple(OverlayBlue, "〜", "まあまあ")
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
    var busy by remember { mutableStateOf(false) }
    var exitGrade by remember { mutableStateOf<Grade?>(null) }
    var flash by remember { mutableStateOf<Color?>(null) }
    val undoStack = remember { mutableStateListOf<UndoEntry>() }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

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
        scope.launch {
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkText)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 12.sp, color = InkTextSub)
                }
            }
            CircleButton(glyph = "↩", enabled = undoStack.isNotEmpty() && !busy, onClick = { undo() })
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
        // Keyed by index: when the question advances, the old card subtree is
        // dropped instantly, so the answer's exit animation cannot flash the
        // next question's answer on the fresh card.
        key(index) {
        val questionFade by animateFloatAsState(
            targetValue = if (revealed) 1f else 0f,
            animationSpec = tween(150),
            label = "questionFade"
        )
        val questionColor = lerp(InkText, InkTextSub, questionFade)

        val dragX = offsetX.value
        val dragY = offsetY.value
        val dragMag = maxOf(abs(dragX), abs(dragY))
        val activeGrade: Grade? = when {
            exitGrade != null -> exitGrade
            !revealed -> null
            abs(dragX) >= abs(dragY) && dragX > 40f -> Grade.KNOWN
            abs(dragX) >= abs(dragY) && dragX < -40f -> Grade.UNKNOWN
            dragY > 40f -> Grade.UNSURE
            else -> null
        }
        val overlay = activeGrade?.let { overlayFor(it) }
        val overlayAlpha = if (exitGrade != null) 0.85f else (dragMag / 220f).coerceIn(0f, 0.85f)

        Box(Modifier.fillMaxWidth().weight(1f)) {
            // Grade color wash behind the card while it flies off (as in the original).
            if (flash != null) {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                        .background(flash!!.copy(alpha = 0.45f))
                )
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
                        rotationZ = (dragX / 22f).coerceIn(-14f, 14f)
                    }
                    .pointerInput(index, busy) {
                        if (busy) return@pointerInput
                        detectDragGestures(
                            onDragEnd = {
                                val x = offsetX.value
                                val y = offsetY.value
                                if (!revealed) {
                                    // First swipe: releasing it only reveals the
                                    // answer. Grading is impossible from here,
                                    // however far the swipe went.
                                    if (maxOf(abs(x), abs(y)) > 30f) revealed = true
                                    scope.launch { offsetX.animateTo(0f, spring()) }
                                    scope.launch { offsetY.animateTo(0f, spring()) }
                                } else if (!busy) {
                                    // Second swipe: this one grades.
                                    val grade = when {
                                        abs(x) >= abs(y) && x > 170f -> Grade.KNOWN
                                        abs(x) >= abs(y) && x < -170f -> Grade.UNKNOWN
                                        y > 170f -> Grade.UNSURE
                                        else -> null
                                    }
                                    if (grade == null) {
                                        scope.launch { offsetX.animateTo(0f, spring()) }
                                        scope.launch { offsetY.animateTo(0f, spring()) }
                                    } else {
                                    busy = true
                                    exitGrade = grade
                                    flash = overlayFor(grade).first
                                    scope.launch {
                                        // Fling the card off sideways: every grade
                                        // exits through the left or right edge,
                                        // accelerating, tilting as it goes.
                                        val targetX = when (grade) {
                                            Grade.KNOWN -> 1600f
                                            Grade.UNKNOWN -> -1600f
                                            Grade.UNSURE ->
                                                if (offsetX.value >= 0f) 1600f else -1600f
                                        }
                                        launch {
                                            offsetX.animateTo(
                                                targetX,
                                                tween(280, easing = FastOutLinearInEasing)
                                            )
                                        }
                                        launch {
                                            offsetY.animateTo(
                                                offsetY.value,
                                                tween(280, easing = FastOutLinearInEasing)
                                            )
                                        }
                                        delay(290)
                                        applyGrade(q, grade)
                                        offsetX.snapTo(0f)
                                        offsetY.snapTo(0f)
                                        exitGrade = null
                                        busy = false
                                        delay(120)
                                        flash = null
                                    }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f, spring()) }
                                scope.launch { offsetY.animateTo(0f, spring()) }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                }
                            }
                        )
                    }
            ) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(18.dp)) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            StatusPill(store = store, questionId = q.id)
                        }
                        Spacer(Modifier.weight(2f))
                        val questionText = if (yearMode) "『${q.a}』に関わる出来事が起きた年は？" else q.q
                        val answerText = if (yearMode) formatYear(q.year) else q.a
                        Text(
                            questionText,
                            fontSize = 19.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = questionColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(28.dp))
                        AnimatedVisibility(
                            visible = revealed,
                            enter = fadeIn(tween(150)),
                            exit = fadeOut(tween(100))
                        ) {
                            val dividerColor = InkOutline
                            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                                drawLine(
                                    color = dividerColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().height(36.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (revealed) {
                                    Text("こたえ", fontSize = 11.sp, color = InkTextSub)
                                }
                            }
                            SmallPillButton(
                                text = "かくす",
                                enabled = revealed,
                                onClick = { revealed = false }
                            )
                            Spacer(Modifier.size(8.dp))
                            SmallPillButton(
                                text = "しおり",
                                enabled = true,
                                onClick = { showBookmark = true }
                            )
                            Spacer(Modifier.size(8.dp))
                            SmallPillButton(
                                text = "出さない",
                                enabled = !busy,
                                onClick = {
                                    // Exclude this card from all future queues
                                    // and move on without grading it.
                                    store.setExcluded(q.id, true)
                                    revealed = false
                                    scope.launch {
                                        offsetX.snapTo(0f)
                                        offsetY.snapTo(0f)
                                    }
                                    index++
                                }
                            )
                        }
                        AnimatedVisibility(
                            visible = revealed,
                            enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                            exit = fadeOut(tween(100)) + shrinkVertically(tween(100))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    answerText,
                                    fontSize = 22.sp,
                                    lineHeight = 30.sp,
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

                    // Direction overlay ON the card (as in the original).
                    if (overlay != null && (dragMag > 30f || exitGrade != null)) {
                        Box(
                            Modifier.fillMaxSize().background(overlay.first.copy(alpha = overlayAlpha)),
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
                }
            }
        }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            if (revealed) "右へ＝合っていた ・ 左へ＝間違っていた ・ 下へ＝まあまあ"
            else "スワイプして離すと答えが出ます。もう一度スワイプすると判定",
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
private fun SmallPillButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .border(
                1.dp,
                if (enabled) InkOutline else InkOutline.copy(alpha = 0.4f),
                RoundedCornerShape(50)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) InkText else InkTextSub.copy(alpha = 0.4f)
        )
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
        val dotOff = InkOutline
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { i ->
                val c = when {
                    i >= 5 - blueDots && blueDots > 0 -> AccentBlue
                    i == 4 && redDot -> AccentRed
                    else -> dotOff
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
