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
package org.opennars.applications.crossing;

import processing.core.PApplet;
import static processing.core.PConstants.DOWN;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.RIGHT;
import static processing.core.PConstants.UP;

public class Viewport
{
    PApplet applet;
    public Viewport(PApplet applet){ 
        this.applet = applet;
    }
    float savepx=0;
    float savepy=0;
    int selID=0;
    float zoom=1.0f;
    float difx=0;
    float dify=0;
    int lastscr=0;
    boolean EnableZooming=true;
    float scrollcamspeed=1.1f;

    float MouseToWorldCoordX(int x)
    {
        return 1/zoom*(x-difx-applet.width/2);
    }
    float MouseToWorldCoordY(int y)
    {
        return 1/zoom*(y-dify-applet.height/2);
    }
    boolean md=false;
    public void mousePressed()
    {
        md=true;
        if(applet.mouseButton==RIGHT)
        {
            savepx=applet.mouseX;
            savepy=applet.mouseY;
        }
    }
    public void mouseReleased()
    {
        md=false;
    }
    public void mouseDragged()
    {
        if(applet.mouseButton==RIGHT)
        {
            difx+=(applet.mouseX-savepx);
            dify+=(applet.mouseY-savepy);
            savepx=applet.mouseX;
            savepy=applet.mouseY;
        }
    }
    float camspeed=20.0f;
    float scrollcammult=0.92f;
    boolean keyToo=true;
    public void keyPressed()
    {
        if((keyToo && applet.key=='w') || applet.keyCode==UP)
        {
            dify+=(camspeed);
        }
        if((keyToo && applet.key=='s') || applet.keyCode==DOWN)
        {
            dify+=(-camspeed);
        }
        if((keyToo && applet.key=='a') || applet.keyCode==LEFT)
        {
            difx+=(camspeed);
        }
        if((keyToo && applet.key=='d') || applet.keyCode==RIGHT)
        {
            difx+=(-camspeed);
        }
        if(!EnableZooming)
        {
            return;
        }
        if(applet.key=='-' || applet.key=='#')
        {
            float zoomBefore=zoom;
            zoom*=scrollcammult;
            difx=(difx)*(zoom/zoomBefore);
            dify=(dify)*(zoom/zoomBefore);
        }
        if(applet.key=='+')
        {
            float zoomBefore=zoom;
            zoom/=scrollcammult;
            difx=(difx)*(zoom/zoomBefore);
            dify=(dify)*(zoom/zoomBefore);
        }
    }
    void Init()
    {
        difx=-applet.width/2;
        dify=-applet.height/2;
    }
    public void mouseScrolled(float mouseScroll)
    {
        if(!EnableZooming)
        {
            return;
        }
        float zoomBefore=zoom;
        if(mouseScroll>0)
        {
            zoom*=scrollcamspeed;
        }
        else
        {
            zoom/=scrollcamspeed;
        }
        difx=(difx)*(zoom/zoomBefore);
        dify=(dify)*(zoom/zoomBefore);
    }
    void Transform()
    {
        applet.translate(difx+0.5f*applet.width,dify+0.5f*applet.height);
        applet.scale(zoom,zoom);
    }
}