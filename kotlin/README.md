# Android (Kotlin)

This directory contains a **native Android client implementation** for VirtualClone,
built using **Kotlin** and **Jetpack Compose**.

This Android app is a **separate, on-device client** and does not replace the
existing Flask-based backend implementation in this repository.

## Overview
- Native Android application (Kotlin-first)
- Jetpack Compose UI
- Clean Architecture
- Offline-first design
- On-device AI inference using MediaPipe LLM Inference

The app focuses on **local, offline model execution on Android devices** and explores
a different architectural direction compared to the server-based Flask pipeline.

## Location
kotlin/android-app/

The entire Android project is self-contained inside the `android-app` directory.

