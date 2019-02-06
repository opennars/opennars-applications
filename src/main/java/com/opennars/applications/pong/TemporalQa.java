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


    public int bufferLength = 20;
    public int horizon = 5;

    public TemporalQa(NarseseConsumer consumer) {
        this.consumer = consumer;
    }

    public void inputEvent(Term term) {
        sequence.add(term.clone());

        if (term.equals(goalTerms.get(0))) {
            informReasoner();
        }
    }

    public void inputEventAsNarsese(String narsese) {
        sequence.add(narsese);

        if (narsese.equals(goalTerms.get(0).toString())) {
            informReasoner();
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
        for(int startIdx=sequence.size()-1-horizon;startIdx<sequence.size()-1-1;startIdx++) {
            informReasonerSlice(startIdx, sequence.size()-1);
        }
    }

    void informReasonerSlice(int startIdx, int endIdx) {
        String narsese = "";

        // build components of sequence
        for(int idx=startIdx;idx<=endIdx-1;idx++) {
            narsese += sequence.get(idx).toString() + ",";
        }

        if (narsese.length() > 0) {
            narsese = narsese.substring(0, narsese.length()-1);
            int debug = 5;
        }

        String narseseTemporalImplPred = sequence.get(endIdx).toString();

        // build conclusion
        String finalNarsese = "<" + "(&/," + narsese + ")" + " =/> " + narseseTemporalImplPred + ">.";

        int debug = 6;

        System.out.println("Temporal Q&A - " + finalNarsese);

        consumer.addInput(finalNarsese);
    }
}
