import os
import whisper

DOWNLOADS_DIR = "downloads"
OUTPUT_DIR = "output"


model = whisper.load_model("base")

def transcribe_audio(file_path):
    print(f"Transcribing {file_path} ...")
    result = model.transcribe(file_path)
    return result['text']

def main():

    os.makedirs(OUTPUT_DIR, exist_ok=True)


    files = [f for f in os.listdir(DOWNLOADS_DIR) if f.endswith(".mp3")]

    for audio_file in files:
        audio_path = os.path.join(DOWNLOADS_DIR, audio_file)
        transcription = transcribe_audio(audio_path)


        txt_file = audio_file.rsplit(".", 1)[0] + ".txt"
        txt_path = os.path.join(OUTPUT_DIR, txt_file)
        with open(txt_path, "w", encoding="utf-8") as f:
            f.write(transcription)
        print(f"Saved transcription to {txt_path}")

if __name__ == "__main__":
    main()



