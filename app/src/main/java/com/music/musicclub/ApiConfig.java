package com.music.musicclub;
public class ApiConfig {

    public static final String BASE_URL = "http://192.168.16.243:8000/api/";

    // Auth
    public static final String LOGIN = BASE_URL + "login";
    public static final String LOGOUT = BASE_URL + "logout";
    public static final String ME = BASE_URL + "me";


    public static final String USERS = BASE_URL + "users";
    public static final String DEPARTMENTS = BASE_URL + "departments";
    public static final String MY_CLASSES = BASE_URL + "myclasses";
    public static final String TRAINING_SESSIONS = BASE_URL + "training-sessions";

    public static final String ATTENDANCE = BASE_URL + "session-attendances";
}
