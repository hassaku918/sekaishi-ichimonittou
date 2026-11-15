package com.hassaku.sekaishiqa.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromString
import java.io.File
import java.time.LocalDate

class AppStore(context: Context) {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val stateFile = File(appContext.filesDir, "store.json")

    val questions: List<Question>
    val essays: List<Essay>
    val questionsById: Map<String, Question>

    private val _data = MutableStateFlow(loadState())
    val data: StateFlow<StoreData> = _data

    init {
        val loaded = mutableListOf<Question>()
        for (name in listOf("questions_a.json", "questions_b.json", "questions_c.json")) {
            runCatching {
                val text = appContext.assets.open(name).bufferedReader().use { it.readText() }
                loaded += json.decodeFromString<List<Question>>(text)
            }
        }
        questions = loaded
        questionsById = loaded.associateBy { it.id }
        essays = runCatching {
            val text = appContext.assets.open("essays.json").bufferedReader().use { it.readText() }
            json.decodeFromString<List<Essay>>(text)
        }.getOrDefault(emptyList())
    }

    private fun loadState(): StoreData {
        return runCatching {
            if (stateFile.exists()) json.decodeFromString<StoreData>(stateFile.readText())
            else StoreData()
        }.getOrDefault(StoreData())
    }

    private fun update(transform: (StoreData) -> StoreData) {
        val next = transform(_data.value)
        _data.value = next
        runCatching { stateFile.writeText(json.encodeToString(StoreData.serializer(), next)) }
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    // ---------- SRS ----------

    fun grade(questionId: String, grade: Grade) {
        val today = today()
        update { data ->
            val prev = data.progress[questionId] ?: Progress()
            val next = when (grade) {
                Grade.KNOWN -> {
                    val streak = prev.streak + 1
                    val idx = minOf(prev.intervalIndex + 1, INTERVALS_DAYS.lastIndex)
                    Progress(
                        seen = true,
                        streak = streak,
                        intervalIndex = idx,
                        nextDueEpochDay = today + INTERVALS_DAYS[idx],
                        graduated = streak >= GRADUATE_STREAK,
                        lastGrade = grade.name,
                        correctCount = prev.correctCount + 1,
                        wrongCount = prev.wrongCount
                    )
                }
                Grade.UNSURE -> prev.copy(
                    seen = true,
                    nextDueEpochDay = today + 1,
                    lastGrade = grade.name
                )
                Grade.UNKNOWN -> prev.copy(
                    seen = true,
                    streak = 0,
                    intervalIndex = 0,
                    nextDueEpochDay = today,
                    graduated = false,
                    lastGrade = grade.name,
                    wrongCount = prev.wrongCount + 1
                )
            }
            val log = data.logs[today] ?: DayLog()
            data.copy(
                progress = data.progress + (questionId to next),
                logs = data.logs + (today to log.copy(
                    answered = log.answered + 1,
                    correct = log.correct + if (grade == Grade.KNOWN) 1 else 0
                ))
            )
        }
    }

    fun recordEssay(essayId: String, score: Grade) {
        val today = today()
        update { data ->
            val log = data.logs[today] ?: DayLog()
            data.copy(
                essayScores = data.essayScores + (essayId to score.name),
                logs = data.logs + (today to log.copy(essays = log.essays + 1))
            )
        }
    }

    fun isDue(q: Question, data: StoreData): Boolean {
        val p = data.progress[q.id] ?: return false
        return p.seen && !p.graduated && p.nextDueEpochDay <= today()
    }

    fun isWeak(q: Question, data: StoreData): Boolean {
        val p = data.progress[q.id] ?: return false
        return p.seen && !p.graduated && (p.lastGrade == Grade.UNKNOWN.name || (p.wrongCount > 0 && p.streak < 2))
    }

    fun dueQuestions(data: StoreData): List<Question> = questions.filter { isDue(it, data) }

    fun streakDays(data: StoreData): Int {
        var day = today()
        if ((data.logs[day]?.answered ?: 0) < DAILY_GOAL) day -= 1
        var count = 0
        while ((data.logs[day]?.answered ?: 0) >= DAILY_GOAL) {
            count++
            day -= 1
        }
        return count
    }

    // ---------- Wordbooks ----------

    fun createWordbook(name: String) {
        update { data ->
            val id = "wb" + System.currentTimeMillis().toString(36)
            data.copy(wordbooks = data.wordbooks + Wordbook(id = id, name = name.ifBlank { "my単語帳" }))
        }
    }

    fun renameWordbook(id: String, name: String) {
        update { data ->
            data.copy(wordbooks = data.wordbooks.map { if (it.id == id) it.copy(name = name) else it })
        }
    }

    fun deleteWordbook(id: String) {
        update { data -> data.copy(wordbooks = data.wordbooks.filterNot { it.id == id }) }
    }

    fun toggleInWordbook(bookId: String, questionId: String) {
        update { data ->
            data.copy(wordbooks = data.wordbooks.map { book ->
                if (book.id != bookId) book
                else if (questionId in book.questionIds) book.copy(questionIds = book.questionIds - questionId)
                else book.copy(questionIds = book.questionIds + questionId)
            })
        }
    }

    // ---------- Export / import / reset ----------

    fun exportJson(): String = json.encodeToString(StoreData.serializer(), _data.value)

    fun importJson(text: String): Boolean {
        return runCatching {
            val parsed = json.decodeFromString<StoreData>(text)
            update { parsed }
            true
        }.getOrDefault(false)
    }

    fun resetAll() {
        update { StoreData() }
    }

    companion object {
        val INTERVALS_DAYS = listOf(0L, 1L, 2L, 4L, 7L, 14L, 30L)
        const val GRADUATE_STREAK = 5
        const val DAILY_GOAL = 10
    }
}
