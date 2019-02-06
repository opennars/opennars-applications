package com.opennars.applications.pong;

import org.opennars.entity.Concept;
import org.opennars.entity.Task;
import org.opennars.io.Narsese;
import org.opennars.io.Parser;
import org.opennars.main.Nar;

import java.io.*;
import java.util.*;

public class WriteOutMemory {
    public static void writeOutConcepts(Nar nar, String filename, long t) throws IOException {
        File file = new File(filename);
        FileWriter fr = new FileWriter(file, true);
        BufferedWriter br = new BufferedWriter(fr);
        PrintWriter pr = new PrintWriter(br);

        pr.println("");
        pr.println("");
        pr.println("");
        pr.println("t=" + t);

        List<E> elements = new ArrayList<>();

        Narsese n = new Narsese(nar);

        /*
        try {
            Concept x = nar.memory.concepts.get(n.parseTerm("V0"));

            for(Task i : x.beliefs) {
                elements.add(new E(i.name().toString(), i.budget.getPriority()));
            }
        } catch (Parser.InvalidInputException e) {
            e.printStackTrace();
        }*/


        for (Iterator<Concept> it = nar.memory.concepts.iterator(); it.hasNext(); ) {
            Concept iConcept = it.next();

            E e = new E(iConcept.name().toString(), iConcept.budget.getPriority());
            //e.pressure = iConcept.beliefs.pressure;

            elements.add(e);
        }



        // sort
        Collections.sort(elements, new Comparator<E>() {
            @Override
            public int compare(E o1, E o2) {
                if (o1.priority == o2.priority) {
                    return 0;
                }

                if (o1.priority > o2.priority) {
                    return 1;
                }
                return -1;
            }
        });

        for(E iE : elements) {
            pr.println("\t" + iE.conceptName +" "+iE.pressure + " " + iE.priority);
        }




        pr.close();
        br.close();
        fr.close();
    }

    private static class E {
        public double priority;
        public String conceptName;
        public long pressure;

        public E(String conceptName, double priority) {
            this.conceptName = conceptName;
            this.priority = priority;
        }
    }
}
