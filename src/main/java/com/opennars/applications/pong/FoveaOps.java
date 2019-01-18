package com.opennars.applications.pong;

import org.opennars.interfaces.NarseseConsumer;

public class FoveaOps {
    public Pong pong;
    public NarseseConsumer consumer;

    public void foveaBigX(String valString) {
        System.out.println("[o] foveaBigX " + valString);
        int val = Integer.parseInt(valString);
        pong.foveaBigX = val;

        // give feedback
        consumer.addInput("<{" + valString + "}-->[doneActionFoveaBigX]>");
    }

    public void foveaBigY(String valString) {
        System.out.println("[o] foveaBigY " + valString);
        int val = Integer.parseInt(valString);
        pong.foveaBigY = val;

        // give feedback
        consumer.addInput("<{" + valString + "}-->[doneActionFoveaBigY]>");
    }

    public void foveaFineX(String valString) {
        System.out.println("[o] foveaFineX " + valString);
        int val = Integer.parseInt(valString);
        pong.foveaFineX = val;

        // give feedback
        consumer.addInput("<{" + valString + "}-->[doneActionFoveaFineX]>");
    }

    public void foveaFineY(String valString) {
        System.out.println("[o] foveaFineY " + valString);
        int val = Integer.parseInt(valString);
        pong.foveaFineY = val;

        // give feedback
        consumer.addInput("<{" + valString + "}-->[doneActionFoveaFineY]>");
    }
}
