package org.fossify.camera.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaCodecList
import androidx.exifinterface.media.ExifInterface
import androidx.heifwriter.HeifWriter
import java.io.File
import java.io.RandomAccessFile

/**
 * Converts the JPEG that CameraX produced into a HEIC file using the device's HEVC encoder.
 * Returns null on any failure, so the caller can simply keep the JPEG.
 */
object HeicConverter {
    private const val TIMEOUT_MS = 60_000L
    private const val TEMP_PREFIX = "SimpleCameraHeic"
    private const val TEMP_SUFFIX = ".tmp"

    fun isSupported(): Boolean {
        return try {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.any {
                    it.equals("image/vnd.android.heic", ignoreCase = true) ||
                        it.equals("video/hevc", ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun convert(jpegFile: File, quality: Int): File? {
        var heicFile: File? = null
        var exifCopy: File? = null
        var source: Bitmap? = null
        var upright: Bitmap? = null
        try {
            val sourceExif = ExifInterface(jpegFile)
            val rotation = sourceExif.rotationDegrees
            val flipped = sourceExif.isFlipped

            val decoded = BitmapFactory.decodeFile(jpegFile.absolutePath) ?: return null
            source = decoded
            val image: Bitmap = if (rotation == 0 && !flipped) {
                decoded
            } else {
                val matrix = Matrix().apply {
                    if (flipped) postScale(-1f, 1f)
                    postRotate(rotation.toFloat())
                }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            }
            upright = image

            // The pixels are now upright, so the copied Exif block must say "normal orientation".
            exifCopy = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX)
            jpegFile.copyTo(exifCopy, overwrite = true)
            ExifInterface(exifCopy).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
            val exifBlock = readExifBlock(exifCopy)

            val output = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX)
            heicFile = output
            HeifWriter.Builder(
                output.absolutePath,
                image.width,
                image.height,
                HeifWriter.INPUT_MODE_BITMAP
            ).setQuality(quality.coerceIn(1, 100))
                .setMaxImages(1)
                .build()
                .use { writer ->
                    writer.start()
                    writer.addBitmap(image)
                    if (exifBlock != null) {
                        writer.addExifData(0, exifBlock, 0, exifBlock.size)
                    }
                    writer.stop(TIMEOUT_MS)
                }
            return output
        } catch (e: Throwable) {
            heicFile?.delete()
            return null
        } finally {
            exifCopy?.delete()
            if (upright !== source) upright?.recycle()
            source?.recycle()
        }
    }

    /** Returns the Exif block ("Exif\\0\\0" + TIFF data) of a JPEG file, or null if there is none. */
    @Suppress("TooGenericExceptionCaught")
    private fun readExifBlock(file: File): ByteArray? {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                var result: ByteArray? = null
                if (raf.readUnsignedShort() == 0xFFD8) {
                    var searching = true
                    while (searching) {
                        val marker = raf.readUnsignedShort()
                        if (marker == 0xFFDA || (marker and 0xFF00) != 0xFF00) {
                            searching = false
                        } else {
                            val length = raf.readUnsignedShort()
                            if (marker == 0xFFE1 && length > 8) {
                                val data = ByteArray(length - 2)
                                raf.readFully(data)
                                if (data.startsWithExifHeader()) {
                                    result = data
                                    searching = false
                                }
                            } else {
                                raf.skipBytes(length - 2)
                            }
                        }
                    }
                }
                result
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun ByteArray.startsWithExifHeader(): Boolean {
        val header = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00)
        return size >= header.size && header.indices.all { this[it] == header[it] }
    }
}
