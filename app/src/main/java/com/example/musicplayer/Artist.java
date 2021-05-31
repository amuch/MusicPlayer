package com.example.musicplayer;

import java.util.ArrayList;

public class Artist {

    private String name;
    private ArrayList<Song> songs;

    public Artist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
    }

    public Artist(String name, ArrayList<Song> songs) {
        this.name = name;
        this.songs = songs;
    }

    public void addSong(Song song) {
        this.songs.add(song);
    }

    public String getName() {
        return name;
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }
}
