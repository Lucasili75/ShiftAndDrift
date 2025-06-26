package com.gepinfo.shiftanddrift;

import androidx.annotation.NonNull;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameClass {
    private String code;
    private String name;
    private String status;

    public void setTrack(String track) {
        this.track = track;
    }

    private String track;

    @Exclude
    public GameClass newTurn() {
        this.currentTurn++;
        return this;
    }

    public GameClass setCurrentTurn(int currentTurn) {
        this.currentTurn = currentTurn;
        return this;
    }

    private int currentTurn;
    private String host;

    public int getTotalLaps() {
        return totalLaps;
    }

    private int totalLaps;

    // ⚠️ Usa una mappa per evitare problemi di deserializzazione
    private Map<String, PlayerClass> players = new HashMap<>();
    private PlayerClass myPlayer = null;

    // 🔹 Costruttore vuoto richiesto da Firebase
    public GameClass() {
    }

    public GameClass(String code, String host, String name, String status, String track, int currentTurn, int totalLaps, Map<String, PlayerClass> players) {
        this.code = code;
        this.host = host;
        this.name = name;
        this.status = status;
        this.track = track;
        this.currentTurn = currentTurn;
        this.totalLaps = totalLaps;
        if (players != null) this.players = players;
    }

    // 🔹 Getter normali
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getTrack() {
        return track;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public String getHost() {
        return host;
    }

    public GameClass setStatus(String status) {
        this.status = status;
        return this;
    }

    public Map<String, PlayerClass> getPlayers() {
        return players;
    }

    public void setPlayers(Map<String, PlayerClass> players) {
        this.players = players;
        this.arrayPlayer.clear();
        for (Map.Entry<String, PlayerClass> entry : players.entrySet()) {
            if (entry.getValue().uid.equals(MyApplication.getUid())) {
                myPlayer = entry.getValue();
            }
            this.arrayPlayer.add(entry.getValue());
        }
    }

    public GameClass setArrayPlayer(List<PlayerClass> arrayPlayer) {
        this.arrayPlayer = arrayPlayer;
        return this;
    }

    private List<PlayerClass> arrayPlayer = new ArrayList<>();

    // 🔹 Metodo di utilità per ottenere una lista
    @Exclude
    public List<PlayerClass> getPlayersList() {
        return arrayPlayer;
    }

    @Exclude
    public GameClass updatePlayerArrayByUid(PlayerClass p) {
        for (int index = 0; index < arrayPlayer.size(); index++)
            if (arrayPlayer.get(index).uid.equals(p.uid)) {
                arrayPlayer.set(index, p);
                break;
            }
        return this;
    }

    @Exclude
    public GameClass updatePlayerMapByUid(PlayerClass p) {
        players.put(p.uid, p);
        return this;
    }

    @Exclude
    public GameClass updatePlayerMapFromArrayList() {
        for (PlayerClass element : arrayPlayer) {
            players.put(element.uid, element);
        }
        return this;
    }

    // 🔹 Controlla se l'utente è nella partita
    @Exclude
    public boolean iAmIn() {
        String myUid = MyApplication.getUid();
        for (PlayerClass player : players.values()) {
            if (player.uid.equals(myUid)) {
                myPlayer = player;
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return name + " - " + players.size() + " giocatori (" + MainActivity.getStatus(status) + ")";
    }

    @Exclude
    public PlayerClass getMyPlayer() {
        return myPlayer;
    }

}
