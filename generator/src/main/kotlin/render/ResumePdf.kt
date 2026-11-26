package dimaruns.generator.render

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import java.nio.file.Path
import kotlin.io.path.outputStream

object ResumePdf {
    private val fonts = mapOf("Barlow-Regular.ttf" to 400, "Barlow-SemiBold.ttf" to 600, "Barlow-Bold.ttf" to 700)

    fun write(
        xhtml: String,
        file: Path,
    ) {
        file.outputStream().use { stream ->
            PdfRendererBuilder()
                .apply {
                    fonts.forEach { (name, weight) ->
                        useFont(
                            { ResumePdf::class.java.getResourceAsStream("/fonts/$name") },
                            "Barlow",
                            weight,
                            FontStyle.NORMAL,
                            true,
                        )
                    }
                    withHtmlContent(xhtml, null)
                    toStream(stream)
                }.run()
        }
    }
}
