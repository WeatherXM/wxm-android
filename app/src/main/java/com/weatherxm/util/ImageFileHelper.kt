package com.weatherxm.util

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

object ImageFileHelper {
    fun compressImageFile(bitmap: Bitmap): InputStream? {
        var quality = 100
        var inputStream: InputStream? = null
        var bufferSize: Int
        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            do {
                byteArrayOutputStream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
                bufferSize = byteArrayOutputStream.size()
                quality -= 10
                Timber.d("[IMAGE COMPRESSING] Quality: $quality -> Size in KB: ${bufferSize / 1000}")
            } while (bufferSize > 1024 * 1024)

            byteArrayOutputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            inputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
            byteArrayOutputStream.close()
        } catch (e: Exception) {
            Timber.e(e, "Exception when compressing file image: ${e.message}")
        }

        return inputStream
    }

    fun File.copyInputStreamToFile(inputStream: InputStream?) {
        this.outputStream().use { fileOut ->
            inputStream?.copyTo(fileOut)
        }
    }

    fun copyExifMetadata(sourcePath: String, destPath: String) {
        val oldExifInterface = ExifInterface(sourcePath)
        val newExifInterface = ExifInterface(destPath)

        val location = android.location.Location("")
        val latLon = oldExifInterface.latLong
        location.latitude = latLon?.get(0) ?: 0.0
        location.longitude = latLon?.get(1) ?: 0.0
        newExifInterface.setLatLong(location.latitude, location.longitude)

        newExifInterface.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            oldExifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)
        )
        newExifInterface.saveAttributes()
    }
}
