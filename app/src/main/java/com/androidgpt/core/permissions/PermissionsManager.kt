package com.androidgpt.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionsManager @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    fun missing(permissions: List<String>): List<String> =
        permissions.filterNot { has(it) }

    companion object {
        val MICROPHONE = listOf(Manifest.permission.RECORD_AUDIO)
        val CALL = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
        )
        val NOTIFICATIONS = listOf(Manifest.permission.POST_NOTIFICATIONS)
    }
}
