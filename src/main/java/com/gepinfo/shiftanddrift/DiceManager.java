package com.gepinfo.shiftanddrift;

import java.util.Random;

public class DiceManager {

    private static final Random random = new Random();

    public static int tiraDado(int marcia) {
        switch (marcia) {
            case 1:
                return randomBetween(1, 2);     // D4: [1–2]
            case 2:
                return randomBetween(2, 4);     // D6: [2–4]
            case 3:
                return randomBetween(4, 8);     // D8: [4–8]
            case 4:
                return randomBetween(7, 12);    // D12: [7–12]
            case 5:
                return randomBetween(11, 20);   // D20: [11–20]
            case 6:
                return randomBetween(21, 30);   // D30: [21–30]
            case 99:
                return randomBetween(1, 20);   // DNERO: [1–20]
            default:
                throw new IllegalArgumentException("Marcia non valida: " + marcia);
        }
    }

    private static int randomBetween(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}
