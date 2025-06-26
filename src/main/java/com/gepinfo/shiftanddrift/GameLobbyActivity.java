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

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GameLobbyActivity extends AppCompatActivity {

    private ArrayAdapter<GameClass> gameListAdapter;
    private List<GameClass> games = new ArrayList<>();

    private String gameName;
    DbManager dbManager;
    ValueEventListener eventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbManager = new DbManager(this, "");
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

        dbManager.listenToGame(eventListener);

        buttonCreateGame.setOnClickListener(v -> {
            requestGameName();
        });

        listViewGames.setOnItemClickListener((parent, view, position, id) -> {
            GameClass selectedGame = games.get(position);
            Task<Void> retVal = dbManager.joinGame(selectedGame);
            if (retVal == null) {
                MainActivity.navigateToGame(this, selectedGame.getCode(), MyApplication.getUid(), MainActivity.playerName, true, selectedGame.getStatus());
            } else {
                retVal.addOnSuccessListener(unused -> {
                    MainActivity.checkAndNotify(selectedGame.getCode(), "");
                    MainActivity.navigateToGame(this, selectedGame.getCode(), MyApplication.getUid(), MainActivity.playerName, true, selectedGame.getStatus());
                });
            }
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
                        dbManager.createGame(MyApplication.getUid(), gameName);
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
