package com.example.incidentreporter.incident.api

import com.example.incidentreporter.incident.domain.PhotoStore
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

@Component
class PhotoPreviewEncoder(private val photoStore: PhotoStore) {
    fun encode(uri: String): String {
        val photo = photoStore.load(uri) ?: throw ResourceNotFoundException()
        val bytes = photo.openStream().use { it.readBytes() }
        val image = (try {
            ImageIO.read(bytes.inputStream())
        } catch (_: IOException) {
            null
        }) ?: return dataUri(photo.contentType, bytes)

        val scale = minOf(1.0, 512.0 / maxOf(image.width, image.height))
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
                    compressionQuality = 0.7f
                }
                writer.write(null, javax.imageio.IIOImage(preview, null, null), options)
            }
        } finally {
            writer.dispose()
        }
        return dataUri("image/jpeg", output.toByteArray())
    }

    private fun dataUri(contentType: String, bytes: ByteArray): String =
        "data:$contentType;base64,${Base64.getEncoder().encodeToString(bytes)}"
}
