package com.gepinfo.shiftanddrift;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GamesManager {

    private final Context context;
    public BotAI botAI = new BotAI();

    public DatabaseReference getGamesRef() {
        return gamesRef;
    }

    private final DatabaseReference gamesRef;

    /*public PlayerClass getMyPlayer() {
        return myPlayer;
    }

    public void setMyPlayer(PlayerClass myPlayer) {
        this.myPlayer = myPlayer;
    }

    private PlayerClass myPlayer = null;*/

    public GamesManager(Context context, String child) {
        this.context = context;
        if ((child == null) || (child.isEmpty()))
            this.gamesRef = MyApplication.getGamesRef().child("games");
        else this.gamesRef = MyApplication.getGamesRef().child("games").child(child);
    }

    private String generateGameCode() {
        return "R" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    // 🔧 CREA PARTITA
    public void createGame(String uid, String playerName, String gameName) {
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
    public void joinGame(GameClass game) {//String code, String uid, String playerName, boolean iAmInGame, String status) {
        PlayerClass player;
        if (!game.iAmIn()) {
            player = new PlayerClass(MyApplication.getUid(), MainActivity.playerName, 1, 0, MainActivity.carColorFront, MainActivity.carColorRear, MainActivity.carColorBody);
            game.getPlayers().put(MyApplication.getUid(), player);
            updateGame(game, true).addOnSuccessListener(unused -> {
                MainActivity.checkAndNotify(gamesRef.getKey(), "");
                navigateToGame(game.getCode(), MyApplication.getUid(), MainActivity.playerName, true, game.getStatus());
            });
        } else
            navigateToGame(game.getCode(), MyApplication.getUid(), MainActivity.playerName, false, game.getStatus());
    }

    // AZZERA I TIRI INIZIALI E PREDISPONE PER DETERMINARE LA GRIGLIA DI PARTENZA
    public void prepareGridRoll(GameClass game, List<PlayerClass> players) {
        Collections.shuffle(players);
        int i = 0;
        for (PlayerClass element : players) {
            element.roll = -1;
            element.position = ++i;
            element.turn=-1;
            gamesRef.child("players").child(element.uid).setValue(element);
        }
        game.setStatus("rolling");
        updateGame(game);
    }

    public Task<Void> updateGame(GameClass game) {
        return updateGame(game, false);
    }

    public Task<Void> updateGame(GameClass game, boolean useCode) {
        return (useCode) ? gamesRef.child(game.getCode()).setValue(game) : gamesRef.setValue(game);
    }

    // 🎲 TIRA DADO
    public void rollDice(String uid, boolean isBot, int diceValue) {
        if (isBot) {
            DatabaseReference rollRef = gamesRef.child("players").child(uid).child("roll");

            rollRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Integer currentRoll = currentData.getValue(Integer.class);
                    if (currentRoll == null || currentRoll < 0) {
                        currentData.setValue(diceValue);
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
        } else {
            gamesRef.child("players").child(uid).child("roll").setValue(diceValue);
        }
    }

    // ⚙️ CAMBIA MARCIA
    // ⏭️ TURNO SUCCESSIVO
    public PlayerClass nextTurn(List<PlayerClass> lista, int turno, boolean sortByPositionFirst) {
        PlayerClass nextToRoll = null;
        if (sortByPositionFirst) {
            lista.sort(Comparator.comparingInt(player -> player.position));
        }
        for (PlayerClass p : lista) {
            if (p.roll < 0 && p.turn == turno) {
                nextToRoll = p;
                break;
            }
        }
        return nextToRoll;
    }

    // 📡 ASCOLTA MODIFICHE PARTITA
    public void listenToGame(ValueEventListener listener) {
        gamesRef.addValueEventListener(listener);
    }

    public void removeListener(ValueEventListener listener) {
        gamesRef.removeEventListener(listener);
    }

    // ▶️ NAVIGAZIONE
    public void navigateToGame(String code, String uid, String playerName, boolean notify, String status) {
        if (notify)
            MainActivity.checkAndNotify(code, "&fun=newPlayer&player=" + MainActivity.playerName);
        Intent intent = new Intent(context, getActivityFromStatus(status));
        intent.putExtra("gameCode", code);
        intent.putExtra("uid", uid);
        intent.putExtra("playerName", playerName);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public List<PlayerClass> sortByPositionAndUpdate(List<PlayerClass> lista, boolean gridPlacing) {
        if (gridPlacing) {
            lista.sort(Comparator.comparingInt(player -> player.position));
            List<TrackCell> startingCells = new ArrayList<>();
            for (TrackCell cell : trackMap.getCells()) {
                if (cell.getStart() > 0) {
                    startingCells.add(cell);
                }
            }
            // Ordina per valore di start
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startingCells.sort(Comparator.comparingInt(TrackCell::getStart));
            }
            int i = 0;
            for (PlayerClass element : lista)
                if (i < startingCells.size()) {
                    TrackCell startCell = startingCells.get(i);
                    element.row = startCell.getRow();
                    element.column = startCell.getColumn();
                    element.turn=0;
                    element.roll = -1;
                    gamesRef.child("players").child(element.uid).setValue(element);//child("position").setValue(element.position);
                    i++;
                }
        } else {
            // TODO ORDINARE PER POSIZIONE SULLA PISTA
            lista.sort((p1, p2) -> {
                TrackCell c1 = getTrackCellAt(p1.row, p1.column);
                TrackCell c2 = getTrackCellAt(p2.row, p2.column);

                if (c1.getRow() != c2.getRow()) {
                    return Integer.compare(c2.getRow(), c1.getRow()); // decrescente
                } else {
                    return Integer.compare(c1.getPriority(), c2.getPriority()); // crescente
                }
            });
            for (PlayerClass element : lista) {
                gamesRef.child("players").child(element.uid).setValue(element);
            }
        }
        return lista;
        //gamesRef.child("players").child(element.uid).child("position").setValue(element.position);
    }

    public List<PlayerClass> assignGridPositions(List<PlayerClass> lista) {
        lista.sort((a, b) -> Integer.compare(b.roll, a.roll)); // decrescente
        for (int i = 0; i < lista.size(); i++) {
            lista.get(i).position = i + 1;
        }
        return sortByPositionAndUpdate(lista, true);
    }

    public Class getActivityFromStatus(String status) {
        switch (status) {
            case "started":
                return CurrentGameActivity.class;
            case "waiting":
                return GameActivity.class;
            case "rolling":
                return GridRollActivity.class;
            default:
                return GameLobbyActivity.class;
        }
    }

    public TrackMap getTrackMap() {
        return trackMap;
    }

    private static TrackMap trackMap;

    public TrackMap loadTrackMap(String trackName) {
        trackMap = TrackMap.loadTrackFromJson(trackName, context);
        return trackMap;
    }

    public static TrackCell getTrackCellAt(int row, int column) {
        for (TrackCell cell : trackMap.getCells()) {
            if (cell.getRow() == row && cell.getColumn() == column) {
                return cell;
            }
        }
        return null;
    }

    public static Set<TrackCell> calcolaTuttiGliArrivi(
            TrackCell start,
            int passi,
            Set<TrackCell> celleOccupate) {
        return new HashSet<>(PathFinder.calcolaArrivi(start, passi, celleOccupate));
    }
}
