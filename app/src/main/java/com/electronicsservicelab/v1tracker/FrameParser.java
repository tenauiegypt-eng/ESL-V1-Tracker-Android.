package com.electronicsservicelab.v1tracker;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Protocol: A5 5A 20 01 + 32 x (Xlo Xhi Ylo Yhi) + XOR checksum. */
public class FrameParser {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    public synchronized List<int[][]> feed(byte[] data, int count) {
        buffer.write(data, 0, count);
        byte[] all = buffer.toByteArray();
        List<int[][]> frames = new ArrayList<>();
        int p=0;
        while (p+133<=all.length) {
            if ((all[p]&255)!=0xA5 || (all[p+1]&255)!=0x5A || (all[p+2]&255)!=32 || (all[p+3]&255)!=1) {p++; continue;}
            int checksum=0;
            for(int j=0;j<132;j++) checksum ^= all[p+j]&255;
            if (checksum!=(all[p+132]&255)) {p++; continue;}
            int[][] samples=new int[2][32];
            for(int j=0;j<32;j++) {
                int off=p+4+j*4;
                samples[0][j]=(all[off]&255)|((all[off+1]&255)<<8);
                samples[1][j]=(all[off+2]&255)|((all[off+3]&255)<<8);
            }
            frames.add(samples); p+=133;
        }
        buffer.reset();
        // Prevent unlimited growth on malformed or noisy streams.
        int remaining=all.length-p;
        if (remaining>132) {p=all.length-132; remaining=132;}
        if (remaining>0) buffer.write(all,p,remaining);
        return frames;
    }
}
