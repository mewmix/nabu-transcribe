package com.k2fsa.sherpa.onnx.mysherpaapp.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `float array conversion preserves values`() {
        val original = floatArrayOf(0.12f, -3.5f, 42.0f)

        val serialized = converters.fromFloatArray(original)
        val restored = converters.toFloatArray(serialized)

        assertArrayEquals(original, restored, 1e-6f)
    }

    @Test
    fun `empty float arrays serialize to blank strings`() {
        val serialized = converters.fromFloatArray(FloatArray(0))

        assertEquals("", serialized)
        assertEquals(0, converters.toFloatArray(serialized).size)
    }

    @Test
    fun `string list conversion handles blanks`() {
        assertEquals(emptyList<String>(), converters.fromString(""))
        assertEquals("", converters.fromList(emptyList()))

        val list = listOf("alpha", "beta")
        assertEquals(list, converters.fromString(converters.fromList(list)))
    }
}
