package com.modem.demodulator2;

import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

public abstract class Demodulator {
    protected static final double CARRIER_FREQUENCY = 8000;
    protected static final double FREQUENCY_CHANGE = 1000;

    protected static final int DATA_RATE = 100; // time for one bit of data in ms
    protected static final int SAMPLE_RATE = AudioRecorder.SAMPLE_RATE; // number of samples taken per second

    protected static final int MAX_REC_TIME = AudioRecorder.MAX_REC_TIME; // recording time in seconds

    protected static final byte DEFAULT_BYTE_MAGNITUDE = 0;

    protected static final int BLOCK_SIZE = (int) Math.ceil((((double) DATA_RATE) / 1000.0) * SAMPLE_RATE); // number of samples for one bit

    protected final char[] blockValues;

    protected final double[] values;

    protected static final int SKIP_SIZE = BLOCK_SIZE / (7);

    protected static final String HANDSHAKE_MESSAGE = "01010101010101010101010101010101";

    protected final int audio_format;

    protected Demodulator(int audioFormat) {
        int bufferSize = MAX_REC_TIME * SAMPLE_RATE;
        blockValues = new char[getBlockNum(bufferSize - 1) + 1];
        values = new double[bufferSize];
        audio_format = audioFormat;
    }

    public abstract void demodulate(byte[] audioData, int start, int len) throws Exception;

    public abstract void demodulate(short[] audioData, int start, int len) throws Exception;

    protected void updatePartiallyBlockMagnitude(int start, int len) {
        if (getBlockNum(start + len - 1) < 0) {
            return;
        }

        int startBlockNum = Math.max(0, getBlockNum(start));
        int endBlockNum = getBlockNum(start + len - 1);
        for (int i = startBlockNum; i <= endBlockNum; i++) {
            if (i % SKIP_SIZE == 0) {
                calcBlockValue(i);
                Log.i("TAG3", "block " + i + ": " + blockValues[i]);
            }
        }
    }

    protected void calcBlockValue(int blockNum) {
        int startIndex = getStartIndex(blockNum);
        char bitVal = '!';
        try {
            bitVal = getDigitalBit(startIndex, startIndex + BLOCK_SIZE);
        }
        catch (Exception e) {
            Log.e("TAG2", "Error during getDigitalBit: " + e.getMessage());
        }

        blockValues[blockNum] = bitVal;
    }

    protected char getDigitalBit(int offset, int len) throws Exception {
        double detectedFrequency = detectFrequency(offset, len);

        if (detectedFrequency == CARRIER_FREQUENCY) {
            return '0';
        }
        else if (detectedFrequency == (CARRIER_FREQUENCY + FREQUENCY_CHANGE)) {
            return '1';
        }

        throw new Exception("Something went wrong with the frequencies config");
    }

    protected double detectFrequency(int offset, int len) {
        int paddedValuesLen = Integer.highestOneBit(len) << 1; // rounds the length to be a power of 2
        double[] paddedValues = new double[paddedValuesLen];
        if (DEFAULT_BYTE_MAGNITUDE != 0) {
            Arrays.fill(paddedValues, DEFAULT_BYTE_MAGNITUDE);
        }
        System.arraycopy(values, offset, paddedValues, 0, len); // copy array

        DoubleFFT_1D fft = new DoubleFFT_1D(paddedValuesLen);
        fft.realForward(paddedValues);

        int carrierFrequencyIndex = (int) Math.round(CARRIER_FREQUENCY * paddedValuesLen / SAMPLE_RATE);
        int changedFrequencyIndex = (int) Math.round((CARRIER_FREQUENCY + FREQUENCY_CHANGE) * paddedValuesLen / SAMPLE_RATE);

        double realPart1 = paddedValues[2 * carrierFrequencyIndex];
        double realPart2 = paddedValues[2 * changedFrequencyIndex];
        double imagPart1 = paddedValues[2 * carrierFrequencyIndex + 1];
        double imagPart2 = paddedValues[2 * changedFrequencyIndex + 1];
        Complex complex1 = new Complex(realPart1, imagPart1);
        Complex complex2 = new Complex(realPart2, imagPart2);

        double mag1 = complex1.abs();
        double mag2 = complex2.abs();

        Log.i("TAG4", "mag1: " + mag1 + ", mag2: " + mag2);

        // proposal: if the max mag is lower than 300 return different

        double detectedFrequency = (mag1 > mag2) ? CARRIER_FREQUENCY : (CARRIER_FREQUENCY + FREQUENCY_CHANGE);

        //System.out.println("detected frequency: " + detectedFrequency);
        //System.out.println("magnitude difference: " + Math.abs(mag1 - mag2));

        return detectedFrequency;
    }

    protected String partiallyAnalyzeData() {
        boolean found = false;
        int blockIn = 0;

        while (!found && (blockIn + BLOCK_SIZE * (HANDSHAKE_MESSAGE.length() - 1)) < blockValues.length) {
            found = true;
            for (int i = 0; i < HANDSHAKE_MESSAGE.length() && found; i++) {
                if (blockValues[blockIn + i * BLOCK_SIZE] != HANDSHAKE_MESSAGE.charAt(i)) {
                    found = false;
                }
            }

            blockIn += SKIP_SIZE;
        }

        blockIn -= SKIP_SIZE;

        if (!found) {
            return "message not found";
        }

        Log.i("TAG3", "message found at: " + blockIn);

        blockIn += HANDSHAKE_MESSAGE.length() * BLOCK_SIZE; // index of message length

        byte len = 0;
        for (int i = 0; i < 8; i++) {
            int val = blockValues[blockIn + i * BLOCK_SIZE] == '0' ? 0 : 1;
            len += val * Math.pow(2, 7 - i);
        }

        Log.i("TAG3", "message len: " + len);

        blockIn += 8 * BLOCK_SIZE; // index of message

        return readMessage(blockIn, len);
    }

    protected String readMessage(int blockIn, int len) {
        String message = "";
        for (int i = 0; i < len; i++) {
            message += blockValues[blockIn + i * BLOCK_SIZE];
        }

        Log.i("TAG3", "message: " + message);

        return message;
    }

    protected static int getBlockNum(int endIndex) {
        return  endIndex + 1 - BLOCK_SIZE;
    }

    protected static int getStartIndex(int blockNum) {
        return blockNum;
    }

    public int getAudio_format() {
        return audio_format;
    }
}
