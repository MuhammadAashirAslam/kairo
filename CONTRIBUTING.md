# Contributing to Kairo

Thank you for your interest in contributing to **Kairo**! Kairo is an open-source, 100% on-device Retrieval-Augmented Generation (RAG) assistant running locally on Android.

We welcome contributions from developers, researchers, designers, and documentation writers of all experience levels.

---

## Code of Conduct

All contributors and maintainers are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please treat everyone with respect, kindness, and professional courtesy.

---

## Development Prerequisites

Before building Kairo from source, ensure your environment meets the following specifications:

- **Android Studio**: Android Studio Ladybug (2024.2.1+) or newer
- **JDK**: Java Development Kit 17 or 21 (configured in Android Studio under *Settings > Build Tools > Gradle > Gradle JDK*)
- **Android SDK**:
  - `compileSdk`: 34
  - `minSdk`: 29 (Android 10+)
  - `targetSdk`: 34
- **Build System**: Gradle 8.9+ with Kotlin 2.0.21
- **Physical Test Device (Recommended)**: Android phone with ARM64-v8a architecture and 6GB+ RAM (8GB+ recommended for 1B+ models). While the Android Emulator can be used for UI development, on-device GGUF LLM inference requires ARM64 hardware for real-time tokens/sec performance.

---

## Getting Started

1. **Fork and Clone the Repository**:
   ```bash
   git clone https://github.com/<your-username>/Kairo.git
   cd Kairo
   ```

2. **Open in Android Studio**:
   - Select **File > Open...** and navigate to the cloned `Kairo` directory.
   - Allow Gradle to sync dependencies automatically.

3. **Verify Local Build**:
   Run the test suite from your terminal or Android Studio:
   ```bash
   # Windows PowerShell
   .\gradlew.bat testDebugUnitTest

   # macOS / Linux
   ./gradlew testDebugUnitTest
   ```

4. **Run on Device**:
   - Connect your Android device via USB with **USB Debugging** enabled.
   - Click the green **Run** button in Android Studio, or execute:
     ```bash
     .\gradlew.bat installDebug
     ```

---

## Architecture Overview

Kairo is built following **Clean Architecture** principles and Jetpack Compose:

```
app/src/main/java/com/example/kairo/
├── KairoApp.kt                  # Application singleton & dependency container
├── MainActivity.kt              # Root activity & chat orchestration
├── data/                        # Persistent stores & repositories
│   ├── KairoPreferences.kt      # SharedPreferences configuration engine
│   ├── ChunkStore.kt            # In-memory indexed document chunks
│   ├── ConversationStore.kt     # Chat history persistence
│   ├── DocumentRepository.kt    # PDF/Text ingestion & storage
│   └── ImageOcrRepository.kt    # On-device ML Kit text recognition
├── domain/                      # Pure business logic & algorithms
│   ├── Chunker.kt               # Sliding-window document chunking
│   ├── RagPromptBuilder.kt      # Grounded prompt builder & system personas
│   ├── RetrievalService.kt      # Hybrid retrieval orchestrator
│   ├── parser/                  # Document structure & breadcrumb parsers
│   └── retrieval/               # Okapi BM25 & Reciprocal Rank Fusion (RRF)
├── presentation/                # Jetpack Compose UI components
│   ├── chat/                    # Main chat interface & streaming bubbles
│   ├── components/              # Sidebar drawer, search, attachments
│   ├── settings/                # Configurable hyperparameters & personas
│   └── setup/                   # Model downloader & storage manager
└── ui/theme/                    # Design tokens & dark minimal palettes
```

---

## Configuring Local Settings

Kairo is built so that anyone can configure and run the app according to their requirements:

- **Adding New Models**: Models can be registered in `KairoApp.kt` via `RunAnywhere.models.register(...)`. Any compatible GGUF quant (Q4_K_M, Q8_0, etc.) hosted on Hugging Face can be added.
- **Custom System Personas**: Personas are defined in `KairoPreferences.SystemPersona` and formatted in `RagPromptBuilder.kt`.
- **Inference & RAG Hyperparameters**: Hyperparameters (temperature, top-k chunks, max tokens) can be adjusted dynamically in the in-app **Settings** or programmed via `KairoPreferences.kt`.

---

## Pull Request Guidelines

1. **Create a Feature Branch**:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```

2. **Keep Commits Atomic & Well-Described**:
   - Use conventional commit style (e.g., `feat: add support for custom GGUF model URLs`, `fix: prevent clipboard NPE on empty generation`).

3. **Ensure Tests Pass**:
   - Run `.\gradlew.bat testDebugUnitTest` prior to pushing your branch.
   - Add new unit tests under `app/src/test/` for any new business logic or retrieval algorithms.

4. **Submit Your Pull Request**:
   - Open a PR against the `main` branch.
   - Describe the changes made, the problem solved, and attach screenshots/recordings if modifying the UI.

---

## License

By contributing to Kairo, you agree that your contributions will be licensed under the project's [Apache License 2.0](LICENSE).
