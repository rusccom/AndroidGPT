package com.androidgpt.features.local_llm.storage

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveModelPrefs @Inject constructor(@ApplicationContext ctx: Context) {

    private val prefs: SharedPreferences = ctx.getSharedPreferences("active_model", Context.MODE_PRIVATE)
    private val _activeId = MutableStateFlow(prefs.getString(KEY, null))
    val activeId: StateFlow<String?> = _activeId

    fun set(id: String?) {
        prefs.edit().apply { if (id == null) remove(KEY) else putString(KEY, id) }.apply()
        _activeId.value = id
    }

    private companion object { const val KEY = "active_id" }
}
