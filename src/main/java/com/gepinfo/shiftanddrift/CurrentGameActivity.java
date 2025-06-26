package com.gepinfo.shiftanddrift;

import static com.gepinfo.shiftanddrift.TrackCell.ItemType.CURVE;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CurrentGameActivity extends AppCompatActivity {

    private GameBoardOverlay overlay;
    ZoomableImageView imageView;
    TextView textPlayerName;
    TextView textPlayerLap;
    TextView textPlayerCurveStops;
    DbManager dbManager;
    private String gameCode;
    private String status, trackName;
    private boolean isHost = false;
    ValueEventListener eventListener;
    GameClass thisGame = new GameClass();
    LinearLayout brakesBoxContainer;
    LinearLayout tiresBoxContainer;
    LinearLayout fuelBoxContainer;
    LinearLayout bodyBoxContainer;
    LinearLayout engineBoxContainer;
    ImageView gears;
    ImageView dice;
    PlayerClass currentPlayer;
    Handler handler;
    int tempBrakes;
    int tempEngine;
    int tempTires;
    int marciaSelezionata;
    List<TrackCell> celleEvidenziate = new ArrayList<>();
    Set<TrackCell> celleOccupate = new HashSet<>();

    private static final Map<Integer, Integer> DICE_IMAGE = new HashMap<>();

    static {
        DICE_IMAGE.put(1, R.drawable.dado_giallo);
        DICE_IMAGE.put(2, R.drawable.dado_arancio);
        DICE_IMAGE.put(3, R.drawable.dado_rosso);
        DICE_IMAGE.put(4, R.drawable.dado_verde);
        DICE_IMAGE.put(5, R.drawable.dado_viola);
        DICE_IMAGE.put(6, R.drawable.dado_blu);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_current);
        handler = new Handler(getMainLooper());
        textPlayerName = findViewById(R.id.textPlayerName);
        textPlayerLap = findViewById(R.id.textPlayerLap);
        textPlayerCurveStops = findViewById(R.id.textPlayerCurveStops);
        overlay = findViewById(R.id.overlay);
        imageView = findViewById(R.id.imageViewTrack);
        gameCode = getIntent().getStringExtra("gameCode");
        dbManager = new DbManager(this, gameCode);
        MainActivity.gamePrefs = getSharedPreferences("ShiftAndDriftPrefs", MODE_PRIVATE);
        MainActivity.loadParms();
        eventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                thisGame = snapshot.getValue(GameClass.class);
                marciaSelezionata = -1;
                celleEvidenziate = new ArrayList<>();
                if (thisGame != null) {
                    String name = thisGame.getName();
                    if (name != null) {
                        //textGameName.setText(getString(R.string.gara) + name);
                    }
                    // Controlla se sei l’host
                    isHost = thisGame.getHost().equals(MyApplication.getUid());
                    overlay.setPlayers(thisGame.getPlayersList());

                    trackName = thisGame.getTrack();
                    if ((trackName != null) && (!trackName.isEmpty())) {
                        if (GameManager.getTrackMap() == null)
                            GameManager.loadTrackMap(trackName, CurrentGameActivity.this);
                        loadTrack();
                    }

                    currentPlayer = GameManager.nextTurn(thisGame.getPlayersList(), thisGame.getCurrentTurn(), true);

                    if (currentPlayer != null) {
                        // imposto tutte le celle occupate dai giocatori
                        celleOccupate.clear();
                        for (PlayerClass p : thisGame.getPlayersList())
                            celleOccupate.add(GameManager.getTrackCellAt(p.row, p.column));
                        overlay.setSelectedPlayer(currentPlayer);
                        marciaSelezionata = currentPlayer.gear;

                        // 👉 Se è un bot e tocca a lui, lo facciamo tirare
                        if (currentPlayer.isBot()) {
                            // Qualsiasi client può far tirare un bot se roll ancora assente
                            int diceValue = GameManager.botAI.tiraDado(currentPlayer, GameManager.getTrackMap().getCells(), celleOccupate);
                            TrackCell arrivo = GameManager.botAI.scegliArrivoBot(currentPlayer, diceValue, GameManager.getTrackMap().getCells(), celleOccupate);
                            if (arrivo != null) {
                                currentPlayer.row = arrivo.getRow();
                                currentPlayer.column = arrivo.getColumn();
                                thisGame.updatePlayerArrayByUid(currentPlayer).updatePlayerMapFromArrayList();
                                dbManager.updateGame(thisGame);
                            } else {
                                // NESSUNA POSIZIONE DI ARRIVO DOPO IL TIRO... IN TEORIA VUOL DIRE CHE IL BOT E' FUORI...
                            }
                        } else {
                            if (!currentPlayer.uid.equals(MyApplication.getUid())) {
                                if (currentPlayer.status.equals("rolled")) {
                                    celleEvidenziate = new ArrayList<>(GameManager.calcolaTuttiGliArrivi(GameManager.getTrackCellAt(currentPlayer.row, currentPlayer.column), currentPlayer.roll, celleOccupate));
                                }
                                MainActivity.checkAndNotify(gameCode, "&fun=rollToGrid&toUid=" + currentPlayer.uid);
                            } else {
                                /*Map<TrackCell.Direction, TrackCell> opzioni = GamesManager.calcolaArriviPossibili(GamesManager.getCellForPlayer(currentPlayer,thisGame.getTrackMap().getCells()), tiroDado, thisGame.getTrackMap().getCells());
                                List<TrackCell> evidenziate = new ArrayList<>(opzioni.values());
                                overlay.setHighlightCells(evidenziate);*/
                            }
                        }
                        updatePlayerCardContainer();
                        updateBoardOverlay();
                    } else {
                        endTurn();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        dbManager.listenToGame(eventListener);
        trackName = null;
        imageView.setOnTouchListener((v, event) -> {
            // Dopo ogni gesto touch, aggiorniamo la matrice
            TrackCell selected = overlay.clickOnArrival(event);
            if (selected != null) {
                TrackCell previous = GameManager.getTrackCellAt(currentPlayer.row, currentPlayer.column); // ad esempio, salvata prima del movimento

                if (previous.getItemType() == CURVE &&
                        selected.getItemType() != CURVE) {

                    if (currentPlayer.getCurveStops() < previous.getRequiredStops()) {
                        // Penalizza o avvisa!
                        //currentPlayer.applyPenalty("Curva non rispettata");
                    }

                    currentPlayer.resetCurveStops(); // esce dalla curva, reset
                }
                if (selected.getItemType() == CURVE) {
                    currentPlayer.incrementCurveStops();
                } else {
                    currentPlayer.resetCurveStops(); // se esce dalla curva, resetta il conteggio
                }
                currentPlayer.row = selected.getRow();
                currentPlayer.column = selected.getColumn();
                overlay.clearHighlightCells();
                overlay.clearSelectedPlayer();
                thisGame.updatePlayerMapFromArrayList();
                dbManager.updateGame(thisGame);
            } else overlay.setTransformMatrix(imageView.getImageMatrixCopy());
            return false; // oppure true se gestisci tu i tocchi
        });

        brakesBoxContainer = findViewById(R.id.brakeBoxes);
        tiresBoxContainer = findViewById(R.id.tireBoxes);
        fuelBoxContainer = findViewById(R.id.fuelBoxes);
        bodyBoxContainer = findViewById(R.id.bodyBoxes);
        engineBoxContainer = findViewById(R.id.engineBoxes);
        gears = findViewById(R.id.imageGears);
        gears.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int totalGears = 6;
                float width = v.getWidth();
                float touchX = event.getX();

                int gearIndex = (int) (touchX / (width / totalGears)); // da 0 a 5
                marciaSelezionata = gearIndex + 1;

                if (isAllowed(marciaSelezionata)) {
                    onMarciaSelezionata(marciaSelezionata);
                }
            }
            return true;
        });
        dice = findViewById(R.id.imageDice);
        dice.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int diceValue = DiceManager.tiraDado(marciaSelezionata);
                currentPlayer.roll = diceValue;
                currentPlayer.gear = marciaSelezionata;
                currentPlayer.status = "rolled";
                dbManager.updatePlayerWithTransaction(currentPlayer);
                updatePlayerCardContainer();
                NumberPopup.showNumber(this, String.valueOf(diceValue));
                handler.postDelayed(() -> {
                    celleEvidenziate = new ArrayList<>(GameManager.calcolaTuttiGliArrivi(GameManager.getTrackCellAt(currentPlayer.row, currentPlayer.column), diceValue, celleOccupate));
                    for (TrackCell destinazione : celleEvidenziate) {
                        List<TrackCell> path = GameManager.botAI.simulaPercorso(GameManager.getTrackCellAt(currentPlayer.row, currentPlayer.column), diceValue, GameManager.getTrackMap().getCells());
                        boolean penalized = curvaNonRispettata(path, currentPlayer);
                        //int brakes = stimaFreni(path, currentPlayer);
                        //int tires = stimaGomme(path, currentPlayer);

                        //overlay.highlightInfos.add(new GameBoardOverlay.HighlightInfo(destinazione, penalized, brakes, tires));
                    }

                    updateBoardOverlay();
                }, 1000);
            }
            return true;
        });
    }

    private void removeListener() {
        if (eventListener != null) {
            dbManager.removeListener(eventListener);
            eventListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        removeListener();
        super.onDestroy();
    }

    private void loadTrack() {
        if ((trackName != null) && (!trackName.isEmpty())) {
            // Imposta immagine della pista
            String imageFileName = GameManager.getTrackMap().getImagePath(); // es: "track_background.png"
            File imageFile = new File(MainActivity.getLocalPath(this), imageFileName);

            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
                imageView.post(() -> {
                    // Imposta scala minima in base a 80% altezza
                    int viewWidth = imageView.getWidth();
                    int viewHeight = imageView.getHeight();
                    float imgW = bitmap.getWidth();
                    float imgH = bitmap.getHeight();

                    float minZoom = Math.max(viewWidth / imgW, (viewHeight) / imgH);
                    imageView.setMinZoom(minZoom);

                    overlay.setCells(GameManager.getTrackMap().getCells());
                    overlay.setTransformMatrix(imageView.getImageMatrixCopy());
                });
            } else {
                Log.e("TrackLoad", "Image not found: " + imageFile.getAbsolutePath());
            }
        }
    }

    private void endTurn() {
        handler.postDelayed(() -> {
            GameManager.sortByPositionAndUpdate(thisGame.getPlayersList(), false);
            thisGame.updatePlayerMapFromArrayList().newTurn();
            dbManager.updateGame(thisGame);
        }, 1500);
    }

    public void highlightGear(int gearIndex) {
        View highlight = findViewById(R.id.gearHighlight);

        // Assumi che le marce siano 6, distribuite equamente in larghezza
        int totalGears = 6;

        gears.post(() -> {
            int totalWidth = gears.getWidth();
            int gearWidth = totalWidth / totalGears;

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    gearWidth,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            params.leftMargin = (gearIndex - 1) * gearWidth;
            highlight.setLayoutParams(params);
            highlight.setVisibility(View.VISIBLE);
        });
    }

    private boolean isAllowed(int marcia) {
        // 1. Memorizza la marcia nel player
        int currGear = currentPlayer.getGear();
        if (currGear == marcia) return true;
        if (currentPlayer.turn == 0) {
            if (marcia == 1) return true;
        } else {
            if (marcia > currGear + 1) return false;
            if (marcia > currGear) return true;
            else {
                if ((currGear - marcia) == 1) return true;
                if ((currGear - marcia) == 2) return currentPlayer.getRemainingBrakes() > 0;
                if ((currGear - marcia) == 3)
                    return (currentPlayer.getRemainingBrakes() > 0 && currentPlayer.getRemainingEngine() > 0);
                //if ((currGear - marcia) <= (currentPlayer.getRemainigBrakes())) return true;
            }
        }
        return false;
    }

    private void onMarciaSelezionata(int marcia) {
        // 1. Memorizza la marcia nel player
        //currentPlayer.setGear(marcia);
        //thisGame.updatePlayerArrayByUid(currentPlayer);
        //dbManager.updateGame(thisGame);
        int currGear = currentPlayer.getGear();
        tempBrakes = 0;
        tempEngine = 0;
        if ((currGear - marcia) == 2)
            tempBrakes = 1;
        if ((currGear - marcia) == 3) {
            tempBrakes = 1;
            tempEngine = 1;
        }
        updatePlayerCardContainer();
    }


    public void updateIndicator(LinearLayout boxContainer, int totalBoxes, int usedBoxes, Drawable lastBox) {
        boxContainer.removeAllViews();
        for (int i = 0; i < totalBoxes; i++) {
            View box = new View(this);
            int size = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(4, 0, 4, 0);
            box.setLayoutParams(params);

            if (i < usedBoxes) {
                box.setBackgroundResource(R.drawable.box_crossed);
            } else {
                if ((i == (totalBoxes - 1)) && (lastBox != null)) box.setBackground(lastBox);
                else box.setBackgroundResource(R.drawable.box_empty);
            }

            boxContainer.addView(box);
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        MainActivity.toGameLobby(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        overlay.invalidate();
    }

    private void updatePlayerCardContainer() {
        textPlayerName.setText(getString(R.string.giocatore) + currentPlayer.getName());
        textPlayerLap.setText(getString(R.string.giro) + currentPlayer.lap);
        textPlayerCurveStops.setText(getString(R.string.stops) + currentPlayer.curveStops);
        highlightGear(marciaSelezionata);
        if (marciaSelezionata != 0) {
            dice.setImageDrawable(getDrawable(DICE_IMAGE.get(marciaSelezionata)));
        }
        dice.setEnabled(true);
        dice.setAlpha(1f);
        gears.setEnabled(true);
        gears.setAlpha(1f);
        if(currentPlayer.status.equals("rolled")){
            dice.setEnabled(false);
            dice.setAlpha(0.4f);
            gears.setEnabled(false);
            gears.setAlpha(0.4f);
        }
        updateIndicator(tiresBoxContainer, PlayerClass.maxTires, currentPlayer.tires + tempTires, getDrawable(R.drawable.box_spin));
        updateIndicator(brakesBoxContainer, PlayerClass.maxBrakes, currentPlayer.brakes + tempBrakes, null);
        updateIndicator(fuelBoxContainer, PlayerClass.maxFuel, currentPlayer.fuel, null);
        updateIndicator(bodyBoxContainer, PlayerClass.maxBody, currentPlayer.body, getDrawable(R.drawable.box_explosion));
        updateIndicator(engineBoxContainer, PlayerClass.maxEngine, currentPlayer.engine + tempEngine, getDrawable(R.drawable.box_explosion));
    }

    private void updateBoardOverlay() {
        if (!celleEvidenziate.isEmpty()) overlay.setHighlightCells(celleEvidenziate);
    }

    private boolean curvaNonRispettata(List<TrackCell> path, PlayerClass player) {
        for (int i = 1; i < path.size(); i++) {
            TrackCell prev = path.get(i - 1);
            TrackCell curr = path.get(i);

            if (prev.getItemType() == CURVE && curr.getItemType() != CURVE) {
                // uscita dalla curva
                return player.getCurveStops() < prev.getRequiredStops();
            }
        }
        return false;
    }
}
