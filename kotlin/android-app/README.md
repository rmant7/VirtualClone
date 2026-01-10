# VirtualClone

A production-grade Android app that runs a Small Language Model (SLM) entirely on-device using ONNX
Runtime, with PDF import capabilities and RAG (Retrieval-Augmented Generation) for enhanced
conversations.

## 🚀 Quick Start

1. Clone this repository
2. Download and place the ONNX model file as described above
3. Open `android` folder in Android Studio
4. Build and run the project

### Prerequisites:

- **Git LFS** (for downloading the model file)
- **Android Studio** with JDK 17
- **Android SDK 34**

### Installation:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/KralAthena/virtualclone.git
   cd virtualclone
   ```

2. **Install Git LFS** (if not already installed):
   ```bash
   git lfs install
   ```

3. **Pull the model file:**
   ```bash
   git lfs pull
   ```

4. **Open in `android` folder Android Studio** and build the project

### ⚠️ If Git LFS Issues:

If you encounter problems with Git LFS, you can manually download the model:

// TODO: Only download models on demand

1. **Download DistilGPT-2 ONNX model** from Hugging Face  
   - Place it in: `android/app/src/main/assets/models/distilgpt2.onnx`  
   - Verify tokenizer files are present in: `android/app/src/main/assets/tokenizer/`

2. **Download Gemma3-1B-IT model** (`Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task`)  
   - Download from: [Hugging Face – litert-community/Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT/tree/main)  
   - Place it in: `android/app/src/main/assets/models/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task`


## Features

- **On-Device AI**: Runs a small language model locally using ONNX Runtime
- **PDF Analysis**: Import PDF documents and ask questions about their content
- **RAG Pipeline**: Retrieval-Augmented Generation using Room FTS for context-aware responses
- **Material 3 UI**: Modern, polished interface with animations and dark/light themes
- **Privacy-First**: All processing happens locally - no data leaves your device
- **Offline Capable**: Works completely offline after initial setup

## Build Requirements

- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 34
- Minimum SDK 26 (Android 8.0)

