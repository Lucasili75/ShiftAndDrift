package com.gepinfo.shiftanddrift;

public class TrackCell {

    public enum ItemType {
        NORMAL,
        CURVE,
        BOX
    }

    private int row;
    private int column;
    private ItemType itemType;
    private boolean canGoForward;
    private boolean canGoLeft;
    private boolean canGoRight;

    private double posX;
    private double posY;
    private double angle;
    private double width;
    private double height;

    private int start; // Numero posizione griglia, se start > 0
    private boolean isFinish;
    private int requiredStops; // Per le curve
    private int priority;

    public TrackCell() {
        // Costruttore vuoto necessario per il parser JSON (Gson, Moshi, etc.)
    }

    // Getter e Setter
    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getColumn() { return column; }
    public void setColumn(int column) { this.column = column; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public boolean isCanGoForward() { return canGoForward; }
    public void setCanGoForward(boolean canGoForward) { this.canGoForward = canGoForward; }

    public boolean isCanGoLeft() { return canGoLeft; }
    public void setCanGoLeft(boolean canGoLeft) { this.canGoLeft = canGoLeft; }

    public boolean isCanGoRight() { return canGoRight; }
    public void setCanGoRight(boolean canGoRight) { this.canGoRight = canGoRight; }

    public double getPosX() { return posX; }
    public void setPosX(double posX) { this.posX = posX; }

    public double getPosY() { return posY; }
    public void setPosY(double posY) { this.posY = posY; }

    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; }

    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }

    public boolean isFinish() { return isFinish; }
    public void setFinish(boolean finish) { isFinish = finish; }

    public int getRequiredStops() { return requiredStops; }
    public void setRequiredStops(int requiredStops) { this.requiredStops = requiredStops; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    // Utility method to check movement permission
    public boolean isMoveAllowed(Direction dir) {
        switch (dir) {
            case FORWARD:
                return canGoForward;
            case LEFT:
                return canGoLeft;
            case RIGHT:
                return canGoRight;
            default:
                return false;
        }
    }

    public enum Direction {
        FORWARD, LEFT, RIGHT
    }

    public int getAvanzamentoScore() {
        // Colonna centrale (1) è "più avanti", poi 0, poi 2
        int colScore;
        switch (this.column) {
            case 1: colScore = 0; break; // centrale → più avanzata
            case 0: colScore = 1; break; // destra
            case 2: colScore = 0; break; // sinistra
            default: colScore = 3;       // qualsiasi altra (fallback)
        }

        return this.row * 10 + colScore;
    }

    public TrackCell getAdjacent(Direction dir) {
        int newRow = this.row;
        int newCol = this.column;

        switch (dir) {
            case FORWARD:
                newRow += 1;
                break;
            case LEFT:
                if (column == 1) { newCol = 2; newRow += 1; }
                else if (column == 0) { newCol = 1; } // stessa riga
                break;
            case RIGHT:
                if (column == 2) { newCol = 1; } // stessa riga
                else if (column == 1) { newCol = 0; newRow += 1; }
                break;
        }

        // Puoi implementarlo come vuoi, ma in generale devi avere accesso alla track
        return GameManager.getTrackCellAt(newRow, newCol); // adatta in base alla tua struttura
    }

}
