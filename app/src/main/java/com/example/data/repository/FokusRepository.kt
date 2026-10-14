package com.example.data.repository

import com.example.data.dao.FokusDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.BlockerConfigEntity
import com.example.data.model.BlockerMode
import com.example.data.model.ChapterEntity
import com.example.data.model.InterruptionLogEntity
import com.example.data.model.LearningStatus
import com.example.data.model.MistakeRecordEntity
import com.example.data.model.PersonalNoteEntity
import com.example.data.model.PracticeStatus
import com.example.data.model.PreparationGoalEntity
import com.example.data.model.Priority
import com.example.data.model.RevisionRecordEntity
import com.example.data.model.RevisionStatus
import com.example.data.model.SessionStatus
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.SyllabusEntity
import com.example.data.model.TaskStatus
import com.example.data.model.TopicEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SyllabusProgressSummary(
    val totalChapters: Int,
    val completedChapters: Int,
    val inProgressChapters: Int,
    val notStartedChapters: Int,
    val skippedChapters: Int,
    val overallPercentage: Float, // 0f to 100f
    val hasContent: Boolean
)

data class SubjectWithProgress(
    val subject: SubjectEntity,
    val totalChapters: Int,
    val completedChapters: Int,
    val inProgressChapters: Int,
    val completionPercentage: Float
)

class FokusRepository(private val dao: FokusDao) {

    // --- Goals & Syllabuses ---
    val activeGoals: Flow<List<PreparationGoalEntity>> = dao.getActiveGoals()
    val allActiveSyllabuses: Flow<List<SyllabusEntity>> = dao.getAllActiveSyllabuses()

    suspend fun insertGoal(goal: PreparationGoalEntity) = dao.insertGoal(goal)
    suspend fun updateGoal(goal: PreparationGoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(goal: PreparationGoalEntity) = dao.deleteGoal(goal)

    suspend fun insertSyllabus(syllabus: SyllabusEntity) = dao.insertSyllabus(syllabus)
    suspend fun updateSyllabus(syllabus: SyllabusEntity) = dao.updateSyllabus(syllabus)
    suspend fun deleteSyllabus(syllabus: SyllabusEntity) = dao.deleteSyllabus(syllabus)

    // --- Subjects ---
    val allActiveSubjects: Flow<List<SubjectEntity>> = dao.getAllActiveSubjects()
    val archivedSubjects: Flow<List<SubjectEntity>> = dao.getArchivedSubjects()

    fun getSubjectByIdFlow(id: String): Flow<SubjectEntity?> = dao.getSubjectByIdFlow(id)
    suspend fun getSubjectById(id: String): SubjectEntity? = dao.getSubjectById(id)
    suspend fun insertSubject(subject: SubjectEntity) = dao.insertSubject(subject)
    suspend fun updateSubject(subject: SubjectEntity) = dao.updateSubject(subject)
    suspend fun deleteSubject(subject: SubjectEntity) = dao.deleteSubject(subject)

    suspend fun archiveSubject(subject: SubjectEntity) {
        dao.updateSubject(subject.copy(isArchived = true, updatedAt = System.currentTimeMillis()))
    }

    suspend fun restoreSubject(subject: SubjectEntity) {
        dao.updateSubject(subject.copy(isArchived = false, updatedAt = System.currentTimeMillis()))
    }

    // --- Chapters ---
    val allActiveChapters: Flow<List<ChapterEntity>> = dao.getAllActiveChapters()
    fun getChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>> = dao.getChaptersForSubject(subjectId)
    fun getChapterByIdFlow(id: String): Flow<ChapterEntity?> = dao.getChapterByIdFlow(id)
    suspend fun getChapterById(id: String): ChapterEntity? = dao.getChapterById(id)
    suspend fun insertChapter(chapter: ChapterEntity) = dao.insertChapter(chapter)
    suspend fun updateChapter(chapter: ChapterEntity) = dao.updateChapter(chapter)
    suspend fun deleteChapter(chapter: ChapterEntity) = dao.deleteChapter(chapter)

    // --- Topics ---
    fun getTopicsForChapter(chapterId: String): Flow<List<TopicEntity>> = dao.getTopicsForChapter(chapterId)
    suspend fun insertTopic(topic: TopicEntity) = dao.insertTopic(topic)
    suspend fun updateTopic(topic: TopicEntity) = dao.updateTopic(topic)
    suspend fun deleteTopic(topic: TopicEntity) = dao.deleteTopic(topic)

    // --- Progress Calculation Engine (Section 8.4) ---
    val syllabusProgressSummary: Flow<SyllabusProgressSummary> = combine(
        dao.getAllActiveChapters(),
        dao.getAllActiveTopics()
    ) { chapters, topics ->
        if (chapters.isEmpty()) {
            return@combine SyllabusProgressSummary(
                totalChapters = 0,
                completedChapters = 0,
                inProgressChapters = 0,
                notStartedChapters = 0,
                skippedChapters = 0,
                overallPercentage = 0f,
                hasContent = false
            )
        }

        val topicsByChapter = topics.groupBy { it.chapterId }
        var totalWeightedPercentage = 0f
        var eligibleChaptersCount = 0
        var completedCount = 0
        var inProgressCount = 0
        var notStartedCount = 0
        var skippedCount = 0

        chapters.forEach { chapter ->
            val chTopics = topicsByChapter[chapter.id].orEmpty()
            if (chTopics.isNotEmpty()) {
                // Topic-driven progress
                val activeTopics = chTopics.filter { it.learningStatus != LearningStatus.SKIPPED }
                if (activeTopics.isEmpty()) {
                    skippedCount++
                } else {
                    eligibleChaptersCount++
                    val topicSum = activeTopics.sumOf { topic ->
                        when (topic.learningStatus) {
                            LearningStatus.COMPLETED -> 100
                            LearningStatus.IN_PROGRESS -> 50
                            LearningStatus.NOT_STARTED -> 0
                            LearningStatus.SKIPPED -> 0
                        }
                    }
                    val chPercent = topicSum.toFloat() / activeTopics.size
                    totalWeightedPercentage += chPercent

                    if (chPercent >= 99.9f) {
                        completedCount++
                    } else if (chPercent > 0f) {
                        inProgressCount++
                    } else {
                        notStartedCount++
                    }
                }
            } else {
                // Chapter manual learning status
                when (chapter.learningStatus) {
                    LearningStatus.COMPLETED -> {
                        eligibleChaptersCount++
                        totalWeightedPercentage += 100f
                        completedCount++
                    }
                    LearningStatus.IN_PROGRESS -> {
                        eligibleChaptersCount++
                        totalWeightedPercentage += 50f
                        inProgressCount++
                    }
                    LearningStatus.NOT_STARTED -> {
                        eligibleChaptersCount++
                        notStartedCount++
                    }
                    LearningStatus.SKIPPED -> {
                        skippedCount++
                    }
                }
            }
        }

        val overall = if (eligibleChaptersCount > 0) {
            totalWeightedPercentage / eligibleChaptersCount
        } else {
            0f
        }

        SyllabusProgressSummary(
            totalChapters = chapters.size,
            completedChapters = completedCount,
            inProgressChapters = inProgressCount,
            notStartedChapters = notStartedCount,
            skippedChapters = skippedCount,
            overallPercentage = overall,
            hasContent = chapters.isNotEmpty()
        )
    }

    val subjectsWithProgress: Flow<List<SubjectWithProgress>> = combine(
        dao.getAllActiveSubjects(),
        dao.getAllActiveChapters(),
        dao.getAllActiveTopics()
    ) { subjects, chapters, topics ->
        val chaptersBySubject = chapters.groupBy { it.subjectId }
        val topicsByChapter = topics.groupBy { it.chapterId }

        subjects.map { subject ->
            val subChapters = chaptersBySubject[subject.id].orEmpty()
            if (subChapters.isEmpty()) {
                SubjectWithProgress(
                    subject = subject,
                    totalChapters = 0,
                    completedChapters = 0,
                    inProgressChapters = 0,
                    completionPercentage = 0f
                )
            } else {
                var completed = 0
                var inProgress = 0
                var totalWeighted = 0f
                var eligibleCount = 0

                subChapters.forEach { chapter ->
                    val chTopics = topicsByChapter[chapter.id].orEmpty()
                    if (chTopics.isNotEmpty()) {
                        val activeTopics = chTopics.filter { it.learningStatus != LearningStatus.SKIPPED }
                        if (activeTopics.isNotEmpty()) {
                            eligibleCount++
                            val topicSum = activeTopics.sumOf { topic ->
                                when (topic.learningStatus) {
                                    LearningStatus.COMPLETED -> 100
                                    LearningStatus.IN_PROGRESS -> 50
                                    LearningStatus.NOT_STARTED -> 0
                                    LearningStatus.SKIPPED -> 0
                                }
                            }
                            val chPercent = topicSum.toFloat() / activeTopics.size
                            totalWeighted += chPercent
                            if (chPercent >= 99.9f) completed++ else if (chPercent > 0f) inProgress++
                        }
                    } else {
                        if (chapter.learningStatus != LearningStatus.SKIPPED) {
                            eligibleCount++
                            when (chapter.learningStatus) {
                                LearningStatus.COMPLETED -> {
                                    totalWeighted += 100f
                                    completed++
                                }
                                LearningStatus.IN_PROGRESS -> {
                                    totalWeighted += 50f
                                    inProgress++
                                }
                                else -> {}
                            }
                        }
                    }
                }

                val percent = if (eligibleCount > 0) totalWeighted / eligibleCount else 0f
                SubjectWithProgress(
                    subject = subject,
                    totalChapters = subChapters.size,
                    completedChapters = completed,
                    inProgressChapters = inProgress,
                    completionPercentage = percent
                )
            }
        }
    }

    // --- Bulk Chapter Creation ---
    suspend fun bulkAddChapters(
        subjectId: String,
        rawText: String,
        defaultEstimatedMinutes: Int = 120,
        priority: Priority = Priority.MEDIUM
    ): Int {
        val lines = rawText.lines()
            .map { cleanChapterTitle(it) }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return 0

        // Get existing chapters count for custom order offset
        val existingChapters = dao.getChaptersForSubject(subjectId)
        val initialOrder = System.currentTimeMillis().toInt() % 1000

        val newChapters = lines.mapIndexed { index, title ->
            ChapterEntity(
                id = UUID.randomUUID().toString(),
                subjectId = subjectId,
                title = title,
                chapterNumber = "${index + 1}",
                learningStatus = LearningStatus.NOT_STARTED,
                practiceStatus = PracticeStatus.NOT_STARTED,
                revisionStatus = RevisionStatus.NOT_SCHEDULED,
                priority = priority,
                estimatedDurationMinutes = defaultEstimatedMinutes,
                customOrder = initialOrder + index
            )
        }

        dao.insertChapters(newChapters)
        return newChapters.size
    }

    private fun cleanChapterTitle(raw: String): String {
        return raw.trim()
            .replaceFirst(Regex("^(Chapter|অধ্যায়|Ch\\.?|No\\.?)\\s*[0-9০-৯]+[:.\\-]?\\s*", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^[0-9০-৯]+[:.)\\-]\\s*"), "")
            .trim()
    }

    // --- Tasks & Planner ---
    fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<StudyTaskEntity>> =
        dao.getTasksForDate(startOfDay, endOfDay)

    val allTasks: Flow<List<StudyTaskEntity>> = dao.getAllTasks()

    suspend fun insertTask(task: StudyTaskEntity) = dao.insertTask(task)
    suspend fun updateTask(task: StudyTaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(task: StudyTaskEntity) = dao.deleteTask(task)

    suspend fun updateTaskStatus(task: StudyTaskEntity, newStatus: TaskStatus) {
        dao.updateTask(task.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
    }

    // --- Study Sessions ---
    val allSessions: Flow<List<StudySessionEntity>> = dao.getAllStudySessions()
    fun getSessionsSince(timestamp: Long): Flow<List<StudySessionEntity>> = dao.getStudySessionsSince(timestamp)
    fun getSessionsForChapter(chapterId: String): Flow<List<StudySessionEntity>> = dao.getSessionsForChapter(chapterId)

    suspend fun recordStudySession(session: StudySessionEntity) {
        dao.insertStudySession(session)
        // If session has associated chapter, update chapter's actual duration and last studied
        session.chapterId?.let { chId ->
            val chapter = dao.getChapterById(chId)
            if (chapter != null) {
                dao.updateChapter(
                    chapter.copy(
                        actualDurationSeconds = chapter.actualDurationSeconds + session.actualDurationSeconds,
                        lastStudiedAt = session.endedAt,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // --- Revisions ---
    val pendingRevisions: Flow<List<RevisionRecordEntity>> = dao.getPendingRevisions()
    val allRevisions: Flow<List<RevisionRecordEntity>> = dao.getAllRevisions()
    fun getRevisionsForChapter(chapterId: String): Flow<List<RevisionRecordEntity>> = dao.getRevisionsForChapter(chapterId)

    suspend fun scheduleRevision(
        subjectId: String,
        chapterId: String,
        topicId: String? = null,
        scheduledDateMillis: Long,
        notes: String = ""
    ) {
        val record = RevisionRecordEntity(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            chapterId = chapterId,
            topicId = topicId,
            scheduledAt = scheduledDateMillis,
            notes = notes,
            status = RevisionStatus.SCHEDULED
        )
        dao.insertRevision(record)

        // Update chapter revision status
        val chapter = dao.getChapterById(chapterId)
        if (chapter != null) {
            dao.updateChapter(
                chapter.copy(
                    revisionStatus = RevisionStatus.SCHEDULED,
                    nextRevisionAt = scheduledDateMillis,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun completeRevision(record: RevisionRecordEntity, confidenceRating: Int, notes: String) {
        val updated = record.copy(
            completedAt = System.currentTimeMillis(),
            confidenceRating = confidenceRating,
            notes = notes,
            status = RevisionStatus.REVISED
        )
        dao.updateRevision(updated)

        val chapter = dao.getChapterById(record.chapterId)
        if (chapter != null) {
            dao.updateChapter(
                chapter.copy(
                    revisionStatus = RevisionStatus.REVISED,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // --- Notes & Mistakes ---
    fun getNotesForChapter(chapterId: String): Flow<List<PersonalNoteEntity>> = dao.getNotesForChapter(chapterId)
    suspend fun insertNote(note: PersonalNoteEntity) = dao.insertNote(note)
    suspend fun deleteNote(note: PersonalNoteEntity) = dao.deleteNote(note)

    fun getMistakesForChapter(chapterId: String): Flow<List<MistakeRecordEntity>> = dao.getMistakesForChapter(chapterId)
    val allMistakes: Flow<List<MistakeRecordEntity>> = dao.getAllMistakes()
    suspend fun insertMistake(mistake: MistakeRecordEntity) = dao.insertMistake(mistake)
    suspend fun updateMistake(mistake: MistakeRecordEntity) = dao.updateMistake(mistake)
    suspend fun deleteMistake(mistake: MistakeRecordEntity) = dao.deleteMistake(mistake)

    // --- Blocker Config & Interruption Logging ---
    val blockerConfigFlow: Flow<BlockerConfigEntity?> = dao.getBlockerConfigFlow()
    suspend fun getBlockerConfig(): BlockerConfigEntity? = dao.getBlockerConfig()
    suspend fun saveBlockerConfig(config: BlockerConfigEntity) = dao.insertBlockerConfig(config)
    val totalInterruptionsCount: Flow<Int> = dao.getTotalInterruptionsCount()
    val recentInterruptions: Flow<List<InterruptionLogEntity>> = dao.getRecentInterruptionLogs()

    suspend fun recordInterruption(packageName: String, appName: String, sessionId: String? = null) {
        dao.insertInterruptionLog(
            InterruptionLogEntity(
                packageName = packageName,
                appName = appName,
                duringSessionId = sessionId
            )
        )
        val config = dao.getBlockerConfig() ?: BlockerConfigEntity()
        dao.insertBlockerConfig(
            config.copy(
                totalBlockedAttempts = config.totalBlockedAttempts + 1,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // --- Backup & Export Tools ---
    suspend fun exportJsonBackup(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "FOKUS")
        root.put("exportedAt", System.currentTimeMillis())

        // Collect entities
        // Let's create JSON arrays
        val subjectsArray = JSONArray()
        // Subjects
        return root.toString(2)
    }

    suspend fun exportSyllabusCsv(subjectList: List<SubjectEntity>, chapterList: List<ChapterEntity>): String {
        val sb = StringBuilder()
        sb.append("Subject,Chapter Number,Chapter Title,Learning Status,Practice Status,Revision Status,Est Minutes,Actual Seconds\n")
        val subjectMap = subjectList.associateBy { it.id }
        chapterList.forEach { ch ->
            val subName = subjectMap[ch.subjectId]?.title ?: "Unknown"
            sb.append("\"${escapeCsv(subName)}\",")
            sb.append("\"${escapeCsv(ch.chapterNumber)}\",")
            sb.append("\"${escapeCsv(ch.title)}\",")
            sb.append("\"${ch.learningStatus.name}\",")
            sb.append("\"${ch.practiceStatus.name}\",")
            sb.append("\"${ch.revisionStatus.name}\",")
            sb.append("${ch.estimatedDurationMinutes},")
            sb.append("${ch.actualDurationSeconds}\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(str: String): String = str.replace("\"", "\"\"")
}
