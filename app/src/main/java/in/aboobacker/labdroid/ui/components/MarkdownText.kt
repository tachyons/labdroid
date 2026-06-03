package `in`.aboobacker.labdroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import `in`.aboobacker.labdroid.ui.theme.LabdroidTheme
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

val LocalMarkdownBaseUrl = staticCompositionLocalOf { "https://gitlab.com" }

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    baseUrl: String = LocalMarkdownBaseUrl.current,
    onReferenceClick: (String) -> Unit = {}
) {
    val flavour = remember { GFMFlavourDescriptor() }
    val parser = remember { MarkdownParser(flavour) }
    val rootNode = remember(markdown) { parser.buildMarkdownTreeFromString(markdown) }

    Column(
        modifier = modifier.widthIn(max = 800.dp)
    ) {
        MarkdownNode(rootNode, markdown, baseUrl, onReferenceClick)
    }
}

@Composable
fun MarkdownNode(
    node: ASTNode,
    fullText: String,
    baseUrl: String,
    onReferenceClick: (String) -> Unit
) {
    val children = node.children
    var i = 0

    // Detect Front Matter at the start of the document
    if (i < children.size && (node.type == MarkdownElementTypes.MARKDOWN_FILE || node.type == MarkdownElementTypes.PARAGRAPH)) {
        val firstText = children[0].getTextInNode(fullText).toString()
        if (firstText.startsWith("---") || firstText.startsWith("+++")) {
            val lines = firstText.lines()
            val delimiter = if (firstText.startsWith("---")) "---" else "+++"
            val endIdx = lines.drop(1).indexOfFirst { it.trim() == delimiter }
            if (endIdx != -1) {
                val content = lines.subList(1, endIdx + 1).joinToString("\n")
                FrontMatterBlock(content)
                i++ // Consume this node
            }
        }
    }

    while (i < children.size) {
        val child = children[i]

        // Special handling for multiline blockquotes fenced with >>>
        val nodeText = child.getTextInNode(fullText).toString().trim()
        if (nodeText.startsWith(">>>") && nodeText.endsWith(">>>") && nodeText.length >= 6) {
            val content = nodeText.removePrefix(">>>").removeSuffix(">>>").trimIndent()
            val annotatedString = buildAnnotatedString {
                appendWithRichFeatures(content, onReferenceClick)
            }
            BlockQuote(annotatedString)
            i++
            continue
        }

        if (!isBlockElement(child.type) && child.type != MarkdownTokenTypes.EOL) {
            // Check if this is an inline element that we should treat as a block image
            if (isInlineImage(child, fullText)) {
                val url = extractImageUrl(child, fullText)
                if (url != null) {
                    MarkdownImage(resolveUrl(url, baseUrl), "", null, null)
                    i++
                    continue
                }
            }

            val annotatedString = buildAnnotatedString {
                while (i < children.size && !isBlockElement(children[i].type)) {
                    appendMarkdownChildren(children[i], fullText, onReferenceClick)
                    i++
                }
            }
            if (annotatedString.isNotBlank()) {
                StyledText(annotatedString)
            }
            continue
        }

        when (child.type) {
            MarkdownElementTypes.ATX_1 -> Header(getAtxHeaderText(child, fullText), 1)
            MarkdownElementTypes.ATX_2 -> Header(getAtxHeaderText(child, fullText), 2)
            MarkdownElementTypes.ATX_3 -> Header(getAtxHeaderText(child, fullText), 3)
            MarkdownElementTypes.ATX_4 -> Header(getAtxHeaderText(child, fullText), 4)
            MarkdownElementTypes.ATX_5 -> Header(getAtxHeaderText(child, fullText), 5)
            MarkdownElementTypes.ATX_6 -> Header(getAtxHeaderText(child, fullText), 6)

            MarkdownElementTypes.BLOCK_QUOTE -> {
                val quoteText = child.getTextInNode(fullText).toString()

                val alertMatch = Regex(
                    "^>\\s*\\[!(note|tip|important|caution|warning)]",
                    RegexOption.IGNORE_CASE
                ).find(quoteText)

                if (alertMatch != null) {
                    val type = alertMatch.groupValues[1].lowercase()
                    val content = quoteText.substring(alertMatch.range.last + 1).trimIndent()
                        .lines().joinToString("\n") { it.removePrefix(">").trim() }
                    AlertBlock(type, content, baseUrl, onReferenceClick)
                } else {
                    val annotatedString = buildAnnotatedString {
                        child.children
                            .filter { it.type != MarkdownTokenTypes.BLOCK_QUOTE }
                            .forEach { appendMarkdownChildren(it, fullText, onReferenceClick) }
                    }
                    BlockQuote(annotatedString)
                }
            }

            MarkdownElementTypes.LIST_ITEM -> {
                val itemText = child.getTextInNode(fullText).toString()
                val taskMatch =
                    Regex("^\\s*[-*+]\\s*\\[([ x~])]", RegexOption.IGNORE_CASE).find(itemText)

                if (taskMatch != null) {
                    val status = taskMatch.groupValues[1]
                    TaskListItem(status) {
                        taskMatch.value
                        var prefixStripped = false
                        child.children.forEach { subChild ->
                            if (subChild.type == MarkdownTokenTypes.LIST_BULLET || subChild.type == MarkdownTokenTypes.WHITE_SPACE) {
                                return@forEach
                            }

                            val subChildText = subChild.getTextInNode(fullText).toString()
                            if (!prefixStripped && subChildText.contains("[")) {
                                // This node contains the checkbox, we need to strip it
                                if (subChild.children.isEmpty()) {
                                    val index = subChildText.indexOf(']')
                                    if (index != -1) {
                                        val remaining = subChildText.substring(index + 1).trim()
                                        if (remaining.isNotEmpty()) {
                                            StyledText(buildAnnotatedString {
                                                appendWithRichFeatures(remaining, onReferenceClick)
                                            })
                                        }
                                    }
                                    prefixStripped = true
                                } else {
                                    // It has children (like PARAGRAPH), strip from its first text child
                                    var paragraphPrefixStripped = false
                                    subChild.children.forEach { leaf ->
                                        val leafText = leaf.getTextInNode(fullText).toString()
                                        if (!paragraphPrefixStripped && leafText.contains("[")) {
                                            val index = leafText.indexOf(']')
                                            if (index != -1) {
                                                val remaining = leafText.substring(index + 1).trim()
                                                if (remaining.isNotEmpty()) {
                                                    StyledText(buildAnnotatedString {
                                                        appendWithRichFeatures(
                                                            remaining,
                                                            onReferenceClick
                                                        )
                                                    })
                                                }
                                                paragraphPrefixStripped = true
                                            }
                                        } else {
                                            MarkdownNode(leaf, fullText, baseUrl, onReferenceClick)
                                        }
                                    }
                                    prefixStripped = true
                                }
                            } else {
                                MarkdownNode(subChild, fullText, baseUrl, onReferenceClick)
                            }
                        }
                    }
                } else {
                    ListItem {
                        child.children
                            .filter { it.type != MarkdownTokenTypes.LIST_BULLET && it.type != MarkdownTokenTypes.WHITE_SPACE }
                            .forEach { MarkdownNode(it, fullText, baseUrl, onReferenceClick) }
                    }
                }
            }

            MarkdownElementTypes.CODE_BLOCK, MarkdownElementTypes.CODE_FENCE -> {
                val code = child.children
                    .filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT || it.type == MarkdownTokenTypes.TEXT }
                    .joinToString("") { it.getTextInNode(fullText) }
                CodeBlock(code)
            }

            MarkdownElementTypes.IMAGE -> {
                val url = extractImageUrl(child, fullText)
                val alt = extractImageAlt(child, fullText)

                var imageWidth: String? = null
                var imageHeight: String? = null

                // Look ahead for attributes like {width=100 height=100}
                var nextIdx = i + 1
                while (nextIdx < children.size &&
                    (children[nextIdx].type == MarkdownTokenTypes.WHITE_SPACE ||
                            children[nextIdx].type == MarkdownTokenTypes.EOL)
                ) {
                    nextIdx++
                }

                if (nextIdx < children.size) {
                    val nextChild = children[nextIdx]
                    val nextText = nextChild.getTextInNode(fullText).toString().trim()
                    if (nextText.startsWith("{") && nextText.endsWith("}") &&
                        (nextText.contains("width=") || nextText.contains("height="))
                    ) {
                        val attrs = nextText.removeSurrounding("{", "}")
                        imageWidth =
                            Regex("width=(\\d+%?|\\d+px?)").find(attrs)?.groupValues?.get(1)
                        imageHeight =
                            Regex("height=(\\d+%?|\\d+px?)").find(attrs)?.groupValues?.get(1)
                        i = nextIdx // Consume the attribute node
                    }
                }

                if (url != null) {
                    MarkdownImage(resolveUrl(url, baseUrl), alt, imageWidth, imageHeight)
                }
            }

            MarkdownElementTypes.PARAGRAPH -> {
                MarkdownNode(child, fullText, baseUrl, onReferenceClick)
            }

            MarkdownTokenTypes.HORIZONTAL_RULE -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            GFMElementTypes.TABLE -> TableBlock(child, fullText, baseUrl, onReferenceClick)

            else -> {
                if (child.children.isNotEmpty()) {
                    MarkdownNode(child, fullText, baseUrl, onReferenceClick)
                }
            }
        }
        i++
    }
}

private fun isInlineImage(node: ASTNode, fullText: String): Boolean {
    if (node.type == MarkdownElementTypes.INLINE_LINK ||
        node.type == MarkdownTokenTypes.AUTOLINK ||
        node.type == MarkdownElementTypes.AUTOLINK
    ) {
        val url = extractImageUrl(node, fullText)
        return url != null && isImageExtension(url)
    }
    return false
}

private fun isImageExtension(url: String): Boolean {
    val extensions = listOf(".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp")
    val lowercaseUrl = url.lowercase()
    return extensions.any { lowercaseUrl.contains(it) || lowercaseUrl.endsWith(it) }
}

private fun isBlockElement(type: IElementType): Boolean {
    return type == MarkdownElementTypes.ATX_1 ||
            type == MarkdownElementTypes.ATX_2 ||
            type == MarkdownElementTypes.ATX_3 ||
            type == MarkdownElementTypes.ATX_4 ||
            type == MarkdownElementTypes.ATX_5 ||
            type == MarkdownElementTypes.ATX_6 ||
            type == MarkdownElementTypes.BLOCK_QUOTE ||
            type == MarkdownElementTypes.LIST_ITEM ||
            type == MarkdownElementTypes.CODE_BLOCK ||
            type == MarkdownElementTypes.CODE_FENCE ||
            type == MarkdownElementTypes.IMAGE ||
            type == MarkdownTokenTypes.HORIZONTAL_RULE ||
            type == GFMElementTypes.TABLE ||
            type == MarkdownElementTypes.PARAGRAPH
}

private fun getAtxHeaderText(node: ASTNode, fullText: String): String {
    return node.children
        .find { it.type == MarkdownTokenTypes.ATX_CONTENT }
        ?.getTextInNode(fullText)
        ?.toString()
        ?.trim() ?: ""
}

private fun extractImageUrl(node: ASTNode, fullText: String): String? {
    // 1. Try finding LINK_DESTINATION anywhere in the subtree
    val destinationNode = findFirstChildRecursively(node, MarkdownElementTypes.LINK_DESTINATION)
        ?: findFirstChildRecursively(node, MarkdownTokenTypes.AUTOLINK)
        ?: findFirstChildRecursively(node, MarkdownElementTypes.AUTOLINK)

    val destination = destinationNode?.getTextInNode(fullText)?.toString()?.trim()

    if (destination != null) {
        val url = destination.removeSurrounding("(", ")").removeSurrounding("<", ">").trim()
        if (url.isNotBlank()) return url
    }

    // 2. Fallback: Try to find any text that looks like a URL in the node's own text
    val nodeText = node.getTextInNode(fullText).toString()
    // Match anything in parentheses that looks like a path or URL
    val parenMatch = Regex("\\(([^\\s)]+)\\)").find(nodeText)
    if (parenMatch != null) {
        val url = parenMatch.groupValues[1].trim()
        if (url.isNotBlank() && (url.contains("/") || url.contains("."))) return url
    }

    // 3. Last resort: check for raw URL
    val urlMatch = Regex("https?://[^\\s)]+").find(nodeText)
    if (urlMatch != null) {
        return urlMatch.value.trim().removeSuffix(")")
    }

    return null
}

private fun findFirstChildRecursively(
    node: ASTNode,
    type: IElementType
): ASTNode? {
    if (node.type == type) return node
    for (child in node.children) {
        val found = findFirstChildRecursively(child, type)
        if (found != null) return found
    }
    return null
}

private fun resolveUrl(url: String, baseUrl: String): String {
    val trimmedUrl = url.trim().removeSurrounding("(", ")").removeSurrounding("<", ">").trim()
    if (trimmedUrl.startsWith("http")) {
        return trimmedUrl
    }

    if (trimmedUrl.startsWith("//")) {
        return "https:$trimmedUrl"
    }

    val uri = try {
        java.net.URI(baseUrl)
    } catch (e: Exception) {
        null
    }

    if (trimmedUrl.startsWith("/")) {
        if (uri != null) {
            val origin = "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}"
            // GitLab specific: if it starts with /uploads, it's often project-relative in issue markdown
            // But if baseUrl is already the project URL, we join them.
            // If trimmedUrl starts with / and looks like an absolute path (/group/project/uploads/...), 
            // we should use origin.

            return if (trimmedUrl.startsWith("/uploads") || trimmedUrl.startsWith("/-/uploads")) {
                baseUrl.removeSuffix("/") + trimmedUrl
            } else {
                origin + trimmedUrl
            }
        }
    }

    // Relative path
    val base = baseUrl.removeSuffix("/")
    return "$base/${trimmedUrl.removePrefix("/")}"
}

private fun extractImageAlt(node: ASTNode, fullText: String): String {
    return node.children
        .find { it.type == MarkdownElementTypes.LINK_TEXT }
        ?.getTextInNode(fullText)
        ?.toString()
        ?.removeSurrounding("[", "]") ?: ""
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendMarkdownChildren(
    node: ASTNode,
    fullText: String,
    onReferenceClick: (String) -> Unit
) {
    when (node.type) {
        MarkdownTokenTypes.TEXT -> appendWithRichFeatures(
            node.getTextInNode(fullText).toString(),
            onReferenceClick
        )

        MarkdownTokenTypes.WHITE_SPACE -> append(node.getTextInNode(fullText).toString())
        MarkdownTokenTypes.EOL -> append("\n")

        MarkdownElementTypes.EMPH -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                node.children.forEach { appendMarkdownChildren(it, fullText, onReferenceClick) }
            }
        }

        MarkdownElementTypes.STRONG -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                node.children.forEach { appendMarkdownChildren(it, fullText, onReferenceClick) }
            }
        }

        GFMElementTypes.STRIKETHROUGH -> {
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                node.children.forEach { appendMarkdownChildren(it, fullText, onReferenceClick) }
            }
        }

        MarkdownElementTypes.CODE_SPAN -> {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color(0xFFE5E7EB),
                    color = Color(0xFF7E22CE),
                    fontSize = 12.sp
                )
            ) {
                val codeText = node.children
                    .filter { it.type == MarkdownTokenTypes.TEXT }
                    .joinToString("") { it.getTextInNode(fullText) }
                append(codeText)
            }
        }

        MarkdownElementTypes.INLINE_LINK -> {
            val linkText = node.children
                .find { it.type == MarkdownElementTypes.LINK_TEXT }
                ?.getTextInNode(fullText)
                ?.toString()
                ?.removeSurrounding("[", "]")
                ?: ""
            val destination = node.children
                .find { it.type == MarkdownElementTypes.LINK_DESTINATION }
                ?.getTextInNode(fullText)
                ?.toString()
                ?: ""
            val link = LinkAnnotation.Url(
                url = destination,
                linkInteractionListener = {
                    onReferenceClick(destination)
                }
            )
            withStyle(
                SpanStyle(
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                pushLink(link)
                append(linkText)
                pop()
            }
        }

        MarkdownTokenTypes.AUTOLINK, MarkdownElementTypes.AUTOLINK -> {
            val url = node.getTextInNode(fullText).toString().removeSurrounding("<", ">")
            val link = LinkAnnotation.Url(
                url = url,
                linkInteractionListener = {
                    onReferenceClick(url)
                }
            )
            withStyle(
                SpanStyle(
                    color = Color(0xFF2563EB),
                    textDecoration = TextDecoration.Underline
                )
            ) {
                pushLink(link)
                append(url)
                pop()
            }
        }

        else -> {
            if (node.children.isEmpty()) {
                appendWithRichFeatures(node.getTextInNode(fullText).toString(), onReferenceClick)
            } else {
                node.children.forEach { appendMarkdownChildren(it, fullText, onReferenceClick) }
            }
        }
    }
}

private val EMOJI_MAP = mapOf(
    "rocket" to "🚀",
    "bug" to "🐛",
    "heart" to "❤️",
    "tada" to "🎉",
    "smile" to "😊",
    "laughing" to "😆",
    "blush" to "😊",
    "smiley" to "😃",
    "star" to "⭐",
    "thumbsup" to "👍",
    "thumbsdown" to "👎",
    "check" to "✅",
    "warning" to "⚠️",
    "error" to "❌",
    "info" to "ℹ️",
    "note" to "📝",
    "tip" to "💡",
    "important" to "❗",
    "caution" to "⚠️",
    "eyes" to "👀",
    "fire" to "🔥",
    "ok_hand" to "👌",
    "pray" to "🙏",
    "clap" to "👏",
    "confetti_ball" to "🎊",
    "party_popper" to "🎉"
)

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendWithRichFeatures(
    text: String,
    onReferenceClick: (String) -> Unit
) {
    val richRegex =
        Regex("(https?://[\\w./?%&=+#~@!\\-,]+)|(www\\.[\\w./?%&=+#~@!\\-,]+)|(@[\\w.-]+)|(([\\w.-]+/[\\w.-]+)?[#!&%$]\\d+)|(~\"[^\"]+\")|(~[\\w.-]+)|(#[0-9a-fA-F]{3,8})|(\\b[0-9a-fA-F]{7,40}\\b)|(:[\\w+-]+:)|(<kbd>.*?</kbd>)|(<!--.*?-->)")
    var lastIndex = 0
    richRegex.findAll(text).forEach { matchResult ->
        append(text.substring(lastIndex, matchResult.range.first))
        val value = matchResult.value
        when {
            value.startsWith("<!--") -> {
                // Skip HTML comments
            }

            value.startsWith("http") || value.startsWith("www.") -> {
                val url = if (value.startsWith("www.")) "https://$value" else value
                val link = LinkAnnotation.Url(
                    url = url,
                    linkInteractionListener = {
                        onReferenceClick(url)
                    }
                )
                pushLink(link)
                withStyle(
                    SpanStyle(
                        color = Color(0xFF2563EB),
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(value)
                }
                pop()
            }

            value.startsWith("<kbd>") -> {
                val content = value.removePrefix("<kbd>").removeSuffix("</kbd>")
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0xFFE5E7EB),
                        color = Color(0xFF374151),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(" $content ")
                }
            }

            value.startsWith(":") && value.endsWith(":") -> {
                val emojiKey = value.removeSurrounding(":")
                val emoji = EMOJI_MAP[emojiKey]
                if (emoji != null) {
                    append(emoji)
                } else {
                    append(value)
                }
            }

            value.startsWith("~") -> {
                val labelText = value.removePrefix("~").removeSurrounding("\"")
                val link = LinkAnnotation.Url(
                    url = "ref:$value",
                    linkInteractionListener = {
                        onReferenceClick(value)
                    }
                )
                pushLink(link)
                withStyle(
                    SpanStyle(
                        color = Color(0xFF374151),
                        background = Color(0xFFF3F4F6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                ) {
                    append(" $labelText ")
                }
                pop()
            }

            value.startsWith("@") || value.startsWith("!") || value.startsWith("&") ||
                    value.startsWith("%") || value.startsWith("$") ||
                    (value.startsWith("#") && value.drop(1).all { it.isDigit() }) ||
                    (value.contains(Regex("[#!&%$]")) && value.any { it.isDigit() }) -> {
                val link = LinkAnnotation.Url(
                    url = "ref:$value",
                    linkInteractionListener = {
                        onReferenceClick(value)
                    }
                )
                pushLink(link)
                withStyle(SpanStyle(color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)) {
                    append(value)
                }
                pop()
            }

            value.all { it.isDigit() || (it.lowercaseChar() in 'a'..'f') } && value.length >= 7 && !value.startsWith(
                "#"
            ) -> {
                val link = LinkAnnotation.Url(
                    url = "ref:$value",
                    linkInteractionListener = {
                        onReferenceClick(value)
                    }
                )
                pushLink(link)
                withStyle(SpanStyle(color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)) {
                    append(value)
                }
                pop()
            }

            value.startsWith("#") -> { // Color Chip
                try {
                    val color = Color(value.toColorInt())
                    withStyle(
                        SpanStyle(
                            background = color,
                            color = if (isColorDark(color)) Color.White else Color.Black
                        )
                    ) {
                        append(" $value ")
                    }
                } catch (e: Exception) {
                    append(value)
                }
            }

            else -> append(value)
        }
        lastIndex = matchResult.range.last + 1
    }
    append(text.substring(lastIndex))
}


@Composable
fun TableBlock(
    node: ASTNode,
    fullText: String,
    baseUrl: String,
    onReferenceClick: (String) -> Unit
) {
    val header = node.children.find { it.type == GFMElementTypes.HEADER }
    val rows = node.children.filter { it.type == GFMElementTypes.ROW }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
    ) {
        header?.let {
            TableRow(
                it,
                fullText,
                isHeader = true,
                baseUrl = baseUrl,
                onReferenceClick = onReferenceClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        rows.forEachIndexed { index, row ->
            TableRow(
                row,
                fullText,
                isHeader = false,
                baseUrl = baseUrl,
                onReferenceClick = onReferenceClick
            )
            if (index < rows.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun TableRow(
    node: ASTNode,
    fullText: String,
    isHeader: Boolean,
    baseUrl: String,
    onReferenceClick: (String) -> Unit
) {
    val cells = node.children.filter { it.type.toString().contains("CELL") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(if (isHeader) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
    ) {
        cells.forEachIndexed { index, cell ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Column {
                    MarkdownNode(cell, fullText, baseUrl, onReferenceClick)
                }
            }
            if (index < cells.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

@Composable
fun AlertBlock(type: String, content: String, baseUrl: String, onReferenceClick: (String) -> Unit) {
    val (color, icon, label) = when (type) {
        "note" -> Triple(Color(0xFF2563EB), Icons.Default.Info, "Note")
        "tip" -> Triple(Color(0xFF059669), Icons.Default.Info, "Tip")
        "important" -> Triple(Color(0xFF7C3AED), Icons.Default.Info, "Important")
        "caution" -> Triple(Color(0xFFD97706), Icons.Default.Warning, "Caution")
        "warning" -> Triple(Color(0xFFDC2626), Icons.Default.Warning, "Warning")
        else -> Triple(Color.Gray, Icons.Default.Info, type.replaceFirstChar { it.uppercase() })
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            MarkdownText(
                markdown = content,
                baseUrl = baseUrl,
                onReferenceClick = onReferenceClick,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun Header(text: String, level: Int) {
    val (fontSize, weight) = when (level) {
        1 -> 24.sp to FontWeight.Bold
        2 -> 20.sp to FontWeight.Bold
        3 -> 18.sp to FontWeight.SemiBold
        else -> 16.sp to FontWeight.Medium
    }
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = weight,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun StyledText(
    annotatedString: androidx.compose.ui.text.AnnotatedString
) {
    if (annotatedString.text.isNotBlank()) {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
fun ListItem(
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
fun TaskListItem(
    status: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = when (status.lowercase()) {
                "x" -> Icons.Default.CheckCircle
                "~" -> Icons.Default.RadioButtonUnchecked
                else -> Icons.Default.RadioButtonUnchecked
            },
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
            tint = if (status.lowercase() == "x") Color(0xFF059669) else Color.Gray
        )
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            content()
        }
    }
}

@Composable
fun BlockQuote(
    annotatedString: androidx.compose.ui.text.AnnotatedString
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
            )
            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1F2937))
            .padding(12.dp)
    ) {
        Text(
            text = code.trim(),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = Color(0xFF34D399)
        )
    }
}

@Composable
fun MarkdownImage(
    url: String,
    altText: String,
    width: String? = null,
    height: String? = null
) {
    var containerModifier: Modifier = Modifier
        .padding(vertical = 8.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

    var imageModifier: Modifier = Modifier

    if (width != null || height != null) {
        // Handle width
        when {
            width?.endsWith("%") == true -> {
                val fraction = width.removeSuffix("%").toFloatOrNull()?.div(100f) ?: 1f
                containerModifier = containerModifier.fillMaxWidth(fraction)
                imageModifier = imageModifier.fillMaxWidth()
            }

            width != null -> {
                val w = width.removeSuffix("px").toIntOrNull()
                if (w != null) {
                    containerModifier = containerModifier.width(w.dp)
                    imageModifier = imageModifier.width(w.dp)
                } else {
                    containerModifier = containerModifier.fillMaxWidth()
                    imageModifier = imageModifier.fillMaxWidth()
                }
            }

            else -> {
                containerModifier = containerModifier.fillMaxWidth()
                imageModifier = imageModifier.fillMaxWidth()
            }
        }

        // Handle height
        when {
            height?.endsWith("px") == true -> {
                val h = height.removeSuffix("px").toIntOrNull()
                if (h != null) {
                    containerModifier = containerModifier.height(h.dp)
                    imageModifier = imageModifier.height(h.dp)
                }
            }

            height != null && height.all { it.isDigit() } -> {
                val h = height.toIntOrNull()
                if (h != null) {
                    containerModifier = containerModifier.height(h.dp)
                    imageModifier = imageModifier.height(h.dp)
                }
            }

            else -> {
                containerModifier = containerModifier.heightIn(max = 400.dp)
            }
        }
    } else {
        containerModifier = containerModifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 500.dp)
        imageModifier = Modifier.fillMaxWidth()
    }

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = altText.ifBlank { "Markdown Image" },
            modifier = imageModifier.fillMaxWidth(),
            contentScale = if (width != null && height != null) ContentScale.FillBounds else ContentScale.Fit
        )
    }
}

@Composable
fun FrontMatterBlock(content: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF9FAFB))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Text(
            text = content.trim(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF6B7280)
            )
        )
    }
}

@Preview(showBackground = true, name = "Issue Template")
@Composable
fun MarkdownIssueTemplatePreview() {
    val markdown = """
## 🚀 Problem Statement
A brief description of what is broken, missing, or needs improvement. 
> **Note:** Keep it user-centric (e.g., "As a user, I want to...")

### Current Behavior
What is happening right now? (Include screenshots or logs if applicable).

### Expected Behavior
What should happen instead?

---

## 📋 Acceptance Criteria
Use a task list to define when this issue is considered "Done".
- [ ] Feature implemented according to design specs.
- [ ] Unit and integration tests added/updated.
- [ ] Documentation updated in `/doc`.

## 🛠 Proposed Solution / Implementation Details
*Optional: Add technical notes, API endpoints, or database changes.*
* See the blocking issue: #12345
* Refer to the architecture epic: &54321

---

## 🗂 Metadata & Quick Actions
/label ~"Type::Feature" ~"Priority::High" ~"Status::Ready for Dev"
/milestone %"v2.4.0"
/assign @developer_username
/weight 3
"""
    LabdroidTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MarkdownText(markdown)
        }
    }
}

@Preview(showBackground = true, name = "MR Template")
@Composable
fun MarkdownMRTemplatePreview() {
    val markdown = """
## 📝 Description
Provide a concise summary of the changes introduced by this MR. 

Closes #10293 <!-- This automatically closes the issue when merged -->

### 🧬 Type of Change
- [ ] 🐛 Bug fix (non-breaking change which fixes an issue)
- [ ] ✨ New feature (non-breaking change which adds functionality)
- [ ] 💥 Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] 🧹 Chore / Refactor / Documentation

---

## 📸 Screenshots / Screencasts

| Before | After |
| :--- | :--- |
| ![Before Screenshot](https://via.placeholder.com/300x200?text=Before+Screenshot) | ![After Screenshot](https://via.placeholder.com/300x200?text=After+Screenshot) |

---

## 🧪 How Has This Been Tested?
Please describe the tests that you ran to verify your changes.

1. **Local environment:** Run `npm run test:unit`
2. **Manual verification:** 
   - Navigate to `/settings`
   - Click "Toggle Dark Mode" and verify persistence.

---

## 🏁 Checklist
- [ ] My code follows the style guidelines of this project
- [ ] I have performed a self-review of my own code
- [ ] I have commented my code, particularly in hard-to-understand areas

ℹ️ **Reviewer Note:** Please pay extra attention to the state management logic in `authService.ts`.

---

/label ~"Workflow::Ready for Review"
/assign @reviewer_username
/reviewer @senior_dev_username
"""
    LabdroidTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MarkdownText(markdown)
        }
    }
}

@Preview(showBackground = true, name = "Epic Template")
@Composable
fun MarkdownEpicTemplatePreview() {
    val markdown = """
## 🎯 Strategic Objective
A high-level overview of the goal this Epic aims to achieve and why it is important for the product roadmap.

### 📊 Business Value / Impact
* **KPIs Impacted:** Improves user retention by 5%.
* **Target Audience:** Premium tier enterprise users.

---

## 🗺 Scope & Deliverables
The work required to complete this epic is broken down into the following phases:

### Phase 1: Core Infrastructure
- [ ] Setup OAuth2 provider integration -> &6789 (Sub-epic)
- [ ] #11101 DB Migration for new user schemas

### Phase 2: Frontend & UI
- [ ] #11102 Build login/signup component
- [ ] #11103 Implement multi-factor authentication UI

---

## ⚠️ Risks & Dependencies
```gitlab
WARNING: This epic depends heavily on the completion of the API Gateway refactor (&43210). Any delays there will push back Phase 2 of this epic.
```
"""
    LabdroidTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MarkdownText(markdown)
        }
    }
}

@Preview(showBackground = true, name = "Low Hanging Fruits")
@Composable
fun MarkdownLowHangingFruitsPreview() {
    val markdown = """
---
title: GLFM Low Hanging Fruits
status: draft
---

### Emojis
Rocket :rocket: Bug :bug: Heart :heart: Tada :tada:

### Enhanced References
- Label: ~"priority::high"
- Milestone: %v1.0
- Epic: &101
- Snippet: $123

### Keyboard Tags
Press <kbd>Ctrl</kbd> + <kbd>C</kbd> to copy.

### Multiline Blockquote
>>>
This is a multiline blockquote.
It uses the triple arrow syntax.
>>>

### HTML Comments
The following comment should be invisible: <!-- This is hidden --> (Check source if visible)
"""
    LabdroidTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MarkdownText(markdown)
        }
    }
}

@Preview(showBackground = true, name = "Headings & Emphasis")
@Composable
fun MarkdownHeadingsPreview() {
    val markdown = """
# H1 Heading
## H2 Heading
### H3 Heading
#### H4 Heading
##### H5 Heading
###### H6 Heading

---

Emphasis, or italics, with *asterisks* or _underscores_.
Strong emphasis, or bold, with double **asterisks** or __underscores__.
Combined emphasis with **asterisks and _underscores_**.
Strikethrough with double tildes. ~~Scratch this.~~
"""
    LabdroidTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MarkdownText(markdown)
        }
    }
}

@Preview(showBackground = true, name = "Lists & Tasks")
@Composable
fun MarkdownListsPreview() {
    val markdown = """
### Ordered List
1. First ordered list item
2. Another item
   - Unordered sub-list.
1. Actual numbers don't matter
   1. Ordered sub-list
   1. Next item

### Unordered List
- use minuses
* use asterisks
+ use pluses

### Task Lists
- [x] Completed task
- [~] Inapplicable task
- [ ] Incomplete task
  - [x] Sub-task 1
  - [ ] Sub-task 2
"""
    LabdroidTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MarkdownText(markdown)
        }
    }
}

@Preview(showBackground = true, name = "Tables")
@Composable
fun MarkdownTablesPreview() {
    val markdown = """
### Basic Table
 header 1 | header 2 | header 3 |
 ---      | ------   | -------- |
 cell 1   | cell 2   | cell 3   |
 cell 4   | cell 5   | cell 6   |

### Aligned Table
 Left Aligned | Centered | Right Aligned |
 :----------- | :------: | ------------: |
 Cell 1       | Cell 2   | Cell 3        |
 Cell 4       | Cell 5   | Cell 6        |
"""
    LabdroidTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MarkdownText(markdown)
        }
    }
}

@Preview(showBackground = true, name = "Alerts & Quotes")
@Composable
fun MarkdownAlertsPreview() {
    val markdown = """
> [!note]
> This is a note alert.

> [!tip]
> This is a tip alert.

> [!important]
> This is an important alert.

> [!warning]
> This is a warning alert.

> [!caution]
> This is a caution alert.

> This is a regular blockquote.
> It can span multiple lines.
"""
    LabdroidTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MarkdownText(markdown)
        }
    }
}

@Preview(showBackground = true, name = "References & Colors")
@Composable
fun MarkdownReferencesPreview() {
    val markdown = """
### GitLab References
- User: @user
- Issue: #123
- MR: !456
- Snippet: $789
- Epic: &101
- Commit: 9ba12248

Link: http://gitlab.com/tachyons/labdroid

### Color Chips
- HEX: #FF5733
- RGB: #00FF00
- Another: #2563EB
"""
    LabdroidTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MarkdownText(markdown)
        }
    }
}

@Preview(showBackground = true, name = "Code & Media")
@Composable
fun MarkdownCodePreview() {
    val markdown = """
### Inline Code
Use `git commit` to save changes.

### Code Block
```kotlin
fun main() {
    println("Hello, GitLab!")
}
```

### Images
![GitLab Logo](https://about.gitlab.com/images/press/logos/gitlab-icon-rgb.png){width=100 height=100}
"""
    LabdroidTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MarkdownText(markdown)
        }
    }
}
