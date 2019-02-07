package com.opennars.applications.pong;

import org.opennars.interfaces.NarseseConsumer;
import org.opennars.language.Interval;
import org.opennars.language.Term;

import java.util.ArrayList;
import java.util.List;

// TODO< receive sentence and compute truth >
public class TemporalQa {
    private NarseseConsumer consumer;

    // TODO< put under AIKR with a good strategy >
    public List<Term> goalTerms = new ArrayList<>();

    // sequence with intervals
    // sequence of Term, Interval or String of narsese
    public List<Object> sequence = new ArrayList<>();


    public int bufferLength = 500;
    public int horizon = 5;

    public TemporalQa(NarseseConsumer consumer) {
        this.consumer = consumer;
    }

    public void inputEvent(Term term) {
        sequence.add(term.clone());

        if (term.equals(goalTerms.get(0))) {
            informReasoner();
            informReasonerAboutPrecondOpGoalForm();
        }
    }

    public void inputEventAsNarsese(String narsese) {
        sequence.add(narsese);

        if (narsese.equals(goalTerms.get(0).toString())) {
            informReasoner();
            informReasonerAboutPrecondOpGoalForm();
        }
    }

    public void endTimestep() {
        if (sequence.size() > 0 && sequence.get(sequence.size()-1) instanceof Interval) { // is last an interval
            // increment interval
            long intervalTime = ((Interval)sequence.get(sequence.size()-1)).time;
            sequence.set(sequence.size()-1, new Interval(intervalTime+1));
        }
        else {
            // add interval
            sequence.add(new Interval(1));
        }

        while(sequence.size() > bufferLength) {
            sequence.remove(0);
        }
    }

    void informReasoner() {
        informReasonerOfPossibleSlices(sequence);
    }

    // informs the reasoner about (&/, C, OP) =/> G if it appears
    void informReasonerAboutPrecondOpGoalForm() {
        List<Object> conditionalSlice = new ArrayList<>();

        // we need to kepp track of the accumulated time between the goal and the first op!
        long accumulatedTime = 0;

        // scan from back to front and skip last sequence element
        boolean wasOpScaned = false;
        for(int idx=sequence.size()-1-1;idx>=0;idx--) {
            Object io = sequence.get(idx);

            // accumulate time before op
            if (!wasOpScaned && io.toString().contains("+")) {
                String intervalTimeAsString = io.toString().substring(1, io.toString().length());

                accumulatedTime += Integer.parseInt(intervalTimeAsString);
            }

            if (io.toString().contains("up") || io.toString().contains("down")) { // is it a op
                wasOpScaned = true;
            }

            if (wasOpScaned) {
                conditionalSlice.add(0, io);
            }
        }

        if (wasOpScaned) {
            List<Object> sequence2 = conditionalSlice; // TODO< copy >

            if (accumulatedTime > 0) {
                sequence2.add(new Interval(accumulatedTime));
            }

            sequence2.add(sequence.get(sequence.size()-1));

            informReasonerOfPossibleSlices(conditionalSlice);
        }
    }

    void informReasonerOfPossibleSlices(final List<Object> arr) {
        for(int startIdx=arr.size()-1-horizon;startIdx<arr.size()-1-3;startIdx++) {
            List<Object> slice = arr.subList(startIdx, arr.size()-1+1);
            informReasonerSlice(slice);
        }
    }


    void informReasonerSlice(List<Object> slice) {
        String narsese = "";

        // build components of sequence
        for(int idx=0;idx<=slice.size()-1-1;idx++) {
            narsese += slice.get(idx).toString() + ",";
        }

        if (narsese.length() > 0) {
            narsese = narsese.substring(0, narsese.length()-1);
            int debug = 5;
        }

        int endIdx = slice.size()-1;
        String narseseTemporalImplPred = slice.get(endIdx).toString();

        // build conclusion
        String finalNarsese = "<" + "(&/," + narsese + ")" + " =/> " + narseseTemporalImplPred + ">.";

        int debug = 6;

        if (false) {
            System.out.println("Temporal Q&A - " + finalNarsese);

            consumer.addInput(finalNarsese);
        }
    }
}
