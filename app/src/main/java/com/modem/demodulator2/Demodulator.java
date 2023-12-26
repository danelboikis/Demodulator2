package com.modem.demodulator2;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Demodulator {
    private static final double CARRIER_FREQUENCY = 8000;
    private static final double FREQUENCY_CHANGE = 1000;

    private static final int DATA_RATE = 100; // time for one bit of data in ms
    private static final int SAMPLE_RATE = AudioRecorder.SAMPLE_RATE; // number of samples taken per second

    private static final int MAX_REC_TIME = AudioRecorder.MAX_REC_TIME; // recording time in seconds

    private static final byte DEFAULT_BYTE_MAGNITUDE = -128;

    private static final int BLOCK_SIZE = (int) Math.ceil((((double) DATA_RATE) / 1000.0) * SAMPLE_RATE); // number of samples for one bit

    //private double[][] blockMagnitudes;

    private final char[] blockValues;

    private final double[] values;




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
    public void demodulate(byte[] audioData, int start, int len) throws Exception {
        if (len == 0) {
            return;
        }
        for (int i = start; i < len; i++) {
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

                }
            }

            updateBlockMagnitude(start, len);

            threadQueue.remove();
            notifyAll();
        }
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

    private void updateBlockMagnitude(int start, int len) {
        if (getBlockNum(start + len - 1) < 0) {
            return;
        }

        int startBlockNum = Math.max(0, getBlockNum(start));
        int endBlockNum = getBlockNum(start + len - 1);
        for (int i = startBlockNum; i <= endBlockNum; i++) {
            calcBlockValue(i);
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
        if (offset == 0) {
            System.out.println(values.length);
        }
        //System.out.println(offset);
        int paddedValuesLen = Integer.highestOneBit(len) << 1; // rounds the length to be a power of 2
        double[] paddedValues = new double[paddedValuesLen];
        Arrays.fill(paddedValues, DEFAULT_BYTE_MAGNITUDE);
        System.arraycopy(values, offset, paddedValues, 0, len); // copy array

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] transformedComplex = transformer.transform(paddedValues, TransformType.FORWARD);

        // magnitude[i] will hold the magnitude of the frequency = (i * sampleRate / paddedValuesLen)
        double[] magnitudes = new double[paddedValues.length / 2]; // only positive frequencies
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = transformedComplex[i].abs();
        }

        int carrierFrequencyIndex = (int) Math.round(CARRIER_FREQUENCY * paddedValuesLen / SAMPLE_RATE);
        int changedFrequencyIndex = (int) Math.round((CARRIER_FREQUENCY + FREQUENCY_CHANGE) * paddedValuesLen / SAMPLE_RATE);

        double mag1 = magnitudes[carrierFrequencyIndex];
        double mag2 = magnitudes[changedFrequencyIndex];

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
}
