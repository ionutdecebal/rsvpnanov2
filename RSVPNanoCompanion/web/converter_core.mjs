export const RSVP_VERSION = "1";
export const WRAP_WIDTH = 96;
export const DEFAULT_OUTPUT_MODE = "unicode";

export const SUPPORTED_EXTENSIONS = new Set([
  ".epub",
  ".html",
  ".htm",
  ".xhtml",
  ".md",
  ".markdown",
  ".txt",
]);

export const SIDE_CAR_SUFFIXES = [".rsvp.failed", ".rsvp.tmp", ".rsvp.converting"];

const BLOCK_TAGS = new Set([
  "address",
  "article",
  "aside",
  "blockquote",
  "body",
  "br",
  "dd",
  "div",
  "dl",
  "dt",
  "figcaption",
  "figure",
  "footer",
  "header",
  "hr",
  "li",
  "main",
  "ol",
  "p",
  "pre",
  "section",
  "table",
  "tbody",
  "td",
  "tfoot",
  "th",
  "thead",
  "tr",
  "ul",
]);

const HEADING_TAGS = new Set(["h1", "h2", "h3", "h4", "h5", "h6"]);
const SKIP_TAGS = new Set(["head", "math", "nav", "script", "style", "svg"]);
const BLOCK_BREAK = "\u0000";
const ASCII_REPLACEMENTS = {
  "\u00A0": " ",
  "\u1680": " ",
  "\u180E": " ",
  "\u2000": " ",
  "\u2001": " ",
  "\u2002": " ",
  "\u2003": " ",
  "\u2004": " ",
  "\u2005": " ",
  "\u2006": " ",
  "\u2007": " ",
  "\u2008": " ",
  "\u2009": " ",
  "\u200A": " ",
  "\u2028": " ",
  "\u2029": " ",
  "\u202F": " ",
  "\u205F": " ",
  "\u3000": " ",
  "\u2018": "'",
  "\u2019": "'",
  "\u201A": "'",
  "\u201B": "'",
  "\u2032": "'",
  "\u2035": "'",
  "\u201C": '"',
  "\u201D": '"',
  "\u201E": '"',
  "\u201F": '"',
  "\u00AB": '"',
  "\u00BB": '"',
  "\u2039": "'",
  "\u203A": "'",
  "\u2033": '"',
  "\u2036": '"',
  "\u300C": '"',
  "\u300D": '"',
  "\u300E": '"',
  "\u300F": '"',
  "\u2010": "-",
  "\u2011": "-",
  "\u2012": "-",
  "\u2013": "-",
  "\u2014": "-",
  "\u2015": "-",
  "\u2043": "-",
  "\u2212": "-",
  "\u2026": "...",
  "\u2022": "*",
  "\u00B7": "*",
  "\u2219": "*",
  "\u207D": "(",
  "\u208D": "(",
  "\u2768": "(",
  "\u276A": "(",
  "\u207E": ")",
  "\u208E": ")",
  "\u2769": ")",
  "\u276B": ")",
  "\u2045": "[",
  "\u2308": "[",
  "\u230A": "[",
  "\u3010": "[",
  "\u3014": "[",
  "\u3016": "[",
  "\u3018": "[",
  "\u301A": "[",
  "\u2046": "]",
  "\u2309": "]",
  "\u230B": "]",
  "\u3011": "]",
  "\u3015": "]",
  "\u3017": "]",
  "\u3019": "]",
  "\u301B": "]",
  "\u2774": "{",
  "\u2776": "{",
  "\u2775": "}",
  "\u2777": "}",
  "\u2329": "<",
  "\u27E8": "<",
  "\u3008": "<",
  "\u300A": "<",
  "\u232A": ">",
  "\u27E9": ">",
  "\u3009": ">",
  "\u300B": ">",
  "\u00A9": "(c)",
  "\u00AE": "(r)",
  "\u2122": "TM",
  "\uFB00": "ff",
  "\uFB01": "fi",
  "\uFB02": "fl",
  "\uFB03": "ffi",
  "\uFB04": "ffl",
  "\uFB05": "st",
  "\uFB06": "st",
  "\uFFFD": "",
};

const SPACE_LIKE_RE =
  /[\u00A0\u1680\u180E\u2000-\u200A\u2028\u2029\u202F\u205F\u3000\r\n\t]/g;
const COMBINING_MARKS_RE = /[\u0300-\u036f]/g;

export async function eventsForFile(file, mode = DEFAULT_OUTPUT_MODE, options = {}) {
  const sourceExt = extensionForName(file.name);
  if (sourceExt === ".epub") {
    return epubEventsAndMetadata(file, mode, options);
  }
  if (sourceExt === ".html" || sourceExt === ".htm" || sourceExt === ".xhtml") {
    const markup = await readTextFile(file);
    const { title, events } = htmlEventsAndTitle(markup, stripExtension(file.name), mode);
    return { title, author: "", events };
  }
  if (sourceExt === ".txt" || sourceExt === ".md" || sourceExt === ".markdown") {
    const text = await readTextFile(file);
    return {
      title: stripExtension(file.name),
      author: "",
      events: textEvents(text, mode),
    };
  }
  throw new Error(`Unsupported extension: ${sourceExt}`);
}

export async function readTextFile(file) {
  const bytes = new Uint8Array(await file.arrayBuffer());
  return decodeTextBytes(bytes);
}

export function decodeTextBytes(bytes) {
  if (bytes.length >= 3 && bytes[0] === 0xef && bytes[1] === 0xbb && bytes[2] === 0xbf) {
    return new TextDecoder("utf-8").decode(bytes.subarray(3));
  }
  if (bytes.length >= 2 && bytes[0] === 0xff && bytes[1] === 0xfe) {
    return new TextDecoder("utf-16le").decode(bytes.subarray(2));
  }
  if (bytes.length >= 2 && bytes[0] === 0xfe && bytes[1] === 0xff) {
    return new TextDecoder("utf-16be").decode(bytes.subarray(2));
  }

  const guessedUtf16 = detectUtf16WithoutBom(bytes);
  if (guessedUtf16) {
    return decodeWithDeclaredEncoding(bytes, guessedUtf16);
  }

  return decodeWithDeclaredEncoding(bytes, "utf-8");
}

function detectUtf16WithoutBom(bytes) {
  if (bytes.length < 4) {
    return null;
  }
  if (bytes[0] === 0x3c && bytes[1] === 0x00 && bytes[2] === 0x3f && bytes[3] === 0x00) {
    return "utf-16le";
  }
  if (bytes[0] === 0x00 && bytes[1] === 0x3c && bytes[2] === 0x00 && bytes[3] === 0x3f) {
    return "utf-16be";
  }
  return null;
}

function decodeWithDeclaredEncoding(bytes, initialEncoding) {
  let decoded = tryDecode(bytes, initialEncoding);
  if (decoded === null) {
    decoded = tryDecode(bytes, "windows-1252");
  }
  if (decoded === null) {
    decoded = new TextDecoder("utf-8").decode(bytes);
  }

  const declaredEncoding = sniffDeclaredEncoding(decoded);
  if (
    declaredEncoding &&
    declaredEncoding.toLowerCase() !== initialEncoding.toLowerCase()
  ) {
    const redecode = tryDecode(bytes, declaredEncoding);
    if (redecode !== null) {
      return redecode;
    }
  }

  return decoded;
}

function tryDecode(bytes, encoding) {
  try {
    return new TextDecoder(encoding, { fatal: true }).decode(bytes);
  } catch (error) {
    return null;
  }
}

function sniffDeclaredEncoding(text) {
  const head = text.slice(0, 512);
  const xmlMatch = head.match(/encoding\s*=\s*["']([^"']+)["']/i);
  if (xmlMatch?.[1]) {
    return xmlMatch[1].trim();
  }
  const htmlMatch = head.match(/charset\s*=\s*["']?\s*([^"'>\s/]+)/i);
  if (htmlMatch?.[1]) {
    return htmlMatch[1].trim();
  }
  return null;
}

async function epubEventsAndMetadata(file, mode, options) {
  const loadJsZip = options.loadJsZip;
  if (typeof loadJsZip !== "function") {
    throw new Error("EPUB conversion requires a JSZip loader.");
  }

  const JSZip = await loadJsZip();
  const zip = await JSZip.loadAsync(file);
  const opfPath = await containerRootfile(zip);
  const { title, author, spinePaths } = await parsePackage(zip, opfPath, mode);

  const events = [];
  for (let index = 0; index < spinePaths.length; index += 1) {
    const spinePath = spinePaths[index];
    const chapterMarkup = await readZipText(zip, spinePath);
    const chapterEvents = htmlEvents(chapterMarkup, mode);
    if (!chapterEvents.some(([kind]) => kind === "text")) {
      continue;
    }
    if (!chapterEvents.some(([kind]) => kind === "chapter")) {
      chapterEvents.unshift(["chapter", fallbackChapterTitle(spinePath, index + 1, mode)]);
    }
    events.push(...chapterEvents);
  }

  if (events.length === 0) {
    throw new Error("EPUB spine does not contain readable XHTML/HTML content.");
  }

  return {
    title: title || stripExtension(file.name),
    author,
    events,
  };
}

async function containerRootfile(zip) {
  const containerXml = await readZipText(zip, "META-INF/container.xml");
  const doc = parseXmlDocument(containerXml, "EPUB container.xml");

  for (const node of Array.from(doc.getElementsByTagName("*"))) {
    if (localName(node) !== "rootfile") {
      continue;
    }
    const fullPath = node.getAttribute("full-path");
    if (fullPath) {
      return normalizeZipPath(fullPath);
    }
  }

  throw new Error("EPUB container.xml does not name an OPF package file.");
}

async function parsePackage(zip, opfPath, mode) {
  const packageXml = await readZipText(zip, opfPath);
  const doc = parseXmlDocument(packageXml, "EPUB package");
  const title = firstNodeText(doc, "title", mode);
  const author = firstNodeText(doc, "creator", mode);

  const manifest = new Map();
  const manifestContentPaths = [];

  for (const node of Array.from(doc.getElementsByTagName("*"))) {
    if (localName(node) !== "item") {
      continue;
    }
    const itemId = node.getAttribute("id");
    const href = node.getAttribute("href");
    const mediaType = node.getAttribute("media-type") || "";
    if (!itemId || !href) {
      continue;
    }

    const resolvedPath = zipJoin(opfPath, href);
    manifest.set(itemId, {
      path: resolvedPath,
      mediaType,
    });

    if (isContentDocument(resolvedPath, mediaType)) {
      manifestContentPaths.push(resolvedPath);
    }
  }

  const spinePaths = [];
  for (const node of Array.from(doc.getElementsByTagName("*"))) {
    if (localName(node) !== "itemref") {
      continue;
    }
    const idref = node.getAttribute("idref");
    if (!idref || !manifest.has(idref)) {
      continue;
    }
    const item = manifest.get(idref);
    if (isContentDocument(item.path, item.mediaType)) {
      spinePaths.push(item.path);
    }
  }

  const readingOrder = spinePaths.length > 0 ? spinePaths : manifestContentPaths;
  if (readingOrder.length === 0) {
    throw new Error("EPUB spine does not contain readable XHTML/HTML documents.");
  }

  return { title, author, spinePaths: readingOrder };
}

function firstNodeText(doc, name, mode) {
  for (const node of Array.from(doc.getElementsByTagName("*"))) {
    if (localName(node) !== name) {
      continue;
    }
    const text = cleanText(node.textContent || "", mode);
    if (text) {
      return text;
    }
  }
  return "";
}

function localName(node) {
  if (node.localName) {
    return node.localName.toLowerCase();
  }
  return node.nodeName.toLowerCase().split(":").pop();
}

function isContentDocument(path, mediaType) {
  const loweredPath = path.toLowerCase();
  const loweredType = mediaType.toLowerCase();
  return (
    loweredType === "application/xhtml+xml" ||
    loweredType === "text/html" ||
    loweredPath.endsWith(".xhtml") ||
    loweredPath.endsWith(".html") ||
    loweredPath.endsWith(".htm")
  );
}

function parseXmlDocument(text, label) {
  if (typeof DOMParser === "undefined") {
    throw new Error(`${label} parsing requires DOMParser.`);
  }
  const parser = new DOMParser();
  const doc = parser.parseFromString(text, "application/xml");
  const parserError = doc.querySelector("parsererror");
  if (parserError) {
    throw new Error(`${label} could not be parsed.`);
  }
  return doc;
}

async function readZipText(zip, requestedPath) {
  const entry = findZipEntry(zip, requestedPath);
  if (!entry) {
    throw new Error(`Missing EPUB member: ${requestedPath}`);
  }
  const bytes = await entry.async("uint8array");
  return decodeTextBytes(bytes);
}

function findZipEntry(zip, requestedPath) {
  const normalizedRequested = normalizeZipPath(requestedPath);
  const exact = zip.file(normalizedRequested);
  if (exact) {
    return exact;
  }

  const loweredRequested = normalizedRequested.toLowerCase();
  return (
    Object.values(zip.files).find(
      (entry) => normalizeZipPath(entry.name).toLowerCase() === loweredRequested,
    ) || null
  );
}

function normalizeZipPath(path) {
  return path.replace(/\\/g, "/").replace(/^\/+/, "");
}

function zipJoin(base, href) {
  const withoutFragment = href.split("#", 1)[0].split("?", 1)[0];
  let decoded = withoutFragment;
  try {
    decoded = decodeURIComponent(withoutFragment);
  } catch (error) {
    decoded = withoutFragment;
  }

  if (decoded.startsWith("/")) {
    decoded = decoded.replace(/^\/+/, "");
  } else {
    decoded = `${zipDirname(base)}${decoded}`;
  }

  return collapseZipPath(decoded);
}

function zipDirname(path) {
  const normalized = normalizeZipPath(path);
  const slashIndex = normalized.lastIndexOf("/");
  if (slashIndex < 0) {
    return "";
  }
  return normalized.slice(0, slashIndex + 1);
}

function collapseZipPath(path) {
  const parts = [];
  for (const part of normalizeZipPath(path).split("/")) {
    if (!part || part === ".") {
      continue;
    }
    if (part === "..") {
      parts.pop();
      continue;
    }
    parts.push(part);
  }
  return parts.join("/");
}

function fallbackChapterTitle(path, index, mode) {
  const base = stripExtension(path.split("/").pop() || `chapter-${index}`);
  const cleaned = cleanText(base.replace(/[_-]+/g, " "), mode);
  return cleaned || `Chapter ${index}`;
}

export function htmlEventsAndTitle(markup, fallbackTitle, mode = DEFAULT_OUTPUT_MODE) {
  if (typeof DOMParser === "undefined") {
    const title = cleanText(titleFromMarkupFallback(markup) || fallbackTitle, mode) || fallbackTitle;
    return {
      title,
      events: htmlEventsFallback(markup, mode),
    };
  }

  const parser = new DOMParser();
  const doc = parser.parseFromString(markup, "text/html");
  const title = cleanText(doc.title || fallbackTitle, mode) || fallbackTitle;
  return {
    title,
    events: htmlEvents(markup, mode),
  };
}

export function htmlEvents(markup, mode = DEFAULT_OUTPUT_MODE) {
  if (typeof DOMParser === "undefined") {
    return htmlEventsFallback(markup, mode);
  }

  const parser = new DOMParser();
  const doc = parser.parseFromString(markup, "text/html");
  const events = [];
  const textParts = [];

  const flushText = () => {
    const text = cleanText(textParts.join(" "), mode);
    textParts.length = 0;
    if (text) {
      events.push(["text", text]);
    }
  };

  const visit = (node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      textParts.push(node.nodeValue || "");
      return;
    }

    if (node.nodeType !== Node.ELEMENT_NODE) {
      return;
    }

    const tag = node.tagName.toLowerCase();
    if (SKIP_TAGS.has(tag)) {
      return;
    }

    if (HEADING_TAGS.has(tag)) {
      flushText();
      const chapterTitle = cleanText(node.textContent || "", mode);
      if (chapterTitle) {
        events.push(["chapter", chapterTitle]);
      }
      return;
    }

    if (tag === "br") {
      flushText();
      return;
    }

    const isBlock = BLOCK_TAGS.has(tag);
    if (isBlock) {
      flushText();
    }

    for (const child of Array.from(node.childNodes)) {
      visit(child);
    }

    if (isBlock) {
      flushText();
    }
  };

  const root = doc.body || doc.documentElement;
  visit(root);
  flushText();
  return events;
}

function htmlEventsFallback(markup, mode) {
  let text = markup;
  for (const tag of SKIP_TAGS) {
    text = text.replace(new RegExp(`<${tag}\\b[\\s\\S]*?<\\/${tag}>`, "gi"), " ");
  }
  text = text.replace(/<h[1-6][^>]*>([\s\S]*?)<\/h[1-6]>/gi, `${BLOCK_BREAK}@chapter $1${BLOCK_BREAK}`);
  text = text.replace(/<br\b[^>]*>/gi, BLOCK_BREAK);
  text = text.replace(
    new RegExp(`</(${Array.from(BLOCK_TAGS).join("|")})\\b[^>]*>`, "gi"),
    BLOCK_BREAK,
  );
  text = text.replace(
    new RegExp(`<(${Array.from(BLOCK_TAGS).join("|")})\\b[^>]*>`, "gi"),
    " ",
  );
  text = text.replace(/<[^>]+>/g, " ");
  text = decodeHtmlEntitiesFallback(text);
  text = text.replace(/[\r\n\t]+/g, " ").replaceAll(BLOCK_BREAK, "\n");

  return text
    .split("\n")
    .map((line) => cleanText(line, mode))
    .filter((line) => line)
    .map((line) => {
      if (line.toLowerCase().startsWith("@chapter ")) {
        const chapter = cleanText(line.slice("@chapter ".length), mode);
        return chapter ? ["chapter", chapter] : null;
      }
      return ["text", line];
    })
    .filter((event) => event !== null);
}

function titleFromMarkupFallback(markup) {
  const match = markup.match(/<title\b[^>]*>([\s\S]*?)<\/title>/i);
  return match ? decodeHtmlEntitiesFallback(match[1].replace(/<[^>]+>/g, " ")) : "";
}

function decodeHtmlEntitiesFallback(text) {
  const named = {
    amp: "&",
    lt: "<",
    gt: ">",
    quot: '"',
    "#39": "'",
    apos: "'",
    nbsp: " ",
    ldquo: '"',
    rdquo: '"',
    lsquo: "'",
    rsquo: "'",
    ndash: "-",
    mdash: "-",
    hellip: "...",
  };

  return text.replace(/&(#x[0-9a-f]+|#\d+|[a-z][a-z0-9]+);/gi, (entity, token) => {
    const lowered = token.toLowerCase();
    if (Object.hasOwn(named, lowered)) {
      return named[lowered];
    }
    if (lowered.startsWith("#x")) {
      return String.fromCodePoint(Number.parseInt(lowered.slice(2), 16));
    }
    if (lowered.startsWith("#")) {
      return String.fromCodePoint(Number.parseInt(lowered.slice(1), 10));
    }
    return entity;
  });
}

export function textEvents(text, mode = DEFAULT_OUTPUT_MODE) {
  const events = [];
  const paragraphParts = [];

  const flushParagraph = () => {
    if (paragraphParts.length === 0) {
      return;
    }
    const paragraph = cleanText(paragraphParts.join(" "), mode);
    paragraphParts.length = 0;
    if (paragraph) {
      events.push(["text", paragraph]);
    }
  };

  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    const chapter = looksLikeChapter(line, mode);
    if (chapter) {
      flushParagraph();
      events.push(["chapter", chapter]);
      continue;
    }

    if (!line) {
      flushParagraph();
      continue;
    }

    paragraphParts.push(line);
  }

  flushParagraph();
  return events;
}

export function convertTextToRsvp({ filename, title, source, text, mode = DEFAULT_OUTPUT_MODE }) {
  const resolvedTitle = title || stripExtension(filename);
  return eventsToRsvp({
    title: resolvedTitle,
    author: "",
    source: source || filename,
    events: textEvents(text, mode),
    mode,
  });
}

export function convertHtmlToRsvp({ filename, title, source, markup, mode = DEFAULT_OUTPUT_MODE }) {
  const fallbackTitle = title || stripExtension(filename);
  const html = htmlEventsAndTitle(markup, fallbackTitle, mode);
  return eventsToRsvp({
    title: title || html.title,
    author: "",
    source: source || filename,
    events: html.events,
    mode,
  });
}

export function eventsToRsvp({ title, author = "", source, events, mode = DEFAULT_OUTPUT_MODE }) {
  const writer = new RsvpWriter({
    title,
    author,
    source,
    mode,
  });

  for (const [kind, value] of events) {
    if (kind === "chapter") {
      writer.addChapter(value);
      continue;
    }
    writer.beginParagraph();
    writer.addText(value);
  }

  return writer.finalize(title);
}

function looksLikeChapter(line, mode) {
  const trimmed = cleanText(line, mode);
  if (!trimmed || trimmed.length > 64) {
    return null;
  }

  if (trimmed.startsWith("#")) {
    const title = cleanText(trimmed.replace(/^#+/, "").trim(), mode);
    return title || null;
  }

  if (/^(chapter|part|book)\b/i.test(trimmed)) {
    return trimmed;
  }

  return null;
}

export class RsvpWriter {
  constructor({ title, author, source, mode }) {
    this.title = directiveText(title, mode);
    this.author = directiveText(author, mode);
    this.mode = mode;
    this.lines = [`@rsvp ${RSVP_VERSION}`, `@title ${this.title}`];
    if (this.author) {
      this.lines.push(`@author ${this.author}`);
    }
    this.lines.push(`@source ${directiveText(source, mode)}`);
    this.lines.push("");
    this.wordCount = 0;
    this.chapterCount = 0;
    this.lineWords = [];
    this.lineLength = 0;
    this.lastChapter = "";
  }

  addChapter(title) {
    const value = directiveText(title, this.mode);
    if (!value || value === this.lastChapter) {
      return;
    }
    this.flushLine();
    if (this.lines.length > 0 && this.lines[this.lines.length - 1] !== "") {
      this.lines.push("");
    }
    this.lines.push(`@chapter ${value}`);
    this.chapterCount += 1;
    this.lastChapter = value;
  }

  beginParagraph() {
    this.flushLine();
    if (this.wordCount > 0) {
      if (this.lines.length > 0 && this.lines[this.lines.length - 1] !== "") {
        this.lines.push("");
      }
      this.lines.push("@para");
    }
  }

  addText(text) {
    const readableWords = iterCleanWords(text, this.mode);
    let readableIndex = 0;
    for (const word of iterOutputTokens(text, this.mode)) {
      const projected =
        this.lineWords.length === 0 ? word.length : this.lineLength + 1 + word.length;
      if (this.lineWords.length > 0 && projected > WRAP_WIDTH) {
        this.flushLine();
      }

      this.lineWords.push(word);
      this.lineLength =
        this.lineWords.length === 1 ? word.length : this.lineLength + 1 + word.length;
      if (readableIndex < readableWords.length && word === readableWords[readableIndex]) {
        this.wordCount += 1;
        readableIndex += 1;
      }
    }
  }

  flushLine() {
    if (this.lineWords.length === 0) {
      return;
    }
    let line = this.lineWords.join(" ");
    if (line.startsWith("@")) {
      line = `@${line}`;
    }
    this.lines.push(line);
    this.lineWords = [];
    this.lineLength = 0;
  }

  finalize(fallbackChapterTitle) {
    this.flushLine();
    if (this.wordCount === 0) {
      throw new Error("No readable words found in this source.");
    }

    if (this.chapterCount === 0) {
      const chapter = `@chapter ${directiveText(fallbackChapterTitle, this.mode)}`;
      const insertIndex = this.lines.findIndex((line) => line === "");
      if (insertIndex >= 0) {
        this.lines.splice(insertIndex, 0, chapter);
      } else {
        this.lines.push(chapter);
      }
    }

    return `${this.lines.join("\n").trim()}\n`;
  }
}

export function iterCleanWords(text, mode = DEFAULT_OUTPUT_MODE) {
  const cleaned = cleanText(text, mode);
  if (!cleaned) {
    return [];
  }

  return cleaned
    .split(/\s+/)
    .filter((token) => token && /[\p{L}\p{N}]/u.test(token));
}

export function iterOutputTokens(text, mode = DEFAULT_OUTPUT_MODE) {
  const cleaned = cleanText(text, mode);
  if (!cleaned) {
    return [];
  }

  return cleaned.split(/\s+/).filter((token) => token);
}

export function cleanText(text, mode = DEFAULT_OUTPUT_MODE) {
  let value = text || "";
  value = value.replace(SPACE_LIKE_RE, " ").replace(/\uFFFD/g, "");

  if (mode === "ascii") {
    value = Array.from(value, (character) => ASCII_REPLACEMENTS[character] ?? character).join("");
    value = value.replace(/[\uFF01-\uFF5E]/g, (character) =>
      String.fromCharCode(character.charCodeAt(0) - 0xfee0),
    );
    value = value.normalize("NFKD").replace(COMBINING_MARKS_RE, "");
    value = value.replace(/[^\x20-\x7E]/g, "");
  } else {
    value = value.normalize("NFC");
  }

  return value.replace(/\s+/g, " ").trim();
}

export function directiveText(text, mode = DEFAULT_OUTPUT_MODE) {
  return cleanText(text, mode).replace(/[\r\n]+/g, " ");
}

export function extensionForName(name) {
  const lastDot = name.lastIndexOf(".");
  return lastDot >= 0 ? name.slice(lastDot).toLowerCase() : "";
}

export function stripExtension(name) {
  const lastSlash = Math.max(name.lastIndexOf("/"), name.lastIndexOf("\\"));
  const base = lastSlash >= 0 ? name.slice(lastSlash + 1) : name;
  const lastDot = base.lastIndexOf(".");
  return lastDot > 0 ? base.slice(0, lastDot) : base;
}
