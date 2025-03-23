package com.kintmin.presentation.ui.audio_play

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kintmin.domain.model.AudioMediaData
import com.kintmin.domain.usecase.FetchAudioMediaListUseCase

class AudioPagingSource(
    private val fetchAudioMediaListUseCase: FetchAudioMediaListUseCase,
) : PagingSource<Int, AudioMediaData>() {
    override fun getRefreshKey(state: PagingState<Int, AudioMediaData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AudioMediaData> {
        val data = fetchAudioMediaListUseCase().onFailure { exception ->
            return LoadResult.Error(exception)
        }.getOrDefault(emptyList())

        val currentPage = params.key ?: STARTING_PAGE
        val prevKey = if (currentPage == STARTING_PAGE) null else currentPage - 1
        val nextKey = if (data.size < params.loadSize) null else currentPage + 1
        return LoadResult.Page(data, prevKey, nextKey)
    }

    companion object {
        const val STARTING_PAGE = 0
    }

}