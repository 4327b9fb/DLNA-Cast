package com.android.cast.dlna.demo.http

import android.content.Context
import com.android.cast.dlna.core.Utils

interface HttpServer {
    fun startServer()
    fun stopServer()
    fun isRunning(): Boolean
}

class LocalServer(
    context: Context,
    private val port: Int = 8192,
    httpServer: HttpServer = NanoHttpServer(port),
) : HttpServer by httpServer {
    val ip: String = Utils.getWiFiIpAddress(context)
    val baseUrl: String = "http://$ip:$port"
}