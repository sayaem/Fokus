package com.example.data.model

enum class LearningStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

enum class PracticeStatus {
    NOT_STARTED,
    IN_PROGRESS,
    NEEDS_PRACTICE,
    PRACTICED,
    MASTERED
}

enum class RevisionStatus {
    NOT_SCHEDULED,
    SCHEDULED,
    DUE,
    OVERDUE,
    REVISED
}

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

enum class TaskStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
    RESCHEDULED
}

enum class SessionStatus {
    COMPLETED,
    CANCELLED,
    INTERRUPTED
}

enum class BlockerMode {
    NORMAL,
    STRICT,
    MAXIMUM
}

enum class PreparationGoalType {
    HSC,
    PUBLIC_UNIVERSITY,
    MEDICAL,
    ENGINEERING,
    OTHER,
    PERSONAL
}
