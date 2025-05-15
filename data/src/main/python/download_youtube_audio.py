import yt_dlp

def get_version():
    return yt_dlp.version.__version__

def download_audio(url, audio_path):
    try:
        ydl_opts = {
            'format': 'bestaudio/best',
            'force_generic_extractor': True,
            'outtmpl': audio_path,
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
            
            title = info_dict.get("title", "Unknown")[:100]
            thumbnail_url = info_dict.get("thumbnail", "")
            duration = info_dict.get("duration")
            uploader = info_dict.get("uploader", "Unknown")[:50]
            description = info_dict.get("description", "")[:100]
            
            return title, thumbnail_url, duration, uploader, description
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