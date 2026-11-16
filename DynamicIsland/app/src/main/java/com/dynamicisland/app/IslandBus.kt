package com.dynamicisland.app

import android.os.Handler
import android.os.Looper

/** Мост между сервисами (уведомления / медиа) и контроллером острова. */
object IslandBus {
    @Volatile
    var controller: IslandController? = null
    private val main = Handler(Looper.getMainLooper())

    fun post(block: IslandController.() -> Unit) {
        main.post { controller?.block() }
    }

    val running get() = controller != null
}
