import yt_dlp
def download_audio(url, audio_path):
    try:
        ydl_opts = {
            'format': 'bestaudio',
            'outtmpl': audio_path,
            'writethumbnail': False,
        }
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.cache.remove()
            info_dict = ydl.extract_info(url, download=True)
            
            title = info_dict.get("title", "Unknown")
            thumbnail_url = info_dict.get("thumbnail", "")
            
            return title, thumbnail_url
    except Exception as e:
        return f"Exception: {e}"