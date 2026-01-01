package com.kintmin.data.local_db_test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.di.TestDatabase
import com.kintmin.data.local_db.model.PlaylistEntity
import com.kintmin.data.util.allNullableCombinations
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PlaylistDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    @TestDatabase
    lateinit var db: JellyTubeDatabase
    lateinit var dao: PlaylistDao

    private val newEntity = PlaylistEntity(
        name = "",
        description = "",
        audioMediaCount = 0,
        rawPlayTimeDuration = 0L,
        isCustomImage = false,
    )

    @Before
    fun setup() {
        hiltRule.inject()
        dao = db.playlistDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun 값이_없을_때_updatePlaylist는_무시돼야한다(): Unit = runBlocking {
        val result = runCatching {
            dao.updatePlaylist(2)
        }
        assert(result.isSuccess)
    }

    @Test
    fun updatePlaylist는_null이_아닌것만_업데이트_돼야한다(): Unit = runBlocking {
        val testEntity = newEntity
        val testCombinations = allNullableCombinations(
            listOf(
                "name",
                "description",
                "1",
                "1",
                "imageFileNameWithExt",
            )
        )

        for (testData in testCombinations) {
            val targetId = dao.insertPlaylist(testEntity).toInt()
            dao.updatePlaylist(
                targetId,
                name = testData[0],
                description = testData[1],
                audioMediaCount = testData[2]?.toInt(),
                rawPlayTimeDuration = testData[3]?.toLong(),
                imageFileNameWithExt = testData[4],
            )
            val resultEntity = dao.getPlaylistById(targetId)
            assert(
                resultEntity == testEntity.copy(
                    id = targetId,
                    name = testData[0] ?: testEntity.name,
                    description = testData[1] ?: testEntity.description,
                    audioMediaCount = testData[2]?.toInt() ?: testEntity.audioMediaCount,
                    rawPlayTimeDuration = testData[3]?.toLong() ?: testEntity.rawPlayTimeDuration,
                    imageFileNameWithExt = testData[4] ?: testEntity.imageFileNameWithExt,
                )
            )
        }
    }
}