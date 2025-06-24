package com.gepinfo.shiftanddrift;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.*;
import com.google.android.gms.tasks.Task;

import java.util.List;

public class GameRepository {

    private final DatabaseReference rootRef;
    private final DatabaseReference gameRef;

    public GameRepository(String gameCode) {
        this.rootRef = MyApplication.getGamesRef().child("games");
        this.gameRef = (gameCode != null && !gameCode.isEmpty())
                ? rootRef.child(gameCode)
                : rootRef;
    }

    // 🔧 Crea nuova partita
    public Task<Void> createGame(GameClass game) {
        return rootRef.child(game.getCode()).setValue(game);
    }

    // 🔄 Aggiorna partita intera
    public Task<Void> updateGame(GameClass game, boolean useCode) {
        return (useCode)
                ? rootRef.child(game.getCode()).setValue(game)
                : gameRef.setValue(game);
    }

    // 🔄 Aggiorna stato
    public Task<Void> updateStatus(String code, String newStatus) {
        return rootRef.child(code).child("status").setValue(newStatus);
    }

    // 🎲 Imposta tiro diretto
    public Task<Void> setRoll(String uid, int value) {
        return gameRef.child("players").child(uid).child("roll").setValue(value);
    }

    // 🎲 Transazione per il tiro del bot
    public void runRollTransaction(String uid, int value) {
        DatabaseReference rollRef = gameRef.child("players").child(uid).child("roll");

        rollRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer currentRoll = currentData.getValue(Integer.class);
                if (currentRoll == null || currentRoll < 0) {
                    currentData.setValue(value);
                    return Transaction.success(currentData);
                } else {
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (committed) {
                    Log.d("BOT", "Tiro bot completato: " + snapshot.getValue());
                } else {
                    Log.d("BOT", "Tiro già eseguito o errore.");
                }
            }
        });
    }

    // ⚙️ Cambio marcia
    public Task<Void> changeGear(String uid, int gear) {
        return gameRef.child("players").child(uid).child("gear").setValue(gear);
    }

    // 📡 Ascolta modifiche
    public void listenToGame(ValueEventListener listener) {
        gameRef.addValueEventListener(listener);
    }

    public void removeListener(ValueEventListener listener) {
        gameRef.removeEventListener(listener);
    }

    // 📍 Imposta o aggiorna player singolo
    public Task<Void> updatePlayer(PlayerClass player) {
        return gameRef.child("players").child(player.uid).setValue(player);
    }

    public DatabaseReference getGameRef() {
        return gameRef;
    }

    public DatabaseReference getRootRef() {
        return rootRef;
    }

    // 📤 Scrive lista di player (override completo)
    public void updatePlayers(List<PlayerClass> players) {
        for (PlayerClass player : players) {
            updatePlayer(player);
        }
    }
}
