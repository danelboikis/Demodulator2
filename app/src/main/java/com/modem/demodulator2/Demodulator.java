package com.modem.demodulator2;

import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.Arrays;

public class Demodulator {
    private static double carrierFrequency = 8000;
    private static double frequencyChange = 1000;

    private static int dataRate = 100; // time for one bit of data in ms
    private static int sampleRate = AudioRecorder.SAMPLE_RATE; // number of samples taken per second

    //private static int recTime = 3; // recording time in seconds

    private static double[] magnitudes = null;

    private static int numSamples = (int) Math.ceil((((double) dataRate) / 1000.0) * sampleRate); // number of samples for one bit

    public static String demodulate(byte[] audioData) throws Exception {
        double values[] = new double[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            byte b = audioData[i];
            double d = b;
            d = d / Byte.MAX_VALUE;
            values[i] = d;
        }

        double detectedFrequency = detectFrequency(values, 0, numSamples);

        Log.i("TAG1", "detected frequency: " + detectedFrequency);

        return null;
    }

    private static char getDigitalBit(double[] value, int offset, int len) throws Exception {
        double detectedFrequency = detectFrequency(value, offset, len);

        if (detectedFrequency == carrierFrequency) {
            return '0';
        }
        else if (detectedFrequency == (carrierFrequency + frequencyChange)) {
            return '1';
        }

        throw new Exception("Something went wrong with the frequencies config");
    }

    private static double detectFrequency(double[] values, int offset, int len) {
        if (offset == 0) {
            System.out.println(values.length);
        }
        //System.out.println(offset);
        int paddedValuesLen = Integer.highestOneBit(len) << 1; // rounds the length to be a power of 2
        double[] paddedValues = new double[paddedValuesLen];
        System.arraycopy(values, offset, paddedValues, 0, len); // copy array

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] transformedComplex = transformer.transform(paddedValues, TransformType.FORWARD);

        // magnitude[i] will hold the magnitude of the frequency = (i * sampleRate / paddedValuesLen)
        double[] magnitudes = new double[paddedValues.length / 2]; // only positive frequencies
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = transformedComplex[i].abs();
        }

        int carrierFrequencyIndex = (int) Math.round(carrierFrequency * paddedValuesLen / sampleRate);
        int changedFrequencyIndex = (int) Math.round((carrierFrequency + frequencyChange) * paddedValuesLen / sampleRate);

        double mag1 = magnitudes[carrierFrequencyIndex];
        double mag2 = magnitudes[changedFrequencyIndex];

        double detectedFrequency = (mag1 > mag2) ? carrierFrequency : (carrierFrequency + frequencyChange);

        //System.out.println("detected frequency: " + detectedFrequency);
        //System.out.println("magnitude difference: " + Math.abs(mag1 - mag2));

        return detectedFrequency;

    }
}
