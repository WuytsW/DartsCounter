package com.example.dartscounter.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PlayerStore {

    private static final String PREFS = "dartscounter_prefs";
    private static final String KEY_PLAYERS = "players";

    private static final Gson gson = new Gson();

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Player> loadPlayers(Context ctx) {
        String json = prefs(ctx).getString(KEY_PLAYERS, null);
        if (json == null) return new ArrayList<>();

        Type type = new TypeToken<ArrayList<Player>>() {}.getType();
        List<Player> list = gson.fromJson(json, type);
        return (list != null) ? list : new ArrayList<>();
    }

    public static void savePlayers(Context ctx, List<Player> players) {
        String json = gson.toJson(players);
        prefs(ctx).edit().putString(KEY_PLAYERS, json).apply();
    }

    public static void addPlayer(Context ctx, Player p) {
        List<Player> players = loadPlayers(ctx);

        // Avoid duplicates by name (simple rule; you can change this later)
        for (Player existing : players) {
            if (existing.getName().equalsIgnoreCase(p.getName())) {
                return;
            }
        }

        players.add(p);
        savePlayers(ctx, players);
    }

    public static void deletePlayerByName(Context ctx, String name) {
        List<Player> players = loadPlayers(ctx);
        players.removeIf(p -> p.getName().equalsIgnoreCase(name));
        savePlayers(ctx, players);
    }

    public static void clear(Context ctx) {
        prefs(ctx).edit().remove(KEY_PLAYERS).apply();
    }
}
