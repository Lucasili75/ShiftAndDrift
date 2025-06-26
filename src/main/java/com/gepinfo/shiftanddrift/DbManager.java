package com.gepinfo.shiftanddrift;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

public class DbManager {

    private final Context context;

    public DatabaseReference getGamesRef() {
        return gamesRef;
    }

    private final DatabaseReference gamesRef;

    public DbManager(Context context, String child) {
        this.context = context;
        if ((child == null) || (child.isEmpty()))
            this.gamesRef = MyApplication.getGamesRef().child("games");
        else this.gamesRef = MyApplication.getGamesRef().child("games").child(child);
    }

    private String generateGameCode() {
        return "R" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    // 🔧 CREA PARTITA
    public void createGame(String uid, String gameName) {
        String code = generateGameCode();
        GameClass newGame = new GameClass(code, uid, gameName, "waiting", "", 0, 2, null);

        gamesRef.child(code).setValue(newGame).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Partita creata: " + code, Toast.LENGTH_SHORT).show();
                joinGame(newGame);
            } else {
                Toast.makeText(context, "Errore creazione partita: " + task.getException(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔗 ENTRA IN PARTITA
    public Task<Void> joinGame(GameClass game) {
        PlayerClass player;
        if (!game.iAmIn()) {
            player = new PlayerClass(MyApplication.getUid(), MainActivity.playerName, 1, 0, MainActivity.carColorFront, MainActivity.carColorRear, MainActivity.carColorBody);
            game.getPlayers().put(MyApplication.getUid(), player);
            return updateGame(game, true);/*.addOnSuccessListener(unused -> {
                MainActivity.checkAndNotify(gamesRef.getKey(), "");
                navigateToGame(game.getCode(), MyApplication.getUid(), MainActivity.playerName, true, game.getStatus());
            });*/
        } else
            return null;
        //navigateToGame(game.getCode(), MyApplication.getUid(), MainActivity.playerName, false, game.getStatus());
    }

    public Task<Void> updateGame(GameClass game) {
        return updateGame(game, false);
    }

    public Task<Void> updateGame(GameClass game, boolean useCode) {
        return (useCode) ? gamesRef.child(game.getCode()).setValue(game) : gamesRef.setValue(game);
    }

    public void listenToGame(ValueEventListener listener) {
        gamesRef.addValueEventListener(listener);
    }

    public void removeListener(ValueEventListener listener) {
        gamesRef.removeEventListener(listener);
    }

    public void updatePlayerWithTransaction(PlayerClass player){
        DatabaseReference rollRef = gamesRef.child("players").child(player.uid);

        rollRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                PlayerClass currentPlayer = currentData.getValue(PlayerClass.class);
                if (currentPlayer == null || currentPlayer.roll < 0) {
                    currentData.setValue(player);
                    return Transaction.success(currentData);
                } else {
                    // Qualcun altro ha già assegnato il tiro
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (committed) {
                    Log.d("BOT", "Tiro bot completato: " + currentData.getValue());
                } else {
                    Log.d("BOT", "Tiro già eseguito da un altro client.");
                }
            }
        });
    }
}
