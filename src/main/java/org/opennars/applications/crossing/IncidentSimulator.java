/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opennars.applications.crossing;

import com.google.common.io.Resources;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.opennars.applications.crossing.NarListener.Prediction;
import org.opennars.applications.gui.NarSimpleGUI;
import org.opennars.main.Nar;
import org.opennars.operator.mental.Want;

/**
 *
 * @author patha
 */
public class IncidentSimulator extends javax.swing.JFrame {

    public static String resourceFileContent(String resourceName) throws IOException {
        String content = "";
        URL n = Resources.getResource(resourceName);
        try {
            System.out.println(n.toURI().toString());
            URLConnection connection = n.openConnection();
            InputStream stream = connection.getInputStream();
            java.util.Scanner s = new java.util.Scanner(stream).useDelimiter("\\A");
            content = s.hasNext() ? s.next() : "";
        } catch (URISyntaxException ex) {
            Logger.getLogger(IncidentSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return content;
    }
    
    /**
     * Creates new form IncidentSimulator
     */
    public IncidentSimulator() {
        initComponents();
        String content = "";
        try {
            content = resourceFileContent("crossing_static_QA.nal");
        } catch (IOException ex) {
            Logger.getLogger(IncidentSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        jTextArea1.setText(content);
        try {
            content = resourceFileContent("crossing_realtime_QA.nal");
        } catch (IOException ex) {
            Logger.getLogger(IncidentSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        Crossing.questions = content;
        
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
              jLabel5.setText(Crossing.said);
            }
      }, 500, 500);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox2 = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton2 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        jButton1.setText("jButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jCheckBox1.setText("Car ignoring traffic light");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jCheckBox2.setText("Pedestrian ignoring traffic light");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });

        jLabel1.setText("Domain knowledge");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jButton2.setText("Analyze current situation");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel2.setText("IMG HERE");
        jLabel2.setToolTipText("");

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane2.setViewportView(jTextArea2);

        jLabel3.setText("Current events, including predictions and spatial relations");

        jLabel4.setText("Output:");

        jLabel5.setText("_");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBox2)
                            .addComponent(jLabel1)
                            .addComponent(jButton2)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jCheckBox1)
                                .addGap(213, 213, 213)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5)))
                        .addGap(0, 408, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBox1)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBox2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 391, Short.MAX_VALUE)
                .addGap(18, 18, 18))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        Entity.carIgnoreTrafficLight = jCheckBox1.isSelected();
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        Entity.pedestrianIgnoreTrafficLight = jCheckBox2.isSelected();
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    Nar newNar = null;
    NarSimpleGUI GUItoHide = null;
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        synchronized(Crossing.said) {
            Crossing.said = "";
        }
        jLabel5.setText("");
        try {
            Files.deleteIfExists(Paths.get(Crossing.snapshotName));
        } catch (IOException ex) {
            Logger.getLogger(IncidentSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Crossing.saveSnapshot = true;
            while(!(new File("current.png").exists()))
            {
                Thread.sleep(100);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(IncidentSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        BufferedImage img = null;
        while(img == null) {
            try {
                Thread.sleep(100); //make sure it's written, suffices for now
                img = ImageIO.read(new File("current.png"));
                Files.delete(Paths.get(Crossing.snapshotName));
            } catch (Exception ex) {
                img = null; //retry
            }
        }
        Image scaled = img.getScaledInstance(300, 255, Image.SCALE_DEFAULT);
        jLabel2.setText("");
        jLabel2.setIcon(new ImageIcon(scaled));
        jTextArea2.setText(Crossing.input_snapshot.replace(":|:", ""));
        String inputs = "";
        InformNARS informer = Crossing.cameras.get(0).informer; //assume predictions to be relative to camera0 for now, can be refined later
        HashSet<String> already_added = new HashSet<String>();
        for(Prediction p : Crossing.predictions_snapshot) {
            String position = informer.getPosition(p.ent.posX, p.ent.posY);
            String input = "";
            if(p.ent instanceof Pedestrian) {
                Pedestrian ped = (Pedestrian) p.ent;
                input = "<(*,"+ ped + "," + position + ") --> pedestrian_prediction>.";
            } 
            else
            if(p.ent instanceof Car) {
                Car car = (Car) p.ent;
                input = "<(*," + car + "," + position + ") --> car_prediction>.";
            }
            if(already_added.contains(input)) {
                continue;
            }
            already_added.add(input);
            inputs += input + "\n";
        }
        for(Entity e : Crossing.entitiesSnapshot) {
            for(Entity f : Crossing.entitiesSnapshot) {
                if(e != f) { //TODO sort according to x and y instead, and let reasoning exploit transitivity, this way we get back to linear scaling
                    if(e.posX < f.posX) {
                        inputs += "<(*,"+e+","+f+") --> left>. \n";
                    }
                    if(e.posX > f.posX) {
                        inputs += "<(*,"+f+","+e+") --> left>. \n";
                    }
                    if(e.posY < f.posY) {
                        inputs += "<(*,"+e+","+f+") --> above>. \n";
                    }
                    if(e.posY > f.posY) {
                        inputs += "<(*,"+f+","+e+") --> above>. \n";
                    }
                }
            }
        }
        jTextArea2.setText(jTextArea2.getText() + inputs);
        if(newNar != null) {
            newNar.stop();
            newNar = null;
            GUItoHide.hide();
            GUItoHide.dispose();
            GUItoHide = null;
        }
        try {
            newNar = new Nar(); //using same Nar as Crossing is also a possibility
            newNar.narParameters.VOLUME = 0;
        } catch (Exception ex) {
            Logger.getLogger(IncidentSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        newNar.addPlugin(new Crossing.Say());
        newNar.addPlugin(new Want());
        newNar.addInput(jTextArea2.getText());
        newNar.addInput(jTextArea1.getText());
        GUItoHide = new NarSimpleGUI(newNar);
        GUItoHide.show();
        newNar.start();
    }//GEN-LAST:event_jButton2ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    // End of variables declaration//GEN-END:variables
}
