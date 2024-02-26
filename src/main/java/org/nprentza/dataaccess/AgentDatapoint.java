package org.nprentza.dataaccess;

import org.nprentza.BaseDatapoint;

public class AgentDatapoint extends BaseDatapoint {

    private String role;    // admin, guest
    private String experience;
    private int age;

    protected AgentDatapoint(int id, String classLabel) {
        super(id, classLabel);
    }

    private AgentDatapoint(int id, String role, String experience, int age, String classLabel){
        super(id, classLabel);
        this.role = role;
        this.experience = experience;
        this.age = age;
    }

    public static AgentDatapoint fromRawData(int id, String role, String experience, int age, String labelClass){
        return new AgentDatapoint(id, role, experience,age, labelClass);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String toString(){
        return id + ":" + role + "," + experience + "," + age;
    }
}
