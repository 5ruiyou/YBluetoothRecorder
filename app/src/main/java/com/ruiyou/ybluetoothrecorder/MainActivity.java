package com.ruiyou.ybluetoothrecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private AudioRecordUtil aRecord = null;
    private File audioFile = null;
    private MediaPlayer mediaPlayer = null;
    private Button bt_play,bt_record,bt_openBluetoothMic;
    private TextView tv_recordTime,tv_playTime,tv_duration;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        checkPermission();
        bindView();
        aRecord=AudioRecordUtil.getInstance(44100,
                AudioFormat.CHANNEL_IN_MONO,16);
        runTimer();
    }

    //检查权限
    private void checkPermission(){
        if (Build.VERSION.SDK_INT< Build.VERSION_CODES.M){
            return;//API小于23（android6.0）不需要动态申请
        }
        String[] ps = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
        };
        for (int i = 0; i<ps.length; i++){
            if(checkSelfPermission(ps[i])!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(ps,678);
            }
        }
    }

    //计时器
    int seconds=0;
    boolean isRecording=false;
    private void runTimer() {
        final Handler handler=new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this,1000);
                //播放显示时间
                if (mediaPlayer!=null && mediaPlayer.isPlaying()){
                    int du,cu;
                    du=mediaPlayer.getDuration();
                    cu=mediaPlayer.getCurrentPosition();
                    seekBar.setEnabled(true);
                    seekBar.setMax(du);
                    seekBar.setProgress(cu);
                    tv_playTime.setText(String.format(Locale.getDefault(),
                            "%02d:%02d:%02d",cu/3600000,
                            cu%3600000/60000,cu%60000/1000));
                    tv_duration.setText(String.format(Locale.getDefault(),
                            "%02d:%02d:%02d",du/3600000,
                            du%3600000/60000,du%60000/1000));
                } else {
                    seekBar.setEnabled(false);
                    bt_play.setText(R.string.play);
                    bt_record.setEnabled(true);
                    tv_duration.setText(R.string.timeFormat);
                    tv_playTime.setText(R.string.timeFormat);
                }
                //录音计时
                if (isRecording== true){
                    tv_recordTime.setText(String.format(Locale.getDefault(),
                            "%02d:%02d:%02d",seconds/3600,
                            seconds%3600/60,seconds%60));
                    seconds++;
                } else {
                    tv_recordTime.setText(R.string.timeFormat);
                    seconds = 0;
                }
            }
        });
    }

    private void bindView() {
        bt_record=findViewById(R.id.bt_record);
        bt_record.setOnClickListener(this);
        bt_play=findViewById(R.id.bt_play);
        bt_play.setOnClickListener(this);
        bt_openBluetoothMic=findViewById(R.id.bt_openBluetoothMic);
        bt_openBluetoothMic.setOnClickListener(this);
        tv_recordTime=findViewById(R.id.tv_recordTime);
        tv_playTime=findViewById(R.id.tv_playTime);
        tv_duration=findViewById(R.id.tv_duration);
        seekBar=findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer!=null&&mediaPlayer.isPlaying()){
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            }
        });
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.bt_record:
                if (bt_record.getText()== getString(R.string.startRecord)){
                    File dir = new File(Environment.getExternalStorageDirectory(),
                            "BluetoothRecording");
                    if (!dir.exists()||!dir.isDirectory()) dir.mkdir();
                    Date time = new Date(System.currentTimeMillis());
                    String fileName=String.format(Locale.getDefault(),
                            "%04d%02d%02d%02d%02d%02d",time.getYear()+1900,
                            time.getMonth()+1, time.getDate(),time.getHours(),time.getMinutes(),
                            time.getSeconds())+".wav";
                    audioFile = new File(dir,   fileName);
                    new Thread(()->{
                        aRecord.startRecord(this,audioFile);
                    }).start();
                    bt_play.setEnabled(false);
                    isRecording=true;
                    bt_record.setText(R.string.stopRecord);
                }else {
                    if (aRecord!=null) aRecord.stopRecord();
                    bt_play.setEnabled(true);
                    isRecording=false;
                    bt_record.setText(R.string.startRecord);
                }
                break;
            case R.id.bt_play:
                if (audioFile ==null||!audioFile.exists()){
                    Toast.makeText(this,"文件不存在",Toast.LENGTH_LONG).show();
                    break;
                }
                String path = audioFile.getAbsolutePath();
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                if (bt_play.getText()==getString(R.string.play)){
                    try {
                        mediaPlayer.reset();//必须重置，否则第二次播放会闪退。
                        mediaPlayer.setDataSource(path);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
                    }
                    mediaPlayer.start();
                    bt_record.setEnabled(false);
                    bt_play.setText(R.string.stop);
                } else {
                    mediaPlayer.stop();
                    bt_record.setEnabled(true);
                    bt_play.setText(R.string.play);
                }
                break;
            case R.id.bt_openBluetoothMic:
                AudioManager audioManager =(AudioManager)getSystemService(AUDIO_SERVICE);
                if (audioManager==null) return;
                if (bt_openBluetoothMic.getText()==getString(R.string.openBluetoothMic)){
                    audioManager.setBluetoothScoOn(true);
                    audioManager.startBluetoothSco();
                    bt_openBluetoothMic.setText(R.string.closeBluetoothMic);
                } else {
                    audioManager.setBluetoothScoOn(false);
                    audioManager.stopBluetoothSco();
                    bt_openBluetoothMic.setText(R.string.openBluetoothMic);
                }
                break;
        }
    }
}