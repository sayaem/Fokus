package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ApplicationStatus {
    NOT_APPLIED,
    IN_PROGRESS,
    SUBMITTED,
    CONFIRMED,
    CANCELLED
}

enum class PaymentStatus {
    NOT_REQUIRED,
    PAYMENT_PENDING,
    PAID
}

enum class AdmitCardStatus {
    NOT_AVAILABLE,
    AVAILABLE,
    DOWNLOADED
}

enum class AdmissionEventType {
    APPLICATION_OPENS,
    APPLICATION_DEADLINE,
    PAYMENT_DEADLINE,
    ADMIT_CARD_OPENS,
    ADMIT_CARD_DEADLINE,
    ADMISSION_TEST,
    RESULT_PUBLISHED,
    SUBJECT_CHOICE_DEADLINE,
    OTHER
}

@Entity(
    tableName = "admission_universities",
    indices = [Index("name"), Index("isArchived")]
)
data class UniversityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val shortName: String = "",
    val websiteUrl: String = "",
    val admissionPortalUrl: String = "",
    val location: String = "",
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "admission_applications",
    foreignKeys = [
        ForeignKey(
            entity = UniversityEntity::class,
            parentColumns = ["id"],
            childColumns = ["universityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("universityId"), Index("applicationStatus"), Index("paymentStatus")]
)
data class AdmissionApplicationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val universityId: String,
    val unitName: String, // e.g. "A Unit (Science)", "Ka Unit", "IBA"
    val admissionCycle: String = "2024-2025",
    val applicationStatus: ApplicationStatus = ApplicationStatus.NOT_APPLIED,
    val paymentStatus: PaymentStatus = PaymentStatus.NOT_REQUIRED,
    val applicationFee: String = "",
    val applicationStartDate: Long? = null,
    val applicationDeadline: Long? = null,
    val submissionDate: Long? = null,
    val paymentCompletionDate: Long? = null,
    val applicationIdRef: String = "", // Optional private user application reference/roll
    val transactionRef: String = "", // Optional private payment slip reference
    val admitCardStatus: AdmitCardStatus = AdmitCardStatus.NOT_AVAILABLE,
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "admission_events",
    foreignKeys = [
        ForeignKey(
            entity = UniversityEntity::class,
            parentColumns = ["id"],
            childColumns = ["universityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("universityId"), Index("applicationId"), Index("eventType"), Index("deadlineDateTimeMillis")]
)
data class AdmissionEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val universityId: String,
    val applicationId: String? = null, // Linked unit application if applicable
    val eventType: AdmissionEventType = AdmissionEventType.ADMISSION_TEST,
    val title: String,
    val startDateTimeMillis: Long? = null,
    val deadlineDateTimeMillis: Long? = null,
    val isDateConfirmed: Boolean = true, // If false, "Date not confirmed", no countdown
    val isAllDay: Boolean = true,
    val location: String = "",
    val officialNoticeUrl: String = "",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "admission_reminders",
    foreignKeys = [
        ForeignKey(
            entity = AdmissionEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("universityId"), Index("scheduledTimeMillis")]
)
data class AdmissionReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val universityId: String,
    val applicationId: String? = null,
    val offsetMinutesBefore: Long = 1440L, // e.g. 1 day before by default
    val scheduledTimeMillis: Long,
    val isEnabled: Boolean = true,
    val title: String,
    val message: String,
    val isDelivered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "admission_checklists",
    foreignKeys = [
        ForeignKey(
            entity = UniversityEntity::class,
            parentColumns = ["id"],
            childColumns = ["universityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("universityId"), Index("applicationId"), Index("eventId")]
)
data class AdmissionChecklistEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val universityId: String,
    val applicationId: String? = null,
    val eventId: String? = null,
    val title: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
