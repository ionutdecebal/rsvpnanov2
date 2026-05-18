package com.rsvpnano.converters

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2
import platform.zlib.z_stream

@OptIn(ExperimentalForeignApi::class)
internal actual object RawDeflateInflater {
    actual fun inflate(data: ByteArray, expectedSize: Int): ByteArray {
        if (expectedSize == 0) {
            return byteArrayOf()
        }

        val output = ByteArray(expectedSize)
        memScoped {
            val stream = alloc<z_stream>()
            data.usePinned { input ->
                output.usePinned { target ->
                    stream.next_in = input.addressOf(0).reinterpret()
                    stream.avail_in = data.size.convert()
                    stream.next_out = target.addressOf(0).reinterpret()
                    stream.avail_out = output.size.convert()

                    val initResult = inflateInit2(stream.ptr, -15)
                    if (initResult != Z_OK) {
                        throw RsvpConversionError.unsupportedEpub
                    }

                    try {
                        val inflateResult = inflate(stream.ptr, Z_FINISH)
                        if (inflateResult != Z_STREAM_END || stream.total_out.toInt() != expectedSize) {
                            throw RsvpConversionError.unsupportedEpub
                        }
                    } finally {
                        inflateEnd(stream.ptr)
                    }
                }
            }
        }
        return output
    }
}
