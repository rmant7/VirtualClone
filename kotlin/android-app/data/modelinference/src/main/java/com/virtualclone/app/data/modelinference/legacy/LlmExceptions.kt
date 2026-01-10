package com.virtualclone.app.data.modelinference.legacy

class ModelLoadFailException : Exception("Failed to load model, please try again")

class ModelSessionCreateFailException :
    Exception("Failed to create model session, please try again")

class ModelDownloadFailException(msg: String) : Exception(msg)