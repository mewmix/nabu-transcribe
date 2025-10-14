package com.k2fsa.sherpa.onnx.mysherpaapp.screens

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.k2fsa.sherpa.onnx.mysherpaapp.SherpaOnnxEngine
import com.k2fsa.sherpa.onnx.mysherpaapp.data.Speaker
import com.k2fsa.sherpa.onnx.mysherpaapp.data.SpeakerDatabase
import com.k2fsa.sherpa.onnx.mysherpaapp.utils.withDebugLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.annotation.SuppressLint

@SuppressLint("MissingPermission")
@Composable
fun EnrollScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember(context) { SpeakerDatabase.getDatabase(context) }

    var speakerName by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val recordedAudio = remember { mutableListOf<Float>() }

    val updatedIsRecording = rememberUpdatedState(isRecording)

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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { withDebugLogging {
                if (isRecording) {
                    isRecording = false
                } else {
                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        recordedAudio.clear()
                        isRecording = true
                    }
                }
            } }) {
                Text(text = if (isRecording) "Stop Recording" else "Start Recording")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { withDebugLogging { 
                val trimmedName = speakerName.trim()
                if (trimmedName.isEmpty()) {
                    Toast.makeText(context, "Please enter a speaker name", Toast.LENGTH_SHORT).show()
                    return@withDebugLogging
                }
                if (recordedAudio.isEmpty()) {
                    Toast.makeText(context, "Record audio before saving", Toast.LENGTH_SHORT).show()
                    return@withDebugLogging
                }

                coroutineScope.launch(Dispatchers.IO) {
                    val stream = SherpaOnnxEngine.speakerEmbeddingExtractor.createStream()
                    stream.acceptWaveform(recordedAudio.toFloatArray(), sampleRate = 16000)
                    val embedding = SherpaOnnxEngine.speakerEmbeddingExtractor.compute(stream)

                    database.speakerDao().insert(Speaker(name = trimmedName, embedding = embedding))
                    withContext(Dispatchers.Main) {
                        status = "Speaker $trimmedName saved"
                    }
                }
            } }) {
                Text(text = "Save Speaker")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = status)
    }

    LaunchedEffect(isRecording) {
        if (!isRecording) {
            return@LaunchedEffect
        }

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
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

                val buffer = ShortArray(1600)

                try {
                    audioRecord.startRecording()
                    while (isActive && updatedIsRecording.value) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            for (i in 0 until read) {
                                val sample = buffer[i] / 32768.0f
                                recordedAudio.add(sample)
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
