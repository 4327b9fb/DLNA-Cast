package com.android.cast.dlna.core.didl

import org.jupnp.support.contentdirectory.DIDLParser
import org.jupnp.support.model.DIDLContent
import org.jupnp.support.model.ProtocolInfo
import org.jupnp.support.model.Res
import org.jupnp.support.model.container.Container
import org.jupnp.support.model.item.AudioItem
import org.jupnp.support.model.item.ImageItem
import org.jupnp.support.model.item.Item
import org.jupnp.support.model.item.VideoItem

/**
 * DIDL-Lite builder using jupnp model classes.
 *
 * Provides type-safe DIDL XML generation for both DMC (casting metadata)
 * and DMS (content directory browse/search results).
 *
 * All methods use [DIDLParser].generate() which handles proper XML escaping.
 */
object DIDLBuilder {

    // ---- DMC: single-item metadata for SetAVTransportURI / SetNextAVTransportURI ----

    /**
     * Build DIDL metadata for a video item.
     *
     * @param uri    Media URL
     * @param title  Display title
     * @param mimeType  MIME type, defaults to "video/mp4"
     * @param duration  Duration in HH:MM:SS format (optional)
     * @param size      File size in bytes (optional, 0 = omit)
     */
    @JvmStatic
    @JvmOverloads
    fun buildVideo(
        uri: String,
        title: String,
        mimeType: String = "video/mp4",
        duration: String? = null,
        size: Long = 0
    ): String {
        val res = createRes(mimeType, uri, duration, size)
        val didl = DIDLContent()
        didl.addItem(VideoItem("0", "-1", title, "", res))
        return DIDLParser().generate(didl)
    }

    /**
     * Build DIDL metadata for an audio item.
     *
     * @param uri    Media URL
     * @param title  Display title
     * @param artist Artist name (optional)
     * @param mimeType  MIME type, defaults to "audio/mpeg"
     * @param duration  Duration in HH:MM:SS format (optional)
     * @param size      File size in bytes (optional, 0 = omit)
     */
    @JvmStatic
    @JvmOverloads
    fun buildAudio(
        uri: String,
        title: String,
        artist: String = "",
        mimeType: String = "audio/mpeg",
        duration: String? = null,
        size: Long = 0
    ): String {
        val res = createRes(mimeType, uri, duration, size)
        val didl = DIDLContent()
        didl.addItem(AudioItem("0", "-1", title, artist, res))
        return DIDLParser().generate(didl)
    }

    /**
     * Build DIDL metadata for an image item.
     *
     * @param uri    Media URL
     * @param title  Display title
     * @param mimeType  MIME type, defaults to "image/jpeg"
     * @param size      File size in bytes (optional, 0 = omit)
     */
    @JvmStatic
    @JvmOverloads
    fun buildImage(
        uri: String,
        title: String,
        mimeType: String = "image/jpeg",
        size: Long = 0
    ): String {
        val res = createRes(mimeType, uri, null, size)
        val didl = DIDLContent()
        didl.addItem(ImageItem("0", "-1", title, "", res))
        return DIDLParser().generate(didl)
    }

    /**
     * Build DIDL metadata auto-detecting item type from MIME type.
     *
     * Falls back to [buildVideo] if MIME type is unrecognized.
     */
    @JvmStatic
    @JvmOverloads
    fun build(
        uri: String,
        title: String,
        mimeType: String = "video/mp4",
        duration: String? = null,
        size: Long = 0
    ): String {
        return when {
            mimeType.startsWith("video/") -> buildVideo(uri, title, mimeType, duration, size)
            mimeType.startsWith("audio/") -> buildAudio(uri, title, mimeType = mimeType, duration = duration, size = size)
            mimeType.startsWith("image/") -> buildImage(uri, title, mimeType, size)
            else -> buildVideo(uri, title, mimeType, duration, size)
        }
    }

    // ---- DMS: multi-item content directory results ----

    /**
     * Build DIDL XML for a list of jupnp [Item]s (browse/search results).
     */
    @JvmStatic
    fun buildItems(items: List<Item>): String {
        val didl = DIDLContent()
        items.forEach { didl.addItem(it) }
        return DIDLParser().generate(didl)
    }

    /**
     * Build DIDL XML for a list of jupnp [Container]s.
     */
    @JvmStatic
    fun buildContainers(containers: List<Container>): String {
        val didl = DIDLContent()
        containers.forEach { didl.addContainer(it) }
        return DIDLParser().generate(didl)
    }

    /**
     * Build DIDL XML for mixed items and containers.
     */
    @JvmStatic
    fun buildMixed(items: List<Item>, containers: List<Container>): String {
        val didl = DIDLContent()
        items.forEach { didl.addItem(it) }
        containers.forEach { didl.addContainer(it) }
        return DIDLParser().generate(didl)
    }

    /**
     * Build an empty DIDL result.
     */
    @JvmStatic
    fun buildEmpty(): String {
        return DIDLParser().generate(DIDLContent())
    }

    // ---- Item creation helpers (for DMS) ----

    /**
     * Create the correct jupnp [Item] subclass based on MIME type.
     *
     * @param id        Item ID
     * @param parentId  Parent container ID
     * @param title     Display title
     * @param creator   Creator/artist
     * @param mimeType  MIME type used to determine item subclass
     * @param uri       Resource URI
     * @param size      File size in bytes (optional)
     * @param duration  Duration in HH:MM:SS (optional)
     */
    @JvmStatic
    @JvmOverloads
    fun createItem(
        id: String,
        parentId: String,
        title: String,
        creator: String,
        mimeType: String,
        uri: String,
        size: Long = 0,
        duration: String? = null
    ): Item {
        val res = createRes(mimeType, uri, duration, size)
        return when {
            mimeType.startsWith("video/") -> VideoItem(id, parentId, title, creator, res)
            mimeType.startsWith("audio/") -> AudioItem(id, parentId, title, creator, res)
            mimeType.startsWith("image/") -> ImageItem(id, parentId, title, creator, res)
            else -> VideoItem(id, parentId, title, creator, res)
        }
    }

    // ---- Internal ----

    private fun createRes(mimeType: String, uri: String, duration: String?, size: Long): Res {
        val protocolInfo = ProtocolInfo("http-get:*:$mimeType:*")
        val res = Res(protocolInfo, size, uri)
        duration?.takeIf { it.isNotEmpty() }?.let { res.duration = it }
        return res
    }
}