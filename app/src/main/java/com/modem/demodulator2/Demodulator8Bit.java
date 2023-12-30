package com.modem.demodulator2;

import android.media.AudioFormat;



public class Demodulator8Bit extends Demodulator{
    public Demodulator8Bit() {
        super(AudioFormat.ENCODING_PCM_8BIT);
    }

    @Override
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
    }

    @Override
    public void demodulate(short[] audioData, int start, int len) throws Exception {
        throw new Exception("unsupported operation");
    }
}
