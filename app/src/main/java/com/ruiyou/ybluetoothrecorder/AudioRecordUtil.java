package com.ruiyou.ybluetoothrecorder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class AudioRecordUtil {

    private AudioRecord audioRecord;
    private int sampleRate, channelConfig, audioFormat,minBufferSize,bitsPerSample;
    private boolean whetherStop;

    public static AudioRecordUtil getInstance(int sampleRate,int channelConfig
    ,int bitsPerSample){
        AudioRecordUtil aUtil = new AudioRecordUtil();
        aUtil.sampleRate=sampleRate;
        aUtil.channelConfig=channelConfig;
        aUtil.bitsPerSample=bitsPerSample;
        aUtil.audioFormat= AudioFormat.ENCODING_PCM_16BIT;
        return aUtil;
    }
    private void initAudioRecord() {
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                audioFormat);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, audioFormat, minBufferSize);
    }

    //开始
    public void startRecord(File file) {
        try {
            if (file.exists()) file.delete();
            file.createNewFile();
            RandomAccessFile rFile = new RandomAccessFile(file,"rw");
            initAudioRecord();
            whetherStop=false;
            audioRecord.startRecording();
            //后面再修改正确参数
            rFile.write(getWavHeader(0));
            while (!whetherStop){
                byte[] bs = new byte[minBufferSize];
                audioRecord.read(bs,0,bs.length);
                rFile.write(bs);
            }
            rFile.seek(0);
            long len = rFile.length();
            rFile.write(getWavHeader(len-44));
            audioRecord.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //停止
    public void stopRecord(){
        whetherStop = true;
    }

    private byte[] getWavHeader(long audioDataSize){
        byte[] bs = new byte[44];
        //ChunkId:RIFF
        bs[0]='R';
        bs[1]='I';
        bs[2]='F';
        bs[3]='F';
        //ChunkSize
        long cSize = audioDataSize+44-8;
        bs[4]=(byte)(cSize&0xff);
        bs[5]=(byte)((cSize>>8)&0xff);
        bs[6]=(byte)((cSize>>16)&0xff);
        bs[7]=(byte)((cSize>>24)&0xff);
        //fccType:WAVE
        bs[8]='W';
        bs[9]='A';
        bs[10]='V';
        bs[11]='E';
        //SubChunkId1:"fmt "
        bs[12]='f';
        bs[13]='m';
        bs[14]='t';
        bs[15]=' ';
        //SubChunkSize1:16
        bs[16]=16;
        bs[17]=0;
        bs[18]=0;
        bs[19]=0;
        //FormatTag:1(PCM encode)
        bs[20]=1;
        bs[21]=0;
        //Channels:1(mono),2(stereo)
        int channels=channelConfig== AudioFormat.CHANNEL_IN_MONO?1:2;
        bs[22]=(byte)channels;
        bs[23]=0;
        //SamplesPerSec
        bs[24]=(byte)(sampleRate&0xff);
        bs[25]=(byte)((sampleRate>>8)&0xff);
        bs[26]=(byte)((sampleRate>>16)&0xff);
        bs[27]=(byte)((sampleRate>>24)&0xff);
        //BytesPerSec:channels*sampleRate*bitsPerSample/8
        int byteRate=channels*sampleRate*bitsPerSample/8;
        bs[28]=(byte)(byteRate&0xff);
        bs[29]=(byte)((byteRate>>8)&0xff);
        bs[30]=(byte)((byteRate>>16)&0xff);
        bs[31]=(byte)((byteRate>>24)&0xff);
        //BlockAlign:channels*bitsPerSample/8
        int blockSize = channels*bitsPerSample/8;
        bs[32]=(byte)(blockSize&0xff);
        bs[33]=(byte)((blockSize>>8)&0xff);
        //BitsPerSample
        bs[34]=(byte)bitsPerSample;
        bs[35]=0;
        //SubChunkId2:"data"
        bs[36]='d';
        bs[37]='a';
        bs[38]='t';
        bs[39]='a';
        //SubChunkSize2:audio data size
        bs[40]=(byte)(audioDataSize&0xff);
        bs[41]=(byte)((audioDataSize>>8)&0xff);
        bs[42]=(byte)((audioDataSize>>16)&0xff);
        bs[43]=(byte)((audioDataSize>>24)&0xff);
        return bs;
    }
}
