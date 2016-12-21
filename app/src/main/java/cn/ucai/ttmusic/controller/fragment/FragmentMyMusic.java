package cn.ucai.ttmusic.controller.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.ucai.ttmusic.R;
import cn.ucai.ttmusic.TTApplication;
import cn.ucai.ttmusic.bean.MusicListBean;
import cn.ucai.ttmusic.controller.activity.MainActivity;
import cn.ucai.ttmusic.controller.adapter.MusicListAdapter;
import cn.ucai.ttmusic.model.I;
import cn.ucai.ttmusic.model.music.MusicListModel;
import cn.ucai.ttmusic.model.utils.ToastUtil;

public class FragmentMyMusic extends BaseFragment {

    Context mContext;
    BroadcastReceiver mReceiver;

    @BindView(R.id.count_local_music)
    TextView countLocalMusic; //本地音乐数量
    @BindView(R.id.count_recents)
    TextView countRecents; //最近播放数量
    @BindView(R.id.count_downloads)
    TextView countDownloads; //下载数量
    @BindView(R.id.count_singers)
    TextView countSingers; //歌手数量
    @BindView(R.id.count_mvs)
    TextView countMvs; //MV数量
    @BindView(R.id.myMusicList)
    ListView myMusicList;

    MusicListModel musicListModel;
    List<MusicListBean> musicList;
    MusicListAdapter adapter;

    LocalBroadcastManager broadcastManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_my_music, container, false);
        ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void initUi() {
        mContext = getActivity();
        broadcastManager = LocalBroadcastManager.getInstance(TTApplication.getContext());

        countLocalMusic.setText("(" + TTApplication.getInstance().getMusicList().size() + ")");
        musicListModel = new MusicListModel();
        loadMusicList();
        initBroadCast();
    }

    private void loadMusicList() {
        musicList = musicListModel.getMusicList();
        adapter = new MusicListAdapter(mContext, musicList);
        myMusicList.setAdapter(adapter);
    }

    private void initBroadCast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(I.BroadCast.UPDATE_LIST);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case I.BroadCast.UPDATE_LIST:
                        loadMusicList();
                        break;
                }
            }
        };
        broadcastManager.registerReceiver(mReceiver, intentFilter);
    }


    @OnClick({R.id.list_localMusic, R.id.list_recents, R.id.list_downloads, R.id.list_my_singers, R.id.list_my_mv})
    public void onClick(View view) {
        MainActivity mainActivity = (MainActivity) getActivity();
        switch (view.getId()) {
            case R.id.list_localMusic:
                mainActivity.goToFragment(new FragmentLocalMusic());
                break;
            case R.id.list_recents:
                ToastUtil.show(mContext, "最近播放(开发中)");
                break;
            case R.id.list_downloads:
                ToastUtil.show(mContext, "下载管理(开发中)");
                break;
            case R.id.list_my_singers:
                ToastUtil.show(mContext, "我的歌手(开发中)");
                break;
            case R.id.list_my_mv:
                ToastUtil.show(mContext, "我的MV(开发中)");
                break;
        }
    }

    @Override
    public void onDestroy() {
        if (mReceiver != null) {
            broadcastManager.unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }
}
