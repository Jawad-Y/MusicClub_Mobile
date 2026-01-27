package com.music.musicclub;

public class Leader {
    private int id;
    private String fullName;

    public Leader(int id, String fullName) {
        this.id = id;
        this.fullName = fullName;
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
}
