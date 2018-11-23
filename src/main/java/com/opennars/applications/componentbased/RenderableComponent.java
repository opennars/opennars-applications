package com.opennars.applications.componentbased;

import processing.core.PApplet;

public interface RenderableComponent {
    void render(final Entity entity, final PApplet applet);
}
