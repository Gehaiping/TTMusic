package cn.ucai.ttmusic.controller.activity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.ttmusic.R;
import cn.ucai.ttmusic.TTApplication;
import cn.ucai.ttmusic.model.I;
import cn.ucai.ttmusic.model.db.DBManager;
import cn.ucai.ttmusic.model.db.Music;
import cn.ucai.ttmusic.model.utils.TimeUtil;
import cn.ucai.ttmusic.model.utils.ToastUtil;
import me.wcy.lrcview.LrcView;

public class PlayActivity extends BaseActivity {

    Context mContext;

    @BindView(R.id.blurView)
    ImageView blurView; //模糊背景层
    @BindView(R.id.play_musicName)
    TextView playMusicName; //歌曲名
    @BindView(R.id.play_singerName)
    TextView playSingerName; //歌手名
    @BindView(R.id.play_start_time)
    TextView playStartTime; //起始时间、已播放时间
    @BindView(R.id.play_seekbar)
    SeekBar playSeekbar; //进度控制条
    @BindView(R.id.play_end_time)
    TextView playEndTime; //歌曲总时间
    @BindView(R.id.play_playMode_btn)
    ImageView playPlayModeBtn; //播放模式按钮
    @BindView(R.id.play_btn)
    ImageView playBtn; //播放按钮
    @BindView(R.id.btn_favorite)
    ImageView btnFavorite; //收藏按钮
    @BindView(R.id.play_lrc)
    LrcView lrcView; //歌词

    Music currentMusic;
    int[] modeIcons = new int[]{R.drawable.mode_normal, R.drawable.mode_single, R.drawable.mode_shuffle};
    boolean check = true; //是否需要检测是否被收藏
    boolean isCollected; //当前歌曲是否被收藏

    MediaPlayer mediaPlayer; // 音乐播放对象
    LocalBroadcastManager broadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        ButterKnife.bind(this);
        // 保持屏幕唤醒
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mContext = this;
        musicService = TTApplication.getInstance().getMusicService();
        if (musicService == null) {
            return;
        }
        broadcastManager = LocalBroadcastManager.getInstance(TTApplication.getContext());
        initView();
    }

    private void initView() {
        handler.sendEmptyMessage(I.Handler.PLAY_MUSIC);
        playSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playStartTime.setText(TimeUtil.toTime(seekBar.getProgress()));
                musicService.moveToProgress(seekBar.getProgress());
                lrcView.onDrag(seekBar.getProgress());
            }
        });
        mediaPlayer = musicService.getMediaPlayer();
        // 设置播放完毕监听
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                // 如果播放完毕就直接下一曲
                musicService.nextMusic();
                showLrc(musicService.getCurrentMusic());
            }
        });
        //加载歌词
        showLrc(musicService.getCurrentMusic());
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == I.Handler.PLAY_MUSIC) {
                currentMusic = musicService.getCurrentMusic();
                playMusicName.setText(currentMusic.getTitle());
                playSingerName.setText(currentMusic.getSinger());

                playSeekbar.setMax(musicService.getDuration());
                playSeekbar.setProgress(musicService.getCurrentTime());
                playStartTime.setText(TimeUtil.toTime(musicService.getCurrentTime()));
                playEndTime.setText(TimeUtil.toTime((int) currentMusic.getTime()));

                playPlayModeBtn.setImageResource(modeIcons[musicService.getPlayMode()]);
                if (musicService.isPlay()) {
                    playBtn.setImageResource(R.drawable.pause_button);
                } else {
                    playBtn.setImageResource(R.drawable.play_button);
                }
                if (check) {
                    isCollected = DBManager.isCollected(currentMusic.getSongId());
                    btnFavorite.setImageResource(isCollected ? R.drawable.favorite_selected_on : R.drawable.favorite_selected_off);
                    check = false;
                }
                handler.sendEmptyMessageDelayed(I.Handler.PLAY_MUSIC, 500); //每半秒刷新一次
            }
            if (msg.what == I.Handler.SHOW_LRC) {
                showLrc(musicService.getCurrentMusic());
            }
        }
    };

    //返回、分享、收藏
    @OnClick({R.id.icon_back, R.id.icon_share, R.id.btn_favorite})
    public void action(View view) {
        switch (view.getId()) {
            case R.id.icon_back:
                onBackPressed();
                break;
            case R.id.icon_share:
                ToastUtil.show(mContext, "分享(开发中)");
                break;
            case R.id.btn_favorite:
                if (isCollected) {
                    DBManager.cancelCollect(currentMusic.getSongId());
                    ToastUtil.show(mContext, "取消收藏");
                } else {
                    DBManager.collectMusic(currentMusic);
                    ToastUtil.show(mContext, "收藏成功");
                }
                check = true;
                Intent update = new Intent(I.BroadCast.UPDATE_LIST);
                broadcastManager.sendBroadcast(update);
                break;
        }
    }

    //播放、上一曲、下一曲
    @OnClick({R.id.play_pre_btn, R.id.play_btn, R.id.play_next_btn})
    public void playControl(View view) {
        switch (view.getId()) {
            case R.id.play_pre_btn:
                musicService.frontMusic();
                check = true;
                handler.sendEmptyMessage(I.Handler.SHOW_LRC);
                break;
            case R.id.play_btn:
                if (musicService.isPlay()) {
                    musicService.setCurrentTime(musicService.getCurrentTime());
                    musicService.pause();
                    handler.removeCallbacks(playLrcs);
                } else {
                    musicService.start();
                    handler.post(playLrcs);
                }
                break;
            case R.id.play_next_btn:
                musicService.nextMusic();
                check = true;
                handler.sendEmptyMessage(I.Handler.SHOW_LRC);
                break;
        }
        handler.sendEmptyMessage(I.Handler.PLAY_MUSIC); //立即生效
    }

    //播放模式切换
    @OnClick(R.id.play_playMode_btn)
    public void setPlayMode(View v) {
        switch (musicService.getPlayMode()) {
            case I.PlayMode.MODE_NORMAL:
                musicService.setPlayMode(I.PlayMode.MODE_SINGLE);
                break;
            case I.PlayMode.MODE_SINGLE:
                musicService.setPlayMode(I.PlayMode.MODE_SHUFFLE);
                break;
            case I.PlayMode.MODE_SHUFFLE:
                musicService.setPlayMode(I.PlayMode.MODE_NORMAL);
                break;
        }
        handler.sendEmptyMessage(I.Handler.PLAY_MUSIC);//立即生效
    }

    Runnable playLrcs = new Runnable() {
        @Override
        public void run() {
            if (musicService.isPlay()) {
                long time = musicService.getCurrentTime();
                lrcView.updateTime(time);
            }
            handler.postDelayed(this, 100);
        }
    };

    public void showLrc(Music music) {
        // 读取同文件夹下的歌词文件
        File f = new File(music.getUrl().replace(".mp3", ".lrc"));
        try {
            lrcView.loadLrc(f);
            handler.post(playLrcs);
        } catch (Exception e) {
            lrcView.setLabel("找不到歌词(&gt;_&lt;)");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(playLrcs);
        super.onDestroy();
    }
}
