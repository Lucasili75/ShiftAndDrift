package com.gepinfo.shiftanddrift;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.drawable.PictureDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class GameBoardOverlay extends View {

    private Paint paint;
    private List<TrackCell> cells = new ArrayList<>();
    private List<ImageView> cars = new ArrayList<>();
    private List<PlayerClass> players = new ArrayList<>();
    private PlayerClass currentPlayer;
    private boolean blinkVisible = true;
    private Handler blinkHandler = new Handler();
    private final int BLINK_INTERVAL = 500; // ms
    private List<TrackCell> highlightCells = new ArrayList<>();
    public List<HighlightInfo> highlightInfos = new ArrayList<>();

    public void setHighlightCells(List<TrackCell> highlightCells) {
        this.highlightCells = highlightCells != null ? highlightCells : new ArrayList<>();
        invalidate();
    }

    private Matrix transformMatrix = new Matrix();

    public void setTransformMatrix(Matrix matrix) {
        transformMatrix.set(matrix);
        invalidate();
    }

    public TrackCell clickOnArrival(MotionEvent event) {
        if (highlightCells == null || highlightCells.isEmpty()) return null;

        // 1. Ottieni coordinate del tocco
        float[] touch = new float[] { event.getX(), event.getY() };

        // 2. Inverti la transformMatrix per ottenere coordinate mondo
        Matrix inverse = new Matrix();
        if (!transformMatrix.invert(inverse)) return null; // inversione fallita

        inverse.mapPoints(touch);
        float worldX = touch[0];
        float worldY = touch[1];

        // 3. Cerca la cella evidenziata più vicina al click
        float clickRadius = 20f; // tolleranza in coordinate mondo

        for (TrackCell cell : highlightCells) {
            float cx = (float) cell.getPosX();
            float cy = (float) cell.getPosY();

            float dx = cx - worldX;
            float dy = cy - worldY;

            if (dx * dx + dy * dy <= clickRadius * clickRadius) {
                return cell;
            }
        }

        return null; // nessuna cella cliccata
    }

    public GameBoardOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
    }

    public void setCells(List<TrackCell> cellList) {
        this.cells = cellList;
        invalidate();
    }

    public void setPlayers(List<PlayerClass> players) {
        this.players = players;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Applica la stessa trasformazione usata dall'immagine
        canvas.concat(transformMatrix);
        for (PlayerClass player : players) {
            TrackCell cell = GamesManager.getTrackCellAt(player.row,player.column);
            if (cell == null) continue;

            float centerX = (float) cell.getPosX();
            float centerY = (float) cell.getPosY();
            float angle = (float) cell.getAngle();

            if (player == currentPlayer && !blinkVisible) continue;

            PictureDrawable carDrawable = CarRenderer.renderCar(getContext(),
                    PlayerClass.colorHexMap.get(player.carColorFront),
                    PlayerClass.colorHexMap.get(player.carColorBody),
                    PlayerClass.colorHexMap.get(player.carColorRear)
            );

            if (carDrawable != null) {
                Picture carPicture = carDrawable.getPicture();
                float scale = 0.04f; // Ridimensionamento se serve

                canvas.save();
                canvas.translate(centerX, centerY);
                canvas.rotate(angle-90);
                canvas.scale(scale, scale);
                canvas.translate(-carPicture.getWidth() / 2f, -carPicture.getHeight() / 2f);
                canvas.drawPicture(carPicture);
                canvas.restore();
            }

            // Evidenzia celle proposte con cerchio giallo
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(Color.YELLOW);

            for (TrackCell hCell : highlightCells) {
                float cx = (float) hCell.getPosX();
                float cy = (float) hCell.getPosY();
                canvas.drawCircle(cx, cy, 6, paint);
            }

            paint.setStyle(Paint.Style.STROKE); // Ripristina stile normale

        }
    }

    public void setSelectedPlayer(PlayerClass player) {
        this.currentPlayer = player;
        startBlinking();
    }

    private void startBlinking() {
        blinkHandler.removeCallbacksAndMessages(null);
        blinkHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                blinkVisible = !blinkVisible;
                invalidate();
                blinkHandler.postDelayed(this, BLINK_INTERVAL);
            }
        }, BLINK_INTERVAL);
    }

    public void clearSelectedPlayer() {
        currentPlayer = null;
        blinkHandler.removeCallbacksAndMessages(null);
        invalidate();
    }

    public void clearHighlightCells() {
        highlightCells.clear();
        invalidate();
    }

    public static class HighlightInfo {
        public TrackCell cell;
        public boolean penalized;
        public int estimatedBrakesUsed;
        public int estimatedTiresUsed;

        public HighlightInfo(TrackCell cell, boolean penalized, int brakes, int tires) {
            this.cell = cell;
            this.penalized = penalized;
            this.estimatedBrakesUsed = brakes;
            this.estimatedTiresUsed = tires;
        }
    }
}

