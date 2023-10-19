package io.agora.videoloaderapi.utils

/**
 * @author create by zhangwei03
 */
abstract class RunnableWithDenied : Runnable{
    abstract fun onDenied()
}