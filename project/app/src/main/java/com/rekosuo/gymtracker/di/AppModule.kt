package com.rekosuo.gymtracker.di

import android.content.Context
import androidx.room.Room
import com.rekosuo.gymtracker.data.local.GymDatabase
import com.rekosuo.gymtracker.data.local.dao.ExerciseDao
import com.rekosuo.gymtracker.data.local.dao.GroupDao
import com.rekosuo.gymtracker.data.local.dao.PerformanceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Automatically injects dependencies (Database, DAO)
 * Infrastructure, create and connect parts.
 */
@Module // Tells Hilt this class contains dependency providers
@InstallIn(SingletonComponent::class) // Dependencies live as long as the app lives
object AppModule {
    
    @Provides
    @Singleton
    fun provideGymDatabase(
        @ApplicationContext context: Context
    ): GymDatabase {
        return Room.databaseBuilder(
            context,
            GymDatabase::class.java,
            "gym_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideExerciseDao(database: GymDatabase): ExerciseDao {
        return database.exerciseDao()
    }
    
    @Provides
    @Singleton
    fun provideGroupDao(database: GymDatabase): GroupDao {
        return database.groupDao()
    }
    
    @Provides
    @Singleton
    fun providePerformanceDao(database: GymDatabase): PerformanceDao {
        return database.performanceDao()
    }
}
