package com.androidgpt.features.settings

enum class ApiKey(val storageKey: String, val title: String, val hint: String) {
    OpenAI("key_openai", "OpenAI API Key", "sk-..."),
    Gemini("key_gemini", "Google AI / Gemini Key", "AIza..."),
    HomeAssistantUrl("key_ha_url", "Home Assistant URL", "http://homeassistant.local:8123"),
    HomeAssistantToken("key_ha_token", "Home Assistant Token", "Long-lived token"),
    TelegramApiId("key_tg_api_id", "Telegram api_id", "12345"),
    TelegramApiHash("key_tg_api_hash", "Telegram api_hash", "abcdef..."),
    CatalogUrl("key_catalog_url", "Удалённый каталог моделей", "https://example.com/catalog.json"),
}

enum class ExpertProvider(val storageKey: String = "expert_provider", val title: String) {
    OpenAi("OpenAI Realtime"),
    Gemini("Google Gemini Live");

    companion object {
        const val PREF_KEY = "expert_provider"
        fun parse(v: String?): ExpertProvider = entries.firstOrNull { it.name == v } ?: OpenAi
    }
}

enum class ExpertVoice(val display: String) {
    Alloy("alloy"), Verse("verse"), Sage("sage"), Coral("coral"), Echo("echo");
    companion object {
        const val PREF_KEY = "expert_voice"
        fun parse(v: String?): ExpertVoice = entries.firstOrNull { it.name == v } ?: Alloy
    }
}
