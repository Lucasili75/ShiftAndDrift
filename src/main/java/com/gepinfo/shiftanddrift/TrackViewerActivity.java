package com.gepinfo.shiftanddrift;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class TrackViewerActivity extends AppCompatActivity {

    private PhotoView imageTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_viewer);

        imageTrack = findViewById(R.id.trackImageView);

        String selectedFile = getIntent().getStringExtra("selectedFileName");
        if (selectedFile == null) return;

        File jsonFile = new File(MainActivity.getLocalPath(this), selectedFile + ".json");

        try (FileInputStream fis = new FileInputStream(jsonFile);
             InputStreamReader reader = new InputStreamReader(fis)) {

            Gson gson = new Gson();
            TrackMap trackMap = gson.fromJson(reader, TrackMap.class);

            File imageFile = new File(jsonFile.getParent(), trackMap.getImagePath());
            if (imageFile.exists()) {
                Glide.with(this)
                        .load(imageFile)
                        .into(imageTrack);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
