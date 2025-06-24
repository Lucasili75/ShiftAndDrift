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
    GamesManager gamesManager;
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
        gamesManager = new GamesManager(this, gameCode);
        MainActivity.gamePrefs = getSharedPreferences("ShiftAndDriftPrefs", MODE_PRIVATE);
        MainActivity.loadParms();
        eventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                thisGame = snapshot.getValue(GameClass.class);
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
                        thisGame.setTrackMap((gamesManager.getTrackMap() == null) ? gamesManager.loadTrackMap(thisGame.getTrack()) : gamesManager.getTrackMap());
                        loadTrack();
                    }

                    currentPlayer = gamesManager.nextTurn(thisGame.getPlayersList(), thisGame.getCurrentTurn(), true);

                    if (currentPlayer != null) {
                        overlay.setSelectedPlayer(currentPlayer);
                        marciaSelezionata = currentPlayer.gear;
                        updatePlayerCardContainer();

                        // 👉 Se è un bot e tocca a lui, lo facciamo tirare
                        if (currentPlayer.isBot()) {
                            // Qualsiasi client può far tirare un bot se roll ancora assente
                            Set<TrackCell> occupate = new HashSet<>();
                            for (PlayerClass p : thisGame.getPlayersList())
                                occupate.add(GamesManager.getTrackCellAt(p.row, p.column));
                            int diceValue = gamesManager.botAI.tiraDado(currentPlayer, thisGame.getTrackMap().getCells(), occupate);
                            TrackCell arrivo = gamesManager.botAI.scegliArrivoBot(currentPlayer, diceValue, thisGame.getTrackMap().getCells(), occupate);
                            if (arrivo != null) {
                                currentPlayer.row = arrivo.getRow();
                                currentPlayer.column = arrivo.getColumn();
                                thisGame.updatePlayerByUid(currentPlayer);
                                thisGame.updatePlayerMapFromArrayList();
                                gamesManager.updateGame(thisGame);
                            } else {
                                // NESSUNA POSIZIONE DI ARRIVO DOPO IL TIRO... IN TEORIA VUOL DIRE CHE IL BOT E' FUORI...
                            }
                        } else {
                            if (currentPlayer.gear != 0) {
                                dice.setImageDrawable(getDrawable(DICE_IMAGE.get(currentPlayer.gear)));
                            }
                            if (!currentPlayer.uid.equals(MyApplication.getUid()))
                                MainActivity.checkAndNotify(gameCode, "&fun=rollToGrid&toUid=" + currentPlayer.uid);
                            else {
                                /*Map<TrackCell.Direction, TrackCell> opzioni = GamesManager.calcolaArriviPossibili(GamesManager.getCellForPlayer(currentPlayer,thisGame.getTrackMap().getCells()), tiroDado, thisGame.getTrackMap().getCells());
                                List<TrackCell> evidenziate = new ArrayList<>(opzioni.values());
                                overlay.setHighlightCells(evidenziate);*/
                            }
                        }
                    } else {
                        endTurn();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        gamesManager.listenToGame(eventListener);
        trackName = null;
        imageView.setOnTouchListener((v, event) -> {
            // Dopo ogni gesto touch, aggiorniamo la matrice
            TrackCell selected = overlay.clickOnArrival(event);
            if (selected != null) {
                TrackCell previous = GamesManager.getTrackCellAt(currentPlayer.row, currentPlayer.column); // ad esempio, salvata prima del movimento

                if (previous.getItemType() == CURVE &&
                        selected.getItemType() != CURVE) {

                    if (currentPlayer.getCurveStops() < previous.getRequiredStops()) {
                        // Penalizza o avvisa!
                        currentPlayer.applyPenalty("Curva non rispettata");
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
                currentPlayer.turn++;
                overlay.clearHighlightCells();
                overlay.clearSelectedPlayer();
                thisGame.updatePlayerMapFromArrayList();
                gamesManager.updateGame(thisGame);
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
                    highlightGear(marciaSelezionata);
                    onMarciaSelezionata(marciaSelezionata);
                }
            }
            return true;
        });
        dice = findViewById(R.id.imageDice);
        dice.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int diceValue = DiceManager.tiraDado(currentPlayer.gear);
                NumberPopup.showNumber(this, String.valueOf(diceValue));
                handler.postDelayed(() -> {
                    Set<TrackCell> occupate = new HashSet<>();
                    for (PlayerClass p : thisGame.getPlayersList())
                        occupate.add(GamesManager.getTrackCellAt(p.row, p.column));
                    List<TrackCell> evidenziate = new ArrayList<>(GamesManager.calcolaTuttiGliArrivi(GamesManager.getTrackCellAt(currentPlayer.row, currentPlayer.column), diceValue, occupate));
                    for (TrackCell destinazione : evidenziate) {
                        List<TrackCell> path = gamesManager.botAI.simulaPercorso(GamesManager.getTrackCellAt(currentPlayer.row, currentPlayer.column), diceValue, gamesManager.getTrackMap().getCells());
                        boolean penalized = curvaNonRispettata(path, currentPlayer);
                        int brakes = stimaFreni(path, currentPlayer);
                        int tires = stimaGomme(path, currentPlayer);

                        overlay.highlightInfos.add(new GameBoardOverlay.HighlightInfo(destinazione, penalized, brakes, tires));
                    }

                    overlay.setHighlightCells(evidenziate);
                }, 1000);
            }
            return true;
        });
    }

    private void removeListener() {
        if (eventListener != null) {
            gamesManager.removeListener(eventListener);
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
            //thisGame.setTrackMap(gamesManager.loadTrackMap(trackName));
                /*TrackMap trackMap = TrackMap.loadTrackFromJson(trackName, this);

            if (trackMap != null) */
            {
                // Imposta immagine della pista
                String imageFileName = thisGame.getTrackMap().getImagePath(); // es: "track_background.png"
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

                        overlay.setCells(thisGame.getTrackMap().getCells());
                        overlay.setTransformMatrix(imageView.getImageMatrixCopy());
                    });
                } else {
                    Log.e("TrackLoad", "Image not found: " + imageFile.getAbsolutePath());
                }
            } /*else{
                Toast.makeText(this, "Errore nel caricamento della mappa", Toast.LENGTH_SHORT).show();
            }*/
        }
    }

    private void endTurn() {
        handler.postDelayed(() -> {
            removeListener();
            gamesManager.sortByPositionAndUpdate(thisGame.getPlayersList(), false);
            gamesManager.updateGame(thisGame);
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
                if ((currGear - marcia) == 2) return currentPlayer.getRemainigBrakes() > 0;
                if ((currGear - marcia) == 3)
                    return (currentPlayer.getRemainigBrakes() > 0 && currentPlayer.getRemainigEngine() > 0);
                //if ((currGear - marcia) <= (currentPlayer.getRemainigBrakes())) return true;
            }
        }
        return false;
    }

    private void onMarciaSelezionata(int marcia) {
        // 1. Memorizza la marcia nel player
        currentPlayer.setGear(marcia);
        thisGame.updatePlayerByUid(currentPlayer);
        gamesManager.updateGame(thisGame);
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
        updateIndicator(tiresBoxContainer, PlayerClass.maxTires, currentPlayer.tires + tempTires, getDrawable(R.drawable.box_spin));
        updateIndicator(brakesBoxContainer, PlayerClass.maxBrakes, currentPlayer.brakes + tempBrakes, null);
        updateIndicator(fuelBoxContainer, PlayerClass.maxFuel, currentPlayer.fuel, null);
        updateIndicator(bodyBoxContainer, PlayerClass.maxBody, currentPlayer.body, getDrawable(R.drawable.box_explosion));
        updateIndicator(engineBoxContainer, PlayerClass.maxEngine, currentPlayer.engine + tempEngine, getDrawable(R.drawable.box_explosion));
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
