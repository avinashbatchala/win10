package com.ab.media

interface MediaActionDispatcher {
    fun play(packageName: String)
    fun pause(packageName: String)
    fun skipNext(packageName: String)
    fun skipPrevious(packageName: String)
    fun seekTo(packageName: String, positionMs: Long)
}
