package com.example.kairo.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes a content URI into a Bitmap off the main thread, downsampled with
 * inSampleSize so full-resolution photos don't cause jank or OOM when only a
 * small thumbnail is displayed.
 */
@Composable
fun rememberDownsampledBitmap(
    context: Context,
    uriString: String?,
    maxDimensionPx: Int = 512
): Bitmap? {
    val state by produceState<Bitmap?>(initialValue = null, uriString) {
        if (uriString == null) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)

                // Pass 1: read bounds only
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

                var sample = 1
                while (bounds.outWidth / (sample * 2) >= maxDimensionPx &&
                    bounds.outHeight / (sample * 2) >= maxDimensionPx
                ) {
                    sample *= 2
                }

                // Pass 2: decode downsampled
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(
                        stream,
                        null,
                        BitmapFactory.Options().apply { inSampleSize = sample }
                    )
                }
            }.getOrNull()
        }
    }
    return state
}
