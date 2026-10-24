package com.example.incidentreporter.incident.application

import com.example.incidentreporter.incident.domain.StoredPhoto
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

data class PhotoPreview(val contentType: String, val bytes: ByteArray)

@Component
class PhotoPreviewEncoder {
    fun encode(photo: StoredPhoto): PhotoPreview {
        val bytes = photo.openStream().use { it.readBytes() }
        val image = (try {
            ImageIO.read(bytes.inputStream())
        } catch (_: IOException) {
            null
        }) ?: return PhotoPreview(photo.contentType, bytes)

        val scale = minOf(1.0, MAX_DIMENSION / maxOf(image.width, image.height))
        val width = maxOf(1, (image.width * scale).toInt())
        val height = maxOf(1, (image.height * scale).toInt())
        val preview = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = preview.createGraphics()
        try {
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, width, height)
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.drawImage(image, 0, 0, width, height, null)
        } finally {
            graphics.dispose()
        }

        val output = ByteArrayOutputStream()
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        try {
            ImageIO.createImageOutputStream(output).use { stream ->
                writer.output = stream
                val options = writer.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = JPEG_QUALITY
                }
                writer.write(null, IIOImage(preview, null, null), options)
            }
        } finally {
            writer.dispose()
        }
        return PhotoPreview("image/jpeg", output.toByteArray())
    }

    private companion object {
        const val MAX_DIMENSION = 512.0
        const val JPEG_QUALITY = 0.7f
    }
}
