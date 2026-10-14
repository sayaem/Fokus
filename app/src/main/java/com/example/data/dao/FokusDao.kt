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

    // --- Chapters ---
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND isArchived = 0 ORDER BY customOrder ASC, createdAt ASC")
    fun getChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>>

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

    // --- Bulk Transactions ---
    @Transaction
    suspend fun clearAllData() {
        // Safe clear for import
    }
}
