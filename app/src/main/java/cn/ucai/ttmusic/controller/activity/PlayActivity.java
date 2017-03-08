package cn.ucai.ttmusic.controller.activity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.ttmusic.R;
import cn.ucai.ttmusic.TTApplication;
import cn.ucai.ttmusic.controller.adapter.PlayPagerAdapter;
import cn.ucai.ttmusic.controller.fragment.DiscoFragment;
import cn.ucai.ttmusic.controller.fragment.LrcFrament;
import cn.ucai.ttmusic.controller.service.MusicService;
import cn.ucai.ttmusic.model.I;
import cn.ucai.ttmusic.model.db.DBManager;
import cn.ucai.ttmusic.model.db.Music;
import cn.ucai.ttmusic.model.utils.BackgroundUtil;
import cn.ucai.ttmusic.model.utils.DialogBuilder;
import cn.ucai.ttmusic.model.utils.TimeUtil;
import cn.ucai.ttmusic.model.utils.ToastUtil;
import cn.ucai.ttmusic.view.MusicListPopWin;

public class PlayActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener, MusicListPopWin.PopWinListener {

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
    @BindView(R.id.play_middle)
    ViewPager playViewPager;

    Music currentMusic;
    int[] modeIcons = new int[]{R.drawable.mode_normal, R.drawable.mode_single, R.drawable.mode_shuffle};
    MediaPlayer mediaPlayer; // 音乐播放对象
    DiscoFragment discoFragment;
    LrcFrament lrcFrament;
    MusicListPopWin win;

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
        initView();
    }

    private void initView() {
        initPlayViewPager();
        playSeekbar.setOnSeekBarChangeListener(this);
        mediaPlayer = musicService.getMediaPlayer();
        // 设置播放完毕监听
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                // 如果播放完毕就直接下一曲
                musicService.nextMusic();
                handler.sendEmptyMessage(I.Handler.NEXT_MUSIC);
                if (win.isShowing()) {
                    win.updateList();
                }
            }
        });
        handler.sendEmptyMessage(I.Handler.INIT_VIEW);
    }

    private void initPlayViewPager() {
        discoFragment = new DiscoFragment();
        lrcFrament = new LrcFrament();
        PlayPagerAdapter adapter = new PlayPagerAdapter(getSupportFragmentManager(), discoFragment, lrcFrament);
        playViewPager.setAdapter(adapter);
        playViewPager.setOffscreenPageLimit(2);
        playViewPager.setCurrentItem(0);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case I.Handler.INIT_VIEW:
                    setMusicInfo();
                    setPlayButton();
                    setPlayMode();
                    handler.post(startDisco);
                    handler.post(setTimeAndProgress);
                    break;
                case I.Handler.PLAY_MUSIC:
                    setPlayButton();
                    discoFragment.reStartRotate();
                    break;
                case I.Handler.PAUSE_MUSIC:
                    setPlayButton();
                    discoFragment.pauseRotate();
                    break;
                case I.Handler.FRONT_MUSIC:
                case I.Handler.NEXT_MUSIC:
                    setMusicInfo();
                    setPlayButton();
                    discoFragment.startRotate(musicService.getCurrentMusic(), true);
                    lrcFrament.showLrc(currentMusic);
                    break;
                case I.Handler.SET_MODE:
                    setPlayMode();
                    break;
            }
        }
    };
    Runnable startDisco = new Runnable() {
        @Override
        public void run() {
            discoFragment.startRotate(musicService.getCurrentMusic(), musicService.isPlay());
            lrcFrament.showLrc(currentMusic);
        }
    };

    Runnable setTimeAndProgress = new Runnable() {
        @Override
        public void run() {
            playStartTime.setText(TimeUtil.toTime(musicService.getCurrentTime()));
            playSeekbar.setProgress(musicService.getCurrentTime());
            handler.postDelayed(this, 500);
        }
    };

    //////////////////////////////////与界面相关的几个方法//////////////////////////////////
    //设置主界面信息
    private void setMusicInfo() {
        currentMusic = musicService.getCurrentMusic();
        //设置歌曲信息
        playMusicName.setText(currentMusic.getTitle());
        playSingerName.setText(currentMusic.getSinger());
        //设置进度条和时间
        playSeekbar.setMax(musicService.getDuration());
        playEndTime.setText(TimeUtil.toTime((int) currentMusic.getTime()));
        //设置模糊化背景
        Drawable bg = BackgroundUtil.getDrawableByMusic(PlayActivity.this, currentMusic);
        blurView.setImageDrawable(bg);
    }

    //设置播放按钮
    private void setPlayButton() {
        if (musicService.isPlay()) {
            playBtn.setImageResource(R.drawable.pause_button);
        } else {
            playBtn.setImageResource(R.drawable.play_button);
        }
    }

    //设置模式按钮
    private void setPlayMode() {
        playPlayModeBtn.setImageResource(modeIcons[musicService.getPlayMode()]);
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    //返回、分享、列表
    @OnClick({R.id.icon_back, R.id.icon_share, R.id.play_music_list})
    public void action(View view) {
        switch (view.getId()) {
            case R.id.icon_back:
                onBackPressed();
                break;
            case R.id.icon_share:
                ToastUtil.show(mContext, "分享(开发中)");
                break;
            case R.id.play_music_list:
                win = new MusicListPopWin(this);
                win.showAtLocation(findViewById(R.id.play_main_view), Gravity.CENTER, 0, 0);
                win.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        backgroundAlpha(1f);
                    }
                });
                win.setListener(this);
                backgroundAlpha(0.5f);
                break;
        }
    }

    //改变弹出框弹出和消失时的背景透明度
    private void backgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = bgAlpha; //0.0-1.0
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    @Override
    public void onItemClick() {
        handler.sendEmptyMessage(I.Handler.INIT_VIEW);
    }

    @Override
    public void onCurrentDelete(int position) {
        musicService = TTApplication.getInstance().getMusicService();
        List<Music> list = musicService.getMusicList();
        if (position > list.size() - 1) {
            position = 0;
        }
        musicService.playMusic(position);
        win.updateList();
        handler.sendEmptyMessage(I.Handler.INIT_VIEW);
    }

    @Override
    public void onCollectAll() {
        List<Music> list = musicService.getMusicList();
        for (Music music : list) {
            DBManager.collectMusic(music);
        }
        ToastUtil.show(mContext, "收藏完毕");
        Intent update = new Intent(I.BroadCast.UPDATE_LIST);
        LocalBroadcastManager.getInstance(TTApplication.getContext()).sendBroadcast(update);
        discoFragment.initCollect(musicService.getCurrentMusic());
    }

    @Override
    public void onClearAll() {
        DialogBuilder builder = new DialogBuilder(this);
        builder.setTitle("提示")
                .setMessage("确定要清空播放列表？")
                .setPositiveButton("清空", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //停止音乐
                        musicService.stop();
                        //重启界面
                        PlayActivity.this.finish();
                        Intent intent = new Intent(PlayActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        //关闭通知
                        NotificationManager manger = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
                        manger.cancelAll();
                    }
                })
                .setNegativeButton("取消", null)
                .create().show();
    }

    //播放、上一曲、下一曲
    @OnClick({R.id.play_pre_btn, R.id.play_btn, R.id.play_next_btn})
    public void playControl(View view) {
        switch (view.getId()) {
            case R.id.play_pre_btn:
                musicService.frontMusic();
                handler.sendEmptyMessage(I.Handler.FRONT_MUSIC);
                break;
            case R.id.play_btn:
                if (musicService.isPlay()) {
                    musicService.setCurrentTime(musicService.getCurrentTime());
                    musicService.pause();
                    handler.sendEmptyMessage(I.Handler.PAUSE_MUSIC);
                } else {
                    musicService.start();
                    handler.sendEmptyMessage(I.Handler.PLAY_MUSIC);
                }
                break;
            case R.id.play_next_btn:
                musicService.nextMusic();
                handler.sendEmptyMessage(I.Handler.NEXT_MUSIC);
                break;
        }
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
        handler.sendEmptyMessage(I.Handler.SET_MODE);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        playStartTime.setText(TimeUtil.toTime(seekBar.getProgress()));
        musicService.moveToProgress(seekBar.getProgress());
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(startDisco);
        handler.removeCallbacks(setTimeAndProgress);
        super.onDestroy();
    }
}
