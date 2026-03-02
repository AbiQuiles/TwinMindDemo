package com.aquiles.twinminddemo.data.models

data class Summary(
    val sessionId: String,
    val title: String,
    val summary: String,
    val actionItems: List<String>,
    val keyPoints: List<String>,
    val status: SummaryStatus,
    val errorMessage: String? = null
)

enum class SummaryStatus {
    IDLE,
    GENERATING,
    DONE,
    FAILED
}
