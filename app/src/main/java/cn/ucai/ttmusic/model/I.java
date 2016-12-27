package cn.ucai.ttmusic.model;

public interface I {

    interface PlayState {
        int IS_INIT = 0;
        int IS_PLAY = 1;
        int IS_PAUSE = 2;
    }

    interface PlayMode {
        int MODE_NORMAL = 0;
        int MODE_SINGLE = 1;
        int MODE_SHUFFLE = 2;
    }

    interface Handler {
        int PLAY_MUSIC = 0X000;
    }

    interface BroadCast {
        String MUSIC_INIT = "play_init";
        String MUSIC_PLAY = "play_music";
        String MUSIC_FRONT = "play_front";
        String MUSIC_NEXT = "play_next";
        String MUSIC_PAUSE = "play_pause";
        String NOTIFY_CANCEL = "notify_cancel";

        String MUSIC_LIST = "musicList";
        String MUSIC_POSITION = "musicPosition";

        String UPDATE_LIST = "update_list";
    }

    interface Intent {
        String MUSIC_SERVICE = "music_service";

        String SEARCH_TYPE = "search_type";
        String SEARCH_DATA = "search_data";
    }

    interface Notification {
        int NOTIFY_ID = 999;
    }

    interface SearchType {
        int MUSIC_LOCAL = 111; //搜索本地音乐
        int MUSIC_LIST = 222;  //搜索歌单音乐
    }

}
