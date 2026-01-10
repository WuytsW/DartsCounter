package com.example.dartscounter.data;

public class CricketGamePlayer {

    private final int id;
    private final String name;
    private int score;

    private int[][] dartsInField;

    public CricketGamePlayer(int id, String name, int score, int n) {
        this.id = id;
        this.name = name;
        this.score = score;
        dartsInField = new int[n][3];
    }

    public int getId() { return id; }

    public String getName() { return name; }

    public int getScore() { return score; }

    public void setScore(int score) { this.score = score; }

    public int[][] getDartsInField() {
        return dartsInField;
    }

    public void setDartsInField(int[][] dartsInField) {
        this.dartsInField = dartsInField;
    }
}
