package com.virtualclone.app.core.domain.hftoken

object HfTokenException {
    object MissingHfTokenException :
        IllegalStateException("Hugging Face token required") {
        private fun readResolve(): Any = MissingHfTokenException
    }

}