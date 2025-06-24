package com.gepinfo.shiftanddrift;

import android.content.Context;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.util.List;

public class TrackMap {
    private String imagePath;
    private List<TrackCell> cells;

    public TrackMap() {
        // Costruttore vuoto per deserializzazione
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public List<TrackCell> getCells() {
        return cells;
    }

    public void setCells(List<TrackCell> cells) {
        this.cells = cells;
    }

    public static TrackMap loadTrackFromJson(String fileName, Context context) {
        try {
            File file = new File(MainActivity.getLocalPath(context), fileName + ".json");
            FileReader reader = new FileReader(file);
            Gson gson = new Gson();
            return gson.fromJson(reader, TrackMap.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
