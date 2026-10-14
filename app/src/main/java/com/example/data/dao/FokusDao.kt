package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AuditLogEntity
import com.example.data.model.BlockerConfigEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.InterruptionLogEntity
import com.example.data.model.MistakeRecordEntity
import com.example.data.model.PersonalNoteEntity
import com.example.data.model.PreparationGoalEntity
import com.example.data.model.RevisionRecordEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.SyllabusEntity
import com.example.data.model.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FokusDao {

    // --- Goals ---
    @Query("SELECT * FROM preparation_goals WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveGoals(): Flow<List<PreparationGoalEntity>>

    @Query("SELECT * FROM preparation_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<PreparationGoalEntity>>

    @Query("SELECT * FROM preparation_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: String): PreparationGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: PreparationGoalEntity)

    @Update
    suspend fun updateGoal(goal: PreparationGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: PreparationGoalEntity)

    // --- Syllabuses ---
    @Query("SELECT * FROM syllabuses WHERE goalId = :goalId AND isArchived = 0 ORDER BY customOrder ASC")
    fun getSyllabusesForGoal(goalId: String): Flow<List<SyllabusEntity>>

    @Query("SELECT * FROM syllabuses WHERE isArchived = 0 ORDER BY customOrder ASC")
    fun getAllActiveSyllabuses(): Flow<List<SyllabusEntity>>

    @Query("SELECT * FROM syllabuses WHERE id = :id LIMIT 1")
    suspend fun getSyllabusById(id: String): SyllabusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyllabus(syllabus: SyllabusEntity)

    @Update
    suspend fun updateSyllabus(syllabus: SyllabusEntity)

    @Delete
    suspend fun deleteSyllabus(syllabus: SyllabusEntity)

    // --- Subjects ---
    @Query("SELECT * FROM subjects WHERE isArchived = 0 ORDER BY customOrder ASC, createdAt ASC")
    fun getAllActiveSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE syllabusId = :syllabusId AND isArchived = 0 ORDER BY customOrder ASC")
    fun getSubjectsForSyllabus(syllabusId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    fun getSubjectByIdFlow(id: String): Flow<SubjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND isArchived = 0 ORDER BY customOrder ASC, createdAt ASC")
    fun getChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND isArchived = 0 ORDER BY customOrder ASC, createdAt ASC")
    suspend fun getChaptersForSubjectSync(subjectId: String): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE isArchived = 0 ORDER BY customOrder ASC")
    fun getAllActiveChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    fun getChapterByIdFlow(id: String): Flow<ChapterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE subjectId = :subjectId")
    suspend fun deleteChaptersForSubject(subjectId: String)

    // --- Topics ---
    @Query("SELECT * FROM topics WHERE chapterId = :chapterId AND isArchived = 0 ORDER BY customOrder ASC, createdAt ASC")
    fun getTopicsForChapter(chapterId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE isArchived = 0")
    fun getAllActiveTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: String): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)

    // --- Tasks ---
    @Query("SELECT * FROM study_tasks WHERE plannedDate >= :startOfDay AND plannedDate <= :endOfDay ORDER BY priority DESC, createdAt ASC")
    fun getTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks ORDER BY plannedDate ASC, priority DESC")
    fun getAllTasks(): Flow<List<StudyTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTaskEntity)

    @Update
    suspend fun updateTask(task: StudyTaskEntity)

    @Delete
    suspend fun deleteTask(task: StudyTaskEntity)

    // --- Study Sessions ---
    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC")
    fun getAllStudySessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE startedAt >= :sinceTimestamp ORDER BY startedAt DESC")
    fun getStudySessionsSince(sinceTimestamp: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE chapterId = :chapterId ORDER BY startedAt DESC")
    fun getSessionsForChapter(chapterId: String): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity)

    @Delete
    suspend fun deleteStudySession(session: StudySessionEntity)

    // --- Revisions ---
    @Query("SELECT * FROM revision_records WHERE status IN ('SCHEDULED', 'DUE', 'OVERDUE') ORDER BY scheduledAt ASC")
    fun getPendingRevisions(): Flow<List<RevisionRecordEntity>>

    @Query("SELECT * FROM revision_records ORDER BY scheduledAt DESC")
    fun getAllRevisions(): Flow<List<RevisionRecordEntity>>

    @Query("SELECT * FROM revision_records WHERE chapterId = :chapterId ORDER BY scheduledAt DESC")
    fun getRevisionsForChapter(chapterId: String): Flow<List<RevisionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: RevisionRecordEntity)

    @Update
    suspend fun updateRevision(revision: RevisionRecordEntity)

    @Delete
    suspend fun deleteRevision(revision: RevisionRecordEntity)

    // --- Notes ---
    @Query("SELECT * FROM personal_notes WHERE chapterId = :chapterId ORDER BY updatedAt DESC")
    fun getNotesForChapter(chapterId: String): Flow<List<PersonalNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: PersonalNoteEntity)

    @Delete
    suspend fun deleteNote(note: PersonalNoteEntity)

    // --- Mistakes ---
    @Query("SELECT * FROM mistake_records WHERE chapterId = :chapterId ORDER BY createdAt DESC")
    fun getMistakesForChapter(chapterId: String): Flow<List<MistakeRecordEntity>>

    @Query("SELECT * FROM mistake_records ORDER BY createdAt DESC")
    fun getAllMistakes(): Flow<List<MistakeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: MistakeRecordEntity)

    @Update
    suspend fun updateMistake(mistake: MistakeRecordEntity)

    @Delete
    suspend fun deleteMistake(mistake: MistakeRecordEntity)

    // --- Blocker Config & Interruption Logs ---
    @Query("SELECT * FROM blocker_config WHERE id = 'primary_blocker_config' LIMIT 1")
    fun getBlockerConfigFlow(): Flow<BlockerConfigEntity?>

    @Query("SELECT * FROM blocker_config WHERE id = 'primary_blocker_config' LIMIT 1")
    suspend fun getBlockerConfig(): BlockerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockerConfig(config: BlockerConfigEntity)

    @Query("SELECT * FROM interruption_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentInterruptionLogs(): Flow<List<InterruptionLogEntity>>

    @Query("SELECT COUNT(*) FROM interruption_logs")
    fun getTotalInterruptionsCount(): Flow<Int>

    @Insert
    suspend fun insertInterruptionLog(log: InterruptionLogEntity)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs WHERE targetId = :targetId ORDER BY timestamp DESC")
    fun getAuditLogsForTarget(targetId: String): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertAuditLog(log: AuditLogEntity)

    // --- Daily Journals ---
    @Query("SELECT * FROM daily_journals ORDER BY dateMillis DESC, createdAt DESC")
    fun getAllJournalEntries(): Flow<List<com.example.data.model.DailyJournalEntity>>

    @Query("SELECT * FROM daily_journals WHERE dateMillis >= :startOfDay AND dateMillis <= :endOfDay LIMIT 1")
    fun getJournalEntryForDate(startOfDay: Long, endOfDay: Long): Flow<com.example.data.model.DailyJournalEntity?>

    @Query("SELECT * FROM daily_journals WHERE id = :id LIMIT 1")
    fun getJournalEntryById(id: String): Flow<com.example.data.model.DailyJournalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: com.example.data.model.DailyJournalEntity)

    @Update
    suspend fun updateJournalEntry(entry: com.example.data.model.DailyJournalEntity)

    @Delete
    suspend fun deleteJournalEntry(entry: com.example.data.model.DailyJournalEntity)

    // --- Bulk Tasks ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyTaskEntity>)

    @Query("DELETE FROM study_tasks WHERE plannedDate >= :startOfDay AND plannedDate <= :endOfDay AND isAutoGenerated = 1 AND status = 'PLANNED'")
    suspend fun deleteAutoGeneratedPendingTasks(startOfDay: Long, endOfDay: Long)

    // --- Bulk Transactions ---
    @Transaction
    suspend fun clearAllData() {
        // Safe clear for import
    }

    // ==========================================
    // --- ADMISSION COMMAND CENTER DAO ---
    // ==========================================

    // --- Universities ---
    @Query("SELECT * FROM admission_universities WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllActiveUniversities(): Flow<List<com.example.data.model.UniversityEntity>>

    @Query("SELECT * FROM admission_universities WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedUniversities(): Flow<List<com.example.data.model.UniversityEntity>>

    @Query("SELECT * FROM admission_universities WHERE id = :id LIMIT 1")
    fun getUniversityByIdFlow(id: String): Flow<com.example.data.model.UniversityEntity?>

    @Query("SELECT * FROM admission_universities WHERE id = :id LIMIT 1")
    suspend fun getUniversityById(id: String): com.example.data.model.UniversityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUniversity(university: com.example.data.model.UniversityEntity)

    @Update
    suspend fun updateUniversity(university: com.example.data.model.UniversityEntity)

    @Delete
    suspend fun deleteUniversity(university: com.example.data.model.UniversityEntity)

    // --- Applications / Units ---
    @Query("SELECT * FROM admission_applications WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getAllActiveApplications(): Flow<List<com.example.data.model.AdmissionApplicationEntity>>

    @Query("SELECT * FROM admission_applications WHERE universityId = :universityId AND isArchived = 0 ORDER BY unitName ASC")
    fun getApplicationsForUniversity(universityId: String): Flow<List<com.example.data.model.AdmissionApplicationEntity>>

    @Query("SELECT * FROM admission_applications WHERE id = :id LIMIT 1")
    fun getApplicationByIdFlow(id: String): Flow<com.example.data.model.AdmissionApplicationEntity?>

    @Query("SELECT * FROM admission_applications WHERE id = :id LIMIT 1")
    suspend fun getApplicationById(id: String): com.example.data.model.AdmissionApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: com.example.data.model.AdmissionApplicationEntity)

    @Update
    suspend fun updateApplication(application: com.example.data.model.AdmissionApplicationEntity)

    @Delete
    suspend fun deleteApplication(application: com.example.data.model.AdmissionApplicationEntity)

    // --- Admission Events ---
    @Query("SELECT * FROM admission_events WHERE isArchived = 0 ORDER BY isDateConfirmed DESC, deadlineDateTimeMillis ASC, startDateTimeMillis ASC")
    fun getAllActiveAdmissionEvents(): Flow<List<com.example.data.model.AdmissionEventEntity>>

    @Query("SELECT * FROM admission_events WHERE universityId = :universityId AND isArchived = 0 ORDER BY isDateConfirmed DESC, deadlineDateTimeMillis ASC")
    fun getEventsForUniversity(universityId: String): Flow<List<com.example.data.model.AdmissionEventEntity>>

    @Query("SELECT * FROM admission_events WHERE applicationId = :applicationId AND isArchived = 0 ORDER BY deadlineDateTimeMillis ASC")
    fun getEventsForApplication(applicationId: String): Flow<List<com.example.data.model.AdmissionEventEntity>>

    @Query("SELECT * FROM admission_events WHERE id = :id LIMIT 1")
    fun getEventByIdFlow(id: String): Flow<com.example.data.model.AdmissionEventEntity?>

    @Query("SELECT * FROM admission_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): com.example.data.model.AdmissionEventEntity?

    @Query("SELECT * FROM admission_events WHERE isArchived = 0 AND isDateConfirmed = 1 AND ((deadlineDateTimeMillis IS NOT NULL AND deadlineDateTimeMillis >= :nowMillis) OR (startDateTimeMillis IS NOT NULL AND startDateTimeMillis >= :nowMillis)) ORDER BY COALESCE(deadlineDateTimeMillis, startDateTimeMillis) ASC")
    fun getUpcomingAdmissionEvents(nowMillis: Long): Flow<List<com.example.data.model.AdmissionEventEntity>>

    @Query("SELECT * FROM admission_events WHERE isArchived = 0 AND ((deadlineDateTimeMillis >= :startMillis AND deadlineDateTimeMillis <= :endMillis) OR (startDateTimeMillis >= :startMillis AND startDateTimeMillis <= :endMillis)) ORDER BY COALESCE(startDateTimeMillis, deadlineDateTimeMillis) ASC")
    fun getAdmissionEventsInRange(startMillis: Long, endMillis: Long): Flow<List<com.example.data.model.AdmissionEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmissionEvent(event: com.example.data.model.AdmissionEventEntity)

    @Update
    suspend fun updateAdmissionEvent(event: com.example.data.model.AdmissionEventEntity)

    @Delete
    suspend fun deleteAdmissionEvent(event: com.example.data.model.AdmissionEventEntity)

    // --- Reminders ---
    @Query("SELECT * FROM admission_reminders WHERE isEnabled = 1 AND scheduledTimeMillis >= :nowMillis ORDER BY scheduledTimeMillis ASC")
    fun getActiveFutureReminders(nowMillis: Long): Flow<List<com.example.data.model.AdmissionReminderEntity>>

    @Query("SELECT * FROM admission_reminders WHERE isEnabled = 1 AND scheduledTimeMillis >= :nowMillis ORDER BY scheduledTimeMillis ASC")
    suspend fun getActiveFutureRemindersSync(nowMillis: Long): List<com.example.data.model.AdmissionReminderEntity>

    @Query("SELECT * FROM admission_reminders WHERE eventId = :eventId ORDER BY scheduledTimeMillis ASC")
    fun getRemindersForEvent(eventId: String): Flow<List<com.example.data.model.AdmissionReminderEntity>>

    @Query("SELECT * FROM admission_reminders WHERE eventId = :eventId")
    suspend fun getRemindersForEventSync(eventId: String): List<com.example.data.model.AdmissionReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmissionReminder(reminder: com.example.data.model.AdmissionReminderEntity)

    @Update
    suspend fun updateAdmissionReminder(reminder: com.example.data.model.AdmissionReminderEntity)

    @Delete
    suspend fun deleteAdmissionReminder(reminder: com.example.data.model.AdmissionReminderEntity)

    @Query("DELETE FROM admission_reminders WHERE eventId = :eventId")
    suspend fun deleteRemindersForEvent(eventId: String)

    // --- Checklists ---
    @Query("SELECT * FROM admission_checklists WHERE universityId = :universityId ORDER BY sortOrder ASC, createdAt ASC")
    fun getChecklistsForUniversity(universityId: String): Flow<List<com.example.data.model.AdmissionChecklistEntity>>

    @Query("SELECT * FROM admission_checklists WHERE applicationId = :applicationId ORDER BY sortOrder ASC, createdAt ASC")
    fun getChecklistsForApplication(applicationId: String): Flow<List<com.example.data.model.AdmissionChecklistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmissionChecklist(checklist: com.example.data.model.AdmissionChecklistEntity)

    @Update
    suspend fun updateAdmissionChecklist(checklist: com.example.data.model.AdmissionChecklistEntity)

    @Delete
    suspend fun deleteAdmissionChecklist(checklist: com.example.data.model.AdmissionChecklistEntity)
}
