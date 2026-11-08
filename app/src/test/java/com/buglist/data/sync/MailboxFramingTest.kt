package com.buglist.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class MailboxFramingTest {
    @Test
    fun shortCiphertextStaysOneMessage() {
        val payload = "abc".repeat(100)
        assertEquals(listOf(payload), MailboxFraming.frames(payload))
        assertEquals(listOf(payload), MailboxFraming.assemble(listOf(payload)))
    }

    @Test
    fun longCiphertextRoundTripsAndDropsAHalf() {
        val payload = "A".repeat(MailboxFraming.INLINE_LIMIT + 50)
        val frames = MailboxFraming.frames(payload)
        assertEquals(payload, MailboxFraming.assemble(frames).single())
        assertEquals(emptyList<String>(), MailboxFraming.assemble(frames.dropLast(1)))
    }

    @Test
    fun keepsSmallNotesAroundASplitBook() {
        val book = "B".repeat(5_000)
        val bodies = listOf("hello-cipher") + MailboxFraming.frames(book) + listOf("role-cipher")
        assertEquals(listOf("hello-cipher", book, "role-cipher"), MailboxFraming.assemble(bodies))
    }

    @Test
    fun ignoresAttachmentPlaceholder() {
        assertEquals(
            listOf("still-here"),
            MailboxFraming.assemble(listOf("You received a file: attachment.txt", "still-here"))
        )
    }
}
