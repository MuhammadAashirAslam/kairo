# 🌐 Kairo — Open-Source On-Device RAG & Consultation Assistant for Android

[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B%20(API%2029%2B)-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose%20M3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Inference](https://img.shields.io/badge/Inference-llama.cpp%20(RunAnywhere)-orange.svg)](https://github.com/ggerganov/llama.cpp)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-brightgreen.svg)](CONTRIBUTING.md)

**Kairo** is an open-source, modular, and privacy-first Android application designed for local Retrieval-Augmented Generation (RAG), clinical consultations, document synthesis, and conversational coding. 

Running 100% on-device with zero telemetry and zero cloud API keys, Kairo executes quantized Large Language Models (GGUF format via `llama.cpp`) and an on-device hybrid retrieval engine (BM25 + Dense Embeddings) directly on mobile hardware.

---

## ✨ Features & Capabilities

- **🔒 100% Offline & Private**: Zero data leaves your device. Model weights, vector stores, inverted indexes, chat history, and document extractions remain sandboxed in internal storage.
- **🩺 Multi-Persona Reasoning & Consultation Harness**:
  - **Clinical / Doctor Consultation**: Tailored for healthcare providers to summarize clinical notes, review symptom timelines, and cross-examine patient records with structured grounding.
  - **Code & Engineering**: Optimized for software developers with complete code generation, syntax highlighting, and architectural reasoning.
  - **Document Analyst**: Deep academic, legal, and report synthesis with citation fidelity.
  - **General Assistant**: Everyday balanced conversational reasoning.
  - **Custom System Instructions**: Define your own domain-specific persona, formatting guidelines, and behavioral rules.
- **⚡ High-Throughput On-Device Inference**:
  - **SmolLM2 360M Instruct (Q4_K_M)**: ~12–18 tokens/second on mid-range ARM64 hardware with ultra-light battery consumption (~270 MB RAM).
  - **SmolLM2 360M Instruct (Q8_0)**: High-precision 8-bit quantization for sensitive text analysis (~388 MB RAM).
  - **Llama 3.2 1B Instruct (Q4_K_M)**: Meta's 1B model delivering complex instruction-following and grounded answers (~747 MB RAM).
  - **Qwen 2.5 Coder 1.5B Instruct (Q4_K_M)**: High-tier coding and mathematical reasoning (~1.1 GB RAM).
- **🧠 Production-Grade Hybrid RAG Engine**:
  - **Hierarchical Breadcrumb Chunking**: Preserves structural document context (`[Source 1 | Chapter 2 > Section 3]`).
  - **Okapi BM25 Sparse Search**: Zero-allocation inverted index for exact keywords, medical terms, and code symbols.
  - **Dense Vector Embeddings**: Cosine similarity against semantic representations.
  - **Reciprocal Rank Fusion (RRF)**: Mathematically merges lexical and vector rankings ($k = 60$).
  - **Maximal Marginal Relevance (MMR)**: Suppresses redundant chunks to maximize context window diversity ($\lambda = 0.75$).
- **📄 Native Multimodal Ingestion**:
  - **PDF Documents**: Page-aware extraction with section boundaries using `pdfbox-android`.
  - **Images & Scans**: On-device Optical Character Recognition (OCR) via Google ML Kit.
  - **Plain Text & Markdown**: Native UTF-8 streaming parser.
- **🎛️ Fully User-Configurable**:
  - Persistent user profile and customizable role/specialty.
  - Real-time temperature slider (`0.00` deterministic to `1.00` creative).
  - Response length limit (`256`, `512`, `1024`, `2048`, `4096` tokens).
  - Configurable Top-K retrieval chunks ($k = 1, 2, 3, 5, 8$) and similarity cutoff threshold.
- **💎 Refined Dark Interface**: Clean charcoal surfaces (`#212121`, `#171717`, `#2F2F2F`) inspired by modern minimalist design with smooth streaming auto-scroll, code copy buttons, and expandable citations.

---

## 🏗️ Architecture

```
                                  User Input / Document
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   PRESENTATION                                         │
│   MainActivity ──► ChatScreen ──► MarkdownRenderer (Click-to-Copy, Math, Syntax)        │
│   KairoSidebarDrawer (Persistent Profile, Sessions, Pinning)                           │
│   SettingsScreen (Hyperparameters, Personas, Storage, Grounding)                       │
│   ModelSetupScreen (GGUF Downloads, Storage Stats, Real-time Validation)               │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                      DOMAIN                                            │
│   DocumentRepository (PDFBox / ML Kit OCR)                                             │
│       └── DocumentStructureParser (Heading & Breadcrumb Hierarchy)                     │
│               └── Chunker (Sliding-window with context retention)                      │
│                                                                                        │
│   RetrievalService                                                                     │
│       ├── Bm25SearchEngine (Sparse Lexical Search)                                    │
│       ├── EmbeddingService (Dense Neural Embeddings)                                  │
│       ├── ReciprocalRankFusion (Mathematical Multi-List RRF)                           │
│       └── MaximalMarginalRelevance (MMR Diversity Optimization)                        │
│                                                                                        │
│   RagPromptBuilder (Persona Injection: Clinical, Dev, Analyst, Custom)                 │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                       DATA                                             │
│   KairoPreferences (Persistent SharedPreferences Configuration Engine)                 │
│   ConversationStore (Session History, Pinned Chats, Timestamps)                        │
│   ChunkStore (In-Memory Inverted Index & Chunk Embeddings)                             │
│   RunAnywhere SDK + LlamaCPP (Native GGUF Inference Engine)                            │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start: Running Kairo

### 1. Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer.
- **JDK**: Java 17 or 21.
- **Android Device**: Physical device with ARM64-v8a processor and 6GB+ RAM recommended (Android 10+, API 29+).

### 2. Clone and Open

```bash
git clone https://github.com/<your-username>/Kairo.git
cd Kairo
```

Open the project folder in Android Studio and let Gradle sync.

### 3. Build & Run from Command Line

```bash
# Windows
.\gradlew.bat assembleDebug

# Install directly to connected device:
adb install -r app/build/outputs/apk/debug/app-debug.apk

# macOS / Linux
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚙️ Configuring Kairo for Your Use Case

Anyone can configure Kairo to match their device capabilities or clinical/domain requirements:

### A. Adjusting Settings in App
1. Open the sidebar drawer and tap the **Profile Card** or the **Settings** icon.
2. **Profile & Identity**: Set your display name (e.g. *Dr. Alex Carter*) and role (*Cardiology Fellow*).
3. **Workflow Persona**:
   - Choose **Clinical / Consultation** to evaluate patient reports with grounded symptom analysis.
   - Choose **Code & Engineering** for technical software questions.
   - Choose **Custom Instructions** to paste your own system prompt.
4. **Inference Parameters**:
   - Set **Temperature** to `0.10` for strict, deterministic medical/legal extraction.
   - Set **Max Output Tokens** to `1024` or `2048` to prevent long responses from cutting off.
   - Adjust **Top-K Chunks** ($k=1$ to $k=8$) depending on how much reference context your document requires.

### B. Registering Additional GGUF Models in Code
You can add any compatible GGUF model hosted on Hugging Face or locally by editing `KairoApp.kt`:

```kotlin
RunAnywhere.models.register(
    ModelRegistration.url(
        id = "my-custom-model",
        name = "My Custom Model (Q4_K_M)",
        url = "https://huggingface.co/path/to/model-Q4_K_M.gguf",
        framework = InferenceFramework.INFERENCE_FRAMEWORK_LLAMA_CPP,
        memoryBytes = 600_000_000L,
        downloadBytes = 550_000_000L,
    )
)
```

---

## 🧪 Testing

Kairo includes an automated test suite verifying parser accuracy, BM25 scoring, Reciprocal Rank Fusion, sliding-window chunking, and prompt persona builders:

```bash
# Run all unit tests
.\gradlew.bat testDebugUnitTest
```

---

## 🤝 Contributing

Contributions are warmly welcomed! Please read our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) before submitting pull requests.

---

## 📄 License

Kairo is released under the [Apache License, Version 2.0](LICENSE). You are free to use, modify, distribute, and commercialize this software according to the terms of the Apache 2.0 license.
