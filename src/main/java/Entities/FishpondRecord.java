package Entities;


import java.util.HashMap;
import java.util.Map;

public class FishpondRecord {

    private float ph_level;
    private float salinity;
    private float temperature;
    private float do_level;
    private String sim_number;
    private Long timestamp;

    public Long getTimestamp() { return timestamp;}

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSim_number() {
        return sim_number;
    }

    public void setSim_number(String sim_number) {
        this.sim_number = sim_number;
    }

    public float getPh_level() {
        return ph_level;
    }

    public void setPh_level(float ph_level) {
        this.ph_level = ph_level;
    }

    public float getSalinity() {
        return salinity;
    }

    public void setSalinity(float salinity) {
        this.salinity = salinity;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getDo_level() {
        return do_level;
    }

    public void setDo_level(float do_level) {
        this.do_level = do_level;
    }


}
