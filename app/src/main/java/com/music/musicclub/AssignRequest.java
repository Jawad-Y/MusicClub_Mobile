package com.music.musicclub;

public class AssignRequest {
    private int instrument_id;
    private int user_id;

    public AssignRequest(int instrument_id, int user_id) {
        this.instrument_id = instrument_id;
        this.user_id = user_id;
    }
}
