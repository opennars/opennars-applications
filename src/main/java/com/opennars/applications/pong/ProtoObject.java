package com.opennars.applications.pong;

/**
 * Not really an object but can be used to identify objects which may be composed out of many protoobjects which have the same behaviour
 */
public class ProtoObject {
    public double posX;
    public double posY;

    public long classificationId; // id of the classification

    // used to decide if a proto object is more important - older are better
    public long age = 0;

    public boolean remove = false;

    public ProtoObject(long classificationId) {
        this.classificationId = classificationId;
    }
}
