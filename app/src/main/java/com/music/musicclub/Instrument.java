package com.music.musicclub;

public class Instrument {

    private int id;
    private String name;
    private String uniqueCode;
    private int instrumentTypeId;

    public Instrument(int id, String name, String uniqueCode, int instrumentTypeId) {
        this.id = id;
        this.name = name;
        this.uniqueCode = uniqueCode;
        this.instrumentTypeId = instrumentTypeId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUniqueCode() {
        return uniqueCode;
    }

    public int getInstrumentTypeId() {
        return instrumentTypeId;
    }

    public void setName(String name) {
        this.name = name;
    }
}
