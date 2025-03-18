import yt_dlp
def download_audio(url, output_path):
    try:
        ydl_opts = {
            'format': 'bestaudio',
            'outtmpl': f'{output_path}',
            'writethumbnail': True,
        }
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.cache.remove()
            info_dict = ydl.extract_info(url, download=True)
            return info_dict.get("title", "unknown")
    except Exception as e:
        return f"Exception: {e}"