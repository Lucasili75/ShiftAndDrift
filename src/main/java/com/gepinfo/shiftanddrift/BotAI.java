package com.gepinfo.shiftanddrift;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BotAI {

    private static final int LOOKAHEAD_CELLS = 6;

    private static final Map<Integer, int[]> DICE_RANGE = new HashMap<>();
    static {
        DICE_RANGE.put(1, new int[]{1, 2});
        DICE_RANGE.put(2, new int[]{2, 4});
        DICE_RANGE.put(3, new int[]{4, 8});
        DICE_RANGE.put(4, new int[]{7, 12});
        DICE_RANGE.put(5, new int[]{11, 20});
        DICE_RANGE.put(6, new int[]{21, 30});
    }

    public int tiraDado(PlayerClass bot, List<TrackCell> trackCells, Set<TrackCell> celleOccupate) {
        TrackCell currentCell = findCurrentCell(bot, trackCells);
        if (currentCell == null) {
            System.err.println("Bot non ha posizione valida!");
            return 0;
        }

        List<TrackCell> upcoming = getUpcomingCells(currentCell, LOOKAHEAD_CELLS, trackCells);
        int marcia = scegliMarciaSimulata(bot, currentCell, trackCells, celleOccupate);
        bot.setGear(marcia);
        int dice = DiceManager.tiraDado(marcia);
        bot.roll = dice;

        return dice;
    }

    private TrackCell findCurrentCell(PlayerClass bot, List<TrackCell> allCells) {
        for (TrackCell cell : allCells) {
            if (cell.getRow() == bot.row && cell.getColumn() == bot.column) {
                return cell;
            }
        }
        return null;
    }

    private List<TrackCell> getUpcomingCells(TrackCell from, int steps, List<TrackCell> allCells) {
        List<TrackCell> future = new ArrayList<>();
        TrackCell current = from;

        for (int i = 0; i < steps; i++) {
            current = getNextCell(current, allCells);
            if (current == null) break;
            future.add(current);
        }

        return future;
    }

    private TrackCell getNextCell(TrackCell current, List<TrackCell> allCells) {
        List<TrackCell> candidates = new ArrayList<>();

        for (TrackCell cell : allCells) {
            if (cell.getRow() <= current.getRow()) continue;

            if (current.isCanGoForward() && cell.getColumn() == current.getColumn()) {
                candidates.add(cell);
            }
            if (current.isCanGoLeft() && cell.getColumn() == current.getColumn() - 1) {
                candidates.add(cell);
            }
            if (current.isCanGoRight() && cell.getColumn() == current.getColumn() + 1) {
                candidates.add(cell);
            }
        }

        candidates.sort(Comparator
                .comparingInt(TrackCell::getRow)
                .thenComparingInt(TrackCell::getPriority));

        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private int distanzaAllaCurva(List<TrackCell> upcoming) {
        for (int i = 0; i < upcoming.size(); i++) {
            if (upcoming.get(i).getItemType() == TrackCell.ItemType.CURVE) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }


    private boolean isCurvaInArrivo(List<TrackCell> upcoming) {
        return upcoming.stream().anyMatch(c -> c.getItemType() == TrackCell.ItemType.CURVE);
    }

    private int scegliMarciaSimulata(PlayerClass bot, TrackCell current, List<TrackCell> allCells, Set<TrackCell> celleOccupate) {
        int bestMarcia = 1;
        int bestScore = Integer.MIN_VALUE;

        if (bot.turn == 0) return bestMarcia;

        for (int marcia = 1; marcia <= 6; marcia++) {
            int[] range = DICE_RANGE.get(marcia);
            int min = range[0];
            int max = range[1];

            for (int tiro = min; tiro <= max; tiro++) {
                List<TrackCell> percorso = simulaPercorso(current, tiro, allCells);
                int score = valutaPercorso(bot, percorso, celleOccupate);
                if (score > bestScore) {
                    bestScore = score;
                    bestMarcia = marcia;
                }
            }
        }

        return bestMarcia;
    }

    public List<TrackCell> simulaPercorso(TrackCell start, int passi, List<TrackCell> allCells) {
        List<TrackCell> percorso = new ArrayList<>();
        TrackCell current = start;
        for (int i = 0; i < passi; i++) {
            current = getNextCell(current, allCells);
            if (current == null) break;
            percorso.add(current);
        }
        return percorso;
    }


    private int valutaPercorso(PlayerClass bot, List<TrackCell> percorso, Set<TrackCell> celleOccupate) {
        int score = 0;
        int stops = 0;
        int risk = bot.getRiskiness();
        int aggr = bot.getAggressiveness();

        for (int i = 0; i < percorso.size(); i++) {
            TrackCell cell = percorso.get(i);

            // Penalità per frenata in curva
            if (cell.getItemType() == TrackCell.ItemType.CURVE) {
                stops++;
                if (i >= cell.getRequiredStops()) {
                    score -= 30;
                }
            }

            // Premio se arrivo
            if (cell.isFinish()) {
                score += 1000;
            }

            // Penalità per celle adiacenti occupate (contatto rischioso)
            for (TrackCell.Direction dir : TrackCell.Direction.values()) {
                TrackCell vicino = cell.getAdjacent(dir);
                if (vicino != null && celleOccupate.contains(vicino)) {
                    if (risk <= 4) score -= 20;  // prudente, evita contatti
                    else if (risk >= 8) score += 5;  // audace, non gli importa
                }
            }

            // Bonus se blocca altri (solo nell'ultima cella)
            if (i == percorso.size() - 1 && aggr >= 7) {
                for (TrackCell.Direction dir : TrackCell.Direction.values()) {
                    TrackCell vicino = cell.getAdjacent(dir);
                    if (vicino != null && celleOccupate.contains(vicino)) {
                        score += 15;  // vuole bloccare
                    }
                }
            }
        }

        // Bonus generale per avanzamento
        score += percorso.size();

        // Penalità per troppe curve se prudente
        if (stops > 0) {
            if (risk <= 4) score -= 10 * stops;
            else if (risk >= 8) score += 5 * stops;
        }

        // Penalità per freni scarsi
        if (bot.getRemainigBrakes() <= 2 && aggr <= 4) {
            score -= 20;
        }

        return score;
    }



    public TrackCell scegliArrivoBot(PlayerClass bot, int movimento, List<TrackCell> trackCells, Set<TrackCell> celleOccupate) {
        TrackCell current = findCurrentCell(bot, trackCells);
        if (current == null) return null;

        Set<TrackCell> arrivi = GamesManager.calcolaTuttiGliArrivi(current, movimento, celleOccupate);
        if (arrivi.isEmpty()) return null;

        TrackCell migliore = null;
        int bestScore = Integer.MIN_VALUE;

        for (TrackCell arrivo : arrivi) {
            int score = 0;

            // ➤ Premio avanzamento
            score += arrivo.getRow() * 10;

            // ➤ Curve: penalità o premio
            if (arrivo.getItemType() == TrackCell.ItemType.CURVE) {
                if (bot.getRiskiness() <= 4) score -= 30;  // prudente
                else if (bot.getRiskiness() >= 8) score += 10;  // spericolato
            }

            // ➤ Bloccare avversari
            if (bot.getAggressiveness() >= 7) {
                for (TrackCell.Direction dir : TrackCell.Direction.values()) {
                    TrackCell vicino = arrivo.getAdjacent(dir);
                    if (vicino != null && celleOccupate.contains(vicino)) {
                        score += 15;  // bonus se può bloccare
                    }
                }
            }

            // ➤ Evitare rischi (celle adiacenti occupate)
            if (bot.getRiskiness() <= 4) {
                for (TrackCell.Direction dir : TrackCell.Direction.values()) {
                    TrackCell vicino = arrivo.getAdjacent(dir);
                    if (vicino != null && celleOccupate.contains(vicino)) {
                        score -= 20;  // penalità se vicino ad altri
                    }
                }
            }

            // ➤ Bonus finish
            if (arrivo.isFinish()) {
                score += 1000;
            }

            if (score > bestScore) {
                bestScore = score;
                migliore = arrivo;
            }
        }

        return migliore;
    }

}
