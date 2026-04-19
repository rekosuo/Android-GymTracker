package com.rekosuo.gymtracker.data.backup

import com.rekosuo.gymtracker.data.local.entity.ExerciseEntity
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupCrossRef
import com.rekosuo.gymtracker.data.local.entity.ExerciseGroupEntity
import com.rekosuo.gymtracker.data.local.entity.PerformanceEntity
import com.rekosuo.gymtracker.data.local.entity.SetEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileSerializationTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    // Seed with multi-entity, multi-row, varying-order data so the round-trip
    // test would fail if ordering, junction multiplicity, or nested list order
    // were not preserved.
    private fun seed() = BackupFile(
        formatVersion = BACKUP_FORMAT_VERSION,
        dbSchemaVersion = BACKUP_DB_SCHEMA_VERSION,
        exportedAt = 1_700_000_000_000L,
        exercises = listOf(
            ExerciseEntity(id = 1, name = "Bench", createdAt = 1000L, isFavorite = true),
            ExerciseEntity(id = 2, name = "Squat", createdAt = 1001L, isFavorite = false),
            ExerciseEntity(id = 3, name = "Deadlift", createdAt = 1002L, isFavorite = false),
        ),
        groups = listOf(
            ExerciseGroupEntity(id = 1, name = "Push Day", createdAt = 2000L, isFavorite = true),
            ExerciseGroupEntity(id = 2, name = "Full Body", createdAt = 2001L, isFavorite = false),
        ),
        groupExerciseCrossRefs = listOf(
            ExerciseGroupCrossRef(exerciseId = 1, groupId = 1, orderIndex = 0),
            ExerciseGroupCrossRef(exerciseId = 2, groupId = 1, orderIndex = 1),
            ExerciseGroupCrossRef(exerciseId = 1, groupId = 2, orderIndex = 0),
            ExerciseGroupCrossRef(exerciseId = 3, groupId = 2, orderIndex = 1),
        ),
        performances = listOf(
            PerformanceEntity(
                id = 1,
                exerciseId = 1,
                date = 3000L,
                sets = listOf(
                    SetEntry(weight = 60f, reps = 10, order = 0),
                    SetEntry(weight = 65f, reps = 8, order = 1),
                    SetEntry(weight = 60f, reps = 7, order = 2),
                ),
                notes = "felt strong"
            ),
            PerformanceEntity(
                id = 2,
                exerciseId = 1,
                date = 3100L,
                sets = listOf(SetEntry(weight = 62.5f, reps = 9, order = 0)),
                notes = ""
            ),
            PerformanceEntity(
                id = 3,
                exerciseId = 2,
                date = 3200L,
                sets = listOf(
                    SetEntry(weight = 100f, reps = 5, order = 0),
                    SetEntry(weight = 100f, reps = 5, order = 1),
                ),
                notes = "warmup skipped"
            ),
        ),
    )

    @Test
    fun roundTrip_preservesAllFields() {
        val original = seed()

        val encoded = json.encodeToString(BackupFile.serializer(), original)
        val decoded = json.decodeFromString(BackupFile.serializer(), encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun roundTrip_preservesCrossRefOrderIndex() {
        val original = seed()
        val decoded = json.decodeFromString(
            BackupFile.serializer(),
            json.encodeToString(BackupFile.serializer(), original)
        )

        // If orderIndex were dropped or zeroed, this would fail even though
        // the list lengths match — orderIndex values are 0,1,0,1 across the
        // four cross-refs and encode a semantic meaning.
        assertEquals(
            original.groupExerciseCrossRefs.map { it.orderIndex },
            decoded.groupExerciseCrossRefs.map { it.orderIndex }
        )
    }

    @Test
    fun roundTrip_preservesNestedSetEntryOrder() {
        val original = seed()
        val decoded = json.decodeFromString(
            BackupFile.serializer(),
            json.encodeToString(BackupFile.serializer(), original)
        )

        // The first performance has return-to-previous-weight pattern
        // (60, 65, 60). If the JSON array were reordered or the `order` field
        // dropped, the grouping into WeightRows would break semantically.
        val firstSets = decoded.performances.first { it.id == 1L }.sets
        assertEquals(listOf(0, 1, 2), firstSets.map { it.order })
        assertEquals(listOf(60f, 65f, 60f), firstSets.map { it.weight })
    }

    @Test
    fun serialized_containsExpectedTopLevelFields() {
        val encoded = json.encodeToString(BackupFile.serializer(), seed())

        assertTrue(encoded.contains("\"formatVersion\""))
        assertTrue(encoded.contains("\"dbSchemaVersion\""))
        assertTrue(encoded.contains("\"exportedAt\""))
        assertTrue(encoded.contains("\"exercises\""))
        assertTrue(encoded.contains("\"groups\""))
        assertTrue(encoded.contains("\"groupExerciseCrossRefs\""))
        assertTrue(encoded.contains("\"performances\""))
    }

    @Test
    fun ignoreUnknownKeys_allowsForwardCompat() {
        // A file written by a newer build might include a field we don't know about.
        // With ignoreUnknownKeys = true, current builds should still be able to read
        // it as long as formatVersion matches.
        val withExtra = """
            {
              "formatVersion": 1,
              "dbSchemaVersion": 3,
              "exportedAt": 1700000000000,
              "futureField": "ignored",
              "exercises": [],
              "groups": [],
              "groupExerciseCrossRefs": [],
              "performances": []
            }
        """.trimIndent()

        val decoded = json.decodeFromString(BackupFile.serializer(), withExtra)
        assertEquals(1, decoded.formatVersion)
    }
}
