package com.k2fsa.sherpa.onnx.mysherpaapp

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.FastClusteringConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationPyannoteModelConfig
import com.k2fsa.sherpa.onnx.OfflinePunctuation
import com.k2fsa.sherpa.onnx.OfflinePunctuationConfig
import com.k2fsa.sherpa.onnx.OfflinePunctuationModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import com.k2fsa.sherpa.onnx.mysherpaapp.utils.withDebugLogging

object SherpaOnnxEngine {
    private const val TAG = "SherpaOnnxEngine"
    private const val MODEL_BASE_PATH = "models/"
    const val SAMPLE_RATE = 16000

    var _sd: OfflineSpeakerDiarization? = null
    val sd: OfflineSpeakerDiarization
        get() = withDebugLogging {
            return checkNotNull(_sd) { notInitializedMessage("sd") }
        }

    var _asr: OnlineRecognizer? = null
    val asr: OnlineRecognizer
        get() = withDebugLogging {
            return checkNotNull(_asr) { notInitializedMessage("asr") }
        }

    var _vad: Vad? = null
    val vad: Vad
        get() = withDebugLogging {
            return checkNotNull(_vad) { notInitializedMessage("vad") }
        }

    var _punct: OfflinePunctuation? = null
    val punct: OfflinePunctuation
        get() = withDebugLogging {
            return checkNotNull(_punct) { notInitializedMessage("punct") }
        }

    var _embeddingExtractor: SpeakerEmbeddingExtractor? = null
    val embeddingExtractor: SpeakerEmbeddingExtractor
        get() = withDebugLogging {
            return checkNotNull(_embeddingExtractor) { notInitializedMessage("embeddingExtractor") }
        }

    fun init(assetManager: AssetManager) = withDebugLogging {
        synchronized(this) {
            if (_sd != null) {
                return@withDebugLogging
            }

            val modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = assetPath("asr/encoder.onnx"),
                    decoder = assetPath("asr/decoder.onnx"),
                    joiner = assetPath("asr/joiner.onnx"),
                ),
                tokens = assetPath("asr/tokens.txt"),
            )

            val asrConfig = OnlineRecognizerConfig(
                modelConfig = modelConfig,
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
            )
            _asr = OnlineRecognizer(assetManager, asrConfig)

            val vadConfig = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = assetPath("vad/silero_vad.onnx"),
                    threshold = 0.5F,
                    minSilenceDuration = 0.25F,
                    minSpeechDuration = 0.25F,
                    windowSize = 512,
                ),
                sampleRate = SAMPLE_RATE,
            )
            _vad = Vad(assetManager, vadConfig)

            val punctConfig = OfflinePunctuationConfig(
                OfflinePunctuationModelConfig(
                    ctTransformer = assetPath("punct/model.onnx"),
                    numThreads = 2,
                    debug = false,
                    provider = ""
                )
            )
            _punct = OfflinePunctuation(assetManager, punctConfig)

            val embeddingConfig = SpeakerEmbeddingExtractorConfig(
                model = assetPath("spk_emb/embedding.onnx"),
                numThreads = 2,
                debug = true,
                provider = ""
            )

            val sdConfig = OfflineSpeakerDiarizationConfig(
                segmentation = OfflineSpeakerSegmentationModelConfig(
                    pyannote = OfflineSpeakerSegmentationPyannoteModelConfig(
                        assetPath("spk_seg/segmentation.onnx")
                    ),
                    debug = true,
                ),
                embedding = embeddingConfig,
                clustering = FastClusteringConfig(numClusters = -1, threshold = 0.5f),
                minDurationOn = 0.2f,
                minDurationOff = 0.5f,
            )
            _sd = OfflineSpeakerDiarization(assetManager = assetManager, config = sdConfig)
            _embeddingExtractor = SpeakerEmbeddingExtractor(
                assetManager,
                embeddingConfig
            )
        }
    }

    fun computeEmbedding(samples: FloatArray): FloatArray = withDebugLogging {
        val extractor = embeddingExtractor
        val stream: OnlineStream = extractor.createStream()
        stream.acceptWaveform(samples, SAMPLE_RATE)
        stream.inputFinished()
        return extractor.compute(stream)
    }

    private fun notInitializedMessage(componentName: String): String {
        return "SherpaOnnxEngine.$componentName is not available. Call SherpaOnnxEngine.init() before accessing it."
    }

    private fun assetPath(relativePath: String): String = MODEL_BASE_PATH + relativePath
}
