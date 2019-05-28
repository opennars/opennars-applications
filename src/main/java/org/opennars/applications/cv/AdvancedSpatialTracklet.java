/*
 * The MIT License
 *
 * Copyright 2019 Robert Wünsche <rt09@protonmail.com>
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

package org.opennars.applications.cv;

import java.util.ArrayList;
import java.util.List;

/**
 * higher order spatial tracklet
 */
public class AdvancedSpatialTracklet {
    public double centerX;
    public double centerY;
    public double width;
    public double height;
    public long id;
    public long idletime = 0;
    public long notTrainedSince = Integer.MAX_VALUE;

    // meta data - collected training samples of this class
    public List<float[]> trainingDataOfThisClass = new ArrayList<>();

    // decoration - classifier used for tracking this
    public MultichannelProtoClassifier prototypeClassifier = null;

    // centers of the prototype classification in this frame
    // used to agther training data
    public double prototypeCenterX = -1;
    public double prototypeCenterY = -1;

    public long associatedClass = Long.MIN_VALUE; // associated class to which the tracklet corresponds to, Long.MIN_VALUE if it is not coresponding to a global class(ification)


    public AdvancedSpatialTracklet(double centerX, double centerY, long id) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.id = id;
    }
}
