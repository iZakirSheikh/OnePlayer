package com.zs.core.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.datasource.BitmapUtil
import androidx.media3.datasource.DataSourceBitmapLoader
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.executeBlocking
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture

/**
 * A [BitmapLoader] implementation that uses Coil to load and decode bitmaps.
 *
 * This loader supports:
 * - Decoding raw byte arrays into [Bitmap].
 * - Loading images from a [Uri] using Coil's [ImageLoader].
 *
 * @property context Android [Context] used to initialize Coil's [ImageLoader].
 */
class CoilBitmapLoader(
    private val context: Context
) : BitmapLoader {

    // Shared Coil ImageLoader instance (singleton for efficiency).
    private val imageLoader: ImageLoader = SingletonImageLoader.get(context)
    // Executor service used to run bitmap decoding/loading tasks off the main thread.
    private val executor = DataSourceBitmapLoader.DEFAULT_EXECUTOR_SERVICE.get()
    // Always returns true since this loader can handle any MIME type.
    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        // Decode the byte array into a Bitmap using utility method.
        return executor.submit<Bitmap> {
            BitmapUtil.decode(data, data.size, null, 256)
        }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return executor.submit<Bitmap> {
            // Build a Coil image request for the given URI.
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(256, 256)
                .allowHardware(false) // Disable hardware bitmaps for compatibility.
                .build()

            // Execute the request synchronously and convert the result to Bitmap.
            imageLoader.executeBlocking(request).image!!.toBitmap()
        }
    }
}