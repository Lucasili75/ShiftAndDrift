package com.gepinfo.shiftanddrift;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GameLobbyActivity extends AppCompatActivity {

    private ArrayAdapter<GameClass> gameListAdapter;
    private List<GameClass> games = new ArrayList<>();

    private String gameName;
    GamesManager gamesManager;
    ValueEventListener eventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gamesManager = new GamesManager(this, "");
        setContentView(R.layout.activity_lobby);

        TextView textPlayerName = findViewById(R.id.textPlayerName);
        Button buttonCreateGame = findViewById(R.id.buttonCreateGame);
        ListView listViewGames = findViewById(R.id.listViewGames);

        textPlayerName.setText(getString(R.string.giocatore) + MainActivity.playerName);

        gameListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, games);
        listViewGames.setAdapter(gameListAdapter);
        eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                games.clear();
                for (DataSnapshot gameSnap : snapshot.getChildren()) {
                    GameClass game = gameSnap.getValue(GameClass.class);
                    if (game != null) {
                        games.add(game);
                    }
                }
                gameListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GameLobbyActivity.this, "Errore caricamento partite", Toast.LENGTH_SHORT).show();
            }
        };

        gamesManager.listenToGame(eventListener);
        //gamesManager.setMyPlayer(null);

        buttonCreateGame.setOnClickListener(v -> {
            requestGameName();
        });

        listViewGames.setOnItemClickListener((parent, view, position, id) -> {
            GameClass selectedGame = games.get(position);
            gamesManager.joinGame(selectedGame);
        });
    }

    private void requestGameName() {
        final EditText txtUrl = new EditText(this);

        // Set the default text to a link of the Queen
        txtUrl.setHint(getString(R.string.inserisci_nome_gara));
        txtUrl.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.nome_gara))
                .setView(txtUrl)
                .setPositiveButton(getString(R.string.conferma), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        gameName = txtUrl.getText().toString();
                        gamesManager.createGame(MyApplication.getUid(), MainActivity.playerName, gameName);
                    }
                })
                .setNegativeButton(getString(R.string.annulla), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
        txtUrl.requestFocus();
    }

    private void removeListener() {
        if (eventListener != null) {
            gamesManager.removeListener(eventListener);
            eventListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        removeListener();
        super.onDestroy();
    }

}
