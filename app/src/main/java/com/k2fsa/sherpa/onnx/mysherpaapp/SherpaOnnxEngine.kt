package com.k2fsa.sherpa.onnx.mysherpaapp

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.*
import com.k2fsa.sherpa.onnx.mysherpaapp.utils.withDebugLogging

object SherpaOnnxEngine {
    private const val TAG = "SherpaOnnxEngine"

    var _sd: OfflineSpeakerDiarization? = null
    val sd: OfflineSpeakerDiarization
        get() = withDebugLogging {
            return _sd!!
        }

    var _asr: OnlineRecognizer? = null
    val asr: OnlineRecognizer
        get() = withDebugLogging {
            return _asr!!
        }

    var _vad: Vad? = null
    val vad: Vad
        get() = withDebugLogging {
            return _vad!!
        }

    var _punct: OnlinePunctuation? = null
    val punct: OnlinePunctuation
        get() = withDebugLogging {
            return _punct!!
        }

    var _tts: Tts? = null
    val tts: Tts
        get() = withDebugLogging {
            return _tts!!
        }

    fun init(assetManager: AssetManager) = withDebugLogging {
        synchronized(this) {
            if (_sd != null) {
                return@withDebugLogging
            }

            val modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = "asr/encoder.onnx",
                    decoder = "asr/decoder.onnx",
                    joiner = "asr/joiner.onnx",
                ),
                tokens = "asr/tokens.txt",
            )

            val asrConfig = OnlineRecognizerConfig(
                modelConfig = modelConfig,
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
            )
            _asr = OnlineRecognizer(assetManager, asrConfig)

            val vadConfig = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = "vad/silero_vad.onnx",
                    threshold = 0.5F,
                    minSilenceDuration = 0.25F,
                    minSpeechDuration = 0.25F,
                    windowSize = 512,
                ),
                sampleRate = 16000,
            )
            _vad = Vad(assetManager, vadConfig)

            val punctConfig = OnlinePunctuationConfig(model = "punct/model.onnx", vocab = "punct/bpe.vocab")
            _punct = OnlinePunctuation(assetManager, punctConfig)

            val ttsConfig = TtsConfig(model = "tts/model.onnx", vocab = "tts/config.json")
            _tts = Tts(assetManager, ttsConfig)

            val sdConfig = OfflineSpeakerDiarizationConfig(
                segmentation = OfflineSpeakerSegmentationModelConfig(
                    pyannote = OfflineSpeakerSegmentationPyannoteModelConfig(
                        "spk_seg/segmentation.onnx"
                    ),
                    debug = true,
                ),
                embedding = SpeakerEmbeddingExtractorConfig(
                    model = "spk_emb/embedding.onnx",
                    debug = true,
                    numThreads = 2,
                ),
                clustering = FastClusteringConfig(numClusters = -1, threshold = 0.5f),
                minDurationOn = 0.2f,
                minDurationOff = 0.5f,
            )
            _sd = OfflineSpeakerDiarization(assetManager = assetManager, config = sdConfig)
        }
    }
}
