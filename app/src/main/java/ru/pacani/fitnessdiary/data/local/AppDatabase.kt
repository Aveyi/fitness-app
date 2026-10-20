package ru.pacani.fitnessdiary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.pacani.fitnessdiary.data.local.dao.ExerciseDao
import ru.pacani.fitnessdiary.data.local.dao.StatsDao
import ru.pacani.fitnessdiary.data.local.dao.UserDao
import ru.pacani.fitnessdiary.data.local.dao.WorkoutDao
import ru.pacani.fitnessdiary.data.local.entity.ExerciseEntity
import ru.pacani.fitnessdiary.data.local.entity.SetEntity
import ru.pacani.fitnessdiary.data.local.entity.UserEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutEntity
import ru.pacani.fitnessdiary.data.local.entity.WorkoutExerciseEntity

@Database(
    entities = [
        UserEntity::class,
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun statsDao(): StatsDao

    companion object {
        private const val DB_NAME = "fitness_diary.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                // На время разработки: при смене схемы база пересоздаётся.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
