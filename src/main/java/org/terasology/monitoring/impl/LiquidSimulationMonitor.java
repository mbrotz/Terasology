package org.terasology.monitoring.impl;

import org.terasology.monitoring.ThreadMonitor.SingleThreadMonitor;

public class LiquidSimulationMonitor extends SingleThreadMonitor {

    public LiquidSimulationMonitor(Thread thread) {
        super("LiquidSimulation", thread, "Blocks");
    }

}
