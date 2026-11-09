package com.example.blockgallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

data class BlockItem(
    val id: Long,
    val topBase64: String,
    val sideBase64: String,
    val faceBase64: String
)

object BlockRepository {
    private const val FILE_NAME = "pixel_blocks_v3.json"

    fun loadBlocks(context: Context): List<BlockItem> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            val jsonArray = JSONArray(file.readText())
            val list = mutableListOf<BlockItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    BlockItem(
                        id = obj.getLong("id"),
                        topBase64 = obj.getString("top"),
                        sideBase64 = obj.getString("side"),
                        faceBase64 = obj.getString("face")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveBlocks(context: Context, blocks: List<BlockItem>) {
        val jsonArray = JSONArray()
        blocks.forEach { block ->
            val obj = JSONObject().apply {
                put("id", block.id)
                put("top", block.topBase64)
                put("side", block.sideBase64)
                put("face", block.faceBase64)
            }
            jsonArray.put(obj)
        }
        File(context.filesDir, FILE_NAME).writeText(jsonArray.toString())
    }

    // Processes URI into an exact 16x16 crisp pixel Base64 PNG
    fun uriTo16pxBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (original == null) return null

            // filter = false forces nearest-neighbor pixel scaling (no blur)
            val scaled = Bitmap.createScaledBitmap(original, 16, 16, false)
            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    // Implements the exact Top, Side, Face smart fallback priority
    fun createSmartBlock(top: String?, side: String?, face: String?): BlockItem? {
        val provided = listOfNotNull(top, side, face)
        if (provided.isEmpty()) return null

        if (provided.size == 1) {
            val single = provided.first()
            return BlockItem(System.currentTimeMillis(), single, single, single)
        }

        // Priority rules:
        // 1. Missing Face -> Steal from Side first, then Top
        val finalFace = face ?: side ?: top!!
        // 2. Missing Side -> Steal from Face first, then Top
        val finalSide = side ?: face ?: top!!
        // 3. Missing Top -> Steal from Side first, then Face
        val finalTop = top ?: side ?: face!!

        return BlockItem(
            id = System.currentTimeMillis(),
            topBase64 = finalTop,
            sideBase64 = finalSide,
            faceBase64 = finalFace
        )
    }
}