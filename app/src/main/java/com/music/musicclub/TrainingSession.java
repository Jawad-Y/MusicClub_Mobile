package com.music.musicclub;

import org.json.JSONObject;

public class TrainingSession {
    private int id;
    private String subject;
    private String date;
    private String startTime;
    private String endTime;
    private String location;
    private String description;

    // Trainer info
    private int trainerId;
    private String trainerName;
    private String trainerEmail;
    private String trainerPhone;

    // Attendance fields
    private int attendanceTotal;
    private int attendancePresent;
    private int attendanceAbsent;
    private int attendanceLate;

    public TrainingSession() {}

    public TrainingSession(int id, String subject, String date, String startTime,
                           String endTime, String location, String description,
                           int trainerId, String trainerName, String trainerEmail, String trainerPhone,
                           int attendanceTotal, int attendancePresent, int attendanceAbsent, int attendanceLate) {
        this.id = id;
        this.subject = subject;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.description = description;
        this.trainerId = trainerId;
        this.trainerName = trainerName;
        this.trainerEmail = trainerEmail;
        this.trainerPhone = trainerPhone;
        this.attendanceTotal = attendanceTotal;
        this.attendancePresent = attendancePresent;
        this.attendanceAbsent = attendanceAbsent;
        this.attendanceLate = attendanceLate;
    }

    // --- Getters ---
    public int getId() { return id; }
    public String getSubject() { return subject; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public int getTrainerId() { return trainerId; }
    public String getTrainerName() { return trainerName; }
    public String getTrainerEmail() { return trainerEmail; }
    public String getTrainerPhone() { return trainerPhone; }
    public int getAttendanceTotal() { return attendanceTotal; }
    public int getAttendancePresent() { return attendancePresent; }
    public int getAttendanceAbsent() { return attendanceAbsent; }
    public int getAttendanceLate() { return attendanceLate; }

    // --- Setters ---
    public void setId(int id) { this.id = id; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setDate(String date) { this.date = date; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setLocation(String location) { this.location = location; }
    public void setDescription(String description) { this.description = description; }
    public void setTrainerId(int trainerId) { this.trainerId = trainerId; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }
    public void setTrainerEmail(String trainerEmail) { this.trainerEmail = trainerEmail; }
    public void setTrainerPhone(String trainerPhone) { this.trainerPhone = trainerPhone; }
    public void setAttendanceTotal(int attendanceTotal) { this.attendanceTotal = attendanceTotal; }
    public void setAttendancePresent(int attendancePresent) { this.attendancePresent = attendancePresent; }
    public void setAttendanceAbsent(int attendanceAbsent) { this.attendanceAbsent = attendanceAbsent; }
    public void setAttendanceLate(int attendanceLate) { this.attendanceLate = attendanceLate; }

    // --- fromJson helper ---
    public static TrainingSession fromJson(JSONObject obj) {
        int id = obj.optInt("id", -1);
        String subject = obj.optString("subject", "");
        String rawDate = obj.optString("date", "");
        String startTime = obj.optString("start_time", "");
        String endTime = obj.optString("end_time", "");
        String location = obj.optString("location", "");
        String description = obj.optString("description", "");

        // Clean date
        String dateOnly = rawDate.contains("T") ? rawDate.split("T")[0]
                : rawDate.contains(" ") ? rawDate.split(" ")[0] : rawDate;

        // Trainer object
        JSONObject trainerObj = obj.optJSONObject("trainer");
        int trainerId = trainerObj != null ? trainerObj.optInt("id", -1) : -1;
        String trainerName = trainerObj != null ? trainerObj.optString("full_name", "") : "";
        String trainerEmail = trainerObj != null ? trainerObj.optString("email", "") : "";
        String trainerPhone = trainerObj != null ? trainerObj.optString("phone", "") : "";

        // Attendance
        int attendanceTotal = obj.optInt("attendance_total", 0);
        int attendancePresent = obj.optInt("attendance_present", 0);
        int attendanceAbsent = obj.optInt("attendance_absent", 0);
        int attendanceLate = obj.optInt("attendance_late", 0);

        return new TrainingSession(id, subject, dateOnly, startTime, endTime,
                location, description,
                trainerId, trainerName, trainerEmail, trainerPhone,
                attendanceTotal, attendancePresent, attendanceAbsent, attendanceLate);
    }

    // --- toJson helper ---
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("subject", subject);
            obj.put("date", date);
            obj.put("start_time", startTime);
            obj.put("end_time", endTime);
            obj.put("location", location);
            obj.put("description", description);
            obj.put("trainer_id", trainerId); // send trainer ID back to API
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }
}