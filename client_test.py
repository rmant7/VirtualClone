import requests
import os


file_path = "/home/isra1elmugrabi/uploads/harvard.wav"


url = "https://isra1elmugrabi.pythonanywhere.com/transcribe"


if not os.path.exists(file_path):
    print(f"File not found: {file_path}")
else:
    try:
        with open(file_path, "rb") as f:
            files = {"file": f}
            response = requests.post(url, files=files)


        print("Raw response text:", response.text)


        response.raise_for_status()


        data = response.json()
        if "transcription" in data:
            print("Transcription received:")
            print(data["transcription"])
        else:
            print("Server response (no transcription):", data)

    except requests.exceptions.RequestException as e:
        print("An error occurred while making the request:", e)
    except Exception as e:
        print("An unexpected error occurred:", e)



