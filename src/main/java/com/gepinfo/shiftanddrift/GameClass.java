package com.gepinfo.shiftanddrift;

import androidx.annotation.NonNull;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private Map<String, PlayerClass> players = new LinkedHashMap<>();

    private String currentPlayerUid = null;

    // 🔹 Costruttore vuoto richiesto da Firebase
    public GameClass() {
    }

    public GameClass(String code, String host, String name, String status, String track, int currentTurn, int totalLaps, Map<String, PlayerClass> players, String currentPlayerUid) {
        this.code = code;
        this.host = host;
        this.name = name;
        this.status = status;
        this.track = track;
        this.currentTurn = currentTurn;
        this.totalLaps = totalLaps;
        if (players != null) this.players = players;
        this.currentPlayerUid = currentPlayerUid;
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
    }

    @Exclude
    public GameClass updatePlayerMapByUid(PlayerClass p) {
        players.put(p.uid, p);
        return this;
    }

    @Exclude
    public PlayerClass getPlayerFromUid(String uid) {
        return players.get(uid);
    }

    // 🔹 Controlla se l'utente è nella partita
    @Exclude
    public boolean iAmIn() {
        String myUid = MyApplication.getUid();
        for (PlayerClass player : players.values()) {
            if (player.uid.equals(myUid)) {
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

    public String getCurrentPlayerUid() {
        return currentPlayerUid;
    }

    public void setCurrentPlayerUid(String currentPlayerUid) {
        this.currentPlayerUid = currentPlayerUid;
    }

    @Exclude
    public List<PlayerClass> getPlayersSortedByPosition() {
        return players.values().stream()
                .sorted(Comparator.comparingInt(PlayerClass::getPosition))
                .collect(Collectors.toList());
    }

    @Exclude
    public List<PlayerClass> getPlayersSortedByName() {
        return players.values().stream()
                .sorted(Comparator.comparing(PlayerClass::getName))
                .collect(Collectors.toList());
    }

    @Exclude
    public List<PlayerClass> getPlayersSortedByRoll() {
        return players.values().stream()
                .sorted(Comparator.comparingInt(PlayerClass::getRoll).reversed())
                .collect(Collectors.toList());
    }

    @Exclude
    public List<PlayerClass> getPlayersShuffled() {
        List<PlayerClass> shuffledList = new ArrayList<>(players.values());
        Collections.shuffle(shuffledList);
        return shuffledList;
    }
}
