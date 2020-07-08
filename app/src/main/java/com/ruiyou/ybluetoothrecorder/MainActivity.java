package com.ruiyou.ybluetoothrecorder;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ruiyou.ybluetoothrecorder.R;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MediaRecorder mediaRecorder = null;
    private File soundFile = null;
    private MediaPlayer mediaPlayer = null;
    private Button bt_play,bt_record,bt_openBluetoothMic;
    private TextView tv_recordTime,tv_playTime,tv_duration;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindView();
        runTimer();
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
                    tv_duration.setText(R.string.timeFormat);
                    tv_playTime.setText(R.string.timeFormat);
                }
                //录音计时
                if (isRecording){
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
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

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
                    startRecord();
                    isRecording=true;
                    bt_record.setText(R.string.stopRecord);
                }else {
                    stopRecord();
                    isRecording=false;
                    bt_record.setText(R.string.startRecord);
                }
                break;
            case R.id.bt_play:
                if (soundFile==null||!soundFile.exists()){
                    Toast.makeText(this,"文件不存在",Toast.LENGTH_LONG).show();
                    break;
                }
                String path = soundFile.getAbsolutePath();
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
                    bt_play.setText(R.string.stop);
                } else {
                    mediaPlayer.stop();
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

    //开始录音
    private void startRecord(){
        File dir = new File(Environment.getExternalStorageDirectory(),"Sounds");
        if (!dir.exists()){
            dir.mkdir();
        }
        soundFile = new File(dir,System.currentTimeMillis()+".amr");
        if (!soundFile.exists()){
            try {
                soundFile.createNewFile();
            }catch (IOException e){
                Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
            }
        }
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        mediaRecorder.setOutputFile(soundFile.getAbsolutePath());
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        }catch (IOException e){
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    //停止录音
    private void stopRecord(){
        if (mediaRecorder != null){
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
}