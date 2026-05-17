package com.rsvpnano.converters

internal actual object PlatformEpubConverter {
    actual fun convert(data: ByteArray, filename: String): RsvpBookFile {
        throw RsvpConversionError.unsupportedEpub
    }
}
