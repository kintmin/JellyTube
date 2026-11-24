package com.kintmin.data.local_db.dao

/** 비밀 플레이리스트에 하나라도 속한 음원 id. 'SECRET' 은 PlaylistType.SECRET.name 이다. */
internal const val SECRET_AUDIO_MEDIA_IDS = """
SELECT SPT.audioMediaId FROM PLAYLIST_TRACK SPT
JOIN PLAYLIST SP ON SP.id = SPT.playlistId
WHERE SP.type = 'SECRET'
"""

/**
 * "전체" 의 곡 수·길이·썸네일은 저장된 값이라 비밀 곡이 들어 있다. 그만큼 빼서 읽는다.
 * 편집본이 있으면 편집본 길이가 재생 길이다. 썸네일은 비밀이 아닌 첫 곡의 것이다.
 */
internal const val VISIBLE_PLAYLIST_COLUMNS = """
id, name, description, rawCreatedTime, isCustomImage, sequence, type,
CASE WHEN type = 'TOTAL' AND NOT isCustomImage
    THEN (
        SELECT AM.imageFileNameWithExt FROM PLAYLIST_TRACK PT
        JOIN AUDIO_MEDIA AM ON AM.id = PT.audioMediaId
        WHERE PT.playlistId = PLAYLIST.id AND PT.audioMediaId NOT IN ($SECRET_AUDIO_MEDIA_IDS)
        ORDER BY PT.sequence LIMIT 1
    )
    ELSE imageFileNameWithExt END AS imageFileNameWithExt,
CASE WHEN type = 'TOTAL'
    THEN audioMediaCount - (SELECT COUNT(*) FROM AUDIO_MEDIA WHERE id IN ($SECRET_AUDIO_MEDIA_IDS))
    ELSE audioMediaCount END AS audioMediaCount,
CASE WHEN type = 'TOTAL'
    THEN rawPlayTimeDuration - (
        SELECT COALESCE(SUM(COALESCE(editedAudioDurationSeconds, rawAudioDurationSeconds, 0)), 0)
        FROM AUDIO_MEDIA WHERE id IN ($SECRET_AUDIO_MEDIA_IDS)
    )
    ELSE rawPlayTimeDuration END AS rawPlayTimeDuration
"""
