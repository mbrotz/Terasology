package org.terasology.monitoring;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.terasology.monitoring.ThreadMonitor.ThreadMonitorEvent.Type;
import org.terasology.monitoring.impl.NullThreadMonitor;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;

public class ThreadMonitor {

    private static final EventBus eventbus = new EventBus("ThreadMonitor");
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final TLongObjectMap<SingleThreadMonitor> threadsById = new TLongObjectHashMap<SingleThreadMonitor>();
    private static final Multimap<String, SingleThreadMonitor> threadsByName = HashMultimap.create();
    private static final LinkedList<SingleThreadMonitor> threads = new LinkedList<SingleThreadMonitor>();
    
    private static boolean enabled = true;
    
    private static void register(SingleThreadMonitor monitor) {
        Preconditions.checkNotNull(monitor, "The parameter 'monitor' must not be null");
        threads.add(monitor);
        threadsById.put(monitor.getThreadId(), monitor);
        threadsByName.put(monitor.getName(), monitor);
        eventbus.post(new ThreadMonitorEvent(monitor, Type.MonitorAdded));
    }
    
    private ThreadMonitor() {}
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void setEnabled(boolean value) {
        enabled = value;
    }
    
    public static EventBus getEventBus() {
        return eventbus;
    }
    
    public static boolean isThreadMonitored(long id) {
        return getThreadMonitor(id) != null;
    }
    
    public static SingleThreadMonitor getThreadMonitor(long id) {
        lock.readLock().lock();
        try {
            return threadsById.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static int getThreadMonitors(List<SingleThreadMonitor> output, boolean aliveThreadsOnly) {
        Preconditions.checkNotNull(output, "The parameter 'output' must not be null");
        lock.readLock().lock();
        try {
            output.clear();
            for (SingleThreadMonitor entry : threads) {
                if (!aliveThreadsOnly || entry.isAlive())
                    output.add(entry);
            }
            return output.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static SingleThreadMonitor create(String name, Thread thread, String... counters) {
        if (enabled) 
            return new SingleThreadMonitorImpl(name, thread, counters);
        return NullThreadMonitor.getInstance();
    }
    
    public static SingleThreadMonitor create(String name, String... counters) {
        if (enabled)
            return new SingleThreadMonitorImpl(name, Thread.currentThread(), counters);
        return NullThreadMonitor.getInstance();
    }
    
    public static class ThreadMonitorEvent {
        
        public enum Type {
            MonitorAdded
        }
        
        public final SingleThreadMonitor monitor;
        public final Type type;
        
        public ThreadMonitorEvent(SingleThreadMonitor monitor, Type type) {
            Preconditions.checkNotNull(monitor, "The parameter 'monitor' must not be null");
            Preconditions.checkNotNull(type, "The parameter 'type' must not be null");
            this.monitor = monitor;
            this.type = type;
        }
    }
    
    protected static class SingleThreadMonitorImpl implements SingleThreadMonitor {
        
        private final String name;
        private final WeakReference<Thread> ref;
        private final String[] keys;
        private final long[] counters;
        
        private final long id;
        
        private LinkedList<Throwable> errors = null;

        private boolean active = true;
        
        protected SingleThreadMonitorImpl(String name, Thread thread, String... keys) {
            Preconditions.checkNotNull(name, "The parameter 'name' must not be null");
            Preconditions.checkNotNull(thread, "The parameter 'thread' must not be null");
            this.name = name;
            this.ref = new WeakReference<Thread>(thread);
            this.id = thread.getId();
            if (keys != null) {
                this.keys = keys;
                this.counters = new long[keys.length];
            } else {
                this.keys = null;
                this.counters = null;
            }
            ThreadMonitor.register(this);
        }

        @Override
        public final boolean isAlive() {
            return ref.get() != null;
        }

        @Override
        public final boolean isActive() {
            return active;
        }
        
        @Override
        public final void setActive(boolean value) {
            this.active = value;
        }

        @Override
        public final String getName() {
            return name;
        }

        @Override
        public final long getThreadId() {
            return id;
        }
        
        @Override
        public final boolean hasErrors() {
            return (errors != null && errors.size() > 0);
        }
        
        @Override
        public final int getNumErrors() {
            if (errors == null)
                return 0;
            return errors.size();
        }
        
        @Override
        public final Throwable getLastError() {
            if (errors == null)
                return null;
            return errors.peekLast();
        }
        
        @Override
        public final LinkedList<Throwable> getErrors() {
            return errors;
        }
        
        @Override
        public final void addError(Throwable error) {
            if (errors == null)
                errors = new LinkedList<Throwable>();
            errors.add(error);
        }
        
        @Override
        public final int getNumCounters() {
            return keys == null ? 0 : keys.length;
        }
        
        @Override
        public final String getKey(int index) {
            return keys[index];
        }
        
        @Override
        public final long getCounter(int index) {
            return counters[index];
        }
        
        @Override
        public final void increment(int index) {
            counters[index]++;
        }
        
        @Override
        public String toString() {
            final StringBuilder b = new StringBuilder(100);
            b.append(name).append(isAlive() ? " [ALIVE]" : " [DEAD]").append(" Id = ").append(id);
            if (keys != null) {
                for (int i = 0; i < keys.length; i++) {
                    b.append(", ").append(keys[i]).append(" = ").append(counters[i]);
                }
            }
            if (hasErrors()) {
                b.append(" [Errors = ").append(getNumErrors()).append(", ").append(getLastError().getClass().getSimpleName()).append("]");
            }
            return b.toString();
        }

        @Override
        public int compareTo(SingleThreadMonitor other) {
            if (other == null) return 0;
            final boolean alive1 = this.isAlive();
            final boolean alive2 = other.isAlive();
            final int relAlive = alive1 ? (alive2 ? 0 : -1) : (alive2 ? 1 : 0);
            if (relAlive == 0) {
                final boolean active1 = this.isActive();
                final boolean active2 = other.isActive();
                final int relActive = active1 ? (active2 ? 0 : -1) : (active2 ? 1 : 0);
                if (relActive == 0) {
                    final String name1 = this.getName();
                    final String name2 = other.getName();
                    final int relName = name1.compareTo(name2);
                    if (relName == 0) {
                        final long id1 = this.getThreadId();
                        final long id2 = other.getThreadId();
                        if (id1 == id2) return 0;
                        if (id1 < id2) return -1;
                        return 1;
                    }
                    return relName;
                }
                return relActive;
            }
            return relAlive;
        }
    }
}