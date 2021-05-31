package com.example.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String homeURL = "http://192.168.0.200/music/api/songs/";
    private final String roadURL = "http://muchserver.ddns.net/music/api/songs/";
    private String[] urls, spinnerURLs;
    private String url;

    private MusicServiceBinder musicServiceBinder = null;
    private MusicService musicService;

    TextView titleTextView, playListTextView;
    String playListText;
    Spinner artistSpinner, songSpinner, urlSpinner;
    RequestQueue requestQueue;
    ArrayList<Artist> artists;
    ArrayList<String> artistNames;
    ArrayList<Song> songs;
    ArrayList<String> titles;
    ArrayList<Song> playList;
    boolean safeToAddToPlaylist, checkedHomeURL, serviceActive;
    Button serviceToggleButton, fetchSongsButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeVariables();
        bindMusicService();
        bindUIWidgets();

        requestQueue = Volley.newRequestQueue(this);
        urls = new String[] {roadURL, homeURL};
        spinnerURLs = new String[] {roadURL.substring(7,17), homeURL.substring(7,20)};
        populateURLSpinner();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTitleTextView();
    }

    @Override
    protected void onDestroy() {
        musicServiceBinder.stopPlayer();
        unbindMusicService();
        stopService();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.fetchSongsButton:
                fetchSongs(url);
                break;

            case R.id.playButton:
                playMusic();
                break;

            case R.id.skipButton:
                skipToNext();
                break;

            case R.id.serviceToggleButton:
                toggleService();
                break;

            default:
                break;
        }
    }

    /**************************************************************************
     *
     * Service connection methods.
     *
     *************************************************************************/
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicServiceBinder = (MusicServiceBinder) service;

            Toast.makeText(getApplicationContext(), "Successfully bound to service.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void bindMusicService() {
        if(musicServiceBinder == null) {
            Intent intent = new Intent(this, MusicService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindMusicService() {
        if(musicServiceBinder != null) {
            unbindService(serviceConnection);
        }
    }
    /**************************************************************************
     *
     *  Volley.
     *
     *************************************************************************/
    // Home URL is tried first, set to check road URL second.
    private void fetchSongs(String url) {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for(int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);

                                parseJSON(jsonObject);

                                String title ="";
                                // Make sure the object has the necessary song components.
                                if(jsonObject.has("song") && jsonObject.has("title")) {

                                    /* TODO Adjust your song POJO to have an artist field. */
                                    // Append the artist name to the title if it's in the object.
                                    if(jsonObject.has("artist")) {

                                        // Artist is an inner JSON object in your backend design.
                                        JSONObject artist = jsonObject.getJSONObject("artist");
                                        if(artist.has("name")){
                                            title = jsonObject.getString("title") + " - " + artist.getString("name");
                                        }

                                        else {
                                            title = jsonObject.getString("title");
                                        }
                                    }
                                    else {
                                        title = jsonObject.getString("title");
                                    }

                                    Song song = new Song(jsonObject.getString("song"), title);
                                    songs.add(song);
                                    titles.add(song.getTitle());
                                }
                            }
                            //initializeSongSpinner();
                            populateArtistSpinner();
                        }
                        catch (JSONException e){
                            e.printStackTrace();
                        }
                        requestQueue.stop();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Report the error and try the other URL if it hasn't already.
                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
//                        if(!checkedHomeURL) {
//                            checkedHomeURL = true;
//                            fetchSongs(roadURL);
//                        }
                    }
                });
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(10000, 2, 2.0f));
        requestQueue.add(jsonArrayRequest);
    }

    private void parseJSON(JSONObject jsonObject) {
        if(jsonObject.has("artist")) {
            JSONObject artistName = null;
            try {
                artistName = jsonObject.getJSONObject("artist");
                String artistNameString;
                if(artistName.has("name")) {
                    artistNameString = artistName.getString("name");

                    if(newArtist(artistNameString)) {
                        Artist artist = new Artist(artistNameString);
                        artists.add(artist);
                    }

                    if (jsonObject.has("song") && jsonObject.has("title")) {
                        Song song = new Song(jsonObject.getString("song"), jsonObject.getString("title"));
                        addSong(artistNameString, song);
                    }

                }
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            }

        }
    }

    /**************************************************************************
     *
     * Helper methods.
     *
     *************************************************************************/
    private void bindUIWidgets() {

        /* TODO Remove this set after replacing the TextView. */
        playListText ="";
        playListTextView = findViewById(R.id.playListTextView);
        playListTextView.setText(playListText);
        playListTextView.setMovementMethod(new ScrollingMovementMethod());

        titleTextView = findViewById(R.id.titleTextView);

        Button playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(this);

        Button skipButton = findViewById(R.id.skipButton);
        skipButton.setOnClickListener(this);

        serviceToggleButton = findViewById(R.id.serviceToggleButton);
        serviceToggleButton.setOnClickListener(this);

        fetchSongsButton = findViewById(R.id.fetchSongsButton);
        fetchSongsButton.setOnClickListener(this);

        artistSpinner = findViewById(R.id.artistSpinner);
        songSpinner = findViewById(R.id.songSpinner);
        urlSpinner = findViewById(R.id.urlSpinner);
    }

    private void initializeVariables() {
        musicService = new MusicService();

        artists = new ArrayList<>();
        artistNames = new ArrayList<>();

        songs = new ArrayList<>();
        titles = new ArrayList<>();
        playList = new ArrayList<>();
        safeToAddToPlaylist = false;
        checkedHomeURL = false;
        serviceActive = false;
    }

    // Populate the spinner with the JSON data from the Volley request.
    private void initializeSongSpinner() {
        songSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, titles));
        songSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get rid of the annoying behavior of adding the first song every time.
                if(safeToAddToPlaylist) {
                    Song song = songs.get(position);
                    playList.add(song);

                    /* TODO Add some sort of layout list with a button to remove each song. */
                    playListText = playListText + song.getTitle() + "\n";
                    playListTextView.setText(playListText);

                    Toast.makeText(getApplicationContext(), "Added " + song.getTitle() + " to playlist.", Toast.LENGTH_SHORT).show();
                }
                // Once the spinner is populated it's safe to allow playlist additions.
                else {
                    safeToAddToPlaylist = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private boolean newArtist(String name) {
        for(int i = 0; i < artists.size(); i++) {
            if(artists.get(i).getName().equals(name)) {
                return false;
            }
        }
        return true;
    }

    private void addSong(String artistName, Song song) {
        for(int i = 0; i < artists.size(); i++) {
            if(artists.get(i).getName().equals(artistName)) {
                artists.get(i).addSong(song);
                break;
            }
        }
    }

    private void sortArtists() {
        Collections.sort(artists, new Comparator<Artist>() {
            @Override
            public int compare(Artist first, Artist second) {
                return first.getName().compareTo(second.getName());
            }
        });
    }

    private void populateURLSpinner() {
        urlSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerURLs));
        urlSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                url = urls[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void populateArtistSpinner() {

        sortArtists();

        for(int i = 0; i < artists.size(); i++) {
            artistNames.add(artists.get(i).getName());
        }

        artistSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, artistNames));
        artistSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                populateSongSpinner(artists.get(position));
                //Toast.makeText(this, artists.get(position).getName() + " " + artists.get(position).getSongs().size(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void populateSongSpinner(Artist artist) {
        songs = new ArrayList<>();
        songs = artist.getSongs();
        titles = new ArrayList<>();

        for(int i = 0; i < songs.size(); i++) {
            titles.add(songs.get(i).getTitle());
        }

        safeToAddToPlaylist = false;

        songSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, titles));
        songSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(safeToAddToPlaylist) {
                    playList.add(songs.get(position));
                    playListText = playListText + songs.get(position).getTitle() + "\n";
                    playListTextView.setText(playListText);
                }
                else {
                    safeToAddToPlaylist = true;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    // Send the current play list to the service.
    private void playMusic() {
        if(playList.size() > 0) {
            musicServiceBinder.startPlayer(getApplicationContext(), playList);
            updateTitleTextView();
        }
        else {
            Toast.makeText(getApplicationContext(), "Tried to play empty playlist.", Toast.LENGTH_SHORT).show();
        }
    }

    // Because the media player is running in the background, the UI and media player might
    // have different play lists. Remove UI tracks to match the background play list.
    private void skipToNext() {
        if(playList.size() > 0) {
            Song song = musicServiceBinder.getNowPlaying();
            boolean foundSong = false;
            do {
                if(playList.size() > 0) {
                    // Set the loop to end here.
                    if(playList.get(0).equals(song)) {
                        foundSong = true;
                    }
                    // But, always remove the song.
                    playList.remove(0);
                }
                else {
                    // Terminate the loop if the playlist is 0 size.
                    foundSong = true;
                }
            }
            while(!foundSong);

            // Play the shortened playlist.
            playMusic();
        }
        else {
            Toast.makeText(getApplicationContext(), "Tried to skip empty playlist.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTitleTextView() {
        if(musicServiceBinder != null) {
            Song song = musicServiceBinder.getNowPlaying();
            if(song != null) {
                titleTextView.setText(song.getTitle());
            }
        }
    }

    private void toggleService() {

        if(serviceActive) {
            stopService();
        }
        else {
            Intent intent;
            intent = new Intent(this, MusicService.class);
            intent.setAction(MusicService.START_MUSIC_SERVICE);
            startService(intent);
            serviceActive = true;
            serviceToggleButton.setText("Stop Service");
        }
    }

    private void stopService() {
        Intent intent;
        intent = new Intent(this, MusicService.class);
        intent.setAction(MusicService.STOP_MUSIC_SERVICE);
        startService(intent);
        serviceActive = false;
        serviceToggleButton.setText("Start Service");
    }

}