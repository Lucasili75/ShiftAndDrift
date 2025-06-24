package com.gepinfo.shiftanddrift;

import static com.gepinfo.shiftanddrift.PlayerClass.colorHexes;
import static com.gepinfo.shiftanddrift.PlayerClass.colorNames;

import android.graphics.drawable.PictureDrawable;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.caverock.androidsvg.SVG;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class PlayerNameActivity extends AppCompatActivity {

    private EditText editTextName;
    private Button buttonSave;

    private ImageView carImage;
    private String originalSvgContent;
    private final HashMap<String, String> currentColors = new HashMap<>();
    Spinner spinnerFront;
    Spinner spinnerRear;
    Spinner spinnerBody;
    Map<String, Integer> colorNameToIndex = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_name);

        for (int i = 0; i < colorNames.length; i++) {
            colorNameToIndex.put(colorNames[i], i);
        }

        editTextName = findViewById(R.id.editTextName);
        buttonSave = findViewById(R.id.buttonSave);

        // Carica nome attuale
        editTextName.setText(MainActivity.playerName);

        buttonSave.setOnClickListener(v -> {
            String newName = editTextName.getText().toString().trim();
            if (!newName.isEmpty()) {
                MainActivity.gamePrefs.edit().putString("playerName", newName).putString("carColorFront", spinnerFront.getSelectedItem().toString()).putString("carColorRear", spinnerRear.getSelectedItem().toString()).putString("carColorBody", spinnerBody.getSelectedItem().toString()).apply();
                Toast.makeText(this, "Dati salvati!", Toast.LENGTH_SHORT).show();
                finish(); // torna indietro al menu
            } else {
                Toast.makeText(this, "Inserisci un nome valido", Toast.LENGTH_SHORT).show();
            }
        });

        carImage = findViewById(R.id.carImage);

        originalSvgContent = readSvgFromRaw(R.raw.race_car_androidsvg);

        // Default colors
        currentColors.put("rear_wing", "#000000");
        currentColors.put("front_wing_top", "#000000");
        currentColors.put("front_wing_bottom", "#000000");
        currentColors.put("main_body", "#000000");

        spinnerBody=findViewById(R.id.spinnerBody);
        spinnerRear=findViewById(R.id.spinnerRearWing);
        spinnerFront=findViewById(R.id.spinnerFrontWing);

        setupSpinner(R.id.spinnerRearWing, new String[]{"rear_wing"});
        setupSpinner(R.id.spinnerFrontWing, new String[]{"front_wing_top", "front_wing_bottom"});
        setupSpinner(R.id.spinnerBody, new String[]{"main_body"});
        spinnerBody.setSelection(colorNameToIndex.get(MainActivity.carColorBody));
        spinnerRear.setSelection(colorNameToIndex.get(MainActivity.carColorRear));
        spinnerFront.setSelection(colorNameToIndex.get(MainActivity.carColorFront));
    }

    private void setupSpinner(int spinnerId, String[] partIds) {
        Spinner spinner = findViewById(spinnerId);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                for (String part : partIds) {
                    currentColors.put(part, colorHexes[position]);
                }
                renderCar();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void renderCar() {
        try {
            String svg = originalSvgContent;
            for (String id : currentColors.keySet()) {
                svg = svg.replaceAll(
                        "(<path[^>]*id=\"" + id + "\"[^>]*fill=\")#[A-Fa-f0-9]{6}(\")",
                        "$1" + currentColors.get(id) + "$2"
                );
            }
            SVG parsedSvg = SVG.getFromString(svg);
            PictureDrawable drawable = new PictureDrawable(parsedSvg.renderToPicture());
            carImage.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);
            carImage.setImageDrawable(drawable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readSvgFromRaw(int rawId) {
        try (InputStream is = getResources().openRawResource(rawId);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
