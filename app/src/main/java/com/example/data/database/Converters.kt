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

    @TypeConverter
    fun fromApplicationStatus(value: com.example.data.model.ApplicationStatus?): String =
        value?.name ?: com.example.data.model.ApplicationStatus.NOT_APPLIED.name

    @TypeConverter
    fun toApplicationStatus(value: String?): com.example.data.model.ApplicationStatus =
        value?.let { runCatching { com.example.data.model.ApplicationStatus.valueOf(it) }.getOrDefault(com.example.data.model.ApplicationStatus.NOT_APPLIED) }
            ?: com.example.data.model.ApplicationStatus.NOT_APPLIED

    @TypeConverter
    fun fromPaymentStatus(value: com.example.data.model.PaymentStatus?): String =
        value?.name ?: com.example.data.model.PaymentStatus.NOT_REQUIRED.name

    @TypeConverter
    fun toPaymentStatus(value: String?): com.example.data.model.PaymentStatus =
        value?.let { runCatching { com.example.data.model.PaymentStatus.valueOf(it) }.getOrDefault(com.example.data.model.PaymentStatus.NOT_REQUIRED) }
            ?: com.example.data.model.PaymentStatus.NOT_REQUIRED

    @TypeConverter
    fun fromAdmitCardStatus(value: com.example.data.model.AdmitCardStatus?): String =
        value?.name ?: com.example.data.model.AdmitCardStatus.NOT_AVAILABLE.name

    @TypeConverter
    fun toAdmitCardStatus(value: String?): com.example.data.model.AdmitCardStatus =
        value?.let { runCatching { com.example.data.model.AdmitCardStatus.valueOf(it) }.getOrDefault(com.example.data.model.AdmitCardStatus.NOT_AVAILABLE) }
            ?: com.example.data.model.AdmitCardStatus.NOT_AVAILABLE

    @TypeConverter
    fun fromAdmissionEventType(value: com.example.data.model.AdmissionEventType?): String =
        value?.name ?: com.example.data.model.AdmissionEventType.OTHER.name

    @TypeConverter
    fun toAdmissionEventType(value: String?): com.example.data.model.AdmissionEventType =
        value?.let { runCatching { com.example.data.model.AdmissionEventType.valueOf(it) }.getOrDefault(com.example.data.model.AdmissionEventType.OTHER) }
            ?: com.example.data.model.AdmissionEventType.OTHER
}
