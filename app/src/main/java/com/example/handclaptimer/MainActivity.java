package com.example.handclaptimer;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int CLAP_THRESHOLD = 20000; // Adjust based on testing

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private boolean isTimerRunning = false;
    private long startTime;
    private Handler handler = new Handler();
    private TextView timerTextView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timerTextView = findViewById(R.id.timerTextView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        } else {
            startListening();
        }
    }

    private void startListening() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        isRecording = true;
        new Thread(() -> {
            audioRecord.startRecording();
            short[] buffer = new short[BUFFER_SIZE];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, BUFFER_SIZE);
                if (read > 0 && detectClap(buffer, read)) {
                    runOnUiThread(this::toggleTimer);
                }
            }
            audioRecord.stop();
            audioRecord.release();
        }).start();
    }

    private boolean detectClap(short[] buffer, int read) {
        int maxAmplitude = 0;
        for (int i = 0; i < read; i++) {
            maxAmplitude = Math.max(maxAmplitude, Math.abs(buffer[i]));
        }
        return maxAmplitude > CLAP_THRESHOLD;
    }

    private void toggleTimer() {
        if (isTimerRunning) {
            isTimerRunning = false;
            handler.removeCallbacks(updateTimer);
        } else {
            isTimerRunning = true;
            startTime = System.currentTimeMillis();
            handler.post(updateTimer);
        }
    }

    private Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            if (isTimerRunning) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                timerTextView.setText("Time: " + elapsedTime / 1000 + "s");
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRecording = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
