# AndroidGPT

Android-ассистент: локальная LLM для команд + OpenAI Realtime / Gemini Live для голосового «эксперта» + Telegram-звонки.

## Сборка APK

### В облаке (GitHub Actions)
Каждый push в `main` собирает debug APK автоматически.
APK лежит в **Actions → последний run → Artifacts → `AndroidGPT-debug-apk`**.

Чтобы собрать вручную: вкладка **Actions → Android CI → Run workflow**.

### Локально (Android Studio)
1. Открыть папку проекта в Android Studio Ladybug+.
2. Sync Gradle, дождаться загрузки SDK и зависимостей.
3. Build → Build Bundle/APK → Build APK.

## Структура

```
app/src/main/java/com/androidgpt/
├── core/              DI, шифрованное хранилище, HTTP, permissions
├── features/
│   ├── settings/      API-ключи (OpenAI, Gemini, Telegram, HA)
│   ├── local_llm/     каталог + скачивание + переключение GGUF моделей
│   ├── voice/         Android STT / TTS
│   ├── tools/         alarm, timer, media, smart_home, telegram_call, expert, chat
│   ├── expert/        OpenAI Realtime + Gemini Live (WebSocket, PCM16)
│   ├── assistant/     IntentRouter, Orchestrator
│   ├── telegram/      TDLib scaffold
│   ├── call_ui/       экран Telegram-звонка
│   └── wake_word/     scaffold под Porcupine
└── ui/                Compose тема и навигация
```

## Внешние SDK (опционально)

Раскомментировать в `app/build.gradle.kts`:

- **llama.cpp Android** — для локального inference (GGUF). Стаб в `local_llm/runtime/LlamaEngine.kt`.
- **TDLib** (`org.drinkless:tdlib`) — для реальных Telegram-звонков.
- **Porcupine** — для wake-word.

## Минимальные требования

- minSdk 26 (Android 8.0)
- targetSdk 35 (Android 15)
- JDK 17
