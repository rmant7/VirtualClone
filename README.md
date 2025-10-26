# VirtualClone

AI-powered chat system with audio transcription capabilities. Built with Flask, Whisper AI, and transformer models.

[![Python 3.11+](https://img.shields.io/badge/python-3.11+-blue.svg)](https://www.python.org/downloads/)
[![Flask](https://img.shields.io/badge/Flask-3.0+-green.svg)](https://flask.palletsprojects.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## Features

### AI Chat System
- Context-aware conversations with intelligent response diversity
- Anti-repetition system using LRU caching
- Conversation history tracking (last 10 exchanges)
- Multi-turn dialogue support

### Audio Transcription
- Real-time audio processing with Whisper AI
- Support for multiple video formats (MP4, MOV, AVI, MKV)
- Automatic audio extraction from video files
- Batch processing capabilities

### Multilingual Support
- Auto-translation using NLLB-200 model
- Support for multiple languages
- Seamless language switching

### Document Processing
- Video file upload and transcription
- Automatic text extraction
- Training data generation (JSONL format)

### Link Processing
- YouTube video transcription
- Playlist processing (up to 5 videos by default)
- Batch URL processing
- Automatic audio download and conversion


## Quick Start

### Prerequisites
- Python 3.11 or higher
- ffmpeg (for audio/video processing)
- git

### Installation

```bash
# Clone the repository
git clone https://github.com/rmant7/VirtualClone
cd VirtualClone

git checkout main

python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Setup environment configuration
cp .env.example .env

# Generate a secure secret key
python -c "import secrets; print(secrets.token_hex(32))"
# Copy the output and paste into .env as FLASK_SECRET_KEY

# Run the application
python run.py
```

Access the application at: `http://localhost:5050`


## Configuration

### Environment Variables

Create a `.env` file in the project root (copy from `.env.example`):

```bash
# Flask Configuration
FLASK_SECRET_KEY=your-secret-key-here-change-this  # REQUIRED
FLASK_ENV=development
FLASK_DEBUG=True
FLASK_HOST=0.0.0.0
FLASK_PORT=5050

# Upload Configuration
MAX_CONTENT_LENGTH=31457280  
UPLOAD_FOLDER=uploads
DOWNLOADS_FOLDER=downloads
ALLOWED_EXTENSIONS=mp4,mov,avi,mkv
MAX_VIDEOS=5

# AI Model Configuration
AI_MODEL_TRANSLATION=facebook/nllb-200-distilled-600M
AI_MODEL_QA=deepset/roberta-base-squad2
WHISPER_MODEL=tiny

# Session Configuration
SESSION_TYPE=filesystem
SESSION_PERMANENT=False
SESSION_USE_SIGNER=True
PERMANENT_SESSION_LIFETIME=3600  

# Logging
LOG_LEVEL=INFO
LOG_FILE=app.log
```

### Configuration Options Explained

| Variable | Description | Default |
|----------|-------------|---------|
| `FLASK_SECRET_KEY` | Secret key for session encryption | None (REQUIRED) |
| `FLASK_DEBUG` | Enable debug mode | False |
| `FLASK_PORT` | Port to run server on | 5050 |
| `MAX_CONTENT_LENGTH` | Max upload size in bytes | 31457280 (30MB) |
| `MAX_VIDEOS` | Max videos in playlist | 5 |
| `WHISPER_MODEL` | Whisper model size (tiny/base/small) | tiny |
| `LOG_LEVEL` | Logging level (DEBUG/INFO/WARNING/ERROR) | INFO |


## Usage

### Chat Interface

Access the main chat interface at `http://localhost:5050`

**Features:**
- Ask questions about the loaded context
- Select different languages for interaction
- View conversation history
- Automatic response diversity to prevent repetition

### Audio Transcription

#### Real-time Transcription
```bash
python realtime_transcription.py
```

#### Document Upload
1. Navigate to `http://localhost:5050/upload`
2. Upload a video file (MP4, MOV, AVI, MKV)
3. Wait for processing
4. View the transcription

### Link Processing

#### Single Video
1. Navigate to `http://localhost:5050/links/submit-link`
2. Select "Single Link"
3. Paste YouTube URL
4. Submit and wait for transcription

#### Playlist
1. Select "Playlist" option
2. Paste YouTube playlist URL
3. System will process up to MAX_VIDEOS (default: 5)

#### Batch Processing
1. Select "Batch" option
2. Enter multiple URLs (one per line)
3. Submit for sequential processing

## Testing

### Run Tests

```bash
# Install test dependencies (if not already installed)
pip install pytest pytest-flask pytest-cov

# Run all tests
pytest

# Run with verbose output
pytest -v

# Run with coverage report
pytest --cov=app tests/

# Run specific test file
pytest tests/test_config.py -v
```

### Available Tests

- `tests/test_config.py` - Configuration tests
- `tests/test_context_loader.py` - Context loading tests
- `tests/test_app.py` - Flask application tests

### Manual Tests

```bash
# Test conversation context
python test_conversation_context.py

# Test conversation flow (takes ~3 minutes due to model loading)
python test_conversation_demo.py
```


## Development

### Project Structure

### Code Quality

```bash
# Format code with black
black app/ tests/

# Lint with flake8
flake8 app/ tests/

# Static analysis with pylint
pylint app/
```

## Troubleshooting

### Common Issues

#### Application won't start
```bash
# Check if port is already in use
lsof -i :5050

# Try a different port
export FLASK_PORT=5051
python run.py
```

#### Secret Key Error
```bash
# Generate a new secret key
python -c "import secrets; print(secrets.token_hex(32))"

# Add to .env file
echo "FLASK_SECRET_KEY=<generated-key>" >> .env
```

#### Module Not Found
```bash
# Reinstall dependencies
pip install -r requirements.txt --upgrade
```

#### Context File Not Found
```bash
# Check if llm-script.txt exists
ls -la llm-script.txt

# Create an empty file if needed
touch llm-script.txt
```

#### ffmpeg Not Found
```bash
# Ubuntu/Debian
sudo apt install ffmpeg

# MacOS
brew install ffmpeg

# Fedora
sudo dnf install ffmpeg
```

## API Endpoints

### Main Routes
- `GET /` - Chat interface
- `POST /` - Submit question (JSON: `{user_input, language}`)
- `GET /hello` - Health check
- `GET /reset` - Clear session

### Upload Routes
- `GET /upload/` - Upload form
- `POST /upload/` - Upload video file

### Links Routes
- `GET /links/submit-link` - Link form
- `POST /links/submit-link` - Process links


## Performance Notes

- **Model Loading**: First request may take 20-30 seconds as models load
- **Subsequent Requests**: <1 second with cached models
- **Memory Usage**: ~2-3GB RAM with models loaded
- **Recommended**: Use GPU for faster inference (CUDA support)


## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow PEP 8 style guide
- Write tests for new features
- Update documentation
- Use type hints where possible
- Add logging for important operations
