package org.opennars.applications.metric;

public interface MetricSensor {
    String getName();

    String getValueAsString(boolean force);

    /**
     * is called to give the sensor a chance to reset the stat after sending
     */
    void resetAfterSending();
}
