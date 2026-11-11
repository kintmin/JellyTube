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
# `download_audio` 와 달리 **분할 포맷(DASH/HLS)을 배제**한다. 분할 포맷은 주소 하나로
# 받을 수 없어서, 전송을 iOS 의 background URLSession 에 맡기는 경로에서는 쓸 수 없다.
#
# **MP4 의 AAC 만 받는다.** iOS 의 AVAudioFile 은 webm(Opus/Vorbis)을 열지 못한다. 그쪽으로
# 넘어가면 받은 곡이 재생되지 않으므로, m4a 가 없는 영상은 실패로 돌린다.
_SINGLE_FILE_AUDIO_FORMAT = (
    "bestaudio[ext=m4a][protocol!*=dash][protocol!*=m3u8]/"
    "bestaudio[acodec^=mp4a][protocol!*=dash][protocol!*=m3u8]"
)


# AVAudioFile 이 여는 형식만 고르되 **전송 방식은 가리지 않는다**. yt-dlp 가 https 는 10MiB 청크로,
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


def extract_audio_source(url):
    """내려받지 않고 **직접 주소와 메타데이터만** 돌려준다.

    전송은 호출자가 맡는다. iOS 는 background URLSession 에 넘겨 앱이 내려가도 이어지게 하고,
    Android 는 이 경로를 쓰지 않는다(WorkManager 가 같은 일을 해 준다).

    돌려주는 순서:
        (direct_url, http_headers_json, ext, title, thumbnail_url, duration, uploader, description,
         filesize)

    `filesize` 를 함께 주는 이유는 호출자가 전송을 쪼갤 때 **첫 응답을 기다리지 않고** 모든
    구간을 한 번에 등록할 수 있어야 하기 때문이다. 정확한 값이 없으면 비운다 —
    `filesize_approx` 는 크기 검증을 망가뜨린다.

    `http_headers` 를 함께 주는 이유는 유튜브가 돌려주는 주소가 **서명된 단기 주소**라
    추출 때와 다른 헤더로 요청하면 거부될 수 있기 때문이다.
    """
    import json
    import traceback

    try:
        ydl_opts = {
            'format': _SINGLE_FILE_AUDIO_FORMAT,
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

            direct_url = selected.get("url")
            if not direct_url:
                raise Exception("No direct url for the selected format")

            protocol = (selected.get("protocol") or "").lower()
            if "dash" in protocol or "m3u8" in protocol:
                raise Exception(f"Selected format is segmented: {protocol}")

            headers = selected.get("http_headers") or {}
            ext = selected.get("ext") or "m4a"
            filesize = selected.get("filesize") or None

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