package com.virtualclone.app.data.modeldownload.repository

import android.content.SharedPreferences
import com.virtualclone.app.core.domain.repository.HfTokenProvider
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class HfTokenProviderImpl @Inject constructor(
    private val prefs: SharedPreferences
) : HfTokenProvider {

    override suspend fun getToken(): String? =
        prefs.getString("hf_token", null)

    override suspend fun saveToken(token: String) {
        prefs.edit { putString("hf_token", token) }
    }
}
