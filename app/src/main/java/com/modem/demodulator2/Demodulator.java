package com.modem.demodulator2;

import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Demodulator {
    private static final double CARRIER_FREQUENCY = 8000;
    private static final double FREQUENCY_CHANGE = 1000;

    private static final int DATA_RATE = 100; // time for one bit of data in ms
    private static final int SAMPLE_RATE = AudioRecorder.SAMPLE_RATE; // number of samples taken per second

    private static final int MAX_REC_TIME = AudioRecorder.MAX_REC_TIME; // recording time in seconds

    private static final byte DEFAULT_BYTE_MAGNITUDE = 0;

    private static final int BLOCK_SIZE = (int) Math.ceil((((double) DATA_RATE) / 1000.0) * SAMPLE_RATE); // number of samples for one bit

    //private double[][] blockMagnitudes;

    private final char[] blockValues;

    private final double[] values;

    private static final int SKIP_SIZE = BLOCK_SIZE / (5 * 7);




    private final Queue<Thread> threadQueue = new LinkedList<>();

    public void addToQueue(Thread th) {
        threadQueue.add(th);
    }

    public Demodulator() {
        int bufferSize = MAX_REC_TIME * SAMPLE_RATE;
        //blockMagnitudes = new double[bufferSize - blockSize + 1][(int) Math.ceil(blockSize / 2.0)];
        blockValues = new char[getBlockNum(bufferSize - 1) + 1];
        values = new double[bufferSize];
        //magnitudes = new double[bufferSize / 2];
    }
    /*public void demodulate(byte[] audioData, int start, int len) throws Exception {
        if (len == 0) {
            return;
        }
        for (int i = start; i < start + len; i++) {
            byte b = audioData[i];
            double d = b;
            d = d / Byte.MAX_VALUE;
            values[i] = d;
        }

        synchronized (this) {
            while (!Thread.currentThread().equals(threadQueue.peek())) {
                try {
                    wait();
                }
                catch (InterruptedException exception) {
                    Log.e("TAG2", "Error during wait: " + exception.getMessage());
                }
            }

            updatePartiallyBlockMagnitude(start, len);

            threadQueue.remove();
            notifyAll();
        }
    }*/

    public void demodulate(byte[] audioData, int start, int len) throws Exception {
        if (len == 0) {
            return;
        }
        for (int i = start; i < start + len; i++) {
            byte b = audioData[i];
            double d = b;
            d = d / Byte.MAX_VALUE;
            values[i] = d;
        }

        updatePartiallyBlockMagnitude(start, len);
        //updateBlockMagnitude(start, len);
    }

    private char getDigitalBit(int offset, int len) throws Exception {
        double detectedFrequency = detectFrequency(offset, len);

        if (detectedFrequency == CARRIER_FREQUENCY) {
            return '0';
        }
        else if (detectedFrequency == (CARRIER_FREQUENCY + FREQUENCY_CHANGE)) {
            return '1';
        }

        throw new Exception("Something went wrong with the frequencies config");
    }

    private void updatePartiallyBlockMagnitude(int start, int len) {
        if (getBlockNum(start + len - 1) < 0) {
            return;
        }

        int startBlockNum = Math.max(0, getBlockNum(start));
        int endBlockNum = getBlockNum(start + len - 1);
        for (int i = startBlockNum; i <= endBlockNum; i++) {
            if (i % SKIP_SIZE == 0) {
                if (i == 207270) {
                    Log.i("TAG2", "here");
                }
                calcBlockValue(i);
                Log.i("TAG3", "block " + i + ": " + blockValues[i]);
            }
        }
    }

    private void updateBlockMagnitude(int start, int len) {
        if (getBlockNum(start + len - 1) < 0) {
            return;
        }

        int startBlockNum = Math.max(0, getBlockNum(start));
        int endBlockNum = getBlockNum(start + len - 1);
        for (int i = startBlockNum; i <= endBlockNum; i++) {
            calcBlockValue(i);
            Log.i("TAG2", "block " + i + ": " + blockValues[i]);
        }

    }

    private void calcBlockValue(int blockNum) {
        /*double[] blockMagnitudes = new double[paddedBlockSize];
        Arrays.fill(blockMagnitudes[blockNum], defaultByteMagnitude);
        System.arraycopy(values, getStartIndex(blockNum), blockMagnitudes[blockNum], 0, blockSize);*/
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

    private static int getBlockNum(int endIndex) {
        return  endIndex + 1 - BLOCK_SIZE;
    }

    private static int getStartIndex(int blockNum) {
        return blockNum;
    }

    private double detectFrequency(int offset, int len) {
        int paddedValuesLen = Integer.highestOneBit(len) << 1; // rounds the length to be a power of 2
        double[] paddedValues = new double[paddedValuesLen];
        try {
            Arrays.fill(paddedValues, DEFAULT_BYTE_MAGNITUDE);
        }
        catch (Throwable e) {
            Log.e("TAG2", "Error during fill: " + e.getMessage());
        }
        System.arraycopy(values, offset, paddedValues, 0, len); // copy array


        /*FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] transformedComplex = null;
        try {
            transformedComplex = transformer.transform(paddedValues, TransformType.FORWARD);
        }
        catch (Throwable e) {
            Log.e("TAG2", "Error during transform: " + e.getMessage());
        }



        // magnitude[i] will hold the magnitude of the frequency = (i * sampleRate / paddedValuesLen)
        double[] magnitudes = new double[paddedValues.length / 2]; // only positive frequencies
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = transformedComplex[i].abs();
        }

        int carrierFrequencyIndex = (int) Math.round(CARRIER_FREQUENCY * paddedValuesLen / SAMPLE_RATE);
        int changedFrequencyIndex = (int) Math.round((CARRIER_FREQUENCY + FREQUENCY_CHANGE) * paddedValuesLen / SAMPLE_RATE);

        double mag1 = magnitudes[carrierFrequencyIndex];
        double mag2 = magnitudes[changedFrequencyIndex];

        double detectedFrequency = (mag1 > mag2) ? CARRIER_FREQUENCY : (CARRIER_FREQUENCY + FREQUENCY_CHANGE);*/

        //System.out.println("detected frequency: " + detectedFrequency);
        //System.out.println("magnitude difference: " + Math.abs(mag1 - mag2));

        //return detectedFrequency;

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

        // if the max mag is lower than 300 return different

        double detectedFrequency = (mag1 > mag2) ? CARRIER_FREQUENCY : (CARRIER_FREQUENCY + FREQUENCY_CHANGE);

        //System.out.println("detected frequency: " + detectedFrequency);
        //System.out.println("magnitude difference: " + Math.abs(mag1 - mag2));

        return detectedFrequency;
    }

    private static final String HANDSHAKE_MESSAGE = "0101010101010101010101010101010101";
    public String analyzeData() {
        boolean found = false;
        int blockIn = 0;

        while (!found && (blockIn + BLOCK_SIZE * (HANDSHAKE_MESSAGE.length() - 1)) < blockValues.length) {
            found = true;
            for (int i = 0; i < HANDSHAKE_MESSAGE.length() && found; i++) {
                if (blockValues[blockIn + i * BLOCK_SIZE] != HANDSHAKE_MESSAGE.charAt(i)) {
                    found = false;
                }
            }

            blockIn++;
        }

        if (found) {
            return "message found at: " + (blockIn - 1);
        }
        else {
            return "message not found";
        }

    }

    public String partiallyAnalyzeData() {
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

        if (!found) {
            return "message not found";
        }
        else {
            return "message found at: " + blockIn;
        }

        /*blockIn -= SKIP_SIZE;

        blockIn += HANDSHAKE_MESSAGE.length() * BLOCK_SIZE;


        byte len = 0;
        for (int i = 0; i < 8; i++) {
            int val = blockValues[blockIn + i * BLOCK_SIZE] == '0' ? 0 : 1;
            len += val * Math.pow(2, 7 - i);
        }

        Log.i("TAG2", "message len: " + len);

        blockIn += 8 * BLOCK_SIZE;

        return readMessage(blockIn, len);*/
    }

    private String readMessage(int blockIn, int len) {
        String message = "";
        for (int i = 0; i < len; i++) {
            message += blockValues[blockIn + i * BLOCK_SIZE];

        }

        return message;
    }

    public void joinThreadQueue(){
        for (Thread thread : threadQueue) {
            try {
                thread.join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public char[] getBlockValues() {
        return blockValues;
    }
}
