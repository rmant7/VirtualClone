# VirtualClone

## Setup Instructions

### 1. Prerequisites

- [CUDA Toolkit](https://developer.nvidia.com/cuda-12-8-0-download-archive) and a compatible GPU with minimum ~3.7 GB GPU memory (or modify code to use CPU)
- [Hugging Face token](https://huggingface.co/settings/tokens) for API access

### 2. Setup

- Set Hugging Face Token as an environment variable:

```
set HF_TOKEN=your_token_here
```
- Install required libraries
```
pip install --no-cache-dir -r requirements.txt
```

- Install PyTorch wheels based on your CUDA version (examples):

##### Example (For CUDA 12.8)
```
pip install --no-cache-dir torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu128
```

## Run
```
python flask_app.py
```
