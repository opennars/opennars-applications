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
