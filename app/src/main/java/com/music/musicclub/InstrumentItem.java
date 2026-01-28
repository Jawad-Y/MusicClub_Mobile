package com.music.musicclub;

public class InstrumentItem {
    private int id;
    private String name;

    public InstrumentItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
