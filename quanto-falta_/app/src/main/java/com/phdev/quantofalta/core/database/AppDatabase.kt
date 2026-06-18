package com.phdev.quantofalta.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [EventEntity::class, EventReminderEntity::class, EventTimelineEntity::class, DeliveredMilestoneEntity::class, ScheduledNotificationEntity::class, PerformanceEntity::class, OutboxEntity::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun eventReminderDao(): EventReminderDao
    abstract fun eventTimelineDao(): EventTimelineDao
    abstract fun deliveredMilestoneDao(): DeliveredMilestoneDao
    abstract fun scheduledNotificationDao(): ScheduledNotificationDao
    abstract fun performanceDao(): PerformanceDao
    abstract fun outboxDao(): OutboxDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `event_reminders` (" +
                    "`id` TEXT NOT NULL, " +
                    "`eventId` TEXT NOT NULL, " +
                    "`triggerType` TEXT NOT NULL, " +
                    "`offsetMinutes` INTEGER, " +
                    "`customDateTimeMillis` INTEGER, " +
                    "`enabled` INTEGER NOT NULL, " +
                    "`allowSnooze` INTEGER NOT NULL, " +
                    "`soundEnabled` INTEGER NOT NULL, " +
                    "`vibrationEnabled` INTEGER NOT NULL, " +
                    "`createdAtMillis` INTEGER NOT NULL, " +
                    "`updatedAtMillis` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`eventId`) REFERENCES `events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_reminders_eventId` ON `event_reminders` (`eventId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `event_timeline` (" +
                    "`id` TEXT NOT NULL, " +
                    "`eventId` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, " +
                    "`timestampMillis` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`eventId`) REFERENCES `events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_timeline_eventId` ON `event_timeline` (`eventId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `events` ADD COLUMN `isPrivate` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `events` ADD COLUMN `coverImageUri` TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `events` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `events_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `iconName` TEXT NOT NULL, `targetDate` INTEGER NOT NULL, `targetTime` INTEGER, `zoneId` TEXT NOT NULL, `referenceDate` INTEGER, `format` TEXT NOT NULL, `direction` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `isPrivate` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `coverImageUri` TEXT, PRIMARY KEY(`id`))")
                db.execSQL("INSERT INTO `events_new` (`id`, `title`, `colorArgb`, `iconName`, `targetDate`, `targetTime`, `zoneId`, `referenceDate`, `format`, `direction`, `createdAtMillis`, `isCompleted`, `isArchived`, `isPrivate`, `isPinned`, `coverImageUri`) SELECT `id`, `title`, `colorArgb`, `iconName`, `dateMillis` / 86400000, NULL, 'UTC', NULL, 'DAYS', 'AUTO', `createdAtMillis`, `isCompleted`, `isArchived`, `isPrivate`, `isPinned`, `coverImageUri` FROM `events`")
                db.execSQL("DROP TABLE `events`")
                db.execSQL("ALTER TABLE `events_new` RENAME TO `events`")
                
                db.execSQL("CREATE TABLE IF NOT EXISTS `delivered_milestones` (`eventId` TEXT NOT NULL, `milestoneKey` TEXT NOT NULL, `deliveredAtMillis` INTEGER NOT NULL, PRIMARY KEY(`eventId`, `milestoneKey`))")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `scheduled_notifications` (" +
                    "`id` TEXT NOT NULL, " +
                    "`eventId` TEXT NOT NULL, " +
                    "`triggerAt` INTEGER NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`status` TEXT NOT NULL, " +
                    "`isExact` INTEGER NOT NULL, " +
                    "`snoozeCount` INTEGER NOT NULL, " +
                    "`scheduledAt` INTEGER NOT NULL, " +
                    "`lastTriggeredAt` INTEGER, " +
                    "`lastError` TEXT, " +
                    "PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `performance_metrics` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`runId` TEXT NOT NULL, " +
                    "`metricType` TEXT NOT NULL, " +
                    "`screenName` TEXT NOT NULL, " +
                    "`interaction` TEXT, " +
                    "`totalFrames` INTEGER NOT NULL, " +
                    "`jankFrames` INTEGER NOT NULL, " +
                    "`durationMs` INTEGER NOT NULL, " +
                    "`createdAtMillis` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add sync fields to events
                db.execSQL("ALTER TABLE `events` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE `events` ADD COLUMN `localRevision` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `events` ADD COLUMN `serverRevision` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `events` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `events` ADD COLUMN `deletedAt` INTEGER")
                // Initialize updatedAt to createdAtMillis for existing records
                db.execSQL("UPDATE `events` SET `updatedAt` = `createdAtMillis`")

                // Create outbox table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `outbox` (" +
                    "`eventId` TEXT NOT NULL, " +
                    "`op` TEXT NOT NULL, " +
                    "`revision` INTEGER NOT NULL, " +
                    "`queuedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`eventId`))"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quanto_falta_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
