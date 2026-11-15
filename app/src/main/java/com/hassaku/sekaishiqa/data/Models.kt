package com.hassaku.sekaishiqa.data

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: String,
    val chapter: String,
    val region: String,
    val level: Int,
    val q: String,
    val a: String,
    val year: Int? = null,
    val note: String = ""
)

@Serializable
data class Essay(
    val id: String,
    val theme: String,
    val prompt: String,
    val answer: String,
    val keywords: List<String> = emptyList()
)

enum class Grade { KNOWN, UNSURE, UNKNOWN }

@Serializable
data class Progress(
    val seen: Boolean = false,
    val streak: Int = 0,
    val intervalIndex: Int = 0,
    val nextDueEpochDay: Long = 0,
    val graduated: Boolean = false,
    val lastGrade: String? = null,
    val correctCount: Int = 0,
    val wrongCount: Int = 0
)

@Serializable
data class Wordbook(
    val id: String,
    val name: String,
    val questionIds: List<String> = emptyList()
)

@Serializable
data class DayLog(
    val answered: Int = 0,
    val correct: Int = 0,
    val essays: Int = 0
)

@Serializable
data class StoreData(
    val progress: Map<String, Progress> = emptyMap(),
    val wordbooks: List<Wordbook> = emptyList(),
    val logs: Map<Long, DayLog> = emptyMap(),
    val essayScores: Map<String, String> = emptyMap()
)
