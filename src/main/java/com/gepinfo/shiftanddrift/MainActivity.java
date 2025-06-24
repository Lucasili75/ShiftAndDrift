package com.gepinfo.shiftanddrift;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView textPlayerName;
    public static SharedPreferences gamePrefs;
    public static String playerName;
    public static String carColorRear;
    public static String carColorBody;
    public static String carColorFront;
    private DatabaseReference gamesRef;


    private ActivityResultLauncher<String> requestReadPermissionLauncher;
    private ActivityResultLauncher<Intent> manageAllFilesPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gamePrefs = getSharedPreferences("ShiftAndDriftPrefs", MODE_PRIVATE);
        gamesRef = MyApplication.getGamesRef();

        Button buttonPlay = findViewById(R.id.buttonPlay);
        Button buttonChangeName = findViewById(R.id.buttonChangeName);
        textPlayerName = findViewById(R.id.textPlayerName);

        buttonPlay.setOnClickListener(v -> {
            toGameLobby(MainActivity.this);
        });

        buttonChangeName.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PlayerNameActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // Launcher per permesso READ_EXTERNAL_STORAGE
        requestReadPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "Permesso di lettura negato", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        // Launcher per permesso speciale MANAGE_EXTERNAL_STORAGE
        manageAllFilesPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (!Environment.isExternalStorageManager()) {
                            Toast.makeText(this, "Permesso MANAGE_EXTERNAL_STORAGE negato", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });

        checkAndRequestPermissions();
        checkNotificationPermission();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                Log.d("FCM_TOKEN", "Token: " + token);
                gamesRef.child("tokens").child(MyApplication.getUid()).setValue(token);
            }
        });

    }

    public static String getStatus(String status) {
        switch (status) {
            case "rolling":
                return "Preparazione";
            case "waiting":
                return "In attesa";
            case "started":
                return "In corso";
            case "finished":
                return "Terminata";
            default:
                return "Sconosciuto";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadParms();
        textPlayerName.setText(getString(R.string.giocatore) + playerName);
    }

    public static void loadParms() {
        playerName = gamePrefs.getString("playerName", "Player");
        carColorRear = gamePrefs.getString("carColorRear", "Bianco");
        carColorBody = gamePrefs.getString("carColorBody", "Bianco");
        carColorFront = gamePrefs.getString("carColorFront", "Bianco");
    }

    public static String getLocalPath(Context context) {
        //if there is no SD card, create new directory objects to make directory on device
        File directory = null;

        if (Environment.getExternalStorageState() == null) {
            //create new file directory object
            directory = new File(Environment.getDataDirectory()
                    + "/ShiftAndDrift/");
            // if no directory exists, create new directory
            if (!directory.exists()) {
                if (!directory.mkdir()) {
                    Toast.makeText(context, "Errore creazione cartella", Toast.LENGTH_SHORT).show();
                }
            }

            // if phone DOES have sd card
        } else if (Environment.getExternalStorageState() != null) {
            // search for directory on SD card
            directory = new File(Environment.getExternalStorageDirectory()
                    + "/ShiftAndDrift/");
            // if no directory exists, create new directory to store files
            if (!directory.exists()) {
                if (!directory.mkdir()) {
                    Toast.makeText(context, "Errore creazione cartella", Toast.LENGTH_SHORT).show();
                }
            }
        }// end of SD card checking
        return (directory.toString() + "/");
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            if (!Environment.isExternalStorageManager()) {
                // Chiedi permesso speciale tramite ActivityResultLauncher
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                manageAllFilesPermissionLauncher.launch(intent);
            }
        } else {
            // Android 6 - 10: chiedi READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestReadPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permesso notifiche concesso!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permesso notifiche negato", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void checkAndNotify(String gameCode, String parms) {
        new Thread(() -> {
            try {
                Log.e("FCM_SERVER", "Invio Notifica: " + gameCode + "parms:"+parms);
                URL url = new URL("https://shiftanddrift.onrender.com/check-and-notify?senderUid="+ MyApplication.getUid() +"&gameCode=" + gameCode + parms);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                Log.d("FCM_SERVER", "Risposta: " + responseCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void toGameLobby(Context context){
        Intent intent = new Intent(context, GameLobbyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
}
