from yt_dlp import YoutubeDL

def get_uploads_playlist_url(channel_url):

    if '/@' in channel_url:
        return f"{channel_url}/videos"
    elif '/channel/' in channel_url:

        return f"{channel_url}/videos"
    else:
        return channel_url

channel_url = input("Enter YouTube channel URL: ")
uploads_url = get_uploads_playlist_url(channel_url)


options = {
    'quiet': True,
    'extract_flat': True,
    'skip_download': True
}

with YoutubeDL(options) as ydl:
    info = ydl.extract_info(uploads_url, download=False)

    if 'entries' not in info:
        print("No videos found.")
        exit()

    videos = info['entries']
    print(f"Found {len(videos)} videos")

    for video in videos:
        print(video.get('url'), "-", video.get('title'))
