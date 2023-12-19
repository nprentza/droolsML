package org.nprentza;

public class Agent {
    private int id;
    private AgentRole role;    // admin, guest
    private String experience;
    private int age;
    private boolean grantAccess;

    public static Agent fromRawData(int id, String role, String experience, int age){
        AgentRole agentRole = AgentRole.valueOf(role.toUpperCase());
        return new Agent(id, agentRole, experience,age);
    }

    private Agent(int id, AgentRole role, String experience, int age){
        this.id = id;
        this.role = role;
        this.experience = experience;
        this.age = age;
    }

    public int getId(){return this.id;}

    public void setRole(AgentRole role){this.role=role;}
    public AgentRole getRole(){return this.role;}

    public void setExperience(String experience) {this.experience = experience;}
    public String getExperience(){return this.experience;}

    public void setGrantAccess(boolean access){this.grantAccess=access;}
    public boolean getGrandAccess(){return this.grantAccess;}

    public void setAge(int age){this.age=age;}
    public int getAge(){return this.age;}
}
