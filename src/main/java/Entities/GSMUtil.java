/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Entities;

import com.fazecast.jSerialComm.*;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseReference.CompletionListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.ZonedDateTime;

/**
 *
 * @author rexen
 */
public class GSMUtil {

    private SerialPort comPort;
    private String gsmPort;
    private final String descriptor = "USB-SERIAL CH340";
    private NewDataListener myListener;
    private String[] smsData = {"NULL", "NULL"};

    public interface NewDataListener {

        public void onNewData(String data);

        public void appendLog(String data);

        public void onNewRecord(String number, String ph_level, String salinity, String temperature, String do_level);

        public void onSMSReceived(String data);
    }

    public GSMUtil(NewDataListener myListener) {
        this.myListener = myListener;
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            System.out.println(port.getDescriptivePortName());
            if (port.getDescriptivePortName().contains(descriptor)) {
                System.out.println("EUREKA!");
                gsmPort = port.getSystemPortName();
            }
        }
    }

    public void StartGSM() {
        comPort = SerialPort.getCommPort(gsmPort);
        System.out.println(comPort.getPortDescription());
        comPort.setBaudRate(9600);
        comPort.openPort();

        comPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    return;
                }
                byte[] newData = new byte[comPort.bytesAvailable()];
                int numRead = comPort.readBytes(newData, newData.length);
                String data = new String(newData, 0, numRead);
                System.out.println("Data: " + data);
                if (data.contains("+CMT") && smsData[0].equals("NULL")) {
                    smsData[0] = data;
                } else if (smsData[1].equals("NULL") && !smsData[0].equals("NULL")) {
                    smsData[1] = data;
                    System.out.println("Message: " + smsData[0] + smsData[1]);
                    readSMS(smsData[0] + smsData[1]);
                } else {
                    myListener.onNewData(data);
                }
            }

        });
    }

    public void readSMS(String data) {
        smsData = new String[2];
        myListener.onSMSReceived(data);
        String sender_number = data.substring(data.indexOf("\"") + 1, data.indexOf("\"", data.indexOf("\"") + 1));
        String recordData = data.substring(data.indexOf("\n", data.indexOf("\n") + 1)).trim();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("operator");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot ds) {
                boolean valid_number = false;
                String sendto_number = "ERROR";
                for (DataSnapshot snap : ds.getChildren()) {
                    if (sender_number.equals(snap.getValue(FishpondOperator.class).getSim2())) {
                        sendto_number = snap.getValue(FishpondOperator.class).getSim1();
                        valid_number = true;
                    }
                }
                if (valid_number) {
                    myListener.appendLog("Number is valid!");
                    myListener.appendLog("Calculating DO Level...");
                    GenerateData(sender_number, recordData, sendto_number);
                } else {
                    myListener.appendLog("Invalid Number");
                }
            }

            @Override
            public void onCancelled(DatabaseError de) {

            }

        });
    }

    public void GenerateData(String number, String data, String sendto_number) {
        float ph_level, salinity, temp;
        String[] recordData = data.split(",");
        ph_level = Float.parseFloat(recordData[0]);
        salinity = Float.parseFloat(recordData[1]);
        temp = Float.parseFloat(recordData[2]);
        float do_level = CalculateDO(temp, salinity);
        

        FishpondRecord record = new FishpondRecord();
        record.setPh_level(ph_level);
        record.setSalinity(salinity);
        record.setTemperature(temp);
        record.setDo_level(do_level);
        record.setSim_number(number);
        record.setTimestamp(ZonedDateTime.now().toInstant().toEpochMilli());
        myListener.onNewRecord(number, recordData[0], recordData[1], recordData[2], String.valueOf(do_level));

        myListener.appendLog("Saving record to database...");
        myListener.appendLog(saveToDatabase(record) ? "Record Saved Successfully!" : "Error Storing Record To Database!");
        
        myListener.appendLog("Sending data to " + sendto_number);
        myListener.appendLog(forwardMessage(record, sendto_number) ? "Data successfully forwarded" : "Error forwarding data");
        
        myListener.appendLog("Setting GSM to RECEIVING MODE...");
        setReceivingMode();
        
    }
    
    public boolean forwardMessage(FishpondRecord record, String number){
        writeGSM("AT+CMGF=1");
        writeGSM("AT+CMGS=\"" + number + "\"");
        byte[] b = (record.getSim_number() + ',' + record.getTimestamp() + ',' + record.getPh_level() + ',' + record.getSalinity() + ',' + record.getTemperature() + ',' + record.getDo_level()).getBytes();
        byte[] done = "\u001A".getBytes();
        comPort.writeBytes(b, b.length);
        comPort.writeBytes(done, done.length);
        return true;
    }

    public boolean saveToDatabase(FishpondRecord record) {
        String timestampString = String.valueOf(record.getTimestamp());
        String testKey = "";
        try {
            testKey = timestampString.substring(6, 13) + record.getSim_number().substring(6) + timestampString.substring(0, 6);
        } catch (java.lang.StringIndexOutOfBoundsException e) {
            return false;
        }
        final String uniqueKey = testKey;
        if (uniqueKey.isEmpty()) {
            return false;
        } else {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("fishpond_record");
            ref.child(uniqueKey).setValue(record, null);
            return true;
        }
    }

    public static float CalculateDO(float tmp, float sal) {
        float atm = 1;
        BigDecimal temp = BigDecimal.valueOf(tmp);
        BigDecimal a = new BigDecimal(-139.34411);
        BigDecimal b = new BigDecimal("1.57501e5").divide(temp, 8, BigDecimal.ROUND_HALF_UP);
        BigDecimal c = new BigDecimal("6.642308e7").divide(temp.pow(2), 8, BigDecimal.ROUND_HALF_UP);
        BigDecimal d = new BigDecimal("1.243800e10").divide(temp.pow(3), 8, BigDecimal.ROUND_HALF_UP);
        BigDecimal e = new BigDecimal("8.621949e11").divide(temp.pow(4), 8, BigDecimal.ROUND_HALF_UP);
        BigDecimal DO_Base = new BigDecimal(Math.exp(a.floatValue() + b.floatValue() - c.floatValue() + d.floatValue() - e.floatValue()));

        a = new BigDecimal(10.754).divide(temp, 8, BigDecimal.ROUND_HALF_UP);
        b = new BigDecimal(2140.7).divide(temp.pow(2), 8, BigDecimal.ROUND_HALF_UP);
        BigDecimal fs = new BigDecimal(Math.exp(-sal * (0.017674 - a.floatValue() + b.floatValue())));

        a = new BigDecimal(3840.70).divide(temp, 8, BigDecimal.ROUND_HALF_UP);
        b = new BigDecimal(216961).divide(temp.pow(2), 8, BigDecimal.ROUND_HALF_UP);
        c = new BigDecimal(Math.exp(11.8571 - a.floatValue() - b.floatValue()));
        BigDecimal tempo = new BigDecimal("1.426e-5").multiply(temp.subtract(BigDecimal.valueOf(273.15)));
        BigDecimal tempo2 = new BigDecimal("6.436e-8").multiply(temp.subtract(BigDecimal.valueOf(273.15)).pow(2));
        d = BigDecimal.valueOf(0.000975).subtract(tempo.add(tempo2));

        BigDecimal fp = ((BigDecimal.valueOf(atm).subtract(c)).multiply(BigDecimal.valueOf(1).subtract(d.multiply(BigDecimal.valueOf(atm))))).divide((BigDecimal.valueOf(1).subtract(c)).multiply(BigDecimal.valueOf(1).subtract(d)), 8, BigDecimal.ROUND_HALF_UP);

        BigDecimal dissolvedOxygen = DO_Base.multiply(fs.multiply(fp));
        System.out.println("DO: " + dissolvedOxygen);
        return dissolvedOxygen.floatValue();
    }

    public void writeGSM(String data) {
        try {
            byte[] b = (data + "\r\n").getBytes();
            comPort.writeBytes(b, b.length);
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(GSMUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setReceivingMode() {
        writeGSM("AT+CMGF=1");
        writeGSM("AT+CNMI=1,2,0,0,0");
        myListener.appendLog("Waiting for SMS...");
    }

    public void StopGSM() {
        comPort.closePort();
    }

    public boolean portIsOpen() {
        if (comPort == null) {
            return false;
        }
        return comPort.isOpen();
    }

}
