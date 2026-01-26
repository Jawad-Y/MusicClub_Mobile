package com.music.musicclub;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiService {

    private final Context appContext;
    private final String token; // Sanctum personal access token (Bearer)

    public ApiService(Context context, String token) {
        this.appContext = context.getApplicationContext();
        this.token = token;
    }

    // LOGIN: no Authorization header
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

        // optional but helpful: timeouts / retries
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000, // 15s
                1,     // retries
                1.0f
        ));

        VolleySingleton.getInstance(context).add(request);
    }

    // Authenticated endpoints
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

        Response.ErrorListener wrappedErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    // Clear token
                    new LoginManager(appContext).clear();
                    // Redirect to login
                    Intent intent = new Intent(appContext, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    appContext.startActivity(intent);
                    // Do not call original errorListener to avoid further handling
                } else {
                    errorListener.onErrorResponse(error);
                }
            }
        };

        JsonObjectRequest request = new JsonObjectRequest(
                method,
                url,
                body,
                listener,
                wrappedErrorListener
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");

                // Only add Authorization if token exists
                if (!TextUtils.isEmpty(token)) {
                    headers.put("Authorization", "Bearer " + token);
                }

                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000, // 15s
                1,     // retries
                1.0f
        ));

        VolleySingleton.getInstance(appContext).add(request);
    }
}
