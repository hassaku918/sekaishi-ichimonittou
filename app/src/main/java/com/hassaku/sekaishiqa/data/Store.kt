package com.hassaku.sekaishiqa.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.util.concurrent.Executors

class AppStore(context: Context) {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val stateJson = Json { ignoreUnknownKeys = true }
    private val stateFile = File(appContext.filesDir, "store.json")

    val questions: List<Question>
    val questionsById: Map<String, Question>
    val chapters: List<String>
    val regions: List<String>
    val byChapter: Map<String, List<Question>>
    val byRegion: Map<String, List<Question>>
    val yearQuestions: List<Question>

    private val _data = MutableStateFlow(loadState())
    val data: StateFlow<StoreData> = _data

    // State persistence runs on a single background thread and coalesces
    // bursts (e.g. rapid grading) into one write of the latest snapshot,
    // so the main thread never blocks on file IO.
    private val writeExecutor = Executors.newSingleThreadExecutor()
    private val writeLock = Any()
    private var writeQueued = false

    private fun persist() {
        synchronized(writeLock) {
            if (writeQueued) return
            writeQueued = true
        }
        writeExecutor.execute {
            synchronized(writeLock) { writeQueued = false }
            val snapshot = _data.value
            runCatching {
                stateFile.writeText(stateJson.encodeToString(StoreData.serializer(), snapshot))
            }
        }
    }

    init {
        val loaded = mutableListOf<Question>()
        val names = runCatching {
            appContext.assets.list("")?.filter { it.startsWith("questions_") && it.endsWith(".json") }?.sorted()
        }.getOrNull() ?: listOf("questions_a.json", "questions_b.json", "questions_c.json")
        for (name in names) {
            runCatching {
                val text = appContext.assets.open(name).bufferedReader().use { it.readText() }
                loaded += json.decodeFromString(ListSerializer(Question.serializer()), text)
            }
        }
        questions = loaded
        questionsById = loaded.associateBy { it.id }
        chapters = loaded.map { it.chapter }.distinct()
        regions = loaded.map { it.region }.distinct()
        byChapter = loaded.groupBy { it.chapter }
        byRegion = loaded.groupBy { it.region }
        yearQuestions = loaded.filter { it.year != null }
    }

    private fun loadState(): StoreData {
        return runCatching {
            if (stateFile.exists()) stateJson.decodeFromString(StoreData.serializer(), stateFile.readText())
            else StoreData()
        }.getOrDefault(StoreData())
    }

    private fun update(transform: (StoreData) -> StoreData) {
        _data.value = transform(_data.value)
        persist()
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

    fun undoGrade(questionId: String, prev: Progress?, grade: Grade) {
        val today = today()
        update { data ->
            val log = data.logs[today] ?: DayLog()
            data.copy(
                progress = if (prev == null) data.progress - questionId
                else data.progress + (questionId to prev),
                logs = data.logs + (today to log.copy(
                    answered = maxOf(0, log.answered - 1),
                    correct = maxOf(0, log.correct - if (grade == Grade.KNOWN) 1 else 0)
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

    // Per-group [seen, graduated] counts in a single pass over progress.
    private fun statsBy(data: StoreData, key: (Question) -> String): Map<String, IntArray> {
        val out = mutableMapOf<String, IntArray>()
        for ((id, p) in data.progress) {
            if (!p.seen) continue
            val q = questionsById[id] ?: continue
            val arr = out.getOrPut(key(q)) { IntArray(2) }
            arr[0]++
            if (p.graduated) arr[1]++
        }
        return out
    }

    fun chapterStats(data: StoreData): Map<String, IntArray> = statsBy(data) { it.chapter }
    fun regionStats(data: StoreData): Map<String, IntArray> = statsBy(data) { it.region }

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
            val parsed = json.decodeFromString(StoreData.serializer(), text)
            update { parsed }
            true
        }.getOrDefault(false)
    }

    fun setDarkMode(value: Boolean) {
        update { it.copy(darkMode = value) }
    }

    fun setExcluded(questionId: String, excluded: Boolean) {
        update {
            it.copy(
                excludedIds = if (excluded) it.excludedIds + questionId
                else it.excludedIds - questionId
            )
        }
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
