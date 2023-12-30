package com.modem.demodulator2;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AudioRecorder {
    public static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static final int MAX_REC_TIME = 100; // max recording time in seconds

    private AudioRecord audioRecord;
    private int bufferSize;
    private short[] audioData;

    private int audioDataIn;

    private volatile boolean isRecording ;

    private MainActivity context;

    public AudioRecorder(MainActivity context) throws Exception {
        this.context = context;
        isRecording = false;
        bufferSize = SAMPLE_RATE * MAX_REC_TIME;
        audioData = new short[bufferSize];


        // Check for RECORD_AUDIO permission is required before initializing AudioRecord
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw new Exception();
        }
        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize);
        }
        catch (Exception e) {
            Log.e("TAG1", "Error during AudioRecord initialization: " + e.getMessage());
            e.printStackTrace();
        }

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            // Handle initialization error
            Log.e("TAG1", "AudioRecord initialization failed");
        }
        else {
            Log.i("TAG1", "AudioRecord initialization succeeded");
        }
    }

    private String toDebug;
    public void startRecording(Demodulator16Bit2 demodulator) {
        // Specify the path to your input WAV file
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            isRecording = true;

            audioDataIn = 0;

            audioRecord.startRecording();

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            int loop = 0;
            while (isRecording) {
                int bytesRead = audioRecord.read(audioData, audioDataIn, /*bufferSize - audioDataIn*/200);
                Log.i("TAG2", "bytesRead: " + bytesRead);


                /*// Read the file into the byte array - tetsing
                int bytesRead = -1;
                try {
                    bytesRead = context.readFromFileWithOffset(audioData, audioDataIn, 4000);
                }
                catch (Exception e) {
                    toDebug = e.getMessage();
                }*/

                if (bytesRead < 0) {
                    isRecording = false;
                    Log.e("TAG2", "Error: audioRecord.read returned negative value: " + bytesRead);
                }
                else {
                    // code sinnipet for logging
                    short temArr[] = new short[bytesRead];
                    System.arraycopy(audioData, audioDataIn, temArr, 0, bytesRead);
                    Log.i("TAG2", "bytesRead: " + Arrays.toString(temArr));

                    int finalAudioDataIn = audioDataIn;
                    int finalBytesRead = bytesRead;
                    int finalLoop = loop;
                    Runnable task = () -> {
                        try {
                            if (!Thread.currentThread().isInterrupted()) {
                                demodulator.demodulate(audioData, finalAudioDataIn, finalBytesRead);
                            }
                            else {
                                Log.i("TAG2", "thread interrupted: " + Thread.currentThread().getName());
                            }
                        } catch (Exception e) {
                            Log.i("TAG2", "Error during demodulation: " + e.getMessage());
                        }
                        finally {
                            Log.i("TAG2", "task finished: " + finalLoop);
                        }
                    };

                    executorService.submit(task);

                    audioDataIn += bytesRead;

                    loop++;
                }
            }
            Log.i("TAG2", "number of loops with task: " + loop);

            executorService.shutdown();
            Log.i("TAG2", "executorService.isTerminated(): " + executorService.isTerminated());

            try {
                // Wait for all tasks to complete, or until the specified timeout (e.g., 1 minute)
                if (!executorService.awaitTermination(2, TimeUnit.MINUTES)) {
                    // Handle the case where not all tasks completed within the timeout
                    Log.e("TAG2", "Error: not all tasks completed within the timeout");
                }
            } catch (InterruptedException e) {
                // Handle interruption while waiting
                e.printStackTrace();
            }

            Log.i("TAG2", "audioDataIn: " + audioDataIn);
        }
        else if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED){
            Log.i("TAG2", "error: audioRecord state uninitialized");
        }
    }

    public void stopRecording() {
        isRecording = false;
        audioRecord.stop();
    }

    public void release() {
        if (audioRecord.getState() != AudioRecord.STATE_UNINITIALIZED) {
            audioRecord.release();
        }
    }
}
