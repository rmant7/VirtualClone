import whisper
import argparse
import sys
from pathlib import Path

def validate_audio_file(audio_path):
    path = Path(audio_path)
    if not path.exists():
        raise FileNotFoundError(f"Audio file not found: {audio_path}")
    if not path.is_file():
        raise ValueError(f"Path is not a file: {audio_path}")
    return path

def transcribe_audio(audio_path, model_type="base"):
    try:
        print(f"Loading {model_type} model...")
        model = whisper.load_model(model_type)
        print(f"Processing {audio_path}...")
        result = model.transcribe(str(audio_path))
        return result["text"]
    except Exception as e:
        print(f"Error during transcription: {str(e)}", file=sys.stderr)
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="Audio Transcription using Whisper")
    parser.add_argument("audio_file", help="Path to audio file")
    parser.add_argument("--model", default="base", help="Whisper model size")
    args = parser.parse_args()

    try:
        audio_path = validate_audio_file(args.audio_file)
    except Exception as e:
        print(f"Error: {str(e)}", file=sys.stderr)
        sys.exit(1)

    transcription = transcribe_audio(audio_path, args.model)
    print("\nTranscription Result:")
    print(transcription)

if __name__ == "__main__":
    main()