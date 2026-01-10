package com.example.dartscounter.gamemodes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.dartscounter.BaseActivity;
import com.example.dartscounter.R;
import com.example.dartscounter.data.ClassicGamePlayer;
import com.example.dartscounter.data.Player;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayDeque;

public class Classic extends BaseActivity {

    // Intent keys
    public static final String EXTRA_DOUBLE_OUT = "DoubleOut";
    public static final String EXTRA_PLAYER_COUNT = "PlayerCount";
    public static final String EXTRA_START_SCORE = "StartScore";
    public static final String EXTRA_PLAYER_NAMES = "PlayerNames";

    private int multiplier = 1; // 1=single, 2=double, 3=triple

    private boolean doubleOut = true;
    private boolean gameFinished = false;

    private Button btnDouble, btnTriple, btnBack;
    private Button btnKeepPlaying, btnEnd;
    private TextView textView;
    private LinearLayout playersContainer;
    private GridLayout endContainer;
    private ConstraintLayout keypadContainer;
    private ScrollView playersScroll;

    private ClassicGamePlayer[] players;
    private PlayerCard[] cards;

    // Current turn tracking (per player, so we can show all players’ turn UI correctly)
    private int currentPlayerIndex = 0;
    private int[] dartIndex;            // 1..3 for each player
    private int[] turnTotal;            // total scored this turn for each player
    private String[][] turnMarks;       // "T20", "D10", "5" for each dart slot, per player
    private boolean[] turnBusted;       // if turn ended in bust, show "BUST"

    // Undo
    private final ArrayDeque<GameSnapshot> undoStack = new ArrayDeque<>();
    private static final int MAX_UNDO = 60;

    private static final int[] NUMBER_IDS = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn10,
            R.id.btn11, R.id.btn12, R.id.btn13, R.id.btn14, R.id.btn15,
            R.id.btn16, R.id.btn17, R.id.btn18, R.id.btn19, R.id.btn20,
            R.id.btn25
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classic);
        enableImmersive();

        textView = findViewById(R.id.textView);
        playersContainer = findViewById(R.id.playersContainer);
        endContainer = findViewById(R.id.endContainer);
        keypadContainer = findViewById(R.id.keypadContainer);
        playersScroll = findViewById(R.id.playersScroll);
        if (playersContainer == null) {
            throw new IllegalStateException("playersContainer not found in activity_classic.xml");
        }

        endContainer.setVisibility(View.GONE);

        // Read extras
        Intent intent = getIntent();
        doubleOut = intent.getBooleanExtra(EXTRA_DOUBLE_OUT, true);
        int playerCount = intent.getIntExtra(EXTRA_PLAYER_COUNT, 2);
        int startScore = intent.getIntExtra(EXTRA_START_SCORE, 501);
        String[] names = intent.getStringArrayExtra(EXTRA_PLAYER_NAMES);


        players = createPlayers(playerCount, startScore, names);

        // Init per-player turn state
        int n = players.length;
        dartIndex = new int[n];
        turnTotal = new int[n];
        turnMarks = new String[n][3];
        turnBusted = new boolean[n];
        for (int i = 0; i < n; i++) {
            resetTurnState(i); // start empty for everyone
        }

        // Buttons
        btnDouble = findViewById(R.id.btnDouble);
        btnTriple = findViewById(R.id.btnTriple);
        btnBack   = findViewById(R.id.btnBack);

        btnDouble.setOnClickListener(v -> multiplier = 2);
        btnTriple.setOnClickListener(v -> multiplier = 3);
        btnBack.setOnClickListener(v -> onBackPressedAction());

        btnKeepPlaying = findViewById(R.id.btnKeepPlaying);
        btnEnd = findViewById(R.id.btnEnd);

        btnKeepPlaying.setOnClickListener(v -> {
            gameFinished = false;
            endContainer.setVisibility(View.GONE);
            keypadContainer.setVisibility(View.VISIBLE);
        });

        btnEnd.setOnClickListener(v-> finish());

        // Number buttons
        for (int id : NUMBER_IDS) {
            Button b = findViewById(id);
            b.setOnClickListener(v -> {
                if (gameFinished) return;
                int value = 0;

                String buttonText = b.getText().toString();
                if(buttonText.contains(getString(R.string.bullseye))){
                    value = 25;
                }else if(!buttonText.contains(getString(R.string.miss))){
                    value = Integer.parseInt(b.getText().toString());
                }

                // Standard darts: no triple bull (25)
                if (value == 25 && multiplier == 3) {
                    Toast.makeText(this, "Triple 25 not allowed", Toast.LENGTH_SHORT).show();
                    multiplier = 1;
                    return;
                }

                addScore(value);
                multiplier = 1;
            });
        }

        // UI cards
        renderPlayerCards();
        // Start with player 0: their turn should be empty
        startTurnForPlayer(0);
        refreshAllCardsFromState();
    }

    // ---------------------------
    // Players / UI
    // ---------------------------

    private ClassicGamePlayer[] createPlayers(int count, int startScore, @Nullable String[] names) {
        if (count < 1) count = 1;

        ClassicGamePlayer[] arr = new ClassicGamePlayer[count];
        for (int i = 0; i < count; i++) {
            String name = (names != null && i < names.length && names[i] != null && !names[i].trim().isEmpty())
                    ? names[i].trim()
                    : "Player " + (i + 1);
            arr[i] = new ClassicGamePlayer(i, name, startScore);
        }
        return arr;
    }

    private void renderPlayerCards() {
        playersContainer.removeAllViews();

        cards = new PlayerCard[players.length];
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < players.length; i++) {
            View cardView = inflater.inflate(R.layout.classic_player_card, playersContainer, false);
            cards[i] = new PlayerCard(cardView);
            playersContainer.addView(cardView);
        }
    }

    private void refreshAllCardsFromState() {
        for (int i = 0; i < players.length; i++) {
            // name + total score
            cards[i].txtName.setText(players[i].getName());
            cards[i].txtScore.setText(String.valueOf(players[i].getScore()));

            // turn cells from state (for ALL players)
            cards[i].txtTurn1.setText(nullToEmpty(turnMarks[i][0]));
            cards[i].txtTurn2.setText(nullToEmpty(turnMarks[i][1]));
            cards[i].txtTurn3.setText(nullToEmpty(turnMarks[i][2]));

            if (turnBusted[i]) {
                String bust = "Bust";
                cards[i].txtTurnTotal.setText(bust);
            } else {
                // show empty when 0 and no darts thrown yet
                boolean noDarts = isEmpty(turnMarks[i][0]) && isEmpty(turnMarks[i][1]) && isEmpty(turnMarks[i][2]);
                cards[i].txtTurnTotal.setText(noDarts ? "" : String.valueOf(turnTotal[i]));
            }
        }

        highlightCurrentPlayer();
    }

    private void highlightCurrentPlayer() {
        for (int i = 0; i < cards.length; i++) {
            MaterialCardView card = cards[i].root;
            if (i == currentPlayerIndex) {
                card.setStrokeWidth(4);
                card.setStrokeColor(getColor(android.R.color.holo_green_light));
            } else {
                card.setStrokeWidth(2);
                card.setStrokeColor(getColor(android.R.color.darker_gray));
            }
        }
    }

    private void startTurnForPlayer(int index) {
        currentPlayerIndex = index;
        // reset THIS player's turn UI/state to empty (your requirement)
        resetTurnState(index);
        players[index].startNewTurn();

        scrollToCurrentPlayerIfNeeded();

        textView.setText(players[index].getName() + "'s turn");
        refreshAllCardsFromState();
    }

    private void scrollToCurrentPlayerIfNeeded() {
        if (cards == null || cards.length == 0 || playersScroll == null) return;

        final View cardView = cards[currentPlayerIndex].root;

        playersScroll.post(() -> {
            int scrollY = playersScroll.getScrollY();
            int visibleTop = scrollY;
            int visibleBottom = scrollY + playersScroll.getHeight();

            int cardTop = cardView.getTop();
            int cardBottom = cardView.getBottom();

            int padding = (int) (12 * getResources().getDisplayMetrics().density);

            boolean fullyVisible = cardTop >= visibleTop && cardBottom <= visibleBottom;
            if (fullyVisible) return;

            // 🔽 Scroll so the card's bottom lines up with ScrollView bottom
            int targetScrollY = cardBottom - playersScroll.getHeight() + padding;

            playersScroll.smoothScrollTo(0, Math.max(0, targetScrollY));
        });
    }





    private void nextPlayer() {
        int next = currentPlayerIndex + 1;
        if (next >= players.length) next = 0;
        currentPlayerIndex = next;

        if(players[next].getScore() == 0){
            nextPlayer();
            return;
        }

        startTurnForPlayer(next);
    }

    private void resetTurnState(int i) {
        dartIndex[i] = 1;
        turnTotal[i] = 0;
        turnMarks[i][0] = "";
        turnMarks[i][1] = "";
        turnMarks[i][2] = "";
        turnBusted[i] = false;
    }

    // ---------------------------
    // Scoring
    // ---------------------------

    private void addScore(int value) {
        // Save state for undo BEFORE changing anything
        pushUndoSnapshot();

        ClassicGamePlayer p = players[currentPlayerIndex];

        int dart = dartIndex[currentPlayerIndex]; // 1..3

        String mark = (multiplier == 2 ? "D" : multiplier == 3 ? "T" : "") + value;
        int scored = value * multiplier;

        int newScore = p.getScore() - scored;

        boolean bust = newScore < 0
                || (doubleOut && newScore == 1)
                || (doubleOut && newScore == 0 && multiplier != 2);

        // Put dart mark in state (even if bust, user sees what was thrown)
        if (dart == 1) turnMarks[currentPlayerIndex][0] = mark;
        if (dart == 2) turnMarks[currentPlayerIndex][1] = mark;
        if (dart == 3) turnMarks[currentPlayerIndex][2] = mark;

        if (bust) {
            p.wentBust();
            turnBusted[currentPlayerIndex] = true;

            String bustText = p.getName() + " went bust!";
            textView.setText(bustText);
            refreshAllCardsFromState();

            // Bust ends turn immediately
            nextPlayer();
            return;
        }

        // Valid throw
        p.setScore(newScore);
        turnTotal[currentPlayerIndex] += scored;

        // Win
        if (newScore == 0) {
            textView.setText(p.getName() + " Wins!");
            refreshAllCardsFromState();
            nextPlayer();

            int activePlayers = 0;
            for (ClassicGamePlayer player : players){
                if(player.getScore() != 0) activePlayers++;
            }
            if(activePlayers < 2) btnKeepPlaying.setVisibility(View.GONE);

            endContainer.setVisibility(View.VISIBLE);
            keypadContainer.setVisibility(View.GONE);

            return;
        }

        // Advance dart
        dartIndex[currentPlayerIndex]++;
        refreshAllCardsFromState();

        // Turn ends after 3 darts
        if (dartIndex[currentPlayerIndex] > 3) {
            nextPlayer();
        }
    }

    // ---------------------------
    // Undo (BACK)
    // ---------------------------

    private void onBackPressedAction() {
        if (undoStack.isEmpty()) {
            Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
            return;
        }
        GameSnapshot s = undoStack.pop();
        restoreSnapshot(s);
        Toast.makeText(this, "Undid last dart", Toast.LENGTH_SHORT).show();
    }

    private void pushUndoSnapshot() {
        if (players == null || cards == null) return;

        GameSnapshot s = new GameSnapshot();
        s.multiplier = multiplier;
        s.currentPlayerIndex = currentPlayerIndex;
        s.gameFinished = gameFinished;
        s.headerText = textView != null ? textView.getText().toString() : "";

        int n = players.length;
        s.scores = new int[n];
        s.scoreBeforeTurn = new int[n];

        s.dartIndex = new int[n];
        s.turnTotal = new int[n];
        s.turnBusted = new boolean[n];
        s.turnMarks = new String[n][3];

        for (int i = 0; i < n; i++) {
            s.scores[i] = players[i].getScore();
            s.scoreBeforeTurn[i] = players[i].getScoreBeforeTurn();

            s.dartIndex[i] = dartIndex[i];
            s.turnTotal[i] = turnTotal[i];
            s.turnBusted[i] = turnBusted[i];

            s.turnMarks[i][0] = turnMarks[i][0];
            s.turnMarks[i][1] = turnMarks[i][1];
            s.turnMarks[i][2] = turnMarks[i][2];
        }

        undoStack.push(s);
        while (undoStack.size() > MAX_UNDO) undoStack.removeLast();
    }

    private void restoreSnapshot(GameSnapshot s) {
        multiplier = s.multiplier;
        currentPlayerIndex = s.currentPlayerIndex;
        gameFinished = s.gameFinished;

        if (textView != null) textView.setText(s.headerText);

        for (int i = 0; i < players.length; i++) {
            players[i].restore(s.scores[i], s.scoreBeforeTurn[i]);

            dartIndex[i] = s.dartIndex[i];
            turnTotal[i] = s.turnTotal[i];
            turnBusted[i] = s.turnBusted[i];

            turnMarks[i][0] = s.turnMarks[i][0];
            turnMarks[i][1] = s.turnMarks[i][1];
            turnMarks[i][2] = s.turnMarks[i][2];
        }

        refreshAllCardsFromState();
        scrollToCurrentPlayerIfNeeded();
    }

    private static class GameSnapshot {
        int multiplier;
        int currentPlayerIndex;
        boolean gameFinished;
        String headerText;

        int[] scores;
        int[] scoreBeforeTurn;

        int[] dartIndex;
        int[] turnTotal;
        boolean[] turnBusted;
        String[][] turnMarks;
    }

    // ---------------------------
    // Helpers / View Cache
    // ---------------------------

    private static class PlayerCard {
        final MaterialCardView root;
        final TextView txtScore, txtName, txtTurn1, txtTurn2, txtTurn3, txtTurnTotal;

        PlayerCard(View v) {
            root = (MaterialCardView) v;
            txtScore = v.findViewById(R.id.txtScore);
            txtName = v.findViewById(R.id.txtName);
            txtTurn1 = v.findViewById(R.id.txtTurn1);
            txtTurn2 = v.findViewById(R.id.txtTurn2);
            txtTurn3 = v.findViewById(R.id.txtTurn3);
            txtTurnTotal = v.findViewById(R.id.txtTurnTotal);
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
