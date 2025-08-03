import os
import yt_dlp
import whisper
import os
os.environ["PATH"] = os.path.expanduser("~/VirtualClone") + ":" + os.environ["PATH"]


transcript_dir = "transcripts"
os.makedirs(transcript_dir, exist_ok=True)

model = whisper.load_model("base")


def download_and_transcribe(video_url):
    print(f"\nProcessing: {video_url}")

    ydl_opts = {
        'format': 'bestaudio/best',
        'outtmpl': 'temp_audio.%(ext)s',
        'quiet': True,
        'postprocessors': [{
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'mp3',
            'preferredquality': '192',
        }],
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        ydl.download([video_url])

    print("Audio downloaded. Transcribing...")
    result = model.transcribe("temp_audio.mp3")

    with yt_dlp.YoutubeDL({'quiet': True}) as ydl:
        info_dict = ydl.extract_info(video_url, download=False)
        title = info_dict.get('title', 'untitled').replace(" ", "_").replace("/", "_")

    filename = os.path.join(transcript_dir, f"{title}.txt")
    with open(filename, "w", encoding="utf-8") as f:
        f.write(result["text"])

    print(f"✅ Transcription saved: {filename}")
    os.remove("temp_audio.mp3")
