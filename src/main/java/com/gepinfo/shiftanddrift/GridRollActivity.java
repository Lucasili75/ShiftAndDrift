package com.gepinfo.shiftanddrift;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GridRollActivity extends AppCompatActivity {

    private ListView playerRollsList;
    private ImageButton buttonToGameActivity;
    private TextView textDice;
    private ImageButton buttonGameLobby;
    private List<PlayerClass> rolls = new ArrayList<>();
    private PlayerRollAdapter adapter;
    private String gameCode, uid;
    private String trackName;
    ImageView diceImage;
    private TextView textGameName;
    private TextView textPlayerName;
    private TextView textTrackName;
    private boolean isHost = false;
    Handler handler;
    DbManager dbManager;
    ValueEventListener eventListener;
    GameClass thisGame = new GameClass();
    PlayerClass nextToRoll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid_roll);

        diceImage = findViewById(R.id.diceImage);
        textDice = findViewById(R.id.diceText);
        playerRollsList = findViewById(R.id.playerRollsList);
        textGameName = findViewById(R.id.textGameName);
        textPlayerName = findViewById(R.id.textPlayerName);
        textTrackName = findViewById(R.id.textGameTrack);
        buttonToGameActivity = findViewById(R.id.buttonToGameActivity);
        buttonGameLobby = findViewById(R.id.buttonToGamesLobbyActivity);

        // Parametri passati da GameActivity
        trackName = getIntent().getStringExtra("trackName");
        gameCode = getIntent().getStringExtra("gameCode");
        uid = getIntent().getStringExtra("uid");
        dbManager = new DbManager(this, gameCode);
        MainActivity.gamePrefs = getSharedPreferences("ShiftAndDriftPrefs", MODE_PRIVATE);
        MainActivity.loadParms();
        textPlayerName.setText(getString(R.string.giocatore) + MainActivity.playerName);
        isHost = uid.equals(MyApplication.getUid());
        handler = new Handler(getMainLooper());

        eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rolls.clear();
                thisGame = snapshot.getValue(GameClass.class);
                if (thisGame != null) {
                    String name = thisGame.getName();
                    if (name != null) {
                        textGameName.setText(getString(R.string.gara) + name);
                    }

                    // Controlla se sei l’host
                    isHost = thisGame.getHost().equals(MyApplication.getUid());
                    rolls.addAll(thisGame.getPlayersSortedByPosition());
                    String status = thisGame.getStatus();
                    if ((!isHost) && (status.equals("waiting"))) {
                        MainActivity.navigateToGame(GridRollActivity.this, gameCode, MyApplication.getUid(), MainActivity.playerName, false, status);
                    }

                    trackName = thisGame.getTrack();
                    if (trackName != null) {
                        textTrackName.setText(trackName);
                    } else {
                        textTrackName.setText(getString(R.string.nessuna_pista_selezionata));
                    }
                    adapter = new PlayerRollAdapter(GridRollActivity.this, rolls);
                    playerRollsList.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    // Trova il prossimo giocatore che deve ancora tirare
                    nextToRoll = GameManager.nextTurn(rolls, -1);

                    if (nextToRoll != null) {
                        // Se il prossimo giocatore da tirare sono io
                        if (nextToRoll.uid.equals(uid)) {
                            diceImage.setEnabled(true);
                            textDice.setEnabled(true);
                            diceImage.setAlpha(1.0f); // Opaco
                        } else {
                            diceImage.setEnabled(false);
                            textDice.setEnabled(false);
                            diceImage.setAlpha(0.4f); // Semi-trasparente
                        }

                        // 👉 Se è un bot e tocca a lui, lo facciamo tirare
                        if (nextToRoll.isBot()) {
                            // Qualsiasi client può far tirare un bot se roll ancora assente
                            if (nextToRoll.roll < 0) {
                                nextToRoll.roll = DiceManager.tiraDado(99);
                                dbManager.updatePlayerWithTransaction(nextToRoll);
                            }
                        } else {
                            if (!nextToRoll.uid.equals(MyApplication.getUid()))
                                MainActivity.checkAndNotify(gameCode, "&fun=rollToGrid&toUid=" + nextToRoll.uid);
                        }
                    } else {
                        // Tutti hanno tirato
                        diceImage.setEnabled(false);
                        textDice.setEnabled(false);
                        diceImage.setAlpha(0.4f);
                    }
                    if (isHost && allPlayersRolled()) assignGridPositions();
                    buttonToGameActivity.setVisibility(isHost && !allPlayersRolled() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        dbManager.listenToGame(eventListener);

        diceImage.setOnClickListener(v -> {
            int diceValue = DiceManager.tiraDado(99);
            NumberPopup.showNumber(this, String.valueOf(diceValue));
            handler.postDelayed(() -> {
                thisGame.getPlayerFromUid(nextToRoll.uid).roll = diceValue;
                thisGame.updatePlayerMapByUid(nextToRoll);
                dbManager.updateGame(thisGame);
            }, 1000);
        });
        buttonToGameActivity.setOnClickListener(v -> {
            removeListener();
            thisGame.setStatus("waiting");
            dbManager.updateGame(thisGame).addOnSuccessListener(unused -> {
                MainActivity.navigateToGame(GridRollActivity.this, gameCode, MyApplication.getUid(), MainActivity.playerName, false, thisGame.getStatus());
            });
        });
        buttonGameLobby.setOnClickListener(v -> {
            MainActivity.toGameLobby(GridRollActivity.this);
        });
    }


    private boolean allPlayersRolled() {
        for (PlayerClass pr : rolls) {
            if (pr.roll < 0) return false;
        }
        return true;
    }

    private void assignGridPositions() {
        removeListener();
        handler.postDelayed(() -> {
            GameManager.loadTrackMap(thisGame.getTrack(), GridRollActivity.this);
            GameManager.assignGridPositions(thisGame);
            thisGame.setCurrentTurn(0).setStatus("started");
            thisGame.setCurrentPlayerUid(thisGame.getPlayersSortedByPosition().get(0).uid);
            dbManager.updateGame(thisGame).addOnSuccessListener(unused -> {
                MainActivity.checkAndNotify(thisGame.getCode(), "&senderUid=" + MyApplication.getUid());
                MainActivity.navigateToGame(GridRollActivity.this, gameCode, MyApplication.getUid(), MainActivity.playerName, false, thisGame.getStatus());
            });
        }, 1500);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
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
