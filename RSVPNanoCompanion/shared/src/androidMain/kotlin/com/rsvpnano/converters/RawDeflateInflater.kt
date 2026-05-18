package com.rsvpnano.converters

import java.util.zip.DataFormatException
import java.util.zip.Inflater

internal actual object RawDeflateInflater {
    actual fun inflate(data: ByteArray, expectedSize: Int): ByteArray {
        if (expectedSize == 0) {
            return byteArrayOf()
        }

        val output = ByteArray(expectedSize)
        val inflater = Inflater(true)
        return try {
            inflater.setInput(data)
            val inflated = inflater.inflate(output)
            if (!inflater.finished() || inflated != expectedSize) {
                throw RsvpConversionError.unsupportedEpub
            }
            output
        } catch (_: DataFormatException) {
            throw RsvpConversionError.unsupportedEpub
        } finally {
            inflater.end()
        }
    }
}
