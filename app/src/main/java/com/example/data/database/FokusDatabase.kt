package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FokusDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.BlockerConfigEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.InterruptionLogEntity
import com.example.data.model.MistakeRecordEntity
import com.example.data.model.PersonalNoteEntity
import com.example.data.model.PreparationGoalEntity
import com.example.data.model.PreparationGoalType
import com.example.data.model.RevisionRecordEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.SyllabusEntity
import com.example.data.model.TopicEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PreparationGoalEntity::class,
        SyllabusEntity::class,
        SubjectEntity::class,
        ChapterEntity::class,
        TopicEntity::class,
        StudyTaskEntity::class,
        StudySessionEntity::class,
        RevisionRecordEntity::class,
        PersonalNoteEntity::class,
        MistakeRecordEntity::class,
        BlockerConfigEntity::class,
        InterruptionLogEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FokusDatabase : RoomDatabase() {

    abstract fun fokusDao(): FokusDao

    companion object {
        @Volatile
        private var INSTANCE: FokusDatabase? = null

        fun getInstance(context: Context): FokusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FokusDatabase::class.java,
                    "fokus_study_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default goal & syllabus on first run
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).fokusDao()
                                val defaultGoal = PreparationGoalEntity(
                                    id = "default_goal_hsc",
                                    title = "HSC & University Admission",
                                    examType = PreparationGoalType.HSC,
                                    description = "Comprehensive syllabus & focus tracker",
                                    isDefault = true
                                )
                                dao.insertGoal(defaultGoal)

                                val defaultSyllabus = SyllabusEntity(
                                    id = "default_syllabus_main",
                                    goalId = defaultGoal.id,
                                    title = "Main Preparation Syllabus",
                                    description = "All core subjects and topics"
                                )
                                dao.insertSyllabus(defaultSyllabus)

                                // Ensure primary blocker config is initialized
                                dao.insertBlockerConfig(
                                    BlockerConfigEntity(
                                        id = "primary_blocker_config",
                                        isEnabled = false
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
