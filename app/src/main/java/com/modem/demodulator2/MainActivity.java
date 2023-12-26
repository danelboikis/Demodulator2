package com.modem.demodulator2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static final int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 123;
    private AudioRecorder audioRecorder;
    private Button btnStart;
    private Button btnStop;
    private Button btnAnalyze;
    private Demodulator demodulator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnAnalyze = findViewById(R.id.btnAnalyze);

        btnStop.setEnabled(false);
        btnAnalyze.setEnabled(false);

        Log.i("TAG1", "before requestRecordAudioPermission");

        // Request the RECORD_AUDIO permission from the user
        requestRecordAudioPermission();

        Log.i("TAG1", "after requestRecordAudioPermission");

        Log.i("TAG2", "permission: " + (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED));

        try {
            audioRecorder = new AudioRecorder(this);
        }
        catch (Exception e) {
            Log.e("TAG1","permission to record not granted");
            btnStart.setEnabled(false);
        }

        btnStart.setOnClickListener(v -> {
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            btnAnalyze.setEnabled(false);
            Log.i("TAG1", "before startRecording");
            startRecording();
            Log.i("TAG1", "after startRecording");
        });

        btnStop.setOnClickListener(v -> {
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            btnAnalyze.setEnabled(true);
            Log.i("TAG1", "before stopRecording");
            stopRecording();
            Log.i("TAG1", "after stopRecording");
        });

        btnAnalyze.setOnClickListener(v -> {
            btnStart.setEnabled(false);
            btnStop.setEnabled(false);
            btnAnalyze.setEnabled(false);
            Log.i("TAG1", "before analyzeRecording");
            analyzeRecording();
            Log.i("TAG1", "after analyzeRecording");
            btnStart.setEnabled(true);
        });
    }

    private void startRecording() {
        demodulator = new Demodulator();
        new Thread(() -> {
            audioRecorder.startRecording(demodulator);
        }).start();
    }

    private void stopRecording() {
        audioRecorder.stopRecording();
    }

    private void analyzeRecording() {
        Log.i("TAG2", demodulator.analyzeData());
    }

    // Request the RECORD_AUDIO permission from the user
    private void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
        );
    }

    //on destroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioRecorder.release();
    }
}