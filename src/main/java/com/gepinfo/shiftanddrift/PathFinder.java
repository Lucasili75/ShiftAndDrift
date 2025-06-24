package com.gepinfo.shiftanddrift;

import java.util.*;

public class PathFinder {

    public static class Nodo {
        TrackCell cell;
        int step;
        List<TrackCell> percorso;
        TrackCell.Direction ultimaLateraleUsata;

        public Nodo(TrackCell cell, int step, List<TrackCell> percorso, TrackCell.Direction ultimaLateraleUsata) {
            this.cell = cell;
            this.step = step;
            this.percorso = new ArrayList<>(percorso);
            this.ultimaLateraleUsata = ultimaLateraleUsata;
        }
    }

    private static final TrackCell.Direction[] DIRECTIONS = TrackCell.Direction.values();

    private static final Map<TrackCell.Direction, TrackCell.Direction> OPPOSTE = Map.of(
            TrackCell.Direction.LEFT, TrackCell.Direction.RIGHT,
            TrackCell.Direction.RIGHT, TrackCell.Direction.LEFT
    );

    public static List<List<TrackCell>> calcolaPercorsi(TrackCell partenza, int passi, Set<TrackCell> occupate) {
        List<List<TrackCell>> percorsiValidi = new ArrayList<>();
        Queue<Nodo> queue = new LinkedList<>();
        queue.add(new Nodo(partenza, 0, List.of(partenza), null));

        while (!queue.isEmpty()) {
            Nodo corrente = queue.poll();

            if (corrente.step == passi) {
                percorsiValidi.add(corrente.percorso);
                continue;
            }

            for (TrackCell.Direction dir : DIRECTIONS) {
                TrackCell prossima = corrente.cell.getAdjacent(dir);
                if (prossima == null) continue;
                if (occupate.contains(prossima)) continue;
                if (corrente.percorso.contains(prossima)) continue; // evita loop

                // Regola anti-zigzag
                boolean cambioLaterale = (dir == TrackCell.Direction.LEFT || dir == TrackCell.Direction.RIGHT);
                boolean zigzag = cambioLaterale && corrente.ultimaLateraleUsata != null && OPPOSTE.get(corrente.ultimaLateraleUsata) == dir;
                if (zigzag) continue;

                TrackCell.Direction nuovaLaterale = cambioLaterale ? dir : corrente.ultimaLateraleUsata;

                List<TrackCell> nuovoPercorso = new ArrayList<>(corrente.percorso);
                nuovoPercorso.add(prossima);

                queue.add(new Nodo(prossima, corrente.step + 1, nuovoPercorso, nuovaLaterale));
            }
        }

        return percorsiValidi;
    }

    public static List<TrackCell> calcolaArrivi(TrackCell partenza, int passi, Set<TrackCell> occupate) {
        List<List<TrackCell>> percorsi = calcolaPercorsi(partenza, passi, occupate);
        List<TrackCell> arrivi = new ArrayList<>();

        for (List<TrackCell> percorso : percorsi) {
            if (percorso.size() == passi + 1) {
                arrivi.add(percorso.get(percorso.size() - 1));
            }
        }
        return arrivi;
    }

    public static List<TrackCell> calcolaArrivoBot(TrackCell partenza, int marcia, Set<TrackCell> occupate, int maxFreniUtilizzabili) {
        for (int riduzione = 0; riduzione <= maxFreniUtilizzabili && marcia - riduzione > 0; riduzione++) {
            int marciaAttuale = marcia - riduzione;
            List<TrackCell> arrivi = calcolaArrivi(partenza, marciaAttuale, occupate);
            if (!arrivi.isEmpty()) return arrivi;
        }
        return List.of(); // Nessun arrivo possibile nemmeno con freno
    }
}
