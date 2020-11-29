/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainWindow;

import Entities.Account;
import Entities.GSMUtil;
import Entities.Options;
import Window.PreferenceWindow;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import Window.SMSContainer;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JOptionPane;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.ini4j.Ini;

/**
 *
 * @author rexen
 */
public class MainWindow extends javax.swing.JFrame implements GSMUtil.NewDataListener, SMSContainer.smsListener, PreferenceWindow.preferenceListener {

    private final long SECONDS = 1000;
    private final long MINUTES = 60000;
    private final long HOURS = 3600000;
    private final String serviceAccountDir = "service-key.json";
    private String activationMessage;
    private boolean autolog;
    javax.swing.JFrame frame;
    GSMUtil gsmUtil;
    List<Account> pendingAccounts;
    boolean started = false;
    Timer timer;
    static boolean timerStarted = false;

    /**
     * Creates new form MainWindow
     */
    public MainWindow() {
        initComponents();
        DefaultCaret caret = (DefaultCaret) text_log.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        initFirebase();
        if(initializeSettings())
            logText("Settings file found. Preferences Applied.");
        gsmUtil = new GSMUtil(this);
    }
    
    private boolean initializeSettings(){
        try {
            File f = new File("settings.ini");
            if(f.isFile()) {
                Ini ini = new Ini(f);
                Options.setAutoLog(ini.get("General", "AutoLog", boolean.class));
                Options.setActivationMessage(ini.get("General", "ActivationMessage", String.class));
                Options.setInterval(ini.get("Interval", "Hours", int.class), ini.get("Interval", "Minutes", int.class), ini.get("Interval", "Seconds", int.class));
                if(Options.getAutoLog() && (Options.getHours() > 0 || Options.getMinutes() > 0 || Options.getSeconds() > 0))
                    startAutoLog();
                return true;
            } else {
                return false;
            }
            
        } catch (IOException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    private void startAutoLog() {
        timer = new Timer();
        timerStarted = true;
        long interval = Options.getHours()*HOURS + Options.getMinutes()*MINUTES + Options.getSeconds()*SECONDS;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (timerStarted) {
                    saveLogToFile(getDate(), false);
                }
            }
        }, interval, interval);
    }
    
    private void stopAutoLog() {
        if(timerStarted) {
            timer.cancel();
            timer.purge();
        }
        timerStarted = false;
    }

    private void initFirebase() {
        InputStream serviceAccount;
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            serviceAccount = classLoader.getResourceAsStream(serviceAccountDir);

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://azizelmer-f6f1f.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);
            logText("Connection to database successful");
            pendingAccounts = new ArrayList<>();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("account");
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot ds) {
                    
                    // Keep track of unactivated accounts on startup
                    if(!started){
                        for(DataSnapshot snap : ds.getChildren()){
                            if(!snap.getValue(Account.class).isActivated() && snap.getValue(Account.class).isOperator()) 
                                pendingAccounts.add(snap.getValue(Account.class));
                        }
                        logText("List of unactivated operators: ");
                        for(Account acc : pendingAccounts){
                            text_log.append(acc.getUsername() + "\n");
                        }
                        started = true;
                    } else {
                        for(DataSnapshot snap : ds.getChildren()){
                            if (!snap.getValue(Account.class).isActivated()) {
                                if(snap.getValue(Account.class).isOperator()){
                                    if(pendingAccounts.isEmpty()) {
                                        pendingAccounts.add(snap.getValue(Account.class));
                                        logText(snap.getValue(Account.class).getUsername() + "'s account has been deactivated!");
                                    } else {
                                        boolean trigger = false;
                                        for(Account acc : pendingAccounts){
                                            if(snap.getValue(Account.class).getUsername().equals(acc.getUsername())){
                                                trigger = true;
                                            }
                                        }
                                        if (!trigger) {
                                            pendingAccounts.add(snap.getValue(Account.class));
                                            logText(snap.getValue(Account.class).getUsername() + "'s account has been deactivated!");
                                        }
                                    }
                                    
                                }
                            } else {
                                for(Account acc : pendingAccounts){
                                    if(snap.getValue(Account.class).getUsername().equals(acc.getUsername())){
                                        if(snap.getValue(Account.class).isActivated() && !acc.isActivated()){
                                            pendingAccounts.remove(acc);
                                            logText(acc.getUsername() + "'s account has been activated!");
                                            notifyUser(acc);
                                        }
                                    }
                                }
                            }
                        }
                            
                    }
                    
          
                }

                @Override
                public void onCancelled(DatabaseError de) {
                }
            });
            
        } catch (IOException e) {
            logText("ERROR CONNECTING TO DATABASE!");
            System.out.println("There seems to be an error...");
        }
    }
    
    public void notifyUser(Account user){
        if(gsmUtil.isRunning()) {
            logText("Notifying " + user.getUsername() + "...\nSending notification to the number: " + user.getSim1());
            gsmUtil.sendMessage(Options.getActivationMessage(), user.getSim1());
        } else {
            logText("GSM Server has not been started.\nCannot notify " + user.getUsername() + " that his/her account has been activated");
        }
    }

    public String getTimestamp() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return formatter.format(date);
    }
    
    public String getDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void logText(String text) {
        text_log.append(getTimestamp() + " (SYSTEM): ");
        text_log.append(text);
        text_log.append("\n");
    }

    public void logTextGSM(String text) {
        text_log.append(getTimestamp() + " (GSM): ");
        text_log.append(text);
        text_log.append("\n");
    }

    public void logTextRecord(String number, String ph_level, String salinity, String temperature, String do_level) {
        text_log.append("Sender Number: " + number);
        text_log.append("\n");
        text_log.append("pH Level: " + ph_level);
        text_log.append("\n");
        text_log.append("Salinity: " + salinity + "‰");
        text_log.append("\n");
        text_log.append("Temperature: " + temperature + "K");
        text_log.append("\n");
        text_log.append("DO_Level: " + do_level + "mg/L");
        text_log.append("\n");
    }
    
    private void saveLogToFile(String name, boolean append){
        String fileName = name + ".txt";
        String directoryName = System.getProperty("user.dir").concat("\\logs");
        File directory = new File(directoryName);
        if (!directory.exists()){
            directory.mkdir();
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(directoryName + "/" + fileName, append))){
            text_log.write(out);
            logText("Log saved as " + directoryName + "\\" +  fileName);
        } catch (IOException e){
            System.err.println("Error occurred: " + e);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        text_log = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("GSM Server");
        setBackground(new java.awt.Color(255, 255, 255));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setMaximumSize(new java.awt.Dimension(586, 411));
        setMinimumSize(new java.awt.Dimension(586, 411));
        setPreferredSize(new java.awt.Dimension(586, 411));
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setForeground(java.awt.Color.white);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        jLabel1.setText("GSM STATUS:");

        jPanel4.setBackground(java.awt.Color.white);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        jLabel2.setForeground(java.awt.Color.red);
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("STOPPED");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 49, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(64, 64, 64))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(12, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jButton1.setBackground(java.awt.Color.lightGray);
        jButton1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton1.setText("Start");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setBackground(java.awt.Color.lightGray);
        jButton2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton2.setText("Stop");
        jButton2.setEnabled(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setBackground(java.awt.Color.lightGray);
        jButton3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton3.setText("Clear Log");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setBackground(java.awt.Color.lightGray);
        jButton4.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton4.setText("Exit");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        text_log.setEditable(false);
        text_log.setColumns(20);
        text_log.setFont(new java.awt.Font("Consolas", 0, 12)); // NOI18N
        text_log.setLineWrap(true);
        text_log.setRows(5);
        jScrollPane1.setViewportView(text_log);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jMenu1.setText("Tools");

        jMenuItem1.setText("Send SMS");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setText("Save Log");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem3.setText("Options");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        Thread t = new Thread(() -> {
            if (!gsmUtil.StartGSM()) {
                logText("GSM Module not detected");
            } else {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        jButton1.setEnabled(false);
                        jButton2.setEnabled(true);
                        jLabel2.setText("RUNNING");
                        jLabel2.setForeground(Color.green);

                    });
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
                logText("Server Started!");
                logText("Checking Signal Strength...");
                gsmUtil.writeGSM("AT+CSQ");
                logText("Setting GSM to RECEIVING MODE...");
                gsmUtil.setReceivingMode();
            }
        });
        t.start();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        logText("Closing Port...");
        gsmUtil.StopGSM();
        do {
        } while (gsmUtil.portIsOpen());
        jButton2.setEnabled(false);
        jButton1.setEnabled(true);
        jLabel2.setText("STOPPED");
        jLabel2.setForeground(Color.red);
        logText("Server Stopped!");
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        text_log.setText("");
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        if (gsmUtil.portIsOpen()) {
            logText("Closing Port...");
            gsmUtil.StopGSM();
            do {
            } while (gsmUtil.portIsOpen());
        }
        saveLogToFile("log", true);
        System.exit(0);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        if (gsmUtil.isRunning()) {
            new SMSContainer(this).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "GSM Server not running", "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        String fileName = JOptionPane.showInputDialog(this, "Enter File Name");
        saveLogToFile(fileName, false);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        new PreferenceWindow(this).setVisible(true);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainWindow().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea text_log;
    // End of variables declaration//GEN-END:variables

    @Override
    public void onNewData(String data) {
        logTextGSM(data);
    }

    @Override
    public void onNewRecord(String number, String ph_level, String salinity, String temperature, String do_level) {
        logTextRecord(number, ph_level, salinity, temperature, do_level);
    }

    @Override
    public void onSMSReceived(String data) {
        logTextGSM("--SMS Received--" + data);
        logText("Checking if number exists in database...");
    }

    @Override
    public void appendLog(String data) {
        logText(data);
    }

    @Override
    public void onSend(String number, String message) {
        logText("Sending Message to " + number + "\nMessage: " + message);
        gsmUtil.sendMessage(message, number);
    }

    @Override
    public void onSave() {
        logText("Settings Changed");
        if (Options.getAutoLog()) {
            if (Options.getHours() > 0 || Options.getMinutes() > 0 || Options.getSeconds() > 0) {
                stopAutoLog();
                startAutoLog();
            }
        } else {
            stopAutoLog();
        }
    }

    @Override
    public void onReset() {
        
    }

}
