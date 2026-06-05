# ELARA — AI English Immersion App

## Overview
Android app for English language immersion via AI conversation. Built with Kotlin, MVVM + Clean Architecture. Uses OpenRouter for chat, Gemini for vision, TTS/STT for voice.

## Tech Stack
- **Language:** Kotlin 2.0.21, Java 17 target
- **UI:** XML layouts, ViewBinding, Material Components, ConstraintLayout
- **Architecture:** MVVM + Clean Architecture (data/domain/ui)
- **DB:** Room (9 entities, destructive migration)
- **Network:** Retrofit + OkHttp + Gson
- **Async:** Kotlin Coroutines, StateFlow
- **Build:** Gradle KTS, AGP 8.7.3, KSP
- **Min SDK:** 26, Target/Compile SDK: 35

## API Keys (in local.properties)
```
OPENROUTER_API_KEY=sk-or-...
GEMINI_API_KEY=AIza...
```
- OpenRouter → DeepSeek Flash (chat, grammar check, memory)
- Gemini → Flash 2.5 (photo description + multimodal chat)
- MyMemory → free translation API (word tap)

## Project Structure
```
com.elara.app/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          — Room DB v9
│   │   ├── entity/                  — 7 entities
│   │   └── dao/                     — 5 DAOs
│   └── remote/
│       ├── OpenRouterApi.kt        — Retrofit interface
│       ├── OpenRouterClient.kt     — Singleton
│       ├── GeminiClient.kt         — Gemini Vision API
│       ├── TranslatorClient.kt     — MyMemory API
│       └── drive/                  — Google Drive backup
├── domain/
│   ├── model/                      — Character, World, Personality, Scenario, ConversationTurn
│   └── usecase/                    — BuildPrompt, CalibrateLevel, UpdateMemory, GenerateExercises
├── ui/
│   ├── main/                       — MainActivity, MessageAdapter, ChatSettings, VoiceManager
│   ├── conversation/               — ConversationViewModel
│   ├── learning/                   — LearningPathActivity (24→36 scenarios A1-C2)
│   ├── reading/                    — ReadingActivity (paste text, tappable words, TTS)
│   ├── errors/                     — ReviewErrorsActivity (error bank + AI exercises)
│   ├── words/                      — WordsActivity, ReviewActivity (SRS flashcards)
│   ├── history/                    — HistoryActivity (past sessions)
│   ├── stats/                      — StatsActivity (dashboard)
│   ├── character/                  — CreateCharacterActivity
│   ├── onboarding/                 — OnboardingActivity (3 welcome screens)
│   ├── camera/                     — ObjectDetectorHelper (unused, replaced by Gemini)
│   └── notifications/              — DailyReminder, DailyWord, BootReceiver, NotificationHelper
└── res/
    ├── layout/                     — 14 screen/item layouts
    ├── drawable/                   — bubbles, avatars, backgrounds, stat cards
    └── values/                     — themes.xml
```

## Current Features (86 features total)
- **6 worlds:** Modern, Medieval, Sci-Fi, Western, Beach, Cyberpunk
- **5 characters:** Elara, Stacy, Layla, Tyler, Lady Raven (each with custom voice pitch/rate)
- **4 personalities:** Friendly, Submissive, Dominatrix, Yandere
- **36 scenarios** A1→C2 with system prompt, vocab, objectives
- **Photo mode:** camera → Gemini Flash → character reacts to image
- **Grammar check:** auto-correction + natural phrasing suggestions
- **Error bank:** auto-saves mistakes, AI generates exercises
- **SRS vocabulary:** save words, spaced repetition flashcards, daily word notification
- **TTS/STT:** push-to-talk, dictation, auto-speak with dedup
- **Immersive mode:** fullscreen overlay with pulse animation
- **Call mode:** screen-off audio-only with keep-screen-on
- **Reading mode:** paste text, tappable words with translation
- **Daily plan card:** shows pending reviews, errors, scenarios
- **Dashboard:** stats with streak, words, errors, scenarios, CEFR level
- **Google Drive sync:** backup/restore words and character memory
- **Session persistence:** history per world+personality+character combo
- **Session memory:** AI extracts facts every 8 messages + summarizes every 25
- **Streaks & timer:** daily streak counter, session timer
- **Onboarding:** 3-screen welcome tutorial
- **Custom characters:** create and manage custom AI personas
- **Topics:** suggested questions by world context

## Design System
- **Background:** #080616 (deep night violet)
- **Surfaces:** #0D0A1C, #13102A, #14102E
- **Primary accent:** #7C3AED / #A78BFA (violet)
- **Secondary:** #F472B6 (pink), #4ADE80 (green), #FBBF24 (amber), #38BDF8 (sky)
- **Text:** #F5F3FF (primary), #8B88A8 (muted), #5B5678 (subtle)
- **Borders:** #161232, #1E1A3A

## Key Constraints
- `fallbackToDestructiveMigration()` — DB version bumps destroy data
- Never commit local.properties with API keys
- AndroidManifest: INTERNET, CAMERA, RECORD_AUDIO, POST_NOTIFICATIONS permissions
- Portrait-only orientation
- All Activities use ViewBinding, no Fragments, no Navigation Component
- DI is manual singletons (no Hilt/Dagger)
