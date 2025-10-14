package com.k2fsa.sherpa.onnx.mysherpaapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.k2fsa.sherpa.onnx.mysherpaapp.SherpaOnnxEngine
import com.k2fsa.sherpa.onnx.mysherpaapp.data.Speaker
import com.k2fsa.sherpa.onnx.mysherpaapp.data.SpeakerDatabase
import com.k2fsa.sherpa.onnx.mysherpaapp.utils.withDebugLogging
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

@Composable
fun EnrollScreen() {
    val context = LocalContext.current
    var speakerName by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val recordedAudio = remember { mutableStateListOf<Float>() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = speakerName,
            onValueChange = { speakerName = it },
            label = { Text("Speaker Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Row {
            Button(onClick = withDebugLogging { { isRecording = !isRecording } }) {
                Text(text = if (isRecording) "Stop Recording" else "Start Recording")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = withDebugLogging {
                {
                    if (speakerName.isNotBlank()) {
                        thread {
                            val embedding = SherpaOnnxEngine.sd.extractEmbedding(recordedAudio.toFloatArray())
                            val speaker = Speaker(name = speakerName, embedding = embedding)
                            coroutineScope.launch {
                                SpeakerDatabase.getDatabase(context).speakerDao().insert(speaker)
                                status = "Speaker $speakerName saved"
                            }
                        }
                    } else {
                        status = "Please enter a speaker name"
                    }
                }
            }) {
                Text(text = "Save Speaker")
            }
        }

        Text(text = status)
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

                while (isRecording) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        for (i in 0 until read) {
                            floatBuffer[i] = buffer[i] / 32768.0f
                        }
                        recordedAudio.addAll(floatBuffer.toList())
                    }
                }
                audioRecord.stop()
                audioRecord.release()
            }
        }
    }
}