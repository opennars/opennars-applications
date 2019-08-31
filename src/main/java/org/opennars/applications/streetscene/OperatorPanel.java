/*
 * The MIT License
 *
 * Copyright 2019 OpenNARS.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.opennars.applications.streetscene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import org.opennars.entity.Task;
import org.opennars.io.Narsese;
import org.opennars.io.Parser;
import org.opennars.io.events.EventEmitter;
import org.opennars.io.events.Events;
import org.opennars.io.events.Events.TaskAdd;
import org.opennars.language.Term;
import org.opennars.main.Nar;

/**
 *
 * @author tc
 */
public class OperatorPanel extends javax.swing.JFrame {

    public static boolean showPredictions = true;
    public Nar nar;
    /**
     * Creates new form OperatorPanel
     */
    public OperatorPanel(Nar nar) {
        initComponents();
        this.nar = nar;
        OperatorPanel.NarListener handler = new OperatorPanel.NarListener();
        nar.on(Events.Answer.class, handler);
        String ontology = VisualReasonerHeadless.trafficMultiNar.informQaNar.ontology;
        String knowledge = ontology.split("//Anomaly ontology:")[1].split("//Motivations:")[0].trim();
        String motivations = ontology.split("//Motivations:")[1].split("//Questions:")[0].trim();
        String questions = ontology.split("//Questions:")[1].split(">>LocationNar:")[0].trim();
        jTextArea3.setText(knowledge);
        jTextArea4.setText(motivations);
        jTextArea1.setText(questions);
        KnowledgeToCrossing();
    }
    
    String narText = "";
    public class NarListener implements EventEmitter.EventObserver {  
        @Override
        public void event(Class event, Object[] args) {
            synchronized(narText) {
                boolean changed = false;
                if (event == Events.Answer.class) {
                    Task t = (Task) args[0];
                    if (t.isInput()) {
                        //if(Math.abs(nar.time() - t.sentence.getOccurenceTime()) < 10000) { //not too long ago //outcommented as system reports newest result anyway
                            for(int i = 0; i<filter.size(); i++) {
                                if(filter.get(i).equals(t.getTerm())) {
                                    narText = "Q"+i+": "+t.getBestSolution() + " (frame=" + VisualReasonerHeadless.i + ")\n" + narText;
                                    if(narText.length() > 2000) {
                                        narText = "";
                                    }
                                }
                            }
                            changed = true;
                        //}
                    }
                }
                int maxChars = Integer.MAX_VALUE; //200
                if(changed) {
                    if(narText.length() > maxChars) {
                        narText = narText.substring(narText.length() / 2, narText.length());
                    }
                    jTextArea2.setText(narText);
                }
            }
        }
    }
    List<Term> filter = new ArrayList<Term>();
    public void KnowledgeToCrossing() {
        filter.clear();
        String narsese = jTextArea3.getText() + "\n" + jTextArea1.getText() + "\n10\n"+jTextArea4.getText();
        if(!jTextArea1.getText().trim().isEmpty()) {
            String[] lines = jTextArea1.getText().split("\n");
            for(String l : lines) {
                if(l.isEmpty()) {
                    continue;
                }
                Task t = null;
                try {
                    t = new Narsese(nar).parseTask(l);
                } catch (Parser.InvalidInputException ex) {
                    Logger.getLogger(OperatorPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(t != null) {
                    filter.add(t.getTerm());
                }
            }
        }
        VisualReasonerHeadless.trafficMultiNar.informQaNar.ontology = narsese;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextArea4 = new javax.swing.JTextArea();
        jCheckBox2 = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextArea5 = new javax.swing.JTextArea();
        saveQANarButton = new javax.swing.JButton();
        stopSaveQANarButton = new javax.swing.JButton();
        stopSaveLocationNarButton = new javax.swing.JButton();
        saveLocationNarButton = new javax.swing.JButton();
        stopSavePredictionNarButton = new javax.swing.JButton();
        savePredictionNarButton = new javax.swing.JButton();
        logOutputCheckBox = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Operator Panel");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setToolTipText("");
        jScrollPane1.setViewportView(jTextArea1);

        jLabel1.setText("Background knowledge and motivations");

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jTextArea2.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        jTextArea2.setEnabled(false);
        jScrollPane2.setViewportView(jTextArea2);

        jLabel2.setText("Current answers");

        jButton1.setText("Apply");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel3.setText("Current motivations");

        jTextArea3.setColumns(20);
        jTextArea3.setRows(5);
        jTextArea3.setToolTipText("");
        jScrollPane3.setViewportView(jTextArea3);

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Learn&Show predictions");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jLabel4.setText("Current questions");

        jTextArea4.setColumns(20);
        jTextArea4.setRows(5);
        jTextArea4.setToolTipText("");
        jScrollPane4.setViewportView(jTextArea4);

        jCheckBox2.setSelected(true);
        jCheckBox2.setText("Allow relative location relations");
        jCheckBox2.setToolTipText("");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });

        jLabel5.setText("Current operator messages");

        jTextArea5.setColumns(20);
        jTextArea5.setRows(5);
        jTextArea5.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        jTextArea5.setEnabled(false);
        jScrollPane5.setViewportView(jTextArea5);

        saveQANarButton.setText("Save QANar experience");
        saveQANarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveQANarButtonActionPerformed(evt);
            }
        });

        stopSaveQANarButton.setText("Stop");
        stopSaveQANarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopSaveQANarButtonActionPerformed(evt);
            }
        });

        stopSaveLocationNarButton.setText("Stop");
        stopSaveLocationNarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopSaveLocationNarButtonActionPerformed(evt);
            }
        });

        saveLocationNarButton.setText("Save LocationNar experience");
        saveLocationNarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveLocationNarButtonActionPerformed(evt);
            }
        });

        stopSavePredictionNarButton.setText("Stop");
        stopSavePredictionNarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopSavePredictionNarButtonActionPerformed(evt);
            }
        });

        savePredictionNarButton.setText("Save PredictionNar experience");
        savePredictionNarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                savePredictionNarButtonActionPerformed(evt);
            }
        });

        logOutputCheckBox.setSelected(true);
        logOutputCheckBox.setText("Log input only");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 352, Short.MAX_VALUE)
                                .addComponent(jCheckBox2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jCheckBox1))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel4))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jScrollPane1)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel5)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(saveQANarButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(stopSaveQANarButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(saveLocationNarButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(stopSaveLocationNarButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(savePredictionNarButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(stopSavePredictionNarButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(logOutputCheckBox)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jCheckBox1)
                    .addComponent(jCheckBox2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(savePredictionNarButton)
                        .addComponent(stopSavePredictionNarButton)
                        .addComponent(logOutputCheckBox))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(saveLocationNarButton)
                        .addComponent(stopSaveLocationNarButton))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(saveQANarButton)
                        .addComponent(stopSaveQANarButton)))
                .addContainerGap(23, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        KnowledgeToCrossing();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        showPredictions = jCheckBox1.isSelected();
        /*if(jCheckBox1.isSelected()) {
            RealCrossing.trafficMultiNar.predictionNar.narParameters.SEQUENCE_BAG_ATTEMPTS = VisualReasoner.trafficMultiNar.SEQUENCE_BAG_ATTEMPTS;
        }
        if(!jCheckBox1.isSelected()) {
            RealCrossing.trafficMultiNar.nar.narParameters.SEQUENCE_BAG_ATTEMPTS = 0;
        }*/
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        //RealCrossing.trafficMultiNar.informQaNar.RELATIVE_LOCATION_RELATIONS = jCheckBox2.isSelected();
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    JFileChooser chooser = new JFileChooser(); 
    public File chooseFile() {
        chooser.setDialogTitle("Select output file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    boolean[] writeQANar = new boolean[] { false };
    boolean[] writeLocationNar = new boolean[] { false };
    boolean[] writePredictionNar = new boolean[] { false };
    FileOutputStream[] qaNarStream = new FileOutputStream[] { null };
    FileOutputStream[] locationNarStream = new FileOutputStream[] { null };
    FileOutputStream[] predictionNarStream = new FileOutputStream[] { null };
    private void saveQANarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveQANarButtonActionPerformed
        LogOutput(qaNarStream, writeQANar, VisualReasonerHeadless.trafficMultiNar.qanar, "  QANAR");
        writeQANar[0] = true;
    }//GEN-LAST:event_saveQANarButtonActionPerformed

    private void saveLocationNarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveLocationNarButtonActionPerformed
        LogOutput(locationNarStream, writeLocationNar, VisualReasonerHeadless.trafficMultiNar.locationNar, " LocNAR");
        writeLocationNar[0] = true;
    }//GEN-LAST:event_saveLocationNarButtonActionPerformed

    private void savePredictionNarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_savePredictionNarButtonActionPerformed
        LogOutput(predictionNarStream, writePredictionNar, VisualReasonerHeadless.trafficMultiNar.predictionNar, "PredNAR");
        writePredictionNar[0] = true;
    }//GEN-LAST:event_savePredictionNarButtonActionPerformed

    private void stopSaveQANarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopSaveQANarButtonActionPerformed
        writeQANar[0] = false;
        StopLogOutput(qaNarStream);
    }//GEN-LAST:event_stopSaveQANarButtonActionPerformed

    private void stopSaveLocationNarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopSaveLocationNarButtonActionPerformed
        writeLocationNar[0] = false;
        StopLogOutput(locationNarStream);
    }//GEN-LAST:event_stopSaveLocationNarButtonActionPerformed

    private void stopSavePredictionNarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopSavePredictionNarButtonActionPerformed
        writePredictionNar[0] = false;
        StopLogOutput(predictionNarStream);
    }//GEN-LAST:event_stopSavePredictionNarButtonActionPerformed

    private void StopLogOutput(final FileOutputStream[] fsl) {
        synchronized(fsl) {
            if(fsl[0] != null) {
                try {
                    fsl[0].close();
                } catch (IOException ex) {
                    Logger.getLogger(OperatorPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    private void LogOutput(final FileOutputStream[] fsl, final boolean[] write, final Nar nar, String prefix) {
        File saveFile = chooseFile();
        try {
            boolean attachEventHandler = fsl[0] == null;
            synchronized(fsl) {
                fsl[0] = new FileOutputStream(saveFile, true);
            }
            if(attachEventHandler) {
                nar.on(TaskAdd.class, (event, args) -> {
                    if(event == TaskAdd.class) {   
                            Task task = (Task) args[0];
                            if(!logOutputCheckBox.isSelected() || task.isInput()) {
                                try {
                                    synchronized(fsl) {
                                        if(write[0]) {
                                            String attachment = task.isInput() ? "IN:  " : "OUT: ";
                                            fsl[0].write(("(frame="+VisualReasonerHeadless.i+")"+prefix + "-" + attachment + task.toString()).getBytes("UTF8"));
                                            fsl[0].write("\n".getBytes("UTF8"));
                                        }
                                    }
                                } catch (UnsupportedEncodingException ex) {
                                    Logger.getLogger(OperatorPanel.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (IOException ex) {
                                    Logger.getLogger(OperatorPanel.class.getName()).log(Level.SEVERE, null, ex);
                                }
                        }
                    }
                });
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(OperatorPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

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
            java.util.logging.Logger.getLogger(OperatorPanel.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OperatorPanel.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OperatorPanel.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OperatorPanel.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new OperatorPanel(new Nar()).setVisible(true);
                } catch (Exception ex) {
                    Logger.getLogger(OperatorPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextArea jTextArea4;
    public javax.swing.JTextArea jTextArea5;
    private javax.swing.JCheckBox logOutputCheckBox;
    private javax.swing.JButton saveLocationNarButton;
    private javax.swing.JButton savePredictionNarButton;
    private javax.swing.JButton saveQANarButton;
    private javax.swing.JButton stopSaveLocationNarButton;
    private javax.swing.JButton stopSavePredictionNarButton;
    private javax.swing.JButton stopSaveQANarButton;
    // End of variables declaration//GEN-END:variables
}
