package nl.dimaruns.generator.content

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SourceDocumentTest :
    FunSpec({
        test("splits front-matter from body") {
            val doc = splitFrontMatter("---\ntitle: Hi\ndate: 2026-01-01\n---\n# Heading\n\nText")
            doc.frontMatter shouldBe "title: Hi\ndate: 2026-01-01"
            doc.body shouldBe "# Heading\n\nText"
        }

        test("handles CRLF line endings and a UTF-8 BOM") {
            val doc = splitFrontMatter("﻿---\r\ntitle: Hi\r\n---\r\nBody")
            doc.frontMatter shouldBe "title: Hi"
            doc.body shouldBe "Body"
        }

        test("a horizontal rule in the body is not mistaken for the delimiter") {
            splitFrontMatter("---\ntitle: Hi\n---\nA\n\n---\n\nB").body shouldBe "A\n\n---\n\nB"
        }

        test("rejects a file without front-matter") {
            shouldThrow<IllegalArgumentException> { splitFrontMatter("# Just markdown") }
                .message shouldContain "missing front-matter"
        }

        test("rejects unterminated front-matter") {
            shouldThrow<IllegalArgumentException> { splitFrontMatter("---\ntitle: Hi\n") }
                .message shouldContain "unterminated"
        }
    })
