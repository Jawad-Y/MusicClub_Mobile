package com.music.musicclub;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TrainingSessionApi {
    private static final String TAG = "TrainingSessionsApi";
    public static final String ENDPOINT = ApiConfig.TRAINING_SESSIONS;

    public static List<TrainingSession> fetchTrainingSessions() throws Exception {
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL url = new URL(ENDPOINT);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP error: " + status);
            }

            is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            String json = sb.toString();
            JSONArray arr = null;
            json = (json == null) ? "" : json.trim();

            if (json.startsWith("[")) {
                arr = new JSONArray(json);
            } else if (json.startsWith("{")) {
                JSONObject root = new JSONObject(json);
                if (root.optJSONArray("data") != null) {
                    arr = root.optJSONArray("data");
                } else if (root.optJSONArray("sessions") != null) {
                    arr = root.optJSONArray("sessions");
                } else {
                    JSONArray names = root.names();
                    if (names != null) {
                        for (int i = 0; i < names.length(); i++) {
                            String key = names.optString(i);
                            Object o = root.opt(key);
                            if (o instanceof JSONArray) {
                                arr = (JSONArray) o;
                                break;
                            }
                        }
                    }
                }
            }

            List<TrainingSession> result = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null) {
                        result.add(TrainingSession.fromJson(obj));
                    }
                }
            } else {
                Log.w(TAG, "No sessions array found in response");
            }

            return result;
        } catch (Exception e) {
            Log.e(TAG, "fetchTrainingSessions error", e);
            throw e;
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
    }
}
