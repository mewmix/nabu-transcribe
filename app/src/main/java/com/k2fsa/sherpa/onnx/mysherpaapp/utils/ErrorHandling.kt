package com.k2fsa.sherpa.onnx.mysherpaapp.utils

import android.util.Log

object AppLog {
    private const val TAG = "MySherpaApp"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}

inline fun <T> withDebugLogging(block: () -> T): T {
    val caller = Thread.currentThread().stackTrace[3]
    val callerClassName = caller.className.substringAfterLast('.')
    val callerMethodName = caller.methodName
    AppLog.d("Entering $callerClassName.$callerMethodName")
    try {
        val result = block()
        AppLog.d("Exiting $callerClassName.$callerMethodName successfully")
        return result
    } catch (e: Exception) {
        AppLog.e("Exception in $callerClassName.$callerMethodName", e)
        throw e // re-throw the exception to be handled by the caller
    }
}
