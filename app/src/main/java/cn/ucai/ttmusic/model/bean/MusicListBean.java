package cn.ucai.ttmusic.model.bean;

import java.util.List;

import cn.ucai.ttmusic.model.db.Music;

public class MusicListBean {


    String list_coverUrl;
    String list_title;
    List<Music> musicList;

    public String getList_coverUrl() {
        return list_coverUrl;
    }

    public void setList_coverUrl(String list_coverUrl) {
        this.list_coverUrl = list_coverUrl;
    }

    public String getList_title() {
        return list_title;
    }

    public void setList_title(String list_title) {
        this.list_title = list_title;
    }

    public List<Music> getMusicList() {
        return musicList;
    }

    public void setMusicList(List<Music> musicList) {
        this.musicList = musicList;
    }

    @Override
    public String toString() {
        return "MusicListBean{" +
                "list_coverUrl='" + list_coverUrl + '\'' +
                ", list_title='" + list_title + '\'' +
                ", musicList=" + musicList +
                '}';
    }
}
