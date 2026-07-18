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