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
package com.opennars.applications.pong.components;

import com.opennars.applications.componentbased.Entity;
import com.opennars.applications.componentbased.RenderableComponent;
import processing.core.PApplet;

public class BallRenderComponent implements RenderableComponent {
    @Override
    public void render(final Entity entity, PApplet applet) {
        final double size = 5;

        applet.pushMatrix();
        applet.translate((float)entity.posX, (float)entity.posY);
        applet.rotate((float)entity.angle);

        /* TODO< we need to render based on truth somehow
        if(truth == null) {
            final float width = Util.discretization/2*scale;
            applet.rect(0.0f*Util.discretization*scale, -0.5f*width, Util.discretization*scale, width);
        }
        */
        applet.fill(255);
        applet.ellipse(0, 0, (float)size, (float)size);

        applet.popMatrix();
        applet.fill(0);
        applet.text(String.valueOf(entity.id), (float)entity.posX, (float)entity.posY);

        // used for debugging the "real position"
        applet.rect((float)entity.posX, (float)entity.posY, 3, 3);
    }
}
