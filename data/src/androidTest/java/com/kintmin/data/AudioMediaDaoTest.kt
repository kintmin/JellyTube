package com.kintmin.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.model.AudioMediaEntity
import com.kintmin.data.util.allCombinations
import com.kintmin.data.util.allNullableCombinations
import com.kintmin.data.util.assertList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioMediaDaoTest {

    private lateinit var db: JellyTubeDatabase
    private lateinit var dao: AudioMediaDao

    private val newEntity = AudioMediaEntity(
        source = "",
        name = "",
        artist = "",
        description = "",
        audioFileNameWithExt = "",
    )

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JellyTubeDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        dao = db.audioMediaDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_시_autoGenerate는_1부터_순차증가_해야한다(): Unit = runBlocking {
        val testCount = 10
        val idList = mutableListOf<Int>()

        for (i in 1..testCount) {
            idList.add(dao.insertAudioMedia(newEntity).toInt())
        }

        for (i in 1..testCount) {
            assert(idList[i - 1] == i)
        }
    }

    @Test
    fun delete_시_autoGenerate는_무시된다(): Unit = runBlocking {
        val id = dao.insertAudioMedia(newEntity)

        dao.deleteById(id.toInt())
        val newId = dao.insertAudioMedia(newEntity)

        assert(newId == id + 1)
    }

    @Test
    fun getDataById_시_id가_없으면_에러발생된다(): Unit = runBlocking {
        val result = runCatching {
            dao.getDataById(0)
        }
        assert(result.isFailure)
    }

    @Test
    fun getDataById_시_삽입된_데이터와_동일해야한다(): Unit = runBlocking {
        val targetEntity = newEntity

        val targetId = dao.insertAudioMedia(targetEntity)
        val resultEntity = dao.getDataById(targetId.toInt())

        assert(resultEntity == targetEntity.copy(id = targetId.toInt()))
    }

    @Test
    fun getAudioMediaListFlow는_변경이_감지돼야한다(): Unit = runBlocking {
        // Given: 충분한 delay가 없으면, 최종 결과만 보장된다.
        val testFlow = dao.getAudioMediaListFlow()
        val testDelayMillis = 200L
        val expectedList = mutableListOf(
            emptyList(),
            listOf(newEntity.copy(id = 1)),
            listOf(newEntity.copy(id = 1, name = "name")),
            emptyList(),
        )

        launch {
            delay(testDelayMillis)
            val newId = dao.insertAudioMedia(newEntity).toInt()
            delay(testDelayMillis)
            dao.updateAudioMedia(id = newId, name = "name")
            delay(testDelayMillis)
            dao.deleteById(newId)
        }

        launch {
            testFlow.take(expectedList.size).withIndex().collect { (index, entityList) ->
                assertList(entityList, expectedList[index])
            }
        }
    }

    @Test
    fun 일치하는_id가_없을_때_deleteById는_무시돼야한다(): Unit = runBlocking {
        val result = runCatching {
            dao.deleteById(0)
        }
        assert(result.isSuccess)
    }

    @Test
    fun 값이_없을_때_getTotalAudioDuration는_0이어야한다(): Unit = runBlocking {
        val result = dao.getTotalAudioDuration(emptyList())
        assert(result == 0L)
    }

    @Test
    fun getTotalAudioDuration는_합계시간을_반환해야한다(): Unit = runBlocking {
        val testCount = 10
        val testIdList = allCombinations((1..testCount).toList())
        // Given: 재생시각이 id값과 동일할 때
        for (i in 1..testCount) {
            dao.insertAudioMedia(
                newEntity.copy(
                    rawAudioDurationSeconds = i.toLong()
                )
            )
        }

        // Then: 총 재생시각은 id와 합과 동일해야 한다.
        for (idList in testIdList) {
            val result = dao.getTotalAudioDuration(idList)
            assert(result == idList.sum().toLong())
        }
    }

    @Test
    fun 값이_없을_때_updateAudioMedia는_무시돼야한다(): Unit = runBlocking {
        val result = runCatching {
            dao.updateAudioMedia(0)
        }
        assert(result.isSuccess)
    }

    @Test
    fun updateAudioMedia는_null이_아닌것만_업데이트_돼야한다(): Unit = runBlocking {
        val testEntity = newEntity
        val testCombinations = allNullableCombinations(
            listOf(
                "name",
                "artist",
                "description",
                "imageFileNameWithExt",
            )
        )

        for (testData in testCombinations) {
            val targetId = dao.insertAudioMedia(testEntity).toInt()
            dao.updateAudioMedia(
                targetId,
                name = testData[0],
                artist = testData[1],
                description = testData[2],
                imageFileNameWithExt = testData[3],
            )
            val resultEntity = dao.getDataById(targetId)
            assert(
                resultEntity == testEntity.copy(
                    id = targetId,
                    name = testData[0] ?: testEntity.name,
                    artist = testData[1] ?: testEntity.artist,
                    description = testData[2] ?: testEntity.description,
                    imageFileNameWithExt = testData[3] ?: testEntity.imageFileNameWithExt,
                )
            )
        }
    }
}