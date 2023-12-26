package com.modem.demodulator2;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.Arrays;

public class AudioRecorder {
    public static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT;

    private AudioRecord audioRecord;
    private int bufferSize;
    private byte[] audioData;

    private volatile int audioDataIn;

    private volatile boolean isRecording ;

    public AudioRecorder(Context context) throws Exception {
        bufferSize = SAMPLE_RATE * 100;
        Log.i("TAG1", "bufferSize: " + bufferSize);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            throw new Exception();
        }
        isRecording = false;
        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize);
        }
        catch (Exception e) {
            Log.e("TAG2", "Error during AudioRecord initialization: " + e.getMessage());
            e.printStackTrace();
        }

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            // Handle initialization error
            Log.e("TAG2", "AudioRecord initialization failed");
        }
        else {
            Log.i("TAG2", "AudioRecord initialization succeeded");
        }

        audioData = new byte[bufferSize];
    }

    public void startRecording() {
        Log.i("TAG2", "1");
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            Log.i("TAG2", "2");
            audioRecord.startRecording();
            isRecording = true;

            Log.i("TAG2", "3");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        audioDataIn = 0;
                        //int audioDataIn = 0;
                        while (isRecording) {
                            int bytesRead = audioRecord.read(audioData, audioDataIn, /*bufferSize - audioDataIn*/10000);
                            audioDataIn += bytesRead;
                            Log.i("TAG1", "bytesRead: " + bytesRead);
                            int audioRecordState = audioRecord.getState();
                            Log.i("TAG1", "audioRecordState good: " + (audioRecordState == AudioRecord.STATE_INITIALIZED));
                            byte[] audioDataRead = new byte[bytesRead];
                            System.arraycopy(audioData, audioDataIn - bytesRead, audioDataRead, 0, bytesRead);
                            Log.i("TAG1", "audioDataRead: " + Arrays.toString(audioDataRead));
                        }
                    } catch (Exception e) {
                        Log.e("TAG1", "Exception in startRecording: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }).start();

            /*new Thread(new Runnable() {
                @Override
                public void run() {
                    int dataRead = 0;
                    while (isRecording) {
                        int blockSize =

                    }
                }
            }).start();*/
        }
        else if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED){
            Log.i("TAG2", "error");
        }
    }

    public void stopRecording() {
        isRecording = false;
        audioRecord.stop();
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public void release() {
        if (audioRecord.getState() != AudioRecord.STATE_UNINITIALIZED) {
            audioRecord.release();
        }
    }
}
