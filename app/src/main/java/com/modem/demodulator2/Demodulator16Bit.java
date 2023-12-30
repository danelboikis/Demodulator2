package com.modem.demodulator2;

import android.media.AudioFormat;

public class Demodulator16Bit extends Demodulator {

    public Demodulator16Bit() {
        super(AudioFormat.ENCODING_PCM_16BIT);
    }

    @Override
    public void demodulate(byte[] audioData, int start, int len) throws Exception {
        throw new Exception("unsupported operation");
    }

    @Override
    public void demodulate(short[] audioData, int start, int len) throws Exception {
        if (len == 0) {
            return;
        }
        for (int i = start; i < start + len; i++) {
            short b = audioData[i];
            double d = b;
            d = d / Short.MAX_VALUE;
            values[i] = d;
        }

        updatePartiallyBlockMagnitude(start, len);
    }



}
