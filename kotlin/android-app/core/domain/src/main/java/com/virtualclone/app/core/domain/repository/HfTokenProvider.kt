package com.virtualclone.app.core.domain.repository

interface HfTokenProvider {
    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
}