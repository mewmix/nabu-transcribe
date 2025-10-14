# Project Log

This file documents the steps taken to build the sherpa-onnx Android application.

## Steps Taken

1.  **Project Setup**: Cloned the `sherpa-onnx` repository and created a new Android project named `MySherpaApp` by copying the `SherpaOnnxSpeakerDiarization` example.
2.  **Dependencies**: Added the `sherpa-onnx` AAR as a local dependency. Added the Room and Kotlinx Serialization dependencies to the `build.gradle.kts` file.
3.  **Model Acquisition**: Downloaded the following pre-trained models:
    *   ASR (streaming zipformer)
    *   VAD (silero-vad)
    *   Speaker Segmentation (pyannote)
    *   Speaker Embedding (ecapa-tdnn)
    *   Punctuation
    *   TTS (piper)
4.  **Core Logic**: Implemented the core logic for ASR, VAD, punctuation, diarization, and TTS in the `SherpaOnnxEngine.kt` file.
5.  **UI**: Created a basic UI with Jetpack Compose that includes:
    *   A screen for the main functionality (`HomeScreen`).
    *   A screen for speaker enrollment (`EnrollScreen`).
    *   UI elements for ASR, diarization, punctuation, and TTS.
6.  **Database**: Implemented a Room database to store speaker embeddings, meetings, and turns.
7.  **Speaker Enrollment**: Implemented the speaker enrollment feature, allowing users to record their voice and save it with a name.
8.  **Speaker Identification**: Implemented speaker identification in the diarization process by comparing the speaker embeddings with the enrolled speakers.
9.  **Export**: Implemented the export feature for JSON, SRT, and VTT formats.
10. **Error Handling**: Implemented a centralized error handling and logging utility. Applied it to the core engine, UI screens, and utility functions to improve debugging and robustness.
11. **Voice Transcription Studio**: Implemented the basic framework for the Voice Transcription Studio, including a meeting list screen and a meeting details screen.

## Path to MVP

The current state of the application is a good starting point, but it is not yet a production-ready application. The following steps are needed to reach an MVP:

1.  **UI Polish**: The UI is very basic and needs to be improved. The layout should be cleaned up, and the user experience should be improved.
2.  **Error Handling**: The basic error handling is in place, but the app should provide better feedback to the user when an error occurs (e.g., showing a dialog).
3.  **Tagging per segment**: The user spec mentions tagging of individual turns, but this is not yet implemented.
4.  **Meeting List**: The basic meeting list screen is implemented. It should be improved with search and filtering capabilities.
5.  **Meeting Details**: The basic meeting details screen is implemented. It should be improved with audio playback, editing capabilities, and the ability to add tags.
6.  **Testing**: The application needs to be thoroughly tested to ensure that all the features are working correctly.
