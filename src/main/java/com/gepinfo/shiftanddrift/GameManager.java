package com.gepinfo.shiftanddrift;

import android.content.Context;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameManager {

    public static BotAI botAI = new BotAI();

    public static void setTrackMap(TrackMap trackMap) {
        GameManager.trackMap = trackMap;
    }

    private static TrackMap trackMap = null;

    // AZZERA I TIRI INIZIALI E PREDISPONE PER DETERMINARE LA GRIGLIA DI PARTENZA
    public static void prepareGridRoll(GameClass game) {
        int i = 0;
        List<PlayerClass> lista=game.getPlayersShuffled();
        for (PlayerClass element : lista) {
            element.roll = -1;
            element.position = ++i;
            element.turn = -1;
            game.getPlayers().put(element.uid,element);
        }
        game.setStatus("rolling");
    }

    // ⚙️ CAMBIA MARCIA
    // ⏭️ TURNO SUCCESSIVO
    public static PlayerClass nextTurn(List<PlayerClass> lista, int turno) {
        for (PlayerClass p : lista) {
            if (p.roll < 0 && p.turn == turno) {
                return p;
            }
        }
        return null;
    }

    public static String nextTurnUid(List<PlayerClass> lista, int turno) {
        PlayerClass p=nextTurn(lista,turno);
        if(p!=null) return p.uid;
        return "";
    }

    public static void calculateNewPositions(GameClass game, boolean gridPlacing) {
        if (gridPlacing) {
            List<TrackCell> startingCells = new ArrayList<>();
            for (TrackCell cell : trackMap.getCells()) {
                if (cell.getStart() > 0) {
                    startingCells.add(cell);
                }
            }
            // Ordina per valore di start
            startingCells.sort(Comparator.comparingInt(TrackCell::getStart));
            int i = 0;
            for (PlayerClass element : game.getPlayersSortedByPosition())
                if (i < startingCells.size()) {
                    TrackCell startCell = startingCells.get(i);
                    element.row = startCell.getRow();
                    element.column = startCell.getColumn();
                    element.turn = 0;
                    element.gear = 1;
                    element.roll = -1;
                    element.status="";
                    i++;
                    game.getPlayers().put(element.uid,element);
                }
        } else {
            // TODO ORDINARE PER POSIZIONE SULLA PISTA
            List<PlayerClass> lista=game.getPlayersSortedByPosition();
            lista.sort((p1, p2) -> {
                TrackCell c1 = getTrackCellAt(p1.row, p1.column);
                TrackCell c2 = getTrackCellAt(p2.row, p2.column);

                if (c1.getRow() != c2.getRow()) {
                    return Integer.compare(c2.getRow(), c1.getRow()); // decrescente
                } else {
                    return Integer.compare(c1.getPriority(), c2.getPriority()); // crescente
                }
            });
            int i=0;
            for (PlayerClass element : lista) {
                element.turn++;
                element.roll = -1;
                element.status="";
                element.position=++i;
                game.getPlayers().put(element.uid,element);
            }
        }
    }

    public static void assignGridPositions(GameClass game) {
        List<PlayerClass> lista=game.getPlayersSortedByRoll();
        for (int i = 0; i < lista.size(); i++) {
            lista.get(i).position = i + 1;
            game.getPlayers().put(lista.get(i).uid,lista.get(i));
        }
        calculateNewPositions(game, true);
    }

    public static TrackMap getTrackMap() {
        return trackMap;
    }

    public static void loadTrackMap(String trackName, Context context) {
        trackMap = TrackMap.loadTrackFromJson(trackName, context);
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
