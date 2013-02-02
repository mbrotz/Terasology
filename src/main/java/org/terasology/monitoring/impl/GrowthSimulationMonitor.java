package org.terasology.monitoring.impl;

import org.terasology.monitoring.ThreadMonitor.SingleThreadMonitor;

public class GrowthSimulationMonitor extends SingleThreadMonitor {

    public GrowthSimulationMonitor(Thread thread) {
        super("GrowthSimulation", thread, "Blocks");
    }

}
