package com.kintmin.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_db.dao.AudioMediaDao
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.model.AudioMediaEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Suppress("NonAsciiCharacters", "FunctionName", "IllegalIdentifier")
class RoomTest {

    private lateinit var db: JellyTubeDatabase
    private lateinit var dao: AudioMediaDao

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
    fun autoGenerate는_1부터_시작해야_한다() = runBlocking {
        val id = dao.insertAudioMedia(
            AudioMediaEntity(
                source = "",
                name = "",
                artist = "",
                description = "",
                rawAudioDurationSeconds = null,
                audioFileNameWithExt = "",
                imageFileNameWithExt = "",
                rawCreatedTime = Instant.now().toEpochMilli(),
            )
        )

        assert(id == 1L)
    }
}