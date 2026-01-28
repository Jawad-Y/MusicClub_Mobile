package com.music.musicclub;

public class Department {

    private int id;
    private String departmentName;
    private int leaderId;
    private String leaderName;

    public Department(int id, String departmentName, int leaderId, String leaderName) {
        this.id = id;
        this.departmentName = departmentName;
        this.leaderId = leaderId;
        this.leaderName = leaderName;
    }

    public int getId() {
        return id;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public String getLeaderName() {
        return leaderName;
    }
}
