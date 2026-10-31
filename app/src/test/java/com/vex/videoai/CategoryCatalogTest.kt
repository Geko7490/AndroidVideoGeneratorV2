package com.vex.videoai

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryCatalogTest {
    @Test
    fun categoryIdsAreUnique() {
        val ids = listOf("horror", "educational", "story", "mystery", "history", "documentary", "comedy", "facts", "motivational")
        assertEquals(ids.size, ids.toSet().size)
    }
}
