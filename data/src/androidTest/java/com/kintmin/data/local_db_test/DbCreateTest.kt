package com.kintmin.data.local_db_test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kintmin.data.local_db.dao.PlaylistDao
import com.kintmin.data.local_db.database.JellyTubeDatabase
import com.kintmin.data.local_db.di.TestDatabase
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
class DbCreateTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    @TestDatabase
    lateinit var db: JellyTubeDatabase
    lateinit var dao: PlaylistDao

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
    fun db_생성시_전체와_미분류가_생겨야한다(): Unit = runBlocking {
        val createdTime = System.currentTimeMillis()
        val totalEntity = dao.getPlaylistById(1)
        val uncategorizedEntity = dao.getPlaylistById(2)

        assert(totalEntity.name == "전체")
        assert(uncategorizedEntity.name == "미분류")
        assert(totalEntity.rawCreatedTime - createdTime < 1000L)
        assert(uncategorizedEntity.rawCreatedTime - createdTime < 1000L)
        assert(!totalEntity.isCustomImage)
        assert(!uncategorizedEntity.isCustomImage)
        assert(totalEntity.description == "")
        assert(uncategorizedEntity.description == "")
        assert(totalEntity.audioMediaCount == 0)
        assert(uncategorizedEntity.audioMediaCount == 0)
        assert(totalEntity.rawPlayTimeDuration == 0L)
        assert(uncategorizedEntity.rawPlayTimeDuration == 0L)
        assert(totalEntity.imageFileNameWithExt == null)
        assert(uncategorizedEntity.imageFileNameWithExt == null)
    }
}