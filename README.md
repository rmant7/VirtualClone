# Speech Recognition Server

This project is a simple Flask server that transcribes audio files using the `SpeechRecognition` library. It includes a client script to test sending audio files to the server.

## Requirements

- Python 3.10+
- Flask
- SpeechRecognition
- Requests (for the client script)

## Setup

1. Create a virtual environment (optional but recommended):

```bash
python3 -m venv smallenv_min
source smallenv_min/bin/activate

Install the required packages:

pip install --no-cache-dir flask SpeechRecognition requests
Make sure your audio files are in the uploads/ folder.

Running the server
python3 server_a.py 8080
This starts the server on port 8080.

Running the client test
python3 client_test.py


