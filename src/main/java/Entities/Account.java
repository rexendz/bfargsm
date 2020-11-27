package Entities;

import java.util.Map;


public class Account{
    private String firstname;
    private String middlename;
    private String lastname;
    private String username;
    private String password;
    private boolean activated;
    private boolean admin;
    private boolean operator;
    private long fla_number;
    private String sim1;
    private String sim2;
    private String activated_by;
    private String made_admin_by;
    private String removed_admin_by;
    private String deactivated_by;

    public Account(){
        this.activated = false;
        this.admin = false;
        this.activated_by = "NONE";
        this.made_admin_by = "NONE";
        this.removed_admin_by = "NONE";
        this.fla_number = -25565;
        this.sim1 = "NONE";
        this.sim2 = "NONE";
        this.deactivated_by = "NONE";
    }

    public Account(Map<String, Object> data){
        this.firstname = (String) data.get("firstname");
        this.middlename = (String) data.get("middlename");
        this.lastname = (String) data.get("lastname");
        this.username = (String) data.get("username");
        this.fla_number = (long) data.get("fla_number");
        this.activated_by = (String) data.get("activated_by");
        this.made_admin_by = (String) data.get("made_admin_by");
        this.removed_admin_by = (String) data.get("removed_admin_by");
        this.deactivated_by = (String) data.get("deactivated_by");
        this.password = (String) data.get("password");
        this.activated = (boolean) data.get("activated");
        this.admin = (boolean) data.get("admin");
        this.operator = (boolean) data.get("operator");
        this.sim1 = (String) data.get("sim1");
        this.sim2 = (String) data.get("sim2");
    }

    public boolean isOperator() {
        return operator;
    }

    public boolean isActivated() {
        return activated;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setSim1(String sim1) {
        this.sim1 = sim1;
    }

    public void setSim2(String sim2) {
        this.sim2 = sim2;
    }

    public String getSim1() {
        return sim1;
    }

    public String getSim2() {
        return sim2;
    }

    public void setOperator(boolean operator){
        this.operator = operator;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getActivated_by() {
        return activated_by;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }
    
    public String getFullname(){
        return firstname + " " + middlename + " " + lastname;
    }

    public String getMade_admin_by() {
        return made_admin_by;
    }

    public String getMiddlename() {
        return middlename;
    }

    public String getRemoved_admin_by() {
        return removed_admin_by;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setActivated_by(String activated_by) {
        this.activated_by = activated_by;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setMade_admin_by(String made_admin_by) {
        this.made_admin_by = made_admin_by;
    }

    public void setMiddlename(String middlename) {
        this.middlename = middlename;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRemoved_admin_by(String removed_admin_by) {
        this.removed_admin_by = removed_admin_by;
    }


    public void setUsername(String username) {
        this.username = username;
    }

    public long getFla_number() {
        return fla_number;
    }

    public void setFla_number(long fla_number) {
        this.fla_number = fla_number;
    }

    public String getDeactivated_by() {
        return deactivated_by;
    }

    public void setDeactivated_by(String deactivated_by) {
        this.deactivated_by = deactivated_by;
    }
}
