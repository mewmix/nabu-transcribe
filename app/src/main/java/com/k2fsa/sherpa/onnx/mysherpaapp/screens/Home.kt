package com.k2fsa.sherpa.onnx.mysherpaapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.k2fsa.sherpa.onnx.mysherpaapp.NavRoutes
import com.k2fsa.sherpa.onnx.mysherpaapp.SherpaOnnxEngine
import com.k2fsa.sherpa.onnx.mysherpaapp.TAG
import com.k2fsa.sherpa.onnx.mysherpaapp.data.Meeting
import com.k2fsa.sherpa.onnx.mysherpaapp.data.SpeakerDatabase
import com.k2fsa.sherpa.onnx.mysherpaapp.data.Turn
import com.k2fsa.sherpa.onnx.mysherpaapp.export.JsonExporter
import com.k2fsa.sherpa.onnx.mysherpaapp.export.SrtExporter
import com.k2fsa.sherpa.onnx.mysherpaapp.export.VttExporter
import com.k2fsa.sherpa.onnx.mysherpaapp.utils.withDebugLogging
import kotlinx.coroutines.launch
import java.io.File
import kotlin.concurrent.thread

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    var transcribedText by remember { mutableStateOf("") }
    var diarizationResult by remember { mutableStateOf("") }
    var ttsText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var punctuationEnabled by remember { mutableStateOf(true) }
    val recordedAudio = remember { mutableStateListOf<Float>() }
    val coroutineScope = rememberCoroutineScope()
    var meetingId by remember { mutableStateOf<Long?>(null) }
    var meetingTags by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                isRecording = true
            } else {
                Toast.makeText(context, "Audio recording permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    )

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

        // ASR Section
        Text(text = "ASR", style = MaterialTheme.typography.titleLarge)
        Row {
            Button(onClick = withDebugLogging {
                {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        launcher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        isRecording = !isRecording
                    }
                }
            }) {
                Text(text = if (isRecording) "Stop Recording" else "Start Recording")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = withDebugLogging {
                {
                    coroutineScope.launch {
                        val meeting = Meeting(title = "New Meeting", tags = meetingTags.split(",").map { it.trim() })
                        meetingId = SpeakerDatabase.getDatabase(context).meetingDao().insert(meeting)
                    }
                }
            }) {
                Text(text = "New Meeting")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = withDebugLogging {
                {
                    coroutineScope.launch {
                        val speakers = SpeakerDatabase.getDatabase(context).speakerDao().getAll()
                        val segments = SherpaOnnxEngine.sd.process(recordedAudio.toFloatArray())
                        var result = ""
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
                            if (maxSimilarity < 0.6) {
                                speakerName = "Unknown"
                                speakerId = null
                            }
                            result += "$speakerName: $text\n"
                            meetingId?.let {
                                val turn = Turn(
                                    meetingId = it.toInt(),
                                    speakerId = speakerId,
                                    startMs = (segment.start * 1000).toLong(),
                                    endMs = (segment.end * 1000).toLong(),
                                    text = text,
                                    conf = 0.0f // TODO: Get confidence from ASR
                                )
                                SpeakerDatabase.getDatabase(context).turnDao().insert(turn)
                            }
                        }
                        diarizationResult = result
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
            Text(text = transcribedText, modifier = Modifier.fillMaxWidth().height(100.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diarization Section
        Text(text = "Diarization", style = MaterialTheme.typography.titleLarge)
        SelectionContainer {
            Text(text = diarizationResult, modifier = Modifier.fillMaxWidth().height(100.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Punctuation Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Enable Punctuation")
            Switch(checked = punctuationEnabled, onCheckedChange = { punctuationEnabled = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TTS Section
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
                    thread {
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
                        audioTrack.play()
                        audioTrack.write(audio.samples, 0, audio.samples.size)
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
                Column {
                    Button(onClick = withDebugLogging {
                        {
                            coroutineScope.launch {
                                meetingId?.let {
                                    val meeting = SpeakerDatabase.getDatabase(context).meetingDao().getById(it.toInt())
                                    val turns = SpeakerDatabase.getDatabase(context).turnDao().getByMeetingId(it.toInt())
                                    if (meeting != null) {
                                        val json = JsonExporter().export(meeting, turns)
                                        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "meeting.json")
                                        file.writeText(json)
                                        Toast.makeText(context, "Exported to Downloads/meeting.json", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            showExportDialog = false
                        }
                    }) {
                        Text(text = "JSON")
                    }
                    Button(onClick = withDebugLogging {
                        {
                            coroutineScope.launch {
                                meetingId?.let {
                                    val turns = SpeakerDatabase.getDatabase(context).turnDao().getByMeetingId(it.toInt())
                                    val srt = SrtExporter().export(turns)
                                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "meeting.srt")
                                    file.writeText(srt)
                                    Toast.makeText(context, "Exported to Downloads/meeting.srt", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showExportDialog = false
                        }
                    }) {
                        Text(text = "SRT")
                    }
                    Button(onClick = withDebugLogging {
                        {
                            coroutineScope.launch {
                                meetingId?.let {
                                    val turns = SpeakerDatabase.getDatabase(context).turnDao().getByMeetingId(it.toInt())
                                    val vtt = VttExporter().export(turns)
                                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "meeting.vtt")
                                    file.writeText(vtt)
                                    Toast.makeText(context, "Exported to Downloads/meeting.vtt", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showExportDialog = false
                        }
                    }) {
                        Text(text = "VTT")
                    }
                }
            },
            confirmButton = { },
            dismissButton = { }
        )
    }

    if (isRecording) {
        thread {
            withDebugLogging {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@withDebugLogging
                }
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

                audioRecord.startRecording()

                val buffer = ShortArray(1600) // 0.1 seconds
                val floatBuffer = FloatArray(1600)

                val stream = SherpaOnnxEngine.asr.createStream()

                while (isRecording) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        for (i in 0 until read) {
                            floatBuffer[i] = buffer[i] / 32768.0f
                        }
                        recordedAudio.addAll(floatBuffer.toList())

                        SherpaOnnxEngine.vad.acceptWaveform(floatBuffer)
                        if (SherpaOnnxEngine.vad.isSpeechDetected()) {
                            while (!SherpaOnnxEngine.vad.empty()) {
                                val segment = SherpaOnnxEngine.vad.front()
                                stream.acceptWaveform(segment.samples, 16000)
                                SherpaOnnxEngine.vad.pop()
                            }
                            SherpaOnnxEngine.asr.decode(stream)
                            var result = SherpaOnnxEngine.asr.getResult(stream).text
                            if (punctuationEnabled) {
                                result = SherpaOnnxEngine.punct.add(result)
                            }
                            transcribedText = result
                        }
                    }
                }
                audioRecord.stop()
                audioRecord.release()
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