import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasRecordAudioPermission = isGranted
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

        Row {
            Button(onClick = withDebugLogging {
                {
                    if (hasRecordAudioPermission) {
                        isRecording = !isRecording
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }) {
                Text(text = if (isRecording) "Stop Recording" else "Start Recording")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = withDebugLogging {
                {
                    if (speakerName.isNotBlank()) {
                        thread {
                            val embedding =
                                SherpaOnnxEngine.computeEmbedding(recordedAudio.toFloatArray())
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
                val audioRecord = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SherpaOnnxEngine.SAMPLE_RATE)
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
                            recordedAudio.add(floatBuffer[i])
                        }
                    }
                }
                audioRecord.stop()
                audioRecord.release()
            }
        }
    }
}