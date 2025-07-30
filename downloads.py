import yt_dlp
import os
import argparse


default_path = "/Users/esraamograbi/Desktop/Projects/downloader script/links.txt"


output_directory = "downloads"
os.makedirs(output_directory, exist_ok=True)


parser = argparse.ArgumentParser(description="Download audio from YouTube videos listed in a file.")

parser.add_argument(
    "input",
    nargs="?",  
    default=default_path,
    help="Path to input file with YouTube links"
)

parser.add_argument(
    "--keep-video",
    action="store_true",
    default=True,  
    help="Keep the original downloaded video"
)

args = parser.parse_args()


with open(args.input, "r") as f:
    links = [line.strip() for line in f if line.strip()]


ydl_opts = {
    'format': 'bestaudio/best',
    'outtmpl': os.path.join(output_directory, '%(title)s.%(ext)s'),
    'postprocessors': [{
        'key': 'FFmpegExtractAudio',
        'preferredcodec': 'mp3',
        'preferredquality': '192',
    }],
    'keepvideo': args.keep_video 
}


with yt_dlp.YoutubeDL(ydl_opts) as ydl:
    for link in links:
        try:
            print(f"\nDownloading: {link}")
            ydl.download([link])
        except Exception as e:
            print(f"Failed to download {link}: {e}")



