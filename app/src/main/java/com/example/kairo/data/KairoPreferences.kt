package com.example.kairo.data

import android.content.Context
import android.content.SharedPreferences

enum class SystemPersona(val displayName: String, val description: String) {
    GENERAL(
        "General Assistant",
        "Balanced on-device reasoning and privacy-first responses for everyday queries."
    ),
    CLINICAL(
        "Clinical / Consultation",
        "Medical consultation harness for clinical case review, symptom analysis, patient records, and objective summaries."
    ),
    CODING(
        "Code & Engineering",
        "Software engineering assistant specialized in robust code solutions, syntax, and debugging."
    ),
    RESEARCH(
        "Document Analyst",
        "Deep analytical synthesis and fact extraction grounded strictly in cited sources."
    ),
    CUSTOM(
        "Custom Instructions",
        "User-defined custom system instructions tailored to specific domain needs."
    );

    companion object {
        fun fromName(name: String): SystemPersona {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) }
                ?: GENERAL
        }
    }
}

class KairoPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
        set(value) = prefs.edit().putString(KEY_USER_NAME, value.trim()).apply()

    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, DEFAULT_USER_ROLE) ?: DEFAULT_USER_ROLE
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value.trim()).apply()

    var selectedModelId: String
        get() = prefs.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_SELECTED_MODEL, value).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value.coerceIn(0.0f, 1.0f)).apply()

    var maxOutputTokens: Int
        get() = prefs.getInt(KEY_MAX_OUTPUT_TOKENS, DEFAULT_MAX_OUTPUT_TOKENS)
        set(value) = prefs.edit().putInt(KEY_MAX_OUTPUT_TOKENS, value.coerceIn(128, 4096)).apply()

    var topKRetrieval: Int
        get() = prefs.getInt(KEY_TOP_K, DEFAULT_TOP_K)
        set(value) = prefs.edit().putInt(KEY_TOP_K, value.coerceIn(1, 10)).apply()

    var similarityThreshold: Float
        get() = prefs.getFloat(KEY_SIMILARITY_THRESHOLD, DEFAULT_SIMILARITY_THRESHOLD)
        set(value) = prefs.edit().putFloat(KEY_SIMILARITY_THRESHOLD, value.coerceIn(0.0f, 1.0f)).apply()

    var systemPersona: SystemPersona
        get() {
            val stored = prefs.getString(KEY_SYSTEM_PERSONA, SystemPersona.GENERAL.name) ?: SystemPersona.GENERAL.name
            return SystemPersona.fromName(stored)
        }
        set(value) = prefs.edit().putString(KEY_SYSTEM_PERSONA, value.name).apply()

    var customSystemPrompt: String
        get() = prefs.getString(KEY_CUSTOM_SYSTEM_PROMPT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_SYSTEM_PROMPT, value).apply()

    fun getUserInitials(): String {
        val name = userName.trim()
        if (name.isBlank()) return "U"
        val parts = name.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts[0].length >= 2 -> parts[0].take(2).uppercase()
            else -> parts[0].take(1).uppercase()
        }
    }

    companion object {
        private const val PREFS_NAME = "kairo_user_preferences"

        private const val KEY_USER_NAME = "pref_user_name"
        private const val KEY_USER_ROLE = "pref_user_role"
        private const val KEY_SELECTED_MODEL = "pref_selected_model"
        private const val KEY_TEMPERATURE = "pref_temperature"
        private const val KEY_MAX_OUTPUT_TOKENS = "pref_max_output_tokens"
        private const val KEY_TOP_K = "pref_top_k"
        private const val KEY_SIMILARITY_THRESHOLD = "pref_similarity_threshold"
        private const val KEY_SYSTEM_PERSONA = "pref_system_persona"
        private const val KEY_CUSTOM_SYSTEM_PROMPT = "pref_custom_system_prompt"

        const val DEFAULT_USER_NAME = "User"
        const val DEFAULT_USER_ROLE = "AI Explorer"
        const val DEFAULT_MODEL = "smollm2-360m-instruct-q4_k_m"
        const val DEFAULT_TEMPERATURE = 0.3f
        const val DEFAULT_MAX_OUTPUT_TOKENS = 1024
        const val DEFAULT_TOP_K = 3
        const val DEFAULT_SIMILARITY_THRESHOLD = 0.65f
    }
}
