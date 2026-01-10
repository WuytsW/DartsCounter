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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

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

        Button btnClassic501 = findViewById(R.id.btnClassic501);
        btnClassic501.setOnClickListener(v -> startClassicWithSelectedPlayers(501));

        Button btnClassic301 = findViewById(R.id.btnClassic301);
        btnClassic301.setOnClickListener(v -> startClassicWithSelectedPlayers(301));
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

        java.util.Set<String> savedSelected = loadSelectedPlayers();
        for (Player p : players) {
            CheckBox cb = new CheckBox(this);
            cb.setText(p.getName());

            // restore checked state
            cb.setChecked(savedSelected.contains(p.getName()));

            // whenever user toggles, persist
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> saveSelectedPlayers());

            playersContainer.addView(cb);
        }

    }

    private String[] getSelectedPlayerNames() {
        ArrayList<String> selected = new ArrayList<>();
        for (int i = 0; i < playersContainer.getChildCount(); i++) {
            if (playersContainer.getChildAt(i) instanceof CheckBox) {
                CheckBox cb = (CheckBox) playersContainer.getChildAt(i);
                if (cb.isChecked()) selected.add(cb.getText().toString());
            }
        }
        return selected.toArray(new String[0]);
    }

    private void startClassicWithSelectedPlayers(int startScore) {
        String[] names = getSelectedPlayerNames();
        if (names.length == 0) {
            Toast.makeText(this, "Select at least 1 player", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, Classic.class);
        intent.putExtra(Classic.EXTRA_DOUBLE_OUT, true);
        intent.putExtra(Classic.EXTRA_PLAYER_COUNT, names.length);
        intent.putExtra(Classic.EXTRA_START_SCORE, startScore);
        intent.putExtra(Classic.EXTRA_PLAYER_NAMES, names);
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
