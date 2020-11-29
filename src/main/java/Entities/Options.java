/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Entities;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Ini;

/**
 *
 * @author rexen
 */
public class Options {
    
    private static boolean autoLog = false;
    private static final String defaultActivationMessage = "[BFAR] This message is to notify you that your operator account in the BFAR System has been activated.\n\nPlease do not reply to this number.";
    private static String activationMessage;
    private static int HOURS = 0;
    private static int MINUTES = 0;
    private static int SECONDS = 0;
    
    public Options() {
        activationMessage = defaultActivationMessage;
    }
    
    public static void reset() {
        activationMessage = defaultActivationMessage;
        HOURS = 0;
        MINUTES = 0;
        SECONDS = 0;
        autoLog = false;
    }
    
    public static void setInterval(int hrs, int min, int sec) {
        HOURS = hrs;
        MINUTES = min;
        SECONDS = sec;
    }
    
    public static boolean getAutoLog() {
        return autoLog;
    }
    
    public static int getHours() {
        return HOURS;
    }
    
    public static int getMinutes() {
        return MINUTES;
    }
    
    public static int getSeconds() {
        return SECONDS;
    }
    
    public static void setAutoLog(boolean set){
        autoLog = set;
    }
    
    public static void setActivationMessage(String set) {
        activationMessage = set;
    }
    
    public static String getActivationMessage() {
        return activationMessage;
    }
    
    public static void saveIniFile() {
        try {
            File f = new File("settings.ini");
            if (!f.exists()) {
                f.createNewFile();
            }
            Ini ini = new Ini(f);
            ini.put("General", "AutoLog", autoLog);
            ini.put("General", "ActivationMessage", activationMessage);
            ini.put("Interval", "Hours", HOURS);
            ini.put("Interval", "Minutes", MINUTES);
            ini.put("Interval", "Seconds", SECONDS);
            ini.store();
        } catch (IOException ex) {
            Logger.getLogger(Options.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
