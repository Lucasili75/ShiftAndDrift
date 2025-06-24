package com.gepinfo.shiftanddrift;

import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackSelectorActivity extends AppCompatActivity {

    ListView listViewFiles;
    List<String> fileNamesWithoutExtension;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_selector);

        listViewFiles = findViewById(R.id.listViewFiles);
        TextView textNoTracks=findViewById(R.id.textNoTracks);
        boolean tracksFound=false;

        // Cartella da cui leggere i file JSON (esempio: files in internal storage)
        File directory = new File(MainActivity.getLocalPath(this));  // oppure altra cartella se vuoi

        // Lista per i nomi senza estensione
        fileNamesWithoutExtension = new ArrayList<>();

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        String nameWithoutExt = file.getName().substring(0, file.getName().length() - 5);
                        fileNamesWithoutExtension.add(nameWithoutExt);
                        tracksFound=true;
                    }
                }
            }
        }
        if(!tracksFound) textNoTracks.setVisibility(VISIBLE);

        // Adapter per mostrare i nomi nella listview
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, fileNamesWithoutExtension);
        listViewFiles.setAdapter(adapter);

        // OnClick: ritorna il nome del file selezionato all'activity chiamante
        listViewFiles.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFile = fileNamesWithoutExtension.get(position);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedFileName", selectedFile);
            setResult(RESULT_OK, resultIntent);
            finish(); // chiude questa activity tornando indietro
        });
        listViewFiles.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedFile = fileNamesWithoutExtension.get(position);
            Intent intent = new Intent(this, TrackViewerActivity.class);
            intent.putExtra("selectedFileName", selectedFile);
            startActivity(intent);
            return true;
        });
    }
}
