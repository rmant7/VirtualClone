import sounddevice as sd
import numpy as np
import queue
from faster_whisper import WhisperModel

print("Loading Whisper model...")

model = WhisperModel("base")
print("✅ Model loaded")

RATE = 16000
CHANNELS = 1
CHUNK = 1024
MIC_INDEX = 0
sd.default.device = MIC_INDEX

audio_queue = queue.Queue()
buffer = np.array([], dtype=np.float32)
last_language = None

def callback(indata, frames, time, status):
    if status:
        print(f"⚠️ Stream status: {status}")
    audio_queue.put(indata.copy())

print("🎤 Live transcription started... Press Ctrl+C to stop")

try:
    with sd.InputStream(samplerate=RATE, channels=CHANNELS, callback=callback, blocksize=CHUNK):
        while True:
            if not audio_queue.empty():

                new_audio = audio_queue.get().flatten()
                buffer = np.concatenate((buffer, new_audio))


                if len(buffer) > RATE * 5:
                    segments, info = model.transcribe(
                        buffer,
                        beam_size=5,
                        language=None,
                        task="transcribe"
                    )


                    if info.language != last_language:
                        print(f"🌐 Detected language: {info.language}")
                        last_language = info.language


                    for segment in segments:
                        text = segment.text.strip()
                        if text:
                            print("📝", text)


                    buffer = buffer[-RATE*2:]

except KeyboardInterrupt:
    print("\n🛑 Stopped by user")
