package com.gepinfo.shiftanddrift;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class PlayerClass {

    public static final String[] colorNames = {"Bianco", "Rosso", "Verde", "Blu", "Giallo", "Nero"};
    public static final String[] colorHexes = {"#FFFFFF", "#F44336", "#4CAF50", "#2196F3", "#FFEB3B", "#212121"};
    public static final String[] botNames = {
            "Lewis Hammel", "Sebastien Vettle", "Mika Häknin", "Nico Rosbrek", "Fernando Alonze",
            "Jenson Butner", "Kimi Reikkon", "Max Verstappenko", "Daniel Rickardo", "Charles Leclaire",
            "Lando Norridge", "Carlos Saintz", "George Russan", "Valterri Bottman", "Ayrton Seela",
            "Nigel Mansfield", "Alain Prostov", "David Coulthardson", "Rubens Barichell", "Jacques Villanuff"
    };

    public String name;
    public int gear;
    public int position;
    public int row;
    public int column;

    public String carColorFront;
    public String carColorRear;
    public String carColorBody;

    private String playerType;
    public int aggressiveness;
    public int riskiness;
    public int lap;
    public int roll=-1;
    public String uid;
    public int turn;
    public int tires;
    public int brakes;
    public int body;
    public int fuel;
    public int engine;
    public int curveStops;

    public static final int maxTires=4;
    public static final int maxBrakes=3;
    public static final int maxBody=2;
    public static final int maxFuel=2;
    public static final int maxEngine=3;

    public static final Map<String, String> colorHexMap = new HashMap<>();

    static {
        for (int i = 0; i < colorNames.length; i++) {
            colorHexMap.put(colorNames[i], colorHexes[i]);
        }
    }

    public PlayerClass() {
    } // Necessario per Firebase

    // Costruttore completo (usalo per bot e umani)
    public PlayerClass(String uid, String name, int gear, int position,
                       String carColorFront, String carColorRear, String carColorBody,
                       String playerType, int aggressiveness, int riskiness, int lap, int roll, int turn, int row, int column, int tires, int brakes, int body, int fuel, int engine,int curveStops) {
        this.uid=uid;
        this.name = name;
        this.gear = gear;
        this.position = position;
        this.carColorFront = carColorFront;
        this.carColorRear = carColorRear;
        this.carColorBody = carColorBody;
        this.playerType = playerType;
        this.aggressiveness = aggressiveness;
        this.riskiness = riskiness;
        this.lap = lap;
        this.roll = roll;
        this.turn=turn;
        this.row=row;
        this.column=column;
        this.tires=tires;
        this.brakes=brakes;
        this.body=body;
        this.fuel=fuel;
        this.engine=engine;
        this.curveStops=curveStops;
    }

    // Costruttore compatibile per vecchi usi (senza colori né AI)
    public PlayerClass(String uid,String name, int gear, int position, String colorFront, String colorRear, String carColorBody) {
        this(uid,name, gear, position, colorFront, colorRear, carColorBody, "player", 0, 0, 0, -1,0,-1,-1,0,0,0,0,0,0);
    }

    // Getter
    public String getName() {
        return name;
    }
    public String getPlayerType() {
        return playerType;
    }

    public void setGear(int gear) {
        this.gear = gear;
    }

    public int getGear() {
        return gear;
    }

    public int getTurn() {
        return turn;
    }

    public int getTires() {
        return tires;
    }

    public int getBrakes() {
        return brakes;
    }

    public int getBody() {
        return body;
    }

    public int getFuel() {
        return fuel;
    }

    public int getEngine() {
        return engine;
    }

    public int getPosition() {
        return position;
    }

    @Exclude
    public boolean isBot() {
        return playerType!=null&&playerType.equals("bot");
    }

    @Exclude
    public int getRemainigBrakes(){ return maxBrakes-brakes;}
    @Exclude
    public int getRemainigFuel(){ return maxFuel-fuel;}
    @Exclude
    public int getRemainigEngine(){ return maxEngine-engine;}
    @Exclude
    public int getRemainigTires(){ return maxTires-tires;}
    @Exclude
    public int getRemainigBody(){ return maxBody-body;}
    public int getAggressiveness() {
        return aggressiveness;
    }
    public int getRiskiness() {
        return riskiness;
    }

    @Exclude
    public void incrementCurveStops() {
        curveStops++;
    }

    @Exclude
    public void resetCurveStops() {
        curveStops = 0;
    }

    @Exclude
    public int getCurveStops() {
        return curveStops;
    }
}
