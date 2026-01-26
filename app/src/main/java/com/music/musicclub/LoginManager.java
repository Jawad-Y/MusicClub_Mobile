package com.music.musicclub;
import android.content.Context;
import android.content.SharedPreferences;

public class LoginManager {
        private static final String PREF = "app_pref";
        private static final String KEY_TOKEN = "token";

        private final SharedPreferences sp;

        public LoginManager(Context ctx) {
            sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        }

        public void saveToken(String token) {
            sp.edit().putString(KEY_TOKEN, token).apply();
        }

        public String getToken() {
            return sp.getString(KEY_TOKEN, null);
        }

        public void clear() {
            sp.edit().clear().apply();
        }

}
