package com.example.musicplayer;


import android.content.Context;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.PowerManager;

import java.io.IOException;
import java.util.List;

public class MusicServiceBinder extends Binder {

    private MediaPlayer mediaPlayer = null;
    private Song nowPlaying;

    /**************************************************************************
     *
     * MediaPlayer methods.
     *
     *************************************************************************/

    // Set up the player to asynchronously load a data source.
    // This should work to load from a url or internal storage.
    private void initializePlayer(String url) {

        if(mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();

            boolean foundSource = false;
            try {
                mediaPlayer.setDataSource(url);
                foundSource = true;
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if(foundSource) {
                mediaPlayer.prepareAsync();
            }
        }
    }

    // Make sure the player is released and null.
    private void destroyPlayer() {
        if(mediaPlayer != null) {
            if(mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // Start the player with a single data source.
    public void startPlayer(String url) {
        // Call destroy to assure the player initializes properly.
        destroyPlayer();
        initializePlayer(url);

        if(mediaPlayer != null) {
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
        }
    }

    // Start the player with a playlist.
    public void startPlayer(List<Song> songs) {

        // Make sure the list is not empty and play the track at index 0.
        if(songs.size() > 0) {
            startPlayer(songs.get(0).getUrl());
            setNowPlaying(songs.get(0));

            // Recursively add a shortened playlist on completion of the first track.
            if(songs.size() > 1) {
                final List<Song> subSongs = songs.subList(1, songs.size());
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        startPlayer(subSongs);
                    }
                });
            }
        }

    }

    // Start the player with a playlist.
    public void startPlayer(Context context, List<Song> songs) {

        // Make sure the list is not empty and play the track at index 0.
        if(songs.size() > 0) {
            startPlayer(songs.get(0).getUrl());
            setNowPlaying(songs.get(0));

            // Recursively add a shortened playlist on completion of the first track.
            if(songs.size() > 1) {
                final List<Song> subSongs = songs.subList(1, songs.size());
                mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        startPlayer(subSongs);
                    }
                });
            }
        }

    }


    public void pausePlayer() {
        if(mediaPlayer != null) {
            mediaPlayer.pause();
        }

    }

    public void stopPlayer() {
        if(mediaPlayer != null) {
            mediaPlayer.stop();
            destroyPlayer();
        }
    }

    /**************************************************************************
     *
     * Get and set methods.
     *
     *************************************************************************/

    public Song getNowPlaying() {
        return nowPlaying;
    }

    public void setNowPlaying(Song nowPlaying) {
        this.nowPlaying = nowPlaying;
    }
}
