/*
 * The MIT License
 *
 * Copyright 2018 The OpenNARS authors.
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
package com.opennars.applications.pong;

import com.opennars.applications.componentbased.Entity;
import com.opennars.applications.crossing.Util;
import com.opennars.applications.crossing.Viewport;
import com.opennars.applications.pong.components.BallBehaviour;
import com.opennars.applications.pong.components.BallRenderComponent;
import com.opennars.applications.pong.components.MappedPositionInformer;
import com.opennars.sgui.NarSimpleGUI;
import org.opennars.interfaces.pub.Reasoner;
import org.opennars.io.events.Events;
import org.opennars.io.events.OutputHandler;
import org.opennars.main.Nar;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class Pong extends PApplet {
    Reasoner reasoner;
    int entityID = 1;

    List<Prediction> predictions = new ArrayList<>();
    List<Prediction> disappointments = new ArrayList<>();


    List<Entity> entities = new ArrayList<>();
    int t = 0;
    public static boolean showAnomalies = false;

    String questions = "<trafficLight --> [?whatColor]>? :|:";
    int perception_update = 1;

    InformReasoner informReasoner = new InformReasoner();

    GridMapper mapper = new GridMapper();



    final int fps = 50;
    @Override
    public void setup() {
        mapper.cellsize = 10;

        try {
            reasoner = new Nar();
            ((Nar)reasoner).narParameters.VOLUME = 0;
            ((Nar)reasoner).narParameters.DURATION*=10;
            ReasonerListener listener = new ReasonerListener(reasoner, predictions, disappointments, entities, mapper);
            reasoner.on(Events.TaskAdd.class, listener);
            reasoner.on(OutputHandler.DISAPPOINT.class, listener);
        } catch (Exception ex) {
            System.out.println(ex);
            System.exit(1);
        }

        setupScene();

        size(1000, 1000);
        frameRate(fps);
        new NarSimpleGUI((Nar)reasoner);
    }

    void setupScene() {
        final double posX = 1.0;
        final double posY = 1.0;
        final double velocity = 0.0;
        final double angle = 0.0;

        Entity ballEntity = new Entity(entityID++, posX, posY, velocity, angle, "ball");
        ballEntity.velocityX = 100.0;
        ballEntity.velocityY = 0; //23.7;

        ballEntity.renderable = new BallRenderComponent();
        ballEntity.behaviour = new BallBehaviour();
        ballEntity.components.add(new MappedPositionInformer(mapper));

        entities.add(ballEntity);
    }

    void tick() {

        // REFACTOR< TODO< use tick of entity >
        // tick
        for (Entity ie : entities) {
            if (ie.behaviour != null) {
                ie.behaviour.tick(ie);
            }
        }


        t++;
        reasoner.cycles(50);
        removeOutdatedPredictions(predictions);
        removeOutdatedPredictions(disappointments);


        informReasoner.informAboutEntities(entities);

        if (t % perception_update == 0) {
            final boolean force = false; // not required HACK
            final boolean hasInput = informReasoner.Input(reasoner, force);

            if(hasInput) {
                //reasoner.addInput(questions);
            }
        }

        System.out.println("Concepts: " + ((Nar)reasoner).memory.concepts.size());
    }

    @Override
    public void draw() {
        tick();

        viewport.Transform();
        background(64,128,64);
        fill(0);


        for (Entity e : entities) {
            e.render(this);
        }

        for (Prediction pred : predictions) {
            Entity e = pred.ent;

            float transparency = Util.truthToValue(pred.truth) * Util.timeToValue(pred.time - reasoner.time());

            // HACK< we need to cast to some class with translucency >

            BallRenderComponent ballRender = (BallRenderComponent)e.renderable;
            ballRender.translucency = transparency;

            e.render(this);
        }
        if(showAnomalies) {
            // REFACTOR< TODO< render >
            /*
            for (ReasonerListener.Prediction pred : disappointments) {
                Entity e = pred.ent;
                if(e instanceof Car) {
                    fill(255,0,0);
                }
                if(e instanceof Pedestrian) {
                    fill(0,0,255);
                }
                this.text("ANOMALY", (float)e.posX, (float)e.posY);
                e.draw(this, streets, trafficLights, entities, pred.truth, pred.time - ((Reasoner)reasoner).time());
            }
             */
        }

    }

    public void removeOutdatedPredictions(List<Prediction> predictions) {
        List<Prediction> toDelete = new ArrayList<>();
        for(Prediction pred : predictions) {
            if(pred.time <= reasoner.time()) {
                toDelete.add(pred);
            }
        }
        predictions.removeAll(toDelete);
    }

    float mouseScroll = 0;
    Viewport viewport = new Viewport(this);
    public void mouseWheel(MouseEvent event) {
        mouseScroll = -event.getCount();
        viewport.mouseScrolled(mouseScroll);
    }
    @Override
    public void keyPressed() {
        viewport.keyPressed();
    }
    @Override
    public void mousePressed() {
        viewport.mousePressed();
    }
    @Override
    public void mouseReleased() {
        viewport.mouseReleased();
    }
    @Override
    public void mouseDragged() {
        viewport.mouseDragged();
    }

    public static void main(String[] args) {
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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NarSimpleGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        String[] args2 = {"Pong"};
        Pong mp = new Pong();
        PApplet.runSketch(args2, mp);
    }
}
