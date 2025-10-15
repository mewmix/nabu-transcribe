package com.k2fsa.sherpa.onnx.mysherpaapp

import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SherpaOnnxEngineTest {

    @After
    fun tearDown() {
        SherpaOnnxEngine._sd = null
        SherpaOnnxEngine._asr = null
        SherpaOnnxEngine._vad = null
        SherpaOnnxEngine._punct = null
        SherpaOnnxEngine._embeddingExtractor = null
    }

    @Test
    fun `accessing speech recognizer before init throws helpful error`() {
        val thrown = try {
            @Suppress("UNUSED_EXPRESSION")
            SherpaOnnxEngine.asr
            null
        } catch (t: Throwable) {
            t
        }

        val exception = (thrown ?: fail("Expected IllegalStateException to be thrown")) as Throwable
        assertTrue("Unexpected exception type: ${exception::class.java.name}", exception is IllegalStateException)
        assertTrue(exception.message?.contains("SherpaOnnxEngine.asr") == true)
    }

    @Test
    fun `accessing diarization before init throws helpful error`() {
        val thrown = try {
            @Suppress("UNUSED_EXPRESSION")
            SherpaOnnxEngine.sd
            null
        } catch (t: Throwable) {
            t
        }

        val exception = (thrown ?: fail("Expected IllegalStateException to be thrown")) as Throwable
        assertTrue("Unexpected exception type: ${exception::class.java.name}", exception is IllegalStateException)
        assertTrue(exception.message?.contains("SherpaOnnxEngine.sd") == true)
    }
}
