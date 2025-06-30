package com.gepinfo.shiftanddrift;

import static com.gepinfo.shiftanddrift.PlayerClass.colorNames;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private TextView textGameName;
    private TextView textPlayerName;
    private TextView textGameStatus;
    private TextView textTrackName;
    private ListView listViewPlayers;
    private ImageButton buttonLeaveGame;
    private ImageButton buttonStartGame;
    private ImageButton buttonGameLobby;
    private Button buttonEndGame;
    private ImageButton buttonAddBot;
    private ImageButton buttonSelectTrack;
    private boolean isHost = false;

    private String gameCode, status;
    DbManager dbManager;
    private String trackName;
    GameClass thisGame = new GameClass();

    //    private List<PlayerClass> playerList = new ArrayList<>();
    private PlayerListAdapter playerAdapter;

    List<String> usedBotNames = new ArrayList<>();
    private ActivityResultLauncher<Intent> trackSelectorLauncher;
    ValueEventListener eventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        textGameName = findViewById(R.id.textGameName);
        textPlayerName = findViewById(R.id.textPlayerName);
        textGameStatus = findViewById(R.id.textGameStatus);
        textTrackName = findViewById(R.id.textGameTrack);
        listViewPlayers = findViewById(R.id.listViewPlayers);
        buttonLeaveGame = findViewById(R.id.buttonLeaveGame);
        buttonStartGame = findViewById(R.id.buttonStartGame);
        buttonEndGame = findViewById(R.id.buttonEndGame);
        buttonAddBot = findViewById(R.id.buttonAddBot);
        buttonGameLobby = findViewById(R.id.buttonToGamesLobbyActivity);
        buttonAddBot.setOnClickListener(v -> addBot());
        buttonSelectTrack = findViewById(R.id.buttonSelectTrack);
        buttonSelectTrack.setOnClickListener(v -> selectTrack());

        gameCode = getIntent().getStringExtra("gameCode");
        dbManager = new DbManager(this, gameCode);
        MainActivity.gamePrefs = getSharedPreferences("ShiftAndDriftPrefs", MODE_PRIVATE);
        MainActivity.loadParms();
        textPlayerName.setText(getString(R.string.giocatore) + MainActivity.playerName);

        eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usedBotNames.clear();
                //playerList.clear();
                thisGame = snapshot.getValue(GameClass.class);
                if (thisGame != null) {
                    String name = thisGame.getName();
                    if (name != null) {
                        textGameName.setText(getString(R.string.gara) + name);
                    }
                    // Controlla se sei l’host
                    isHost = thisGame.getHost().equals(MyApplication.getUid());
                    for (PlayerClass player : thisGame.getPlayers().values()) {
                        if (player.isBot()) {
                            usedBotNames.add(player.getName());
                        }
                    }
                    playerAdapter = new PlayerListAdapter(GameActivity.this, thisGame.getPlayersSortedByName());
                    listViewPlayers.setAdapter(playerAdapter);
                    playerAdapter.notifyDataSetChanged();

                    trackName = thisGame.getTrack();
                    if ((trackName != null) && (!trackName.isEmpty())) {
                        textTrackName.setText(trackName);
                        textTrackName.setTextColor(Color.BLACK);
                    } else {
                        textTrackName.setText(getString(R.string.nessuna_pista_selezionata));
                        textTrackName.setTextColor(Color.GRAY);
                    }

                    status = thisGame.getStatus();
                    buttonLeaveGame.setVisibility((status.equals("waiting") ? View.VISIBLE : View.GONE));
                    if (status != null) {
                        textGameStatus.setText(getString(R.string.stato) + MainActivity.getStatus(thisGame.getStatus()));
                        switch (status) {
                            case "waiting":
                                textGameStatus.setTextColor(ContextCompat.getColor(GameActivity.this, R.color.status_waiting));
                                buttonStartGame.setImageDrawable(getDrawable(R.drawable.starting_grid));
                                break;
                            case "started":
                                textGameStatus.setTextColor(ContextCompat.getColor(GameActivity.this, R.color.status_started));
                                buttonStartGame.setImageDrawable(getDrawable(R.drawable.race_icon));
                                break;
                            case "finished":
                                textGameStatus.setTextColor(ContextCompat.getColor(GameActivity.this, R.color.status_finished));
                                break;
                            case "rolling":
                                textGameStatus.setTextColor(ContextCompat.getColor(GameActivity.this, R.color.status_rolling));
                                if (!isHost) continueGame();
                                break;
                            default:
                                textGameStatus.setTextColor(Color.GRAY);
                        }
                    } else {
                        status = "unknown";
                        textGameStatus.setText(getString(R.string.stato) + getString(R.string.missing));
                        textGameStatus.setTextColor(Color.GRAY);
                    }
                    buttonStartGame.setVisibility(isHost && (trackName != null && !trackName.isEmpty()) ? View.VISIBLE : View.GONE);
                    buttonEndGame.setVisibility(isHost ? View.VISIBLE : View.GONE);
                    buttonAddBot.setVisibility(isHost && status.equals("waiting") ? View.VISIBLE : View.GONE);
                    buttonSelectTrack.setVisibility(isHost && status.equals("waiting") ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        dbManager.listenToGame(eventListener);

        buttonLeaveGame.setOnClickListener(v -> leaveGame());
        buttonStartGame.setOnClickListener(v -> {
            if ((trackName != null) && (!trackName.isEmpty())) continueGame();
        });

        buttonEndGame.setOnClickListener(v -> {
            endGame();
        });
        buttonGameLobby.setOnClickListener(v -> {
            MainActivity.toGameLobby(GameActivity.this);
        });
        listViewPlayers.setOnItemLongClickListener((parent, view, position, id) -> {
            PlayerClass selected = thisGame.getPlayersSortedByName().get(position);
            if (!selected.isBot()) return true;

            new AlertDialog.Builder(this)
                    .setTitle("Elimina bot")
                    .setMessage("Vuoi eliminare " + selected.name + "?")
                    .setPositiveButton("Sì", (dialog, which) -> {
                        thisGame.getPlayers().remove(selected.uid);
                        dbManager.updateGame(thisGame);
                    })
                    .setNegativeButton("No", null)
                    .show();

            return true;
        });

        // Registra il launcher per ricevere il risultato
        trackSelectorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            trackName = data.getStringExtra("selectedFileName");
                            thisGame.setTrack(trackName);
                            dbManager.updateGame(thisGame);
                        }
                    }
                }
        );

    }

    private void leaveGame() {
        thisGame.getPlayers().remove(MyApplication.getUid());
        dbManager.updateGame(thisGame).addOnSuccessListener(unused -> {
            MainActivity.checkAndNotify(thisGame.getCode(), "&fun=deletePlayer&player=" + MainActivity.playerName + "&senderUid=" + MyApplication.getUid());
            Toast.makeText(this, "Hai lasciato la gara", Toast.LENGTH_SHORT).show();
            MainActivity.toGameLobby(this);
        });
    }

    private void continueGame() {
        if (status.equals("waiting")) {
            removeListener();
            GameManager.prepareGridRoll(thisGame);
            dbManager.updateGame(thisGame).addOnSuccessListener(unused -> {
                MainActivity.navigateToGame(this, gameCode, MyApplication.getUid(), MainActivity.playerName, false, "rolling");
            });
        }
    }

    private void endGame() {
        /*gameManager.getGamesRef().child("status").setValue("finished").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Gara Terminata!", Toast.LENGTH_SHORT).show();
                // Puoi aggiungere qui logica per countdown o navigazione
            } else {
                Toast.makeText(this, "Errore avvio gara", Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    private void addBot() {
        String botId = "bot_" + System.currentTimeMillis();
        String botName = getUniqueBotName();
        Random rand = new Random();

        PlayerClass bot = new PlayerClass(
                botId, botName, 1, 0,
                colorNames[rand.nextInt(colorNames.length)],
                colorNames[rand.nextInt(colorNames.length)],
                colorNames[rand.nextInt(colorNames.length)],
                "bot",
                rand.nextInt(11), // aggressività random
                rand.nextInt(11),   // rischio random
                0, -1, 0, -1, -1, 0, 0, 0, 0, 0, 0,""
        );
        thisGame.getPlayers().put(botId, bot);
        dbManager.updateGame(thisGame);
    }

    private String getUniqueBotName() {
        Random rand = new Random();
        boolean alreadyUsed = true;
        String candidate = "";
        while (alreadyUsed) {
            candidate = PlayerClass.botNames[rand.nextInt(PlayerClass.botNames.length)];
            if (!usedBotNames.contains(candidate)) alreadyUsed = false;
        }
        return candidate; // fallback
    }

    private void selectTrack() {
        Intent intent = new Intent(this, TrackSelectorActivity.class);
        trackSelectorLauncher.launch(intent);
    }

    @Override
    public void onBackPressed() {
        MainActivity.toGameLobby(this);
    }

    private void removeListener() {
        if (eventListener != null) {
            dbManager.removeListener(eventListener);
            eventListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        removeListener();
        super.onDestroy();
    }
}