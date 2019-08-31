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

import java.util.List;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.crosswalkers;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.i;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.indangers;
import static org.opennars.applications.streetscene.VisualReasonerHeadless.jaywalkers;
import org.opennars.entity.Task;
import org.opennars.interfaces.Timable;
import org.opennars.language.Term;
import org.opennars.operator.Operation;
import org.opennars.operator.Operator;
import org.opennars.storage.Memory;

public class Say extends Operator {
    public Say() {
        super("^say");
    }
    @Override
    public List<Task> execute(Operation operation, Term[] args, Memory memory, Timable time) {
        if(args.length > 2) { //{SELF} car3 message
            if(args[2].toString().equals("is_jaywalking")) {
                synchronized(jaywalkers) {
                    jaywalkers.put(args[1].toString(), i);
                }
            }
            else
            if(args[2].toString().equals("is_crosswalking")) {
            synchronized(crosswalkers) {
                crosswalkers.put(args[1].toString(), i);
            }
            }
            else
            if(args[2].toString().equals("is_in_danger")) {
                synchronized(indangers) {
                    indangers.put(args[1].toString(), i);
                }
            }
        }
        return null;
    }
}