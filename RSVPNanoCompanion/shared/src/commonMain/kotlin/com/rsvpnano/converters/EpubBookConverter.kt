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

        val packageInfo = parsePackage(packageXml, opfPath, normalizedEntries)
        val paths = packageInfo.spinePaths.ifEmpty { packageInfo.manifestContentPaths }
        if (paths.isEmpty()) {
            throw RsvpConversionError.unsupportedEpub
        }

        val events = mutableListOf<RsvpEvent>()
        paths.forEachIndexed { index, spinePath ->
            val chapterData = normalizedEntries[EpubUtils.normalizeZipPath(spinePath).lowercase()] ?: return@forEachIndexed
            val markup = RsvpTextUtils.decodeText(chapterData) ?: return@forEachIndexed
            val chapterEvents = RsvpTextUtils.htmlEvents(markup).toMutableList()
            val tocTitle = packageInfo.tocTitlesByPath[EpubUtils.normalizeZipPath(spinePath).lowercase()]
            if (tocTitle != null) {
                chapterEvents.removeFirstMatchingChapter()
                chapterEvents.removeFirstChapterMatching(tocTitle)
                chapterEvents.removeFirstChapterPrefixOf(tocTitle)
                chapterEvents.add(0, RsvpEvent.Chapter(tocTitle))
            } else if (packageInfo.tocTitlesByPath.isNotEmpty()) {
                chapterEvents.removeAll { it is RsvpEvent.Chapter }
            } else if (packageInfo.tocTitlesByPath.isEmpty() && chapterEvents.none { it is RsvpEvent.Chapter }) {
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

    private fun parsePackage(xml: String, opfPath: String, entries: Map<String, ByteArray>): EpubPackage {
        val manifest = linkedMapOf<String, EpubManifestItem>()
        val manifestContentPaths = mutableListOf<String>()
        val spinePaths = mutableListOf<String>()
        val navPaths = mutableListOf<String>()
        val ncxPaths = mutableListOf<String>()

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
            if (mediaType.equals("application/x-dtbncx+xml", ignoreCase = true)) {
                ncxPaths += path
            }
            if (attrs["properties"].orEmpty().split(Regex("\\s+")).any { it == "nav" }) {
                navPaths += path
            }
        }

        tagMatches(xml, "itemref").forEach { match ->
            val idref = attributes(match.groupValues[1])["idref"].orEmpty()
            val item = manifest[idref] ?: return@forEach
            if (isContentDocument(item.path, item.mediaType)) {
                spinePaths += item.path
            }
        }

        val title = textContentByTag(metadataXml(xml), "title")

        return EpubPackage(
            title = title,
            author = textContentByTag(metadataXml(xml), "creator"),
            spinePaths = spinePaths,
            manifestContentPaths = manifestContentPaths,
            tocTitlesByPath = tocTitlesByPath(
                entries = entries,
                bookTitle = title,
                navPaths = navPaths,
                ncxPaths = ncxPaths,
            ),
        )
    }

    private fun tocTitlesByPath(
        entries: Map<String, ByteArray>,
        bookTitle: String,
        navPaths: List<String>,
        ncxPaths: List<String>,
    ): Map<String, String> {
        ncxPaths.firstNotNullOfOrNull { path ->
            entries[EpubUtils.normalizeZipPath(path).lowercase()]
                ?.let(RsvpTextUtils::decodeText)
                ?.let { ncxTocTitles(it, path, bookTitle) }
                ?.takeIf { it.isNotEmpty() }
        }?.let { return it }

        navPaths.firstNotNullOfOrNull { path ->
            entries[EpubUtils.normalizeZipPath(path).lowercase()]
                ?.let(RsvpTextUtils::decodeText)
                ?.let { htmlNavTocTitles(it, path, bookTitle) }
                ?.takeIf { it.isNotEmpty() }
        }?.let { return it }

        return emptyMap()
    }

    private fun ncxTocTitles(xml: String, tocPath: String, bookTitle: String): Map<String, String> {
        return Regex(
            "<(?:[A-Za-z_][\\w.-]*:)?navPoint\\b[^>]*>(.*?)</(?:[A-Za-z_][\\w.-]*:)?navPoint>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).findAll(xml).mapNotNull { match ->
            val block = match.groupValues[1]
            val label = textContentByTag(block, "text")
                .takeIf { isContentTocTitle(it, bookTitle) }
                ?: return@mapNotNull null
            val contentAttrs = Regex(
                "<(?:[A-Za-z_][\\w.-]*:)?content\\b([^>]*)>",
                RegexOption.IGNORE_CASE,
            ).find(block)?.groupValues?.getOrNull(1).orEmpty()
            val src = attributes(contentAttrs)["src"].orEmpty()
            tocPathKey(tocPath, src) to label
        }.toMap()
    }

    private fun htmlNavTocTitles(markup: String, tocPath: String, bookTitle: String): Map<String, String> {
        val navBlock = Regex(
            "<(?:[A-Za-z_][\\w.-]*:)?nav\\b[^>]*?(?:epub:type|type)\\s*=\\s*([\"'])toc\\1[^>]*>(.*?)</(?:[A-Za-z_][\\w.-]*:)?nav>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(markup)?.groupValues?.getOrNull(2) ?: markup

        return Regex(
            "<(?:[A-Za-z_][\\w.-]*:)?a\\b([^>]*)>(.*?)</(?:[A-Za-z_][\\w.-]*:)?a>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).findAll(navBlock).mapNotNull { match ->
            val href = attributes(match.groupValues[1])["href"].orEmpty()
            val label = RsvpTextUtils.cleanedLine(
                decodeXmlEntities(match.groupValues[2].replace(Regex("<[^>]+>"), " "))
            ).takeIf { isContentTocTitle(it, bookTitle) } ?: return@mapNotNull null
            tocPathKey(tocPath, href) to label
        }.toMap()
    }

    private fun tocPathKey(tocPath: String, href: String): String {
        val withoutAnchor = href.substringBefore('#').substringBefore('?')
        return EpubUtils.normalizeZipPath(EpubUtils.zipJoin(tocPath, withoutAnchor)).lowercase()
    }

    private fun isContentTocTitle(value: String, bookTitle: String): Boolean {
        val cleaned = RsvpTextUtils.cleanedLine(value)
        val lowered = cleaned.lowercase()
        val normalized = normalizedTocLabel(cleaned)
        val normalizedBookTitle = normalizedTocLabel(bookTitle)
        return cleaned.isNotEmpty() &&
            lowered != "contents" &&
            lowered != "cover" &&
            lowered != "title page" &&
            normalized != "tableofcontents" &&
            (normalizedBookTitle.isEmpty() || normalized != normalizedBookTitle) &&
            cleaned.any(Char::isLetterOrDigit)
    }

    private fun normalizedTocLabel(value: String): String {
        return RsvpTextUtils.cleanedLine(value)
            .filter(Char::isLetterOrDigit)
            .lowercase()
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
        val tocTitlesByPath: Map<String, String>,
    )

    private fun MutableList<RsvpEvent>.removeFirstMatchingChapter() {
        val index = indexOfFirst { it is RsvpEvent.Chapter }
        if (index >= 0) {
            removeAt(index)
        }
    }

    private fun MutableList<RsvpEvent>.removeFirstChapterMatching(title: String) {
        val normalizedTitle = normalizedChapterTitle(title)
        val index = indexOfFirst { event ->
            event is RsvpEvent.Chapter && normalizedChapterTitle(event.title) == normalizedTitle
        }
        if (index >= 0) {
            removeAt(index)
        }
    }

    private fun MutableList<RsvpEvent>.removeFirstChapterPrefixOf(title: String) {
        val normalizedTitle = normalizedChapterTitle(title)
        val index = indexOfFirst { event ->
            event is RsvpEvent.Chapter && normalizedTitle.startsWith(normalizedChapterTitle(event.title) + " ")
        }
        if (index >= 0) {
            removeAt(index)
        }
    }

    private fun normalizedChapterTitle(value: String): String {
        return RsvpTextUtils.cleanedLine(value).lowercase()
    }
}
