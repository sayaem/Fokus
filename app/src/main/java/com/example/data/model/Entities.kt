package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "preparation_goals")
data class PreparationGoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val examType: PreparationGoalType = PreparationGoalType.HSC,
    val targetDate: Long? = null,
    val description: String = "",
    val isArchived: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "syllabuses",
    foreignKeys = [
        ForeignKey(
            entity = PreparationGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
data class SyllabusEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val title: String,
    val description: String = "",
    val customOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = SyllabusEntity::class,
            parentColumns = ["id"],
            childColumns = ["syllabusId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("syllabusId")]
)
data class SubjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val syllabusId: String,
    val title: String,
    val shortName: String = "",
    val description: String = "",
    val colorHex: String = "#A6FF00",
    val priority: Priority = Priority.MEDIUM,
    val customOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class ChapterEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val title: String,
    val chapterNumber: String = "",
    val description: String = "",
    val learningStatus: LearningStatus = LearningStatus.NOT_STARTED,
    val practiceStatus: PracticeStatus = PracticeStatus.NOT_STARTED,
    val revisionStatus: RevisionStatus = RevisionStatus.NOT_SCHEDULED,
    val priority: Priority = Priority.MEDIUM,
    val estimatedDurationMinutes: Int = 120,
    val actualDurationSeconds: Long = 0L,
    val targetDate: Long? = null,
    val lastStudiedAt: Long? = null,
    val nextRevisionAt: Long? = null,
    val customOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId"), Index("parentTopicId")]
)
data class TopicEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val chapterId: String,
    val parentTopicId: String? = null,
    val title: String,
    val description: String = "",
    val learningStatus: LearningStatus = LearningStatus.NOT_STARTED,
    val practiceStatus: PracticeStatus = PracticeStatus.NOT_STARTED,
    val revisionStatus: RevisionStatus = RevisionStatus.NOT_SCHEDULED,
    val estimatedDurationMinutes: Int = 45,
    val actualDurationSeconds: Long = 0L,
    val priority: Priority = Priority.MEDIUM,
    val customOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "study_tasks",
    indices = [Index("subjectId"), Index("chapterId"), Index("topicId"), Index("plannedDate")]
)
data class StudyTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val subjectId: String? = null,
    val chapterId: String? = null,
    val topicId: String? = null,
    val plannedDate: Long, // Epoch millis start of day
    val plannedStartTime: Long? = null,
    val plannedDurationMinutes: Int = 60,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.PLANNED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "study_sessions",
    indices = [Index("subjectId"), Index("chapterId"), Index("startedAt")]
)
data class StudySessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String? = null,
    val chapterId: String? = null,
    val topicId: String? = null,
    val taskId: String? = null,
    val plannedDurationMinutes: Int = 25,
    val actualDurationSeconds: Long = 0L,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long = System.currentTimeMillis(),
    val status: SessionStatus = SessionStatus.COMPLETED,
    val focusQualityRating: Int = 5, // 1 to 5 scale
    val notes: String = "",
    val interruptionCount: Int = 0,
    val blockerEnforced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "revision_records",
    indices = [Index("chapterId"), Index("scheduledAt"), Index("status")]
)
data class RevisionRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val chapterId: String,
    val topicId: String? = null,
    val scheduledAt: Long,
    val completedAt: Long? = null,
    val confidenceRating: Int = 3, // 1 (Poor) to 5 (Mastered)
    val notes: String = "",
    val revisionCount: Int = 1,
    val status: RevisionStatus = RevisionStatus.SCHEDULED,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "personal_notes",
    indices = [Index("chapterId"), Index("subjectId")]
)
data class PersonalNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String? = null,
    val chapterId: String? = null,
    val topicId: String? = null,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mistake_records",
    indices = [Index("chapterId"), Index("subjectId")]
)
data class MistakeRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String? = null,
    val chapterId: String? = null,
    val topicId: String? = null,
    val questionOrDescription: String,
    val explanation: String,
    val isResolved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocker_config")
data class BlockerConfigEntity(
    @PrimaryKey val id: String = "primary_blocker_config",
    val mode: BlockerMode = BlockerMode.NORMAL,
    val selectedPackagesJson: String = "[]", // List of package names as JSON array
    val allowlistedPackagesJson: String = "[]",
    val recurringSchedulesJson: String = "[]",
    val isEnabled: Boolean = false,
    val totalBlockedAttempts: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "interruption_logs",
    indices = [Index("timestamp"), Index("duringSessionId")]
)
data class InterruptionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val duringSessionId: String? = null
)

@Entity(
    tableName = "audit_logs",
    indices = [Index("timestamp"), Index("targetId")]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val targetType: String, // "CHAPTER", "TOPIC", "SUBJECT"
    val targetId: String,
    val action: String, // "STATUS_CHANGE", "REVISION", "COMPLETED"
    val previousValue: String = "",
    val newValue: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
