package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.BlockerMode
import com.example.data.model.LearningStatus
import com.example.data.model.PracticeStatus
import com.example.data.model.PreparationGoalType
import com.example.data.model.Priority
import com.example.data.model.RevisionStatus
import com.example.data.model.SessionStatus
import com.example.data.model.TaskStatus

class Converters {
    @TypeConverter
    fun fromLearningStatus(value: LearningStatus?): String = value?.name ?: LearningStatus.NOT_STARTED.name

    @TypeConverter
    fun toLearningStatus(value: String?): LearningStatus =
        value?.let { runCatching { LearningStatus.valueOf(it) }.getOrDefault(LearningStatus.NOT_STARTED) }
            ?: LearningStatus.NOT_STARTED

    @TypeConverter
    fun fromPracticeStatus(value: PracticeStatus?): String = value?.name ?: PracticeStatus.NOT_STARTED.name

    @TypeConverter
    fun toPracticeStatus(value: String?): PracticeStatus =
        value?.let { runCatching { PracticeStatus.valueOf(it) }.getOrDefault(PracticeStatus.NOT_STARTED) }
            ?: PracticeStatus.NOT_STARTED

    @TypeConverter
    fun fromRevisionStatus(value: RevisionStatus?): String = value?.name ?: RevisionStatus.NOT_SCHEDULED.name

    @TypeConverter
    fun toRevisionStatus(value: String?): RevisionStatus =
        value?.let { runCatching { RevisionStatus.valueOf(it) }.getOrDefault(RevisionStatus.NOT_SCHEDULED) }
            ?: RevisionStatus.NOT_SCHEDULED

    @TypeConverter
    fun fromPriority(value: Priority?): String = value?.name ?: Priority.MEDIUM.name

    @TypeConverter
    fun toPriority(value: String?): Priority =
        value?.let { runCatching { Priority.valueOf(it) }.getOrDefault(Priority.MEDIUM) } ?: Priority.MEDIUM

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus?): String = value?.name ?: TaskStatus.PLANNED.name

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus =
        value?.let { runCatching { TaskStatus.valueOf(it) }.getOrDefault(TaskStatus.PLANNED) } ?: TaskStatus.PLANNED

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus?): String = value?.name ?: SessionStatus.COMPLETED.name

    @TypeConverter
    fun toSessionStatus(value: String?): SessionStatus =
        value?.let { runCatching { SessionStatus.valueOf(it) }.getOrDefault(SessionStatus.COMPLETED) }
            ?: SessionStatus.COMPLETED

    @TypeConverter
    fun fromBlockerMode(value: BlockerMode?): String = value?.name ?: BlockerMode.NORMAL.name

    @TypeConverter
    fun toBlockerMode(value: String?): BlockerMode =
        value?.let { runCatching { BlockerMode.valueOf(it) }.getOrDefault(BlockerMode.NORMAL) } ?: BlockerMode.NORMAL

    @TypeConverter
    fun fromGoalType(value: PreparationGoalType?): String = value?.name ?: PreparationGoalType.HSC.name

    @TypeConverter
    fun toGoalType(value: String?): PreparationGoalType =
        value?.let { runCatching { PreparationGoalType.valueOf(it) }.getOrDefault(PreparationGoalType.HSC) }
            ?: PreparationGoalType.HSC
}
