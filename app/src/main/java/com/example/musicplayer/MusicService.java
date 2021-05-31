package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class MusicService extends Service {
    static final String START_MUSIC_SERVICE = "com.example.musicplayer.startservice";
    static final String STOP_MUSIC_SERVICE = "com.example.musicplayer.stopservice";

    private MusicServiceBinder musicServiceBinder = new MusicServiceBinder();

    public MusicService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicServiceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent.getAction() != null) {

            switch(intent.getAction()) {
                case START_MUSIC_SERVICE:
                    startCustomForeground();
                    break;

                case STOP_MUSIC_SERVICE:
                    stopForeground(true);
                    break;

                default:
                    break;
            }
            return START_STICKY;
        }

        else {
            return START_NOT_STICKY;
        }
    }

    private void startCustomForeground() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "com.example.music";
            String channelName = "Music service";
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if(notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
                builder.setSmallIcon(R.mipmap.ic_launcher_round);
                builder.setColor(Color.rgb(11, 22, 42));
                builder.setContentTitle("Music service running in the background.");
                //builder.setContentText("Text");

                Notification notification = builder.build();
                startForeground(1, notification);
            }
        }

        else {
            startForeground(1, new Notification());
        }
    }
}
