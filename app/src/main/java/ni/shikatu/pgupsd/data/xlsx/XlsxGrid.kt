package ni.shikatu.pgupsd.data.xlsx

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

internal data class CellRef(val row: Int, val col: Int)

internal data class Merge(
    val from: CellRef,
    val to: CellRef,
) {
    fun contains(row: Int, col: Int): Boolean =
        row in from.row..to.row && col in from.col..to.col

    val origin: CellRef get() = from
}

internal class XlsxGrid(
    private val cells: Map<CellRef, String>,
    val merges: List<Merge>,
    val maxRow: Int,
    val maxCol: Int,
) {
    fun raw(row: Int, col: Int): String = cells[CellRef(row, col)].orEmpty()

    fun value(row: Int, col: Int): String {
        val direct = raw(row, col)
        if (direct.isNotBlank()) return direct
        val merge = mergeAt(row, col) ?: return ""
        return raw(merge.origin.row, merge.origin.col)
    }

    fun mergeAt(row: Int, col: Int): Merge? = merges.firstOrNull { it.contains(row, col) }

    companion object {
        fun from(input: InputStream): XlsxGrid {
            val files = LinkedHashMap<String, ByteArray>()
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    files[entry.name] = zip.readBytes()
                    zip.closeEntry()
                }
            }
            val shared = parseSharedStrings(files["xl/sharedStrings.xml"])
            val sheetPath = resolveFirstSheet(files)
                ?: files.keys.firstOrNull { it.startsWith("xl/worksheets/sheet") }
                ?: error("В xlsx нет листа")
            val sheetBytes = files[sheetPath] ?: error("Не найден $sheetPath")
            return parseSheet(sheetBytes, shared)
        }

        private fun resolveFirstSheet(files: Map<String, ByteArray>): String? {
            val workbook = files["xl/workbook.xml"] ?: return null
            val rels = files["xl/_rels/workbook.xml.rels"] ?: return null
            val wb = parseXml(workbook)
            val relDoc = parseXml(rels)
            val relMap = mutableMapOf<String, String>()
            val relNodes = relDoc.getElementsByTagName("Relationship")
            for (i in 0 until relNodes.length) {
                val el = relNodes.item(i) as Element
                relMap[el.getAttribute("Id")] = el.getAttribute("Target")
            }
            val sheets = wb.getElementsByTagName("sheet")
            if (sheets.length == 0) return null
            val first = sheets.item(0) as Element
            val rid = first.getAttribute("r:id").ifBlank {
                first.getAttributeNS(
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
                    "id",
                )
            }
            var target = relMap[rid] ?: return null
            if (target.startsWith("/")) target = target.drop(1)
            if (!target.startsWith("xl/")) target = "xl/$target"
            return target
        }

        private fun parseSharedStrings(bytes: ByteArray?): List<String> {
            if (bytes == null) return emptyList()
            val doc = parseXml(bytes)
            val result = mutableListOf<String>()
            val sis = doc.getElementsByTagName("si")
            for (i in 0 until sis.length) {
                val si = sis.item(i) as Element
                val texts = si.getElementsByTagName("t")
                val sb = StringBuilder()
                for (j in 0 until texts.length) {
                    sb.append(texts.item(j).textContent.orEmpty())
                }
                result += sb.toString()
            }
            return result
        }

        private fun parseSheet(bytes: ByteArray, shared: List<String>): XlsxGrid {
            val doc = parseXml(bytes)
            val merges = mutableListOf<Merge>()
            val mergeNodes = doc.getElementsByTagName("mergeCell")
            for (i in 0 until mergeNodes.length) {
                val ref = (mergeNodes.item(i) as Element).getAttribute("ref")
                parseMerge(ref)?.let(merges::add)
            }
            val cells = mutableMapOf<CellRef, String>()
            var maxRow = 1
            var maxCol = 1
            val cellNodes = doc.getElementsByTagName("c")
            for (i in 0 until cellNodes.length) {
                val el = cellNodes.item(i) as Element
                if (el.parentNode?.nodeName != "row" &&
                    el.parentNode?.localName != "row"
                ) {
                    continue
                }
                val ref = el.getAttribute("r")
                if (ref.isBlank()) continue
                val cellRef = parseCellRef(ref) ?: continue
                val value = cellValue(el, shared)
                if (value.isNotEmpty()) {
                    cells[cellRef] = value
                    if (cellRef.row > maxRow) maxRow = cellRef.row
                    if (cellRef.col > maxCol) maxCol = cellRef.col
                }
            }
            return XlsxGrid(cells, merges, maxRow, maxCol)
        }

        private fun cellValue(el: Element, shared: List<String>): String {
            val type = el.getAttribute("t")
            if (type == "inlineStr") {
                val texts = el.getElementsByTagName("t")
                val sb = StringBuilder()
                for (i in 0 until texts.length) sb.append(texts.item(i).textContent.orEmpty())
                return sb.toString()
            }
            val v = firstChildNamed(el, "v")?.textContent ?: return ""
            return if (type == "s") {
                v.toIntOrNull()?.let { shared.getOrNull(it) }.orEmpty()
            } else {
                v
            }
        }

        private fun firstChildNamed(el: Element, name: String): Element? {
            val children = el.childNodes
            for (i in 0 until children.length) {
                val n = children.item(i)
                if (n.nodeType == Node.ELEMENT_NODE &&
                    (n.nodeName == name || n.localName == name)
                ) {
                    return n as Element
                }
            }
            return null
        }

        private fun parseXml(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            try {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            } catch (_: Exception) {
            }
        }.newDocumentBuilder().parse(bytes.inputStream())

        private fun parseMerge(ref: String): Merge? {
            val parts = ref.split(":")
            if (parts.size != 2) return null
            val a = parseCellRef(parts[0]) ?: return null
            val b = parseCellRef(parts[1]) ?: return null
            return Merge(a, b)
        }

        fun parseCellRef(ref: String): CellRef? {
            val letters = StringBuilder()
            val digits = StringBuilder()
            for (ch in ref) {
                if (ch.isLetter()) letters.append(ch.uppercaseChar())
                else if (ch.isDigit()) digits.append(ch)
            }
            if (letters.isEmpty() || digits.isEmpty()) return null
            var col = 0
            for (ch in letters) col = col * 26 + (ch - 'A' + 1)
            return CellRef(row = digits.toString().toInt(), col = col)
        }
    }
}
