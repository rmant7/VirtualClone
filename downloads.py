import subprocess
import sys
import os
os.environ["PATH"] = os.path.expanduser("~/VirtualClone") + ":" + os.environ["PATH"]


try:
    from audio_transcriber import download_and_transcribe
except ImportError:
    print("❌ ERROR: Cannot import 'download_and_transcribe' from 'audio_transcriber.py'. Please check the function name and file location.")
    sys.exit(1)


def get_video_urls_from_channel(channel_url, max_videos=None):
    print(f"Fetching video URLs from channel: {channel_url}")
    command = ["yt-dlp", "--flat-playlist", "--get-id", channel_url]
    if max_videos:
        command += ["--playlist-end", str(max_videos)]

    result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    video_ids = result.stdout.strip().split('\n')

    video_urls = []
    for vid in video_ids:
        vid = vid.strip()
        if not vid:
            continue

        if vid.startswith("http://") or vid.startswith("https://"):
            video_urls.append(vid)
        else:
            video_urls.append(f"https://www.youtube.com/watch?v={vid}")
    return video_urls


def download_from_channel(channel_url, max_videos=None):
    video_urls = get_video_urls_from_channel(channel_url, max_videos)
    print(f"\n🎬 Found {len(video_urls)} videos. Starting processing...")

    for url in video_urls:
        try:
            download_and_transcribe(url)
        except Exception as e:
            print(f"❌ Error processing {url}: {e}")


if __name__ == "__main__":
    channel_link = input("Enter YouTube channel URL: ")
    limit = input("Limit number of videos? Leave blank for all: ")

    try:
        max_videos = int(limit) if limit else None
    except ValueError:
        max_videos = None

    download_from_channel(channel_link, max_videos)
