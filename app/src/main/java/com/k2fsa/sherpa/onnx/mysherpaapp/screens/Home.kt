package com.k2fsa.sherpa.onnx.mysherpaapp.screens

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.k2fsa.sherpa.onnx.mysherpaapp.NavRoutes
import com.k2fsa.sherpa.onnx.mysherpaapp.SherpaOnnxEngine
import com.k2fsa.sherpa.onnx.mysherpaapp.data.Meeting
import com.k2fsa.sherpa.onnx.mysherpaapp.data.SpeakerDatabase
import com.k2fsa.sherpa.onnx.mysherpaapp.data.Turn
import com.k2fsa.sherpa.onnx.mysherpaapp.export.JsonExporter
import com.k2fsa.sherpa.onnx.mysherpaapp.export.SrtExporter
import com.k2fsa.sherpa.onnx.mysherpaapp.export.VttExporter
import com.k2fsa.sherpa.onnx.mysherpaapp.utils.withDebugLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var transcribedText by remember { mutableStateOf("") }
    var diarizationResult by remember { mutableStateOf("") }
    var ttsText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var punctuationEnabled by remember { mutableStateOf(true) }
    val recordedAudio = remember { mutableListOf<Float>() }
    var meetingId by remember { mutableStateOf<Long?>(null) }
    var meetingTags by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    val database = remember(context) { SpeakerDatabase.getDatabase(context) }

    val updatedIsRecording = rememberUpdatedState(isRecording)
    val updatedPunctuation = rememberUpdatedState(punctuationEnabled)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                recordedAudio.clear()
                isRecording = true
            } else {
                Toast.makeText(
                    context,
                    "Audio recording permission is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun exportMeeting(fileName: String, buildContent: (meeting: Meeting, turns: List<Turn>) -> String) {
        showExportDialog = false
        coroutineScope.launch(Dispatchers.IO) {
            val id = meetingId?.toInt()
            if (id == null) {
                showToast("Create a meeting before exporting")
                return@launch
            }

            val meeting = database.meetingDao().getById(id)
            if (meeting == null) {
                showToast("Unable to find meeting")
                return@launch
            }

            val turns = database.turnDao().getByMeetingId(id).first()
            val target = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            target.writeText(buildContent(meeting, turns))
            showToast("Exported to Downloads/$fileName")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "SherpaOnnx Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = meetingTags,
            onValueChange = { meetingTags = it },
            label = { Text("Meeting Tags (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "ASR", style = MaterialTheme.typography.titleLarge)
        Row {
            Button(onClick = withDebugLogging {
                {
                    if (isRecording) {
                        isRecording = false
                    } else {
                        if (
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            recordedAudio.clear()
                            isRecording = true
                        }
                    }
                }
            }) {
                Text(text = if (isRecording) "Stop Recording" else "Start Recording")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = withDebugLogging {
                {
                    coroutineScope.launch(Dispatchers.IO) {
                        val meeting = Meeting(
                            title = "New Meeting",
                            tags = meetingTags.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                        )
                        val id = database.meetingDao().insert(meeting)
                        withContext(Dispatchers.Main) {
                            meetingId = id
                        }
                    }
                }
            }) {
                Text(text = "New Meeting")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = withDebugLogging {
                {
                    coroutineScope.launch(Dispatchers.IO) {
                        val audioSamples = recordedAudio.toFloatArray()
                        if (audioSamples.isEmpty()) {
                            showToast("Record audio before diarizing")
                            return@launch
                        }

                        val segments = SherpaOnnxEngine.sd.process(audioSamples)
                        val speakers = database.speakerDao().getAll()
                        val builder = StringBuilder()

                        for (segment in segments) {
                            val stream = SherpaOnnxEngine.asr.createStream()
                            stream.acceptWaveform(segment.samples, 16000)
                            SherpaOnnxEngine.asr.decode(stream)
                            val text = SherpaOnnxEngine.asr.getResult(stream).text

                            val embedding = SherpaOnnxEngine.sd.extractEmbedding(segment.samples)
                            var speakerName = "Unknown"
                            var speakerId: Int? = null
                            var maxSimilarity = 0.0f

                            for (speaker in speakers) {
                                val similarity = cosineSimilarity(embedding, speaker.embedding)
                                if (similarity > maxSimilarity) {
                                    maxSimilarity = similarity
                                    speakerName = speaker.name
                                    speakerId = speaker.id
                                }
                            }

                            if (maxSimilarity < 0.6f) {
                                speakerName = "Unknown"
                                speakerId = null
                            }

                            builder.appendLine("$speakerName: $text")

                            meetingId?.let { currentMeetingId ->
                                val turn = Turn(
                                    meetingId = currentMeetingId.toInt(),
                                    speakerId = speakerId,
                                    startMs = (segment.start * 1000).toLong(),
                                    endMs = (segment.end * 1000).toLong(),
                                    text = text,
                                    conf = 0.0f
                                )
                                database.turnDao().insert(turn)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            diarizationResult = builder.toString()
                        }
                    }
                }
            }) {
                Text(text = "Diarize")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = withDebugLogging { { navController.navigate(NavRoutes.Enroll.route) } }) {
                Text(text = "Enroll")
            }
        }

        SelectionContainer {
            Text(
                text = transcribedText,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Diarization", style = MaterialTheme.typography.titleLarge)
        SelectionContainer {
            Text(
                text = diarizationResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Enable Punctuation")
            Switch(checked = punctuationEnabled, onCheckedChange = { punctuationEnabled = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "TTS", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = ttsText,
            onValueChange = { ttsText = it },
            label = { Text("Text to speak") },
            modifier = Modifier.fillMaxWidth()
        )
        Row {
            Button(onClick = withDebugLogging {
                {
                    if (ttsText.isBlank()) {
                        Toast.makeText(context, "Enter text to synthesize", Toast.LENGTH_SHORT).show()
                    } else {
                        coroutineScope.launch(Dispatchers.IO) {
                            val audio = SherpaOnnxEngine.tts.synthesize(ttsText)
                            val audioTrack = AudioTrack.Builder()
                                .setAudioFormat(
                                    AudioFormat.Builder()
                                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                        .setSampleRate(audio.sampleRate)
                                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                        .build()
                                )
                                .setBufferSizeInBytes(audio.samples.size * 2)
                                .build()
                            try {
                                audioTrack.play()
                                audioTrack.write(audio.samples, 0, audio.samples.size)
                            } finally {
                                audioTrack.stop()
                                audioTrack.release()
                            }
                        }
                    }
                }
            }) {
                Text(text = "Speak")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { showExportDialog = true }) {
                Text(text = "Export")
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(text = "Export As") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = withDebugLogging { { exportMeeting("meeting.json") { meeting, turns -> JsonExporter().export(meeting, turns) } } }) {
                        Text(text = "JSON")
                    }
                    Button(onClick = withDebugLogging { { exportMeeting("meeting.srt") { _, turns -> SrtExporter().export(turns) } } }) {
                        Text(text = "SRT")
                    }
                    Button(onClick = withDebugLogging { { exportMeeting("meeting.vtt") { _, turns -> VttExporter().export(turns) } } }) {
                        Text(text = "VTT")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            return@LaunchedEffect
        }

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Audio recording permission is required", Toast.LENGTH_SHORT).show()
            isRecording = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            withDebugLogging {
                val audioRecord = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(16000)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build()
                    )
                    .build()

                val stream = SherpaOnnxEngine.asr.createStream()
                val buffer = ShortArray(1600)

                try {
                    audioRecord.startRecording()
                    while (isActive && updatedIsRecording.value) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            val floatBuffer = FloatArray(read)
                            for (i in 0 until read) {
                                val sample = buffer[i] / 32768.0f
                                floatBuffer[i] = sample
                                recordedAudio.add(sample)
                            }

                            SherpaOnnxEngine.vad.acceptWaveform(floatBuffer)
                            if (SherpaOnnxEngine.vad.isSpeechDetected()) {
                                while (!SherpaOnnxEngine.vad.empty()) {
                                    val segment = SherpaOnnxEngine.vad.front()
                                    stream.acceptWaveform(segment.samples, 16000)
                                    SherpaOnnxEngine.vad.pop()
                                }
                                SherpaOnnxEngine.asr.decode(stream)
                                var result = SherpaOnnxEngine.asr.getResult(stream).text
                                if (updatedPunctuation.value) {
                                    result = SherpaOnnxEngine.punct.add(result)
                                }
                                withContext(Dispatchers.Main) {
                                    transcribedText = result
                                }
                            }
                        }
                    }
                } finally {
                    audioRecord.stop()
                    audioRecord.release()
                }
            }
        }
    }
}

fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float = withDebugLogging {
    var dotProduct = 0.0f
    var norm1 = 0.0f
    var norm2 = 0.0f
    for (i in v1.indices) {
        dotProduct += v1[i] * v2[i]
        norm1 += v1[i] * v1[i]
        norm2 += v2[i] * v2[i]
    }
    return@withDebugLogging dotProduct / (kotlin.math.sqrt(norm1) * kotlin.math.sqrt(norm2))
}
