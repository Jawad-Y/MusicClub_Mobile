package com.music.musicclub;

import org.json.JSONArray;
import org.json.JSONObject;

public class User {
    private int id;
    private String name;
    private String role;
    private String status;

    public User(int id, String name, String role, String status) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    private static String extractStringSafely(JSONObject obj, String key) {
        if (obj == null) return "";
        Object o = obj.opt(key);
        if (o == null) return "";
        // If it's a plain string
        if (o instanceof String) return (String) o;
        // If it's a JSONObject, try common fields
        if (o instanceof JSONObject) {
            JSONObject ro = (JSONObject) o;
            String[] tryKeys = new String[]{"name", "title", "type", "role"};
            for (String k : tryKeys) {
                String v = ro.optString(k, null);
                if (v != null && !v.isEmpty()) return v;
            }
            // fallback to empty
            return "";
        }
        // If it's an array, try first object's name
        if (o instanceof JSONArray) {
            JSONArray arr = (JSONArray) o;
            if (arr.length() > 0) {
                JSONObject first = arr.optJSONObject(0);
                if (first != null) {
                    return extractStringSafely(first, "name");
                } else {
                    // maybe it's array of strings
                    Object firstVal = arr.opt(0);
                    if (firstVal instanceof String) return (String) firstVal;
                }
            }
        }
        // as a last resort call toString but trim to avoid dumping large JSON into UI
        String s = String.valueOf(o);
        if (s.length() > 200) return s.substring(0, 200) + "...";
        return s;
    }

    public static User fromJson(JSONObject obj) {
        int id = obj.optInt("id", -1);
        String name = obj.optString("name", obj.optString("full_name", ""));

        // role may be a string or object
        String role = extractStringSafely(obj, "role");
        if (role.isEmpty()) {
            // try other keys
            role = obj.optString("role_name", obj.optString("role_title", ""));
        }

        String status = extractStringSafely(obj, "status");
        if (status.isEmpty()) status = obj.optString("state", "");

        return new User(id, name, role, status);
    }
}
