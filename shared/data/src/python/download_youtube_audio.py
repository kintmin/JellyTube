import os

import yt_dlp

def get_version():
    return yt_dlp.version.__version__

def test_extract(url, audio_path):
    info_dict = {}
    title = (info_dict.get("title") or "")[:100] or None
    thumbnail_url = info_dict.get("thumbnail") or None
    duration = info_dict.get("duration") or None
    uploader = (info_dict.get("uploader") or "")[:50] or None
    description = (info_dict.get("description") or "")[:100] or None
    return title, thumbnail_url, duration, uploader, description

def download_audio(url, audio_path):
    try:
        # audio_path 는 확장자 없는 base 경로. 실제 컨테이너 확장자는 yt-dlp 가 %(ext)s 로 채운다.
        ydl_opts = {
            'format': 'bestaudio[ext=m4a]/bestaudio[acodec=aac]/bestaudio/best',
            'force_generic_extractor': True,
            'outtmpl': audio_path + '.%(ext)s',
            'cachedir': False,
            'writeinfojson': False,
            'writethumbnail': False,
            'writesubtitles': False,
            'writeautomaticsub': False,
            "quiet": True,
            'noplaylist': True,
            'playlist_items': '1',
        }
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.cache.remove()
            info_dict = ydl.extract_info(url, download=True)
            if info_dict is None:
                raise Exception("No information extracted for URL")

            # 실제 디스크에 저장된 파일 경로를 확보한다. requested_downloads 우선, 없으면 계산값 fallback.
            requested = info_dict.get("requested_downloads")
            filepath = (requested[0].get("filepath") if requested else None) or ydl.prepare_filename(info_dict)
            audio_file_name_with_ext = os.path.basename(filepath)

            title = (info_dict.get("title") or "")[:100] or None
            thumbnail_url = info_dict.get("thumbnail") or None
            duration = info_dict.get("duration") or None
            uploader = (info_dict.get("uploader") or "")[:50] or None
            description = (info_dict.get("description") or "")[:100] or None

            return title, thumbnail_url, duration, uploader, description, audio_file_name_with_ext
    except Exception as e:
        raise Exception(f"Download failed: {e}")

# 전송을 바깥으로 넘길 때 쓰는 포맷 선택자. iOS 만 이 경로를 쓴다.
#
# MP4 의 AAC 만 받는다. iOS 의 AVAudioFile 은 webm(Opus/Vorbis)을 열지 못한다.
_SINGLE_FILE_AUDIO_FORMAT = (
    "bestaudio[ext=m4a][protocol!*=dash][protocol!*=m3u8]/"
    "bestaudio[acodec^=mp4a][protocol!*=dash][protocol!*=m3u8]"
)

# 유튜브 HLS 오디오(233·234)는 코덱이 `unknown` 으로 나와 컨테이너(mp4)로 고른다.
_HLS_AUDIO_FORMAT = "bestaudio[ext=mp4][protocol^=m3u8]"

# 앱이 조각을 직접 받는 HLS 는 이 태그만 쓰는 평문 VOD 미디어 재생목록뿐이다. 나머지는 yt-dlp 가 받는다.
_SUPPORTED_HLS_TAGS = frozenset({
    "#EXTM3U",
    "#EXT-X-VERSION",
    "#EXT-X-PLAYLIST-TYPE",
    "#EXT-X-TARGETDURATION",
    "#EXT-X-MEDIA-SEQUENCE",
    "#EXT-X-INDEPENDENT-SEGMENTS",
    "#EXTINF",
    "#EXT-X-ENDLIST",
})


def parse_media_playlist(text, base_url):
    """조각 주소 목록을 돌려준다. 지원하지 않는 재생목록이면 빈 목록과 그 사유를 돌려준다.

    사유는 fallback 로그에 그대로 실린다: `unknown_tag:<태그들>` · `no_endlist` ·
    `playlist_type:<값>` · `master_playlist` · `no_segments`
    """
    from yt_dlp.utils import urljoin

    segment_urls = []
    unknown_tags = []
    has_endlist = False
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        if not line.startswith("#"):
            segment_urls.append(urljoin(base_url, line))
            continue
        # `#EXT` 로 시작하지 않는 줄은 주석이다(RFC 8216 4.1).
        if not line.startswith("#EXT"):
            continue

        tag, _, value = line.partition(":")
        if tag == "#EXT-X-STREAM-INF":
            return [], "master_playlist"
        if tag not in _SUPPORTED_HLS_TAGS:
            if tag[1:] not in unknown_tags:
                unknown_tags.append(tag[1:])
        elif tag == "#EXT-X-ENDLIST":
            has_endlist = True
        elif tag == "#EXT-X-PLAYLIST-TYPE" and value.strip() != "VOD":
            return [], f"playlist_type:{value.strip()}"

    if unknown_tags:
        return [], "unknown_tag:" + ",".join(unknown_tags)
    if not has_endlist:
        return [], "no_endlist"
    if not segment_urls:
        return [], "no_segments"
    return segment_urls, None


def _starts_with_adts(path):
    """ID3 태그를 건너뛴 첫 프레임이 ADTS(AAC)인지. 유튜브 HLS 조각을 이어 붙인 결과가 이렇다."""
    with open(path, "rb") as f:
        head = f.read(64 * 1024)
    if head[:3] == b"ID3" and len(head) >= 10:
        tag_size = ((head[6] & 0x7F) << 21) | ((head[7] & 0x7F) << 14) | ((head[8] & 0x7F) << 7) | (head[9] & 0x7F)
        head = head[10 + tag_size:]
    # ADTS 동기 워드 0xFFF 와 layer 00.
    return len(head) >= 2 and head[0] == 0xFF and (head[1] & 0xF6) == 0xF0


def _download_hls_in_process(url, audio_path, format_id):
    """앱이 직접 받지 못하는 HLS 를 yt-dlp 가 앱 프로세스 안에서 받는다. 받은 파일 이름(확장자 포함)을 돌려준다."""
    ydl_opts = {
        'format': format_id,
        'outtmpl': audio_path + '.%(ext)s',
        'cachedir': False,
        'quiet': True,
        'noprogress': True,
        'noplaylist': True,
        'playlist_items': '1',
        # API 기본값은 재시도 0회 + 실패한 조각 건너뛰기라, 중간이 빠진 파일이 성공으로 남는다.
        # `fragment_retries` 는 HTTP 오류만, `retries` 는 연결 타임아웃을 다시 시도한다.
        'retries': 10,
        'fragment_retries': 10,
        'skip_unavailable_fragments': False,
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info_dict = ydl.extract_info(url, download=True)
        if info_dict is None:
            raise Exception("No information extracted for URL")
        requested = info_dict.get("requested_downloads")
        filepath = (requested[0].get("filepath") if requested else None) or ydl.prepare_filename(info_dict)

    # yt-dlp 는 ADTS 를 `.mp4` 로 저장한다. AVAudioFile 은 확장자를 힌트로 써서 내용에 맞춘다.
    if _starts_with_adts(filepath):
        adts_path = os.path.splitext(filepath)[0] + ".aac"
        os.replace(filepath, adts_path)
        filepath = adts_path
    return os.path.basename(filepath)


# AVAudioFile 이 여는 형식만 고르되 전송 방식은 가리지 않는다. yt-dlp 가 https 는 10MiB 청크로,
# HLS 는 조각을 받아 이어 붙인다. 유튜브 HLS 오디오(233·234)는 코덱이 `unknown` 으로 나와
# `acodec` 조건에 걸리지 않으므로 컨테이너(mp4)로 받는다. webm 은 제외한다.
_PLAYABLE_AUDIO_FORMAT = "bestaudio[ext=m4a]/bestaudio[acodec^=mp4a]/bestaudio[ext=mp4]"


def download_playable_audio(url, audio_path):
    """앱 프로세스 안에서 음원 파일만 받는다. 데이터 이전처럼 앱이 떠 있는 동안 쓰는 경로다.

    background URLSession 을 쓰지 않으므로 앱이 서스펜드되면 멈춘다. 대신 단일 파일 m4a 가 없고
    HLS 만 남은 영상도 받을 수 있다.

    돌려주는 순서: (audio_file_name_with_ext, duration)
    HLS 결과는 내용이 ADTS(AAC)인데 `.mp4` 로 저장된다. 확장자는 호출자가 내용에 맞춘다.
    """
    try:
        ydl_opts = {
            'format': _PLAYABLE_AUDIO_FORMAT,
            'outtmpl': audio_path + '.%(ext)s',
            'cachedir': False,
            'quiet': True,
            'noprogress': True,
            'noplaylist': True,
            'playlist_items': '1',
            # API 기본값은 재시도 0회 + 실패한 조각 건너뛰기라, 중간이 빠진 파일이 성공으로 남는다.
            # `fragment_retries` 는 HTTP 오류만, `retries` 는 연결 타임아웃을 다시 시도한다.
            'retries': 10,
            'fragment_retries': 10,
            'skip_unavailable_fragments': False,
        }
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.cache.remove()
            info_dict = ydl.extract_info(url, download=True)
            if info_dict is None:
                raise Exception("No information extracted for URL")

            requested = info_dict.get("requested_downloads")
            filepath = (requested[0].get("filepath") if requested else None) or ydl.prepare_filename(info_dict)
            return os.path.basename(filepath), info_dict.get("duration") or None
    except Exception as e:
        raise Exception(f"Download playable audio failed: {e}")


def extract_audio_source(url, audio_path, prefer_hls="0"):
    """내려받지 않고 **직접 주소와 메타데이터만** 돌려준다.

    전송은 호출자가 맡는다. iOS 는 background URLSession 에 넘겨 앱이 내려가도 이어지게 하고,
    Android 는 이 경로를 쓰지 않는다(WorkManager 가 같은 일을 해 준다).

    돌려주는 순서:
        (direct_url, http_headers_json, ext, title, thumbnail_url, duration, uploader, description,
         filesize, route, segments_path, fallback_reason, format_id, protocol)

    `route` 가 전송 방식을 정한다.
    - `https`: `direct_url` 하나를 범위 요청으로 받는다.
    - `hls`: `segments_path`(JSON 배열)의 조각을 전부 받아 순서대로 이어 붙인다.
    - `downloaded`: 앱이 받지 못하는 HLS 라 여기서 `audio_path` 에 이미 받았다.
      `fallback_reason` 에 그 사유가 있다.

    `filesize` 를 함께 주는 이유는 호출자가 전송을 쪼갤 때 **첫 응답을 기다리지 않고** 모든
    구간을 한 번에 등록할 수 있어야 하기 때문이다. 정확한 값이 없으면 비운다 —
    `filesize_approx` 는 크기 검증을 망가뜨린다.

    `http_headers` 를 함께 주는 이유는 유튜브가 돌려주는 주소가 **서명된 단기 주소**라
    추출 때와 다른 헤더로 요청하면 거부될 수 있기 때문이다.
    """
    import json
    import traceback

    if prefer_hls == "1":
        audio_format = f"{_HLS_AUDIO_FORMAT}/{_SINGLE_FILE_AUDIO_FORMAT}"
    else:
        audio_format = f"{_SINGLE_FILE_AUDIO_FORMAT}/{_HLS_AUDIO_FORMAT}"

    try:
        ydl_opts = {
            'format': audio_format,
            'cachedir': False,
            'quiet': True,
            'noplaylist': True,
            'playlist_items': '1',
            'skip_download': True,
        }
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.cache.remove()
            info_dict = ydl.extract_info(url, download=False)
            if info_dict is None:
                raise Exception("No information extracted for URL")

            # 포맷이 골라지면 yt-dlp 가 최상위에 url/ext/http_headers 를 채운다.
            # 버전에 따라 requested_downloads 에만 있는 경우가 있어 그쪽을 먼저 본다.
            requested = info_dict.get("requested_downloads")
            selected = requested[0] if requested else info_dict

            # 라이브는 끝이 없어 받을 수 없다. HLS 만 남으므로 아래 fallback 까지 가기 전에 끊는다.
            if info_dict.get("live_status") == "is_live":
                raise Exception("Live stream is not supported")

            direct_url = selected.get("url")
            if not direct_url:
                raise Exception("No direct url for the selected format")

            protocol = (selected.get("protocol") or "").lower()
            if "dash" in protocol:
                raise Exception(f"Selected format is segmented: {protocol}")

            headers = selected.get("http_headers") or {}
            ext = selected.get("ext") or "m4a"
            filesize = selected.get("filesize") or None
            format_id = selected.get("format_id") or ""
            route = "https"
            segments_path = None
            fallback_reason = None

            if "m3u8" in protocol:
                filesize = None
                urlh = ydl.urlopen(yt_dlp.networking.Request(direct_url, headers=headers))
                playlist_text = urlh.read().decode("utf-8", "ignore")
                # 조각 주소는 리다이렉트된 뒤의 재생목록 주소를 기준으로 푼다. yt-dlp 의 HlsFD 와 같다.
                segment_urls, fallback_reason = parse_media_playlist(playlist_text, urlh.url)
                if fallback_reason is None:
                    route = "hls"
                    ext = "aac"
                    segments_path = audio_path + ".segments.json"
                    with open(segments_path, "w", encoding="utf-8") as f:
                        json.dump(segment_urls, f)
                else:
                    route = "downloaded"
                    file_name = _download_hls_in_process(url, audio_path, format_id)
                    ext = os.path.splitext(file_name)[1].lstrip(".")

            title = (info_dict.get("title") or "")[:100] or None
            thumbnail_url = info_dict.get("thumbnail") or None
            duration = info_dict.get("duration") or None
            uploader = (info_dict.get("uploader") or "")[:50] or None
            description = (info_dict.get("description") or "")[:100] or None

            return (
                direct_url,
                json.dumps(headers),
                ext,
                title,
                thumbnail_url,
                duration,
                uploader,
                description,
                filesize,
                route,
                segments_path,
                fallback_reason,
                format_id,
                protocol,
            )
    except Exception as e:
        # 원래 자리를 지우면 어디서 깨졌는지 알 수 없다. 앱 로그가 이 문자열을 통째로 받는다.
        raise Exception(f"Extract audio source failed: {e}\n{traceback.format_exc()}")


def extract_video_urls_from_playlist(playlist_url):
    try:
        ydl_opts = {
            'quiet': True,
            'extract_flat': True,
            'skip_download': True,
        }
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.cache.remove()
            info_dict = ydl.extract_info(playlist_url, download=False)
            if info_dict is None:
                raise Exception("No information extracted for URL")

            entries = info_dict.get("entries", [])
            video_urls = [entry.get("url") for entry in entries if "url" in entry]

            return video_urls
    except Exception as e:
        raise Exception(f"Extract urls failed: {e}")