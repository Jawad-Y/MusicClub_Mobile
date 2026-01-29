package com.music.musicclub;
public class ApiConfig {

    public static final String BASE_URL = "http://192.168.187.89:8000/api/";

    // Auth
    public static final String LOGIN = BASE_URL + "login";
    public static final String LOGOUT = BASE_URL + "logout";
    public static final String ME = BASE_URL + "me";

    // Examples resources
    public static final String USERS = BASE_URL + "users";
    public static final String DEPARTMENTS = BASE_URL + "departments";
    public static final String INSTRUMENTS = BASE_URL + "instruments";
    public static final String INSTRUMENT_TYPES = BASE_URL + "instrument-types";
    public static final String ASSIGN_INSTRUMENT = BASE_URL + "instrument-assignments";



    public static final String MY_CLASSES = BASE_URL + "myclasses";
    public static final String TRAINING_SESSIONS = BASE_URL + "training-sessions";
    public static final String ATTENDANCE = BASE_URL + "/attendance";

}
