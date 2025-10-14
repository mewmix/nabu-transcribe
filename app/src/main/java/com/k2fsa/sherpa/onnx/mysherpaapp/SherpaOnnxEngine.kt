package com.k2fsa.sherpa.onnx.mysherpaapp

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.*
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig as SherpaSpeakerEmbeddingExtractorConfig
import com.k2fsa.sherpa.onnx.mysherpaapp.utils.withDebugLogging

object SherpaOnnxEngine {
    private const val TAG = "SherpaOnnxEngine"

    var _sd: OfflineSpeakerDiarization? = null
    val sd: OfflineSpeakerDiarization
        get() = withDebugLogging {
            return _sd!!
        }

    var _speakerEmbeddingExtractor: SpeakerEmbeddingExtractor? = null
    val speakerEmbeddingExtractor: SpeakerEmbeddingExtractor
        get() = withDebugLogging {
            return _speakerEmbeddingExtractor!!
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

    var _punct: OfflinePunctuation? = null
    val punct: OfflinePunctuation
        get() = withDebugLogging {
            return _punct!!
        }

    var _tts: OfflineTts? = null
    val tts: OfflineTts
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
                    encoder = "models/asr/encoder.onnx",
                    decoder = "models/asr/decoder.onnx",
                    joiner = "models/asr/joiner.onnx",
                ),
                tokens = "models/asr/tokens.txt",
            )

            val asrConfig = OnlineRecognizerConfig(
                modelConfig = modelConfig,
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
            )
            _asr = OnlineRecognizer(assetManager, asrConfig)

            val vadConfig = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = "models/vad/silero_vad.onnx",
                    threshold = 0.5F,
                    minSilenceDuration = 0.25F,
                    minSpeechDuration = 0.25F,
                    windowSize = 512,
                ),
                sampleRate = 16000,
            )
            _vad = Vad(assetManager, vadConfig)

            val punctConfig = OfflinePunctuationConfig(
                model = OfflinePunctuationModelConfig(
                    ctTransformer = "models/punct/model.onnx",
                    numThreads = 1,
                    debug = false,
                    provider = "cpu"
                )
            )
            _punct = OfflinePunctuation(assetManager, punctConfig)

            val ttsConfig = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = "models/tts/model.onnx",
                        lexicon = "",
                        tokens = "models/tts/config.json",
                        dataDir = "",
                        dictDir = ""
                    ),
                    numThreads = 1,
                    debug = false,
                    provider = "cpu"
                ),
                ruleFsts = "",
                ruleFars = "",
                maxNumSentences = 1
            )
            _tts = OfflineTts(assetManager, ttsConfig)

            val speakerEmbeddingExtractorConfig = SherpaSpeakerEmbeddingExtractorConfig(
                model = "models/spk_emb/embedding.onnx",
                numThreads = 2,
                debug = true,
                provider = "cpu",
            )
            _speakerEmbeddingExtractor =
                SpeakerEmbeddingExtractor(assetManager, speakerEmbeddingExtractorConfig)


            val sdConfig = OfflineSpeakerDiarizationConfig(
                segmentation = OfflineSpeakerSegmentationModelConfig(
                    pyannote = OfflineSpeakerSegmentationPyannoteModelConfig(
                        "models/spk_seg/segmentation.onnx"
                    ),
                    debug = true,
                ),
                embedding = speakerEmbeddingExtractorConfig,
                clustering = FastClusteringConfig(numClusters = -1, threshold = 0.5f),
                minDurationOn = 0.2f,
                minDurationOff = 0.5f,
            )
            _sd = OfflineSpeakerDiarization(assetManager = assetManager, config = sdConfig)
        }
    }
}