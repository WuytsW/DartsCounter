package com.example.dartscounter.data;


import java.io.Serializable;

public class Player implements Serializable {
    private String name;
    private long lastUsed; // 0 = never used

    public Player(){}

    public Player(String name) {
        this.name = name;
        this.lastUsed = 0;
    }

    public String getName() {
        return name;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
}

