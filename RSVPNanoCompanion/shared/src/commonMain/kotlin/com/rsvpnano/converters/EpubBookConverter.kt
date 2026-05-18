package com.rsvpnano.converters

internal object EpubBookConverter {
    fun convert(entries: Map<String, ByteArray>, filename: String): RsvpBookFile {
        val normalizedEntries = entries.mapKeys { (path, _) -> EpubUtils.normalizeZipPath(path).lowercase() }
        val containerXml = normalizedEntries["meta-inf/container.xml"]
            ?.let(RsvpTextUtils::decodeText)
            ?: throw RsvpConversionError.unsupportedEpub
        val opfPath = containerRootfile(containerXml) ?: throw RsvpConversionError.unsupportedEpub
        val packageXml = normalizedEntries[EpubUtils.normalizeZipPath(opfPath).lowercase()]
            ?.let(RsvpTextUtils::decodeText)
            ?: throw RsvpConversionError.unsupportedEpub

        val packageInfo = parsePackage(packageXml, opfPath)
        val paths = packageInfo.spinePaths.ifEmpty { packageInfo.manifestContentPaths }
        if (paths.isEmpty()) {
            throw RsvpConversionError.unsupportedEpub
        }

        val events = mutableListOf<RsvpEvent>()
        paths.forEachIndexed { index, spinePath ->
            val chapterData = normalizedEntries[EpubUtils.normalizeZipPath(spinePath).lowercase()] ?: return@forEachIndexed
            val markup = RsvpTextUtils.decodeText(chapterData) ?: return@forEachIndexed
            val chapterEvents = RsvpTextUtils.htmlEvents(markup).toMutableList()
            if (chapterEvents.none { it is RsvpEvent.Chapter }) {
                chapterEvents.add(0, RsvpEvent.Chapter(EpubUtils.fallbackChapterTitle(spinePath, index + 1)))
            }
            if (chapterEvents.any { it is RsvpEvent.Text }) {
                events += chapterEvents
            }
        }

        if (events.isEmpty()) {
            throw RsvpConversionError.unsupportedEpub
        }

        return RsvpConverter.rsvpFile(
            title = packageInfo.title.ifBlank { RsvpConverter.filenameWithoutExtension(filename) },
            author = packageInfo.author,
            source = filename,
            events = events,
        )
    }

    private fun containerRootfile(xml: String): String? {
        return tagMatches(xml, "rootfile")
            .map { attributes(it.groupValues[1])["full-path"].orEmpty() }
            .firstOrNull { it.isNotBlank() }
    }

    private fun parsePackage(xml: String, opfPath: String): EpubPackage {
        val manifest = linkedMapOf<String, EpubManifestItem>()
        val manifestContentPaths = mutableListOf<String>()
        val spinePaths = mutableListOf<String>()

        tagMatches(xml, "item").forEach { match ->
            val attrs = attributes(match.groupValues[1])
            val id = attrs["id"].orEmpty()
            val href = attrs["href"].orEmpty()
            if (id.isBlank() || href.isBlank()) return@forEach

            val mediaType = attrs["media-type"].orEmpty()
            val path = EpubUtils.zipJoin(opfPath, href)
            val item = EpubManifestItem(path = path, mediaType = mediaType)
            manifest[id] = item
            if (isContentDocument(path, mediaType)) {
                manifestContentPaths += path
            }
        }

        tagMatches(xml, "itemref").forEach { match ->
            val idref = attributes(match.groupValues[1])["idref"].orEmpty()
            val item = manifest[idref] ?: return@forEach
            if (isContentDocument(item.path, item.mediaType)) {
                spinePaths += item.path
            }
        }

        return EpubPackage(
            title = textContentByTag(metadataXml(xml), "title"),
            author = textContentByTag(metadataXml(xml), "creator"),
            spinePaths = spinePaths,
            manifestContentPaths = manifestContentPaths,
        )
    }

    private fun tagMatches(xml: String, tag: String): Sequence<MatchResult> {
        return Regex("<(?:[A-Za-z_][\\w.-]*:)?$tag\\b([^>]*)>", RegexOption.IGNORE_CASE).findAll(xml)
    }

    private fun attributes(raw: String): Map<String, String> {
        return Regex("([A-Za-z_:][\\w:.-]*)\\s*=\\s*([\"'])(.*?)\\2").findAll(raw)
            .associate { match ->
                match.groupValues[1].lowercase() to decodeXmlEntities(match.groupValues[3])
            }
    }

    private fun textContentByTag(xml: String, tag: String): String {
        val value = Regex(
            "<(?:[A-Za-z_][\\w.-]*:)?$tag\\b[^>]*>(.*?)</(?:[A-Za-z_][\\w.-]*:)?$tag>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(xml)?.groupValues?.getOrNull(1).orEmpty()
        return RsvpTextUtils.cleanedLine(decodeXmlEntities(value.replace(Regex("<[^>]+>"), " ")))
    }

    private fun metadataXml(xml: String): String {
        return Regex(
            "<(?:[A-Za-z_][\\w.-]*:)?metadata\\b[^>]*>(.*?)</(?:[A-Za-z_][\\w.-]*:)?metadata>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(xml)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun decodeXmlEntities(value: String): String {
        var text = value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
        text = Regex("&#(x[0-9a-fA-F]+|\\d+);").replace(text) { match ->
            val token = match.groupValues[1]
            val scalarValue = if (token.startsWith("x", ignoreCase = true)) {
                token.drop(1).toUIntOrNull(16)
            } else {
                token.toUIntOrNull(10)
            }
            scalarValue?.toInt()?.toChar()?.toString().orEmpty()
        }
        return text
    }

    private fun isContentDocument(path: String, mediaType: String): Boolean {
        val loweredPath = path.lowercase()
        val loweredType = mediaType.lowercase()
        return loweredType == "application/xhtml+xml" ||
            loweredType == "text/html" ||
            loweredPath.endsWith(".xhtml") ||
            loweredPath.endsWith(".html") ||
            loweredPath.endsWith(".htm")
    }

    private data class EpubManifestItem(val path: String, val mediaType: String)

    private data class EpubPackage(
        val title: String,
        val author: String,
        val spinePaths: List<String>,
        val manifestContentPaths: List<String>,
    )
}
