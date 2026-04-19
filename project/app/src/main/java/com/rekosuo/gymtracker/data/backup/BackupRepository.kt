package com.rekosuo.gymtracker.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.rekosuo.gymtracker.data.local.GymDatabase
import com.rekosuo.gymtracker.data.local.dao.ExerciseDao
import com.rekosuo.gymtracker.data.local.dao.GroupDao
import com.rekosuo.gymtracker.data.local.dao.PerformanceDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GymDatabase,
    private val exerciseDao: ExerciseDao,
    private val groupDao: GroupDao,
    private val performanceDao: PerformanceDao,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val stream = resolver.openOutputStream(uri, "wt")
                ?: error("Could not open output stream for $uri")
            stream.use { exportToStream(it).getOrThrow() }
        }
    }

    suspend fun importFrom(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val stream = resolver.openInputStream(uri)
                ?: error("Could not open input stream for $uri")
            stream.use { importFromStream(it).getOrThrow() }
        }
    }

    internal suspend fun exportToStream(out: OutputStream): Result<Unit> =
        runCatching {
            val file = BackupFile(
                formatVersion = BACKUP_FORMAT_VERSION,
                dbSchemaVersion = BACKUP_DB_SCHEMA_VERSION,
                exportedAt = System.currentTimeMillis(),
                exercises = exerciseDao.getAllForBackup(),
                groups = groupDao.getAllGroupsForBackup(),
                groupExerciseCrossRefs = groupDao.getAllCrossRefsForBackup(),
                performances = performanceDao.getAllForBackup(),
            )
            out.writer(Charsets.UTF_8).use { writer ->
                writer.write(json.encodeToString(BackupFile.serializer(), file))
            }
        }

    internal suspend fun importFromStream(input: InputStream): Result<Unit> =
        runCatching {
            val text = input.reader(Charsets.UTF_8).use { it.readText() }
            val file = try {
                json.decodeFromString(BackupFile.serializer(), text)
            } catch (e: Exception) {
                error("Backup file is malformed: ${e.message}")
            }

            if (file.formatVersion != BACKUP_FORMAT_VERSION) {
                error(
                    "Unsupported backup format version ${file.formatVersion} " +
                        "(this app supports $BACKUP_FORMAT_VERSION)."
                )
            }
            if (file.dbSchemaVersion != BACKUP_DB_SCHEMA_VERSION) {
                error(
                    "Backup was made from database schema v${file.dbSchemaVersion}, " +
                        "this app expects v$BACKUP_DB_SCHEMA_VERSION."
                )
            }

            database.withTransaction {
                // Wipe in reverse FK order so cascading deletes don't fight us.
                performanceDao.deleteAll()
                groupDao.deleteAllCrossRefs()
                groupDao.deleteAllGroups()
                exerciseDao.deleteAll()

                // Insert parents before children.
                exerciseDao.insertAll(file.exercises)
                groupDao.insertAllGroups(file.groups)
                groupDao.insertExerciseGroupCrossRefs(file.groupExerciseCrossRefs)
                performanceDao.insertAll(file.performances)
            }
        }
}
