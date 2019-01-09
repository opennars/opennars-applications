package com.opennars.applications.pong;

/**
 * Not really an object but can be used to identify objects which may be composed out of many protoobjects which have the same behaviour
 */
public class ProtoObject {
    public double posX;
    public double posY;

    public long classificationId; // id of the classification

    public ProtoObject(long classificationId) {
        this.classificationId = classificationId;
    }
}
