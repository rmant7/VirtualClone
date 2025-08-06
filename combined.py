from datetime import datetime, timedelta
from pytube import Channel
from pytube.exceptions import RegexMatchError
from bs4 import BeautifulSoup
import requests
import ssl
import certifi
from yt_dlp import YoutubeDL

from langdetect import detect, LangDetectException


try:
    _create_unverified_https_context = ssl._create_unverified_context
except AttributeError:
    pass
else:
    ssl._create_default_https_context = _create_unverified_https_context
ssl_context = ssl.create_default_context(cafile=certifi.where())

def resolve_channel_url(url):
    try:
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")
        canonical = soup.find("link", rel="canonical")
        if canonical:
            href = canonical.get("href", "")
            if "/channel/" in href:
                return href.split("/channel/")[-1]
            else:
                channel = Channel(url)
                return channel.channel_id
        else:
            channel = Channel(url)
            return channel.channel_id
    except Exception as e:
        print(f"Error resolving URL: {e}")
        return None

def fetch_channel_description(channel_id):
    headers = {"Accept-Language": "en-US,en;q=0.9"}
    try:
        about_url = f"https://www.youtube.com/channel/{channel_id}/about"
        response = requests.get(about_url, headers=headers, timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, 'html.parser')
        meta_desc = soup.find("meta", attrs={"name": "description"})
        if meta_desc:
            desc = meta_desc.get("content", "").strip()
            if desc.endswith(" - YouTube"):
                desc = desc[:-9].strip()
            return desc
        return None
    except Exception as e:
        print(f"Error fetching description: {e}")
        return None

def get_videos_from_videos_playlist(channel_id):
    """
    Fetch videos from the channel's Videos playlist (most complete video list).
    """
    videos = []
    playlist_id = "UU" + channel_id[2:]  # Videos playlist ID
    playlist_url = f"https://www.youtube.com/playlist?list={playlist_id}"

    ydl_opts = {
        'quiet': True,
        'ignoreerrors': True,
        'skip_download': True,
        'extract_flat': False,
    }

    try:
        with YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(playlist_url, download=False)
            entries = info.get('entries', [])
            videos = [video for video in entries if video]
    except Exception as e:
        print(f"Error fetching videos: {e}")

    return videos

def parse_upload_date(date_str):
    try:
        return datetime.strptime(date_str, "%Y%m%d")
    except Exception:
        return None

def main():
    while True:
        url = input("YouTube channel URL: ").strip()
        channel_id = resolve_channel_url(url)
        if channel_id is None:
            print("Invalid or unsupported URL. Please try again.")
            continue
        try:
            channel = Channel(f"https://www.youtube.com/channel/{channel_id}")
            print(f"✅ Resolved channel ID: {channel_id}")
            print(f"✅ Successfully fetched channel: {channel.channel_name}")
            break
        except RegexMatchError:
            print("❌ Invalid channel URL format for pytube. Please try again.")
        except Exception as e:
            print(f"❌ Unexpected error: {e}")
            return

    description = fetch_channel_description(channel_id)
    if description:
        try:
            lang = detect(description)
        except LangDetectException:
            lang = "unknown"
    else:
        lang = "unknown"

    videos = get_videos_from_videos_playlist(channel_id)
    total = len(videos)
    print(f"\nFound {total} videos")

    for v in videos:
        print(f"https://www.youtube.com/watch?v={v.get('id')} - {v.get('title', 'No title')}")

    cutoff_date = datetime.now() - timedelta(days=30)
    recent = [v for v in videos if parse_upload_date(v.get('upload_date', '')) and parse_upload_date(v.get('upload_date', '')) > cutoff_date]
    print(f"\n{len(recent)} videos uploaded in the last month:")

    for v in recent:
        print(f"https://www.youtube.com/watch?v={v.get('id')} - {v.get('title', 'No title')}")

    keyword = input("\nEnter a keyword to filter titles (or press Enter to skip): ").strip()
    if keyword:
        keyword_filtered = [v for v in videos if keyword.lower() in v.get('title', '').lower()]
        print(f"\n{len(keyword_filtered)} videos with keyword '{keyword}':")
        for v in keyword_filtered:
            print(f"https://www.youtube.com/watch?v={v.get('id')} - {v.get('title', 'No title')}")
    else:
        print("\nNo keyword filtering applied.")

    print("\nChannel Description:")
    if description:
        print(description)
    else:
        print("Description not available.")

    print(f"\nLanguage detected: {lang}")

if __name__ == "__main__":
    main()
