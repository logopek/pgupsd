package ni.shikatu.pgupsd.data.plan

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import ni.shikatu.pgupsd.domain.Curriculum
import java.io.ByteArrayInputStream

object PdfPlanLoader {
    @Volatile private var ready = false

    fun parse(context: Context, bytes: ByteArray): Curriculum {
        if (!ready) {
            PDFBoxResourceLoader.init(context.applicationContext)
            ready = true
        }
        PDDocument.load(ByteArrayInputStream(bytes)).use { document ->
            val stripper = TokenStripper()
            stripper.sortByPosition = true
            stripper.getText(document)
            return PlanTableParser.parse(stripper.pages)
        }
    }

    private class TokenStripper : PDFTextStripper() {
        val pages = mutableListOf<MutableList<PlanGlyph>>()
        private var current = mutableListOf<PlanGlyph>()

        override fun startPage(page: PDPage) {
            current = mutableListOf()
            pages.add(current)
            super.startPage(page)
        }

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            val clean = text.trim()
            if (clean.isEmpty() || textPositions.isEmpty()) return
            val first = textPositions.first()
            current.add(PlanGlyph(first.xDirAdj, first.yDirAdj, clean))
        }
    }
}
