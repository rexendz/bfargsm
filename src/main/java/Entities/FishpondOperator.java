package Entities;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FishpondOperator {

    private String firstname;
    private String middlename;
    private String lastname;
    private long fla_number;
    private String fishpond_size;
    private String cityProvince;
    private String barangay;
    private String municipality;
    private String issuance_date;
    private String expiration_date;
    private String sim1;
    private String sim2;
    private boolean isActive;

    private FishpondRecord[] fishpondRecords;

    public FishpondOperator() {

    }

    public FishpondRecord[] getFishpondRecords() {
        return fishpondRecords;
    }

    public FishpondOperator(Map<String, Object> data){
        this.firstname = (String) data.get("firstname");
        this.middlename = (String) data.get("middlename");
        this.lastname = (String) data.get("lastname");
        this.fla_number = (long) data.get("fla_number");
        this.fishpond_size = (String) data.get("fishpond_size");
        this.cityProvince = (String) data.get("cityProvince");
        this.barangay = (String) data.get("barangay");
        this.municipality = (String) data.get("municipality");
        this.issuance_date = (String) data.get("issuance_date");
        this.expiration_date = (String) data.get("expiration_date");
        this.sim1 = (String) data.get("sim1");
        this.sim2 = (String) data.get("sim2");
        this.isActive = (boolean) data.get("isActive");
    }

    public void setFishpondRecords(FishpondRecord[] fishpondRecords) {
        this.fishpondRecords = fishpondRecords;
    }

    public String getSim1() {
        return sim1;
    }

    public void setSim1(String sim_number) {

        this.sim1 = sim_number;
    }

    public String getSim2() {
        return sim2;
    }

    public void setSim2(String sim_number) {

        this.sim2 = sim_number;
    }

    public boolean isIsActive() {
        java.util.Date now = new java.util.Date();
        java.util.Date exp_date = new Date(expiration_date);
        return exp_date.compareTo(now)>0;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getAddress(){
        if(municipality.equals("No Municipality")){
            return String.format("%s, %s", barangay, cityProvince);
        } else {
            return String.format("%s, %s, %s", barangay, municipality, cityProvince);
        }
    }

    public String getFullName(){
        return firstname + " " +middlename+" "+lastname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getMiddlename() {
        return middlename;
    }

    public void setMiddlename(String middlename) {
        this.middlename = middlename;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public long getFla_number() {
        return fla_number;
    }

    public void setFla_number(long fla_number) {
        this.fla_number = fla_number;
    }

    public String getFishpond_size() {
        return fishpond_size;
    }

    public void setFishpond_size(String fishpond_size) {
        this.fishpond_size = fishpond_size;
    }

    public void setCityProvince(String cityprovince) {
        if(cityprovince==null||cityprovince.isEmpty()){
            this.cityProvince = "";
        }else{
            this.cityProvince = cityprovince;
        }
    }

    public void setMunicipality(String municipality) {
        if(municipality==null||municipality.isEmpty()){
            this.municipality = "";
        }else{
            this.municipality = municipality;
        }
    }

    public String getIssuance_date() {
        return issuance_date;
    }

    public void setIssuance_date(String issuance_date) {
        this.issuance_date = issuance_date;
    }

    public String getExpiration_date() {
        return expiration_date;
    }

    public void setExpiration_date(String expiration_date) {
        this.expiration_date = expiration_date;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getCityProvince() {
        return cityProvince;
    }

    public String getMunicipality() {
        return municipality;
    }

}
