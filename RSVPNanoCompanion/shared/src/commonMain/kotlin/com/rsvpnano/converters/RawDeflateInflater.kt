package com.rsvpnano.converters

internal expect object RawDeflateInflater {
    fun inflate(data: ByteArray, expectedSize: Int): ByteArray
}
