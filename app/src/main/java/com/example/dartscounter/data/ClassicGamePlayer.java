package com.example.dartscounter.data;

public class ClassicGamePlayer {

    private final int id;
    private final String name;

    private int score;
    private int scoreBeforeTurn;

    public ClassicGamePlayer(int id, String name, int score) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.scoreBeforeTurn = score;
    }

    public int getId() { return id; }

    public String getName() { return name; }

    public int getScore() { return score; }

    public void setScore(int score) { this.score = score; }

    public void startNewTurn() {
        scoreBeforeTurn = score;
    }

    public void wentBust() {
        score = scoreBeforeTurn;
    }

    public int getScoreBeforeTurn() {
        return scoreBeforeTurn;
    }

    public void restore(int score, int scoreBeforeTurn) {
        this.score = score;
        this.scoreBeforeTurn = scoreBeforeTurn;
    }

}
