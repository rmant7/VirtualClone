from datetime import datetime, timedelta
from yt_dlp import YoutubeDL

channel_url = input("Enter YouTube channel URL : ")


ydl_opts = {
    'quiet': True,
    'extract_flat': True,
    'force_generic_extractor': False,
}

with YoutubeDL(ydl_opts) as ydl:
    info = ydl.extract_info(channel_url, download=False)


    videos = info.get("entries", [])
    description = info.get("description", "No description available")


one_month_ago = datetime.now() - timedelta(days=30)
recent_videos = [
    v for v in videos
    if 'upload_date' in v and datetime.strptime(v['upload_date'], "%Y%m%d") > one_month_ago
]

print(f"\n{len(recent_videos)} videos uploaded in the last month:")
for v in recent_videos:
    print(f"- {v['title']} ({v['webpage_url']})")


keyword = "tutorial"
keyword_matches = [
    v for v in videos
    if 'title' in v and keyword.lower() in v['title'].lower()
]

print(f"\n{len(keyword_matches)} videos with keyword '{keyword}' in the title:")
for v in keyword_matches:
    print(f"- {v['title']} ({v['webpage_url']})")


print("\nChannel Description:")
print(description)
