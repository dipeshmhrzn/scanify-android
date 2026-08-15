package com.scanify.app.presentation.util

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.experimental.and

object JpegPdfWriter {

    fun write(jpegFiles: List<File>, outputFile: File) {
        require(jpegFiles.isNotEmpty()) { "No pages to write." }

        BufferedOutputStream(FileOutputStream(outputFile)).use { rawOut ->
            val out = CountingOutputStream(rawOut)
            val objOffsets = HashMap<Int, Long>()

            fun writeStr(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))

            writeStr("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")

            val n = jpegFiles.size
            val pageObjNums = IntArray(n) { 3 + 3 * it }

            fun startObj(num: Int) {
                objOffsets[num] = out.count
                writeStr("$num 0 obj\n")
            }
            fun endObj() = writeStr("endobj\n")

            startObj(1)
            writeStr("<< /Type /Catalog /Pages 2 0 R >>\n")
            endObj()

            startObj(2)
            val kids = pageObjNums.joinToString(" ") { "$it 0 R" }
            writeStr("<< /Type /Pages /Kids [$kids] /Count $n >>\n")
            endObj()

            for (i in 0 until n) {
                val jpegBytes = jpegFiles[i].readBytes()
                val (w, h, comps) = readJpegDimensions(jpegBytes)
                val colorSpace = when (comps) {
                    1 -> "/DeviceGray"
                    4 -> "/DeviceCMYK"
                    else -> "/DeviceRGB"
                }

                val pageNum = 3 + 3 * i
                val contentNum = 4 + 3 * i
                val imageNum = 5 + 3 * i
                val contentStream = "q $w 0 0 $h 0 0 cm /Im$i Do Q"
                val contentBytes = contentStream.toByteArray(Charsets.ISO_8859_1)

                startObj(pageNum)
                writeStr(
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $w $h] " +
                            "/Resources << /XObject << /Im$i $imageNum 0 R >> >> " +
                            "/Contents $contentNum 0 R >>\n"
                )
                endObj()

                startObj(contentNum)
                writeStr("<< /Length ${contentBytes.size} >>\nstream\n")
                out.write(contentBytes)
                writeStr("\nendstream\n")
                endObj()

                startObj(imageNum)
                writeStr(
                    "<< /Type /XObject /Subtype /Image /Width $w /Height $h " +
                            "/ColorSpace $colorSpace /BitsPerComponent 8 /Filter /DCTDecode " +
                            "/Length ${jpegBytes.size} >>\nstream\n"
                )
                out.write(jpegBytes)
                writeStr("\nendstream\n")
                endObj()
            }

            val totalObjs = 2 + 3 * n
            val xrefOffset = out.count
            writeStr("xref\n0 ${totalObjs + 1}\n")
            writeStr("0000000000 65535 f \n")
            for (num in 1..totalObjs) {
                val offset = objOffsets.getValue(num)
                writeStr("%010d 00000 n \n".format(offset))
            }
            writeStr("trailer\n<< /Size ${totalObjs + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF")
        }
    }

    fun extractEmbeddedJpegs(pdfFile: File): List<ByteArray> {
        val data = pdfFile.readBytes()
        val marker = "/Filter /DCTDecode".toByteArray(Charsets.ISO_8859_1)
        val lengthMarker = "/Length ".toByteArray(Charsets.ISO_8859_1)
        val streamMarker = "stream\n".toByteArray(Charsets.ISO_8859_1)

        val results = mutableListOf<ByteArray>()
        var searchFrom = 0
        while (true) {
            val markerPos = indexOf(data, marker, searchFrom)
            if (markerPos == -1) break

            val dictStart = lastIndexOfDoubleAngle(data, markerPos)
            val lengthPos = indexOf(data, lengthMarker, dictStart, markerPos + 200)
            if (lengthPos == -1) {
                searchFrom = markerPos + marker.size
                continue
            }

            var lengthEnd = lengthPos + lengthMarker.size
            while (lengthEnd < data.size && data[lengthEnd].toInt().toChar().isDigit()) lengthEnd++
            val length = String(data, lengthPos + lengthMarker.size, lengthEnd - (lengthPos + lengthMarker.size), Charsets.ISO_8859_1).toIntOrNull()
                ?: run { searchFrom = markerPos + marker.size; continue }

            val streamPos = indexOf(data, streamMarker, markerPos)
            if (streamPos == -1) break
            val dataStart = streamPos + streamMarker.size
            if (dataStart + length > data.size) break

            results.add(data.copyOfRange(dataStart, dataStart + length))
            searchFrom = dataStart + length
        }
        return results
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray, from: Int, to: Int = haystack.size): Int {
        val limit = to - needle.size
        outer@ for (i in from.coerceAtLeast(0)..limit) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun lastIndexOfDoubleAngle(data: ByteArray, before: Int): Int {
        val needle = "<<".toByteArray(Charsets.ISO_8859_1)
        for (i in (before - 2) downTo 0) {
            if (data[i] == needle[0] && data[i + 1] == needle[1]) return i
        }
        return 0
    }

    private data class JpegInfo(val width: Int, val height: Int, val components: Int)

    fun tryReadJpegDimensions(file: File): IntArray? = try {
        val head = file.inputStream().use { it.readBytes(262_144) }
        val info = readJpegDimensions(head)
        intArrayOf(info.width, info.height)
    } catch (e: Exception) {
        null
    }

    private fun java.io.InputStream.readBytes(limit: Int): ByteArray {
        val buffer = ByteArray(limit)
        var total = 0
        while (total < limit) {
            val read = read(buffer, total, limit - total)
            if (read == -1) break
            total += read
        }
        return buffer.copyOf(total)
    }

    private fun readJpegDimensions(data: ByteArray): JpegInfo {
        var i = 2
        while (i < data.size - 1) {
            if ((data[i] and 0xFF.toByte()) != 0xFF.toByte()) {
                i++
                continue
            }
            val marker = data[i + 1].toInt() and 0xFF
            if (marker == 0xD8 || marker == 0x01 || (marker in 0xD0..0xD7)) {
                i += 2
                continue
            }
            val segLen = ((data[i + 2].toInt() and 0xFF) shl 8) or (data[i + 3].toInt() and 0xFF)
            val isSofMarker = marker in intArrayOf(
                0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF
            )
            if (isSofMarker) {
                val height = ((data[i + 5].toInt() and 0xFF) shl 8) or (data[i + 6].toInt() and 0xFF)
                val width = ((data[i + 7].toInt() and 0xFF) shl 8) or (data[i + 8].toInt() and 0xFF)
                val components = data[i + 9].toInt() and 0xFF
                return JpegInfo(width, height, components)
            }
            i += 2 + segLen
        }
        throw IllegalArgumentException("Not a valid JPEG (SOF marker not found).")
    }

    private class CountingOutputStream(private val delegate: OutputStream) : OutputStream() {
        var count: Long = 0
            private set

        override fun write(b: Int) {
            delegate.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
            count += len
        }

        override fun flush() = delegate.flush()
        override fun close() = delegate.close()
    }
}