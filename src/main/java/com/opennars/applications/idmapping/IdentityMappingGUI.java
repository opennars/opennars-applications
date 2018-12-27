/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.opennars.applications.idmapping;

import com.opennars.sgui.NarSimpleGUI;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opennars.entity.Task;
import org.opennars.interfaces.Timable;
import org.opennars.language.Term;
import org.opennars.main.Nar;
import org.opennars.operator.Operation;
import org.opennars.operator.Operator;
import org.opennars.storage.Memory;

/**
 *
 * @author patha
 */
public class IdentityMappingGUI extends javax.swing.JFrame {
    int successes = 0;
    int trials = 0;
    boolean executed = false;
    Nar nar1 = null;
    String guessed = "";
    public boolean leftIsRight = false;
    int madeWrongDecision = 0;
    public class Left extends Operator {
        public Left(String name) {
            super(name);
        }

        @Override
        public List<Task> execute(Operation operation, Term[] args, Memory memory, Timable time) {
            trials++;
            if(!executed && leftIsRight)
            {
                successes++;
                nar1.addInput("<answer --> correctAnswer>. :|:");
                jLabel2.setText("left chosen, CORRECT" + guessed);
                jLabel2.setForeground(Color.GREEN);
            }
            else
            if(!executed && !leftIsRight)
            {
                madeWrongDecision = 100+rnd.nextInt(200);
                jLabel2.setText("left chosen, WRONG" + guessed);
                jLabel2.setForeground(Color.RED);
            }
            executed = true;
            return null;
        }
    }
    Left left = new Left("^Left");
    Random rnd = new Random();
    public class Right extends Operator {
        public Right(String name) {
            super(name);
        }

        @Override
        public List<Task> execute(Operation operation, Term[] args, Memory memory, Timable time) {
            trials++;
            if(!executed && !leftIsRight)
            {
                successes++;
                nar1.addInput("<answer --> correctAnswer>. :|:");
                jLabel2.setText("right chosen, CORRECT" + guessed);
                jLabel2.setForeground(Color.GREEN);
            }
            else
            if(!executed && leftIsRight)
            {
                nar1.addInput("<answer --> correctAnswer>. :|: %0%");
                madeWrongDecision = 100+rnd.nextInt(200);
                jLabel2.setText("right chosen, WRONG" + guessed);
                jLabel2.setForeground(Color.RED);
            }
            executed = true;
            return null;
        }
    }
    Right right = new Right("^Right");
    /**
     * Creates new form IdentityMapping
     */
    public IdentityMappingGUI() {
        initComponents();
        Random rnd = new Random();
        Timer timer = new Timer();
        try {
            Map<String, Object> parameterOverrides = new HashMap<String, Object>();
            parameterOverrides.put("CONCEPT_BAG_SIZE", 80000);
            nar1 = new Nar(parameterOverrides);
        } catch (Exception ex) {
            Logger.getLogger(IdentityMappingGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        final Nar nar = nar1;
        nar.addPlugin(left);
        nar.addPlugin(right);
        nar.narParameters.VOLUME = 0;
        //nar.narParameters.DURATION*=10;
        //nar.narParameters.ANTICIPATION_CONFIDENCE = 0.9f;
        //nar.narParameters.SEQUENCE_BAG_ATTEMPTS*=10;
        //nar.narParameters.SEQUENCE_BAG_ATTEMPTS*=20;
        NarSimpleGUI narsgui = new NarSimpleGUI(nar);
        timer.schedule(new TimerTask(){
            @Override
            public void run(){
                if(madeWrongDecision>0) {
                    madeWrongDecision--;
                    return;
                }
                System.out.println(nar.memory.concepts.size());
                int num = rnd.nextInt(1000);
                int num2 = rnd.nextInt(1000);
                if(!jCheckBox1.isSelected()) {
                    num=num2=0;
                }
                boolean which = rnd.nextBoolean();
                String As = which ? "A" : "B";
                String Bs =  which ? "B" : "A";
                String A = ""+As+num+"";
                String B = ""+Bs+num2+"";
                jLabel2.setText("");
                jTextField1.setText("");
                jTextField2.setText("");
                jTextField3.setText("");
                boolean b = rnd.nextBoolean();
                if(b)
                {
                    leftIsRight = true;
                    jTextField3.setText("<right --> "+B+">. :|:");
                    nar.addInput("<right --> "+B+">. :|:");
                    jTextField1.setText("<example --> "+A+">. :|:");
                    nar.addInput("<example --> "+A+">. :|:");
                    jTextField2.setText("<left --> "+A+">. :|:");
                    nar.addInput("<left --> "+A+">. :|:");
                }
                else
                {
                    leftIsRight = false;
                    jTextField2.setText("<left --> "+B+">. :|:");
                    nar.addInput("<left --> "+B+">. :|:");
                    jTextField1.setText("<example --> "+A+">. :|:");
                    nar.addInput("<example --> "+A+">. :|:");
                    jTextField3.setText("<right --> "+A+">. :|:");
                    nar.addInput("<right --> "+A+">. :|:");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(IdentityMappingGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
                guessed = "";
                String delay = "10";
                jTextArea2.setText(jTextField1.getText()+"\n"+delay+"\n"+jTextField2.getText()+"\n"+delay+"\n"+jTextField3.getText()+"\n"+delay);
                //nar.memory.seq_current.clear();
                //nar.addInput(jTextArea2.getText());
                jTextArea1.setText("");
                for(Task t : nar.memory.seq_current) {
                    jTextArea1.setText(jTextArea1.getText() + t.sentence.toString()+ " (priority="+t.getPriority()+")\n");
                }
                executed = false;
                nar.cycles(1);
                nar.addInput("<answer --> correctAnswer>! :|:");
                //nar.cycles(300+rnd.nextInt(1000));
                nar.start(10);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(IdentityMappingGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(!executed) { //no decision was made, use a random one
                    boolean ch = rnd.nextBoolean();
                    if(ch) {
                        nar.addInput("<(*,{SELF}) --> ^Left>! :|:");
                    } else {
                        nar.addInput("<(*,{SELF}) --> ^Right>! :|:");
                    }
                    guessed = "(guessed)";
                }
                jLabel4.setText("success rate (0-1, 1=max): " + String.valueOf(((float) successes) / ((float) trials)));
                nar.cycles(5);
            }
        }, 10, 10);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTextField1.setText("jTextField1");

        jTextField2.setText("jTextField1");

        jTextField3.setText("jTextField1");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jLabel1.setText("Input that resulted");

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane2.setViewportView(jTextArea2);

        jLabel2.setText(".");

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Vary properties");

        jLabel3.setText("Event bag elements");

        jLabel4.setText(".");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jCheckBox1))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextField2, javax.swing.GroupLayout.DEFAULT_SIZE, 474, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 273, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jScrollPane1))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jCheckBox1)
                .addGap(16, 16, 16)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addGap(5, 5, 5))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        //</editor-fold>
        //</editor-fold>
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new IdentityMappingGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    // End of variables declaration//GEN-END:variables
}
