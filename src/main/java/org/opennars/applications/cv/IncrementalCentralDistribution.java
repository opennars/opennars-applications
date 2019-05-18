/*
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                    Version 2, December 2004

 Copyright (C) 2019 Robert Wünsche <rt09@protonmail.com>

 Everyone is permitted to copy and distribute verbatim or modified
 copies of this license document, and changing it is allowed as long
 as the name is changed.

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. You just DO WHAT THE FUCK YOU WANT TO.
 */
package org.opennars.applications.cv;

// see http://datagenetics.com/blog/november22017/index.html
public class IncrementalCentralDistribution {
    public void next(final double x) {
        double nextMean = mean + (x - mean)/(n+1);
        double nextS = s + (x - mean)*(x - nextMean);

        mean = nextMean;
        s = nextS;
        n++;
    }

    public double calcVariance() {
        return Math.sqrt(s/n);
    }

    public long n = 0;
    public double mean = 0.0;
    public double s = 0.0;
}
