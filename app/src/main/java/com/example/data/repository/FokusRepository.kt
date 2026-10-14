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

data class ProposedTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val subjectId: String?,
    val subjectTitle: String,
    val chapterId: String?,
    val chapterTitle: String,
    val topicId: String?,
    val durationMinutes: Int,
    val priority: Priority,
    val reason: String,
    val isRevision: Boolean = false
)

data class ProposedPlanResult(
    val proposedTasks: List<ProposedTask>,
    val totalPlannedMinutes: Int,
    val availableMinutes: Int,
    val breakMinutesTotal: Int,
    val unscheduledBacklogCount: Int,
    val unscheduledBacklogPreview: List<String>
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
        val existingChapters = dao.getChaptersForSubjectSync(subjectId)
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

    suspend fun insertBulkChapters(
        subjectId: String,
        chapters: List<ChapterEntity>,
        conflictStrategy: String = "SKIP_DUPLICATES"
    ): Int {
        if (chapters.isEmpty()) return 0
        val existing = dao.getChaptersForSubjectSync(subjectId)
        val existingTitles = existing.associateBy { it.title.trim().lowercase() }

        val toInsert = mutableListOf<ChapterEntity>()
        val toUpdate = mutableListOf<ChapterEntity>()

        chapters.forEach { candidate ->
            val match = existingTitles[candidate.title.trim().lowercase()]
            if (match != null) {
                when (conflictStrategy) {
                    "OVERWRITE" -> {
                        toUpdate.add(
                            match.copy(
                                chapterNumber = candidate.chapterNumber,
                                estimatedDurationMinutes = candidate.estimatedDurationMinutes,
                                priority = candidate.priority,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    "APPEND" -> {
                        toInsert.add(candidate)
                    }
                    else -> {
                        // "SKIP_DUPLICATES" -> skip
                    }
                }
            } else {
                toInsert.add(candidate)
            }
        }

        if (toInsert.isNotEmpty()) {
            dao.insertChapters(toInsert)
        }
        toUpdate.forEach { dao.updateChapter(it) }
        return toInsert.size + toUpdate.size
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

    // --- Revisions Management ---
    suspend fun completeRevision(record: RevisionRecordEntity, confidenceRating: Int, notes: String) {
        completeRevisionWithOutcome(
            record = record,
            outcome = when {
                confidenceRating >= 4 -> "EASY"
                confidenceRating <= 2 -> "DIFFICULT"
                else -> "OKAY"
            },
            confidenceRating = confidenceRating,
            notes = notes,
            customIntervals = listOf(1, 3, 7, 14, 30)
        )
    }

    suspend fun completeRevisionWithOutcome(
        record: RevisionRecordEntity,
        outcome: String, // "EASY", "OKAY", "DIFFICULT"
        confidenceRating: Int,
        notes: String,
        customIntervals: List<Int>
    ): Int {
        val updated = record.copy(
            completedAt = System.currentTimeMillis(),
            confidenceRating = confidenceRating,
            outcomeRating = outcome,
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

        // Determine next interval based on outcome
        val nextIntervalDays = when (outcome.uppercase()) {
            "DIFFICULT" -> 1 // Review next day if struggled
            "EASY" -> {
                val nextIdx = (record.revisionCount).coerceAtMost(customIntervals.size - 1)
                customIntervals[nextIdx]
            }
            else -> { // "OKAY"
                val nextIdx = (record.revisionCount - 1).coerceIn(0, customIntervals.size - 1)
                customIntervals[nextIdx]
            }
        }

        val nextScheduledMillis = System.currentTimeMillis() + (nextIntervalDays * 86400000L)
        val nextRecord = RevisionRecordEntity(
            id = UUID.randomUUID().toString(),
            subjectId = record.subjectId,
            chapterId = record.chapterId,
            topicId = record.topicId,
            scheduledAt = nextScheduledMillis,
            revisionCount = record.revisionCount + 1,
            notes = "Round #${record.revisionCount + 1} ($outcome feedback)",
            status = RevisionStatus.SCHEDULED
        )
        dao.insertRevision(nextRecord)

        return nextIntervalDays
    }

    suspend fun rescheduleRevision(record: RevisionRecordEntity, newScheduledAtMillis: Long) {
        dao.updateRevision(
            record.copy(
                scheduledAt = newScheduledAtMillis,
                status = RevisionStatus.SCHEDULED
            )
        )
    }

    suspend fun skipRevision(record: RevisionRecordEntity) {
        dao.updateRevision(
            record.copy(
                status = RevisionStatus.REVISED,
                notes = (record.notes + " [Skipped]").trim(),
                completedAt = System.currentTimeMillis()
            )
        )
    }

    // --- Daily Study Journal ---
    val allJournalEntries: Flow<List<com.example.data.model.DailyJournalEntity>> = dao.getAllJournalEntries()

    fun getJournalEntryForDate(startOfDay: Long, endOfDay: Long): Flow<com.example.data.model.DailyJournalEntity?> =
        dao.getJournalEntryForDate(startOfDay, endOfDay)

    fun getJournalEntryById(id: String): Flow<com.example.data.model.DailyJournalEntity?> =
        dao.getJournalEntryById(id)

    suspend fun saveJournalEntry(entry: com.example.data.model.DailyJournalEntity) =
        dao.insertJournalEntry(entry)

    suspend fun updateJournalEntry(entry: com.example.data.model.DailyJournalEntity) =
        dao.updateJournalEntry(entry)

    suspend fun deleteJournalEntry(entry: com.example.data.model.DailyJournalEntity) =
        dao.deleteJournalEntry(entry)

    // --- Smart Daily Plan Generation (Feature One) ---
    suspend fun generateDailyPlan(
        availableMinutes: Int,
        sessionDurationMinutes: Int = 50,
        breakDurationMinutes: Int = 10,
        preferredSubjectIds: Set<String> = emptySet(),
        dateMillis: Long,
        chapters: List<ChapterEntity>,
        subjects: List<SubjectEntity>,
        revisions: List<RevisionRecordEntity>,
        existingTasks: List<StudyTaskEntity>
    ): ProposedPlanResult {
        val subjectMap = subjects.associateBy { it.id }
        val chapterMap = chapters.associateBy { it.id }

        // Time already committed to non-cancelled existing tasks
        val existingCommittedMinutes = existingTasks.filter { it.status != TaskStatus.SKIPPED }
            .sumOf { it.plannedDurationMinutes }
        val netAvailableMinutes = (availableMinutes - existingCommittedMinutes).coerceAtLeast(0)

        val candidateList = mutableListOf<ProposedTask>()

        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        // 1. Overdue revisions
        val overdueRevisions = revisions.filter {
            it.scheduledAt < startOfDay && it.status != RevisionStatus.REVISED
        }
        overdueRevisions.forEach { rev ->
            val ch = chapterMap[rev.chapterId]
            val sub = subjectMap[rev.subjectId]
            candidateList.add(
                ProposedTask(
                    title = "Revise: ${ch?.title ?: "Overdue Topic"}",
                    subjectId = rev.subjectId,
                    subjectTitle = sub?.title ?: "General",
                    chapterId = rev.chapterId,
                    chapterTitle = ch?.title ?: "",
                    topicId = rev.topicId,
                    durationMinutes = 35.coerceAtMost(sessionDurationMinutes),
                    priority = Priority.URGENT,
                    reason = "Overdue revision (scheduled on ${formatDateShort(rev.scheduledAt)})",
                    isRevision = true
                )
            )
        }

        // 2. Revisions due today
        val todayRevisions = revisions.filter {
            it.scheduledAt in startOfDay..endOfDay && it.status != RevisionStatus.REVISED
        }
        todayRevisions.forEach { rev ->
            val ch = chapterMap[rev.chapterId]
            val sub = subjectMap[rev.subjectId]
            candidateList.add(
                ProposedTask(
                    title = "Review: ${ch?.title ?: "Due Chapter"}",
                    subjectId = rev.subjectId,
                    subjectTitle = sub?.title ?: "General",
                    chapterId = rev.chapterId,
                    chapterTitle = ch?.title ?: "",
                    topicId = rev.topicId,
                    durationMinutes = 35.coerceAtMost(sessionDurationMinutes),
                    priority = Priority.HIGH,
                    reason = "Spaced repetition scheduled for today",
                    isRevision = true
                )
            )
        }

        // 3. Incomplete / in-progress chapters
        val inProgressChapters = chapters.filter { it.learningStatus == LearningStatus.IN_PROGRESS }
        inProgressChapters.forEach { ch ->
            val sub = subjectMap[ch.subjectId]
            candidateList.add(
                ProposedTask(
                    title = "Continue: ${ch.title}",
                    subjectId = ch.subjectId,
                    subjectTitle = sub?.title ?: "General",
                    chapterId = ch.id,
                    chapterTitle = ch.title,
                    topicId = null,
                    durationMinutes = sessionDurationMinutes,
                    priority = ch.priority,
                    reason = "In-progress chapter nearing completion"
                )
            )
        }

        // 4. Unstarted high & urgent priority chapters
        val unstartedChapters = chapters.filter { it.learningStatus == LearningStatus.NOT_STARTED }
            .sortedWith(compareByDescending<ChapterEntity> { it.priority == Priority.URGENT || it.priority == Priority.HIGH }
                .thenBy { if (preferredSubjectIds.contains(it.subjectId)) 0 else 1 }
                .thenBy { it.customOrder })

        unstartedChapters.forEach { ch ->
            val sub = subjectMap[ch.subjectId]
            candidateList.add(
                ProposedTask(
                    title = "Master: ${ch.title}",
                    subjectId = ch.subjectId,
                    subjectTitle = sub?.title ?: "General",
                    chapterId = ch.id,
                    chapterTitle = ch.title,
                    topicId = null,
                    durationMinutes = sessionDurationMinutes,
                    priority = ch.priority,
                    reason = if (ch.priority == Priority.URGENT || ch.priority == Priority.HIGH)
                        "High priority exam syllabus chapter"
                    else
                        "Next chapter in syllabus sequence"
                )
            )
        }

        // Distinct by chapter to avoid repetitive tasks
        val seenChapters = mutableSetOf<String>()
        val dedupedCandidates = candidateList.filter {
            if (it.chapterId != null) {
                val key = "${it.chapterId}_${it.isRevision}"
                if (seenChapters.contains(key)) false else {
                    seenChapters.add(key)
                    true
                }
            } else true
        }

        // Fit into available minutes
        val acceptedTasks = mutableListOf<ProposedTask>()
        val unscheduledBacklog = mutableListOf<String>()
        var allocatedMinutes = 0
        var totalBreakMinutes = 0

        for (candidate in dedupedCandidates) {
            val taskMinutes = candidate.durationMinutes
            val potentialTotal = allocatedMinutes + taskMinutes + (if (acceptedTasks.isNotEmpty()) breakDurationMinutes else 0)
            if (potentialTotal <= netAvailableMinutes) {
                if (acceptedTasks.isNotEmpty()) {
                    totalBreakMinutes += breakDurationMinutes
                }
                allocatedMinutes += taskMinutes
                acceptedTasks.add(candidate)
            } else {
                unscheduledBacklog.add("${candidate.title} (${candidate.durationMinutes}m)")
            }
        }

        return ProposedPlanResult(
            proposedTasks = acceptedTasks,
            totalPlannedMinutes = allocatedMinutes,
            availableMinutes = availableMinutes,
            breakMinutesTotal = totalBreakMinutes,
            unscheduledBacklogCount = unscheduledBacklog.size,
            unscheduledBacklogPreview = unscheduledBacklog.take(5)
        )
    }

    suspend fun acceptProposedPlan(tasks: List<ProposedTask>, dateMillis: Long) {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis

        // Clear only auto-generated pending tasks for this day to allow safe regeneration
        dao.deleteAutoGeneratedPendingTasks(startOfDay, endOfDay)

        val taskEntities = tasks.mapIndexed { index, task ->
            StudyTaskEntity(
                id = UUID.randomUUID().toString(),
                title = task.title,
                description = task.reason,
                subjectId = task.subjectId,
                chapterId = task.chapterId,
                topicId = task.topicId,
                plannedDate = dateMillis,
                plannedDurationMinutes = task.durationMinutes,
                priority = task.priority,
                status = TaskStatus.PLANNED,
                recommendedReason = task.reason,
                isAutoGenerated = true,
                orderIndex = index
            )
        }
        dao.insertTasks(taskEntities)
    }

    private fun formatDateShort(millis: Long): String {
        return java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(millis))
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
