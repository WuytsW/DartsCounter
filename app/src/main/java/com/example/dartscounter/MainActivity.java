package com.example.dartscounter;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.example.dartscounter.data.Player;
import com.example.dartscounter.data.PlayerStore;
import com.example.dartscounter.gamemodes.Classic;
import com.example.dartscounter.data.IntentKeys;
import com.example.dartscounter.gamemodes.Cricket;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    // Intent keys
    private LinearLayout playersContainer;
    private List<Player> players = new ArrayList<>();
    private static final String PREFS = "darts_prefs";
    private static final String KEY_SELECTED_PLAYERS = "selected_players";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        enableImmersive();

        playersContainer = findViewById(R.id.playersContainer);

        Button btnAddPlayer = findViewById(R.id.btnAddPlayer);
        btnAddPlayer.setOnClickListener(v -> showCreatePlayerDialog());

        // Initial load
        refreshPlayersUI();

        Button btnCricket1 = findViewById(R.id.btnCricket1);
        btnCricket1.setOnClickListener(v -> startCricket(1));

        Button btnCricket15 = findViewById(R.id.btnCricket15);
        btnCricket15.setOnClickListener(v -> startCricket(15));

        Button btnClassic501 = findViewById(R.id.btnClassic501);
        btnClassic501.setOnClickListener(v -> startClassic(501));

        Button btnClassic301 = findViewById(R.id.btnClassic301);
        btnClassic301.setOnClickListener(v -> startClassic(301));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPlayersUI();
    }

    private void refreshPlayersUI() {
        players = PlayerStore.loadPlayers(this);
        playersContainer.removeAllViews();

        if (players.isEmpty()) {
            EditText placeholder = new EditText(this);
            placeholder.setText("No players yet — tap 'Create new player'.");
            placeholder.setEnabled(false);
            placeholder.setBackground(null);
            placeholder.setPadding(0, 8, 0, 8);
            playersContainer.addView(placeholder);
            return;
        }

        players.sort((a, b) -> {
            long la = a.getLastUsed();
            long lb = b.getLastUsed();

            if (la != lb) {
                return Long.compare(lb, la); // newest first
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });

        java.util.Set<String> savedSelected = loadSelectedPlayers();

        for (Player p : players) {
            CheckBox cb = new CheckBox(this);
            cb.setText(p.getName());
            cb.setTag(p);

            cb.setChecked(savedSelected.contains(p.getName()));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> saveSelectedPlayers());

            playersContainer.addView(cb);
        }
    }


    private Player[] getSelectedPlayers(){
        ArrayList<Player> selected = new ArrayList<>();
        for (int i = 0; i < playersContainer.getChildCount(); i++) {
            if (playersContainer.getChildAt(i) instanceof CheckBox) {
                CheckBox cb = (CheckBox) playersContainer.getChildAt(i);
                if (cb.isChecked()) {
                    Player p = (Player) cb.getTag();
                    selected.add(p);
                }
            }
        }
        return selected.toArray(new Player[0]);
    }


    private String[] getPlayerNames(Player[] players) {
        ArrayList<String> names = new ArrayList<>();
        for (Player p: players) { names.add(p.getName());}
        return names.toArray(new String[0]);
    }

    private String[] getSelectedNamesAndMarkLastUsed(){
        long now = System.currentTimeMillis();
        Player[] selected = getSelectedPlayers();
        for (Player p: selected) {p.setLastUsed(now);}
        return getPlayerNames(selected);
    }

    private void startClassic(int startScore) {
        String[] names = getSelectedNamesAndMarkLastUsed();
        if (names.length == 0) {
            Toast.makeText(this, "Select at least 1 player", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, Classic.class);
        intent.putExtra(IntentKeys.EXTRA_DOUBLE_OUT, true);
        intent.putExtra(IntentKeys.EXTRA_PLAYER_COUNT, names.length);
        intent.putExtra(IntentKeys.EXTRA_START_SCORE, startScore);
        intent.putExtra(IntentKeys.EXTRA_PLAYER_NAMES, names);
        startActivity(intent);
    }


    private void startCricket(int minCricket){
        String[] names = getSelectedNamesAndMarkLastUsed();
        if (names.length == 0) {
            Toast.makeText(this, "Select at least 1 player", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, Cricket.class);
        intent.putExtra(IntentKeys.EXTRA_PLAYER_COUNT, names.length);
        intent.putExtra(IntentKeys.EXTRA_PLAYER_NAMES, names);
        intent.putExtra(IntentKeys.EXTRA_MIN_CRICKET, minCricket);
        startActivity(intent);
    }




    private void showCreatePlayerDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Player name");

        new AlertDialog.Builder(this)
                .setTitle("Create player")
                .setView(input)
                .setPositiveButton("Create", (d, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name can't be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PlayerStore.addPlayer(this, new Player(name));

                    // auto-select the new player too
                    java.util.Set<String> s = new java.util.HashSet<>(loadSelectedPlayers());
                    s.add(name);
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putStringSet(KEY_SELECTED_PLAYERS, s).apply();

                    refreshPlayersUI();

                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void saveSelectedPlayers() {
        ArrayList<String> selected = new ArrayList<>();
        for (int i = 0; i < playersContainer.getChildCount(); i++) {
            if (playersContainer.getChildAt(i) instanceof CheckBox) {
                CheckBox cb = (CheckBox) playersContainer.getChildAt(i);
                if (cb.isChecked()) selected.add(cb.getText().toString());
            }
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_SELECTED_PLAYERS, new java.util.HashSet<>(selected))
                .apply();
    }

    private java.util.Set<String> loadSelectedPlayers() {
        return getSharedPreferences(PREFS, MODE_PRIVATE)
                .getStringSet(KEY_SELECTED_PLAYERS, new java.util.HashSet<>());
    }

}
