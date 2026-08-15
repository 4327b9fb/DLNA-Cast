package com.android.cast.dlna.demo.http

import android.text.TextUtils
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status.BAD_REQUEST
import fi.iki.elonen.NanoHTTPD.Response.Status.NOT_FOUND
import fi.iki.elonen.NanoHTTPD.Response.Status.OK
import fi.iki.elonen.NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

// ------------------------------------------------
// ---- Nano Http
// ------------------------------------------------
internal class NanoHttpServer(port: Int) : NanoHTTPD(port), HttpServer {

    private val mimeTypes = mutableMapOf(
        "jpg" to "image/*",
        "jpeg" to "image/*",
        "png" to "image/*",
        "mp3" to "audio/*",
        "mp4" to "video/*",
        "wav" to "video/*",
    )
    private val textPlain = "text/plain"

    override fun serve(session: IHTTPSession): Response {
        println("uri: " + session.uri)
        println("header: " + session.headers.toString())
        println("params: " + session.parms.toString())
        val uri = session.uri
        if (TextUtils.isEmpty(uri) || !uri.startsWith("/")) {
            return newChunkedResponse(BAD_REQUEST, textPlain, null)
        }
        val file = File(uri)
        if (!file.exists() || file.isDirectory) {
            return newChunkedResponse(NOT_FOUND, textPlain, null)
        }
        val type = uri.substring(uri.length.coerceAtMost(uri.lastIndexOf(".") + 1)).lowercase()
        var mimeType = mimeTypes[type]
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = textPlain
        }
        return try {
            newChunkedResponse(OK, mimeType, FileInputStream(file))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            newChunkedResponse(SERVICE_UNAVAILABLE, mimeType, null)
        }
    }

    override fun startServer() {
        try {
            if (!wasStarted()) start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun stopServer() {
        stop()
    }

    override fun isRunning(): Boolean = wasStarted()
}