package com.example.dartscounter.data;

import android.provider.ContactsContract;

import java.util.Date;

public class Player {
    private String name;
    private Date lastUsed;

    public Player() {} // needed for Gson sometimes

    public Player(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Date getLastUsed() {
        return lastUsed;
    }
    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }
}
