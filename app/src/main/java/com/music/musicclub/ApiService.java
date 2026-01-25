package com.music.musicclub;
import android.content.Context;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class ApiService {

    private final Context context;
    private final String token; // Sanctum Token

    public ApiService(Context context, String token) {
        this.context = context;
        this.token = token;
    }

    public static void login(
            Context context,
            String email,
            String password,
            Response.Listener<JSONObject> listener,
            Response.ErrorListener errorListener
    ) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (Exception ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.LOGIN,
                body,
                listener,
                errorListener
        );

        VolleySingleton.getInstance(context).add(request);
    }

    public void me(Response.Listener<JSONObject> listener,
                   Response.ErrorListener errorListener) {
        request(Request.Method.GET, ApiConfig.ME, null, listener, errorListener);
    }

    public void logout(Response.Listener<JSONObject> listener,
                       Response.ErrorListener errorListener) {
        request(Request.Method.POST, ApiConfig.LOGOUT, null, listener, errorListener);
    }

    public void get(String url,
                    Response.Listener<JSONObject> listener,
                    Response.ErrorListener errorListener) {
        request(Request.Method.GET, url, null, listener, errorListener);
    }

    public void post(String url, JSONObject body,
                     Response.Listener<JSONObject> listener,
                     Response.ErrorListener errorListener) {
        request(Request.Method.POST, url, body, listener, errorListener);
    }

    public void put(String url, JSONObject body,
                    Response.Listener<JSONObject> listener,
                    Response.ErrorListener errorListener) {
        request(Request.Method.PUT, url, body, listener, errorListener);
    }

    public void delete(String url,
                       Response.Listener<JSONObject> listener,
                       Response.ErrorListener errorListener) {
        request(Request.Method.DELETE, url, null, listener, errorListener);
    }

    private void request(int method,
                         String url,
                         JSONObject body,
                         Response.Listener<JSONObject> listener,
                         Response.ErrorListener errorListener) {

        JsonObjectRequest request = new JsonObjectRequest(
                method,
                url,
                body,
                listener,
                errorListener
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        VolleySingleton.getInstance(context).add(request);
    }
}

