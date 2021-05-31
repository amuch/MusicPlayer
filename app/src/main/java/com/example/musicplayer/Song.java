package com.example.musicplayer;

public class Song {

    private String url;
    private String title;

    public Song(String url, String title) {
        this.url = url;
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }
}
