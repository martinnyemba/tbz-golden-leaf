package zm.co.tbz.goldenleaf.ui.components

/**
 * Title-cases each word so server data stored in all-caps (names like
 * "MARTIN NYEMBA", roles like "FIELD OFFICER") renders as "Martin Nyemba" /
 * "Field Officer". Splits on whitespace and hyphens so "NORTH-WESTERN" becomes
 * "North-Western". Blank input is returned unchanged.
 */
fun String.toTitleCase(): String {
    if (isBlank()) return this
    val builder = StringBuilder(length)
    var capitalizeNext = true
    for (ch in this) {
        if (ch.isWhitespace() || ch == '-' || ch == '/') {
            capitalizeNext = true
            builder.append(ch)
        } else if (capitalizeNext) {
            builder.append(ch.uppercaseChar())
            capitalizeNext = false
        } else {
            builder.append(ch.lowercaseChar())
        }
    }
    return builder.toString()
}
