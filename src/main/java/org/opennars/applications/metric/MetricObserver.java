package org.opennars.applications.metric;

/**
 * GOF observer for metrics
 */
public abstract class MetricObserver {
    public MetricObserver(MetricListener listener) {
        listener.register(this);
    }

    abstract public void notifyInt(String name, int value);

    abstract public void notifyFloat(String name, float value);
}
