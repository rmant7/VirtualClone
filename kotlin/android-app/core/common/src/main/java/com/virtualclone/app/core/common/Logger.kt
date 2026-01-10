package com.virtualclone.app.core.common

import android.util.Log

/**
 * Simple logging utility for the app.
 */
object Logger {
    private const val TAG = "VirtualCloneLogCat"
    
    fun d(message: String, tag: String = TAG) {
        Log.d(tag, message)
    }
    
    fun i(message: String, tag: String = TAG) {
        Log.i(tag, message)
    }
    
    fun w(message: String, tag: String = TAG) {
        Log.w(tag, message)
    }
    
    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        Log.e(tag, message, throwable)
    }
    
    fun v(message: String, tag: String = TAG) {
        Log.v(tag, message)
    }
}
