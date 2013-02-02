package org.terasology.monitoring;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ThreadMonitor {

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final TLongObjectMap<SingleThreadMonitor> threadsById = new TLongObjectHashMap<SingleThreadMonitor>();
    private static final Multimap<String, SingleThreadMonitor> threadsByName = HashMultimap.create();
    private static final LinkedList<SingleThreadMonitor> threads = new LinkedList<SingleThreadMonitor>();
    
    private static void register(SingleThreadMonitor monitor) {
        Preconditions.checkNotNull(monitor, "The parameter 'monitor' must not be null");
        threads.add(monitor);
        threadsById.put(monitor.getThreadId(), monitor);
        threadsByName.put(monitor.getName(), monitor);
    }
    
    private ThreadMonitor() {}
    
    public static boolean isThreadMonitored(long id) {
        return getThread(id) != null;
    }
    
    public static SingleThreadMonitor getThread(long id) {
        lock.readLock().lock();
        try {
            return threadsById.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getThreadEntries(List<SingleThreadMonitor> output, boolean aliveThreadsOnly) {
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
    
    public abstract static class SingleThreadMonitor {
        
        private final String name;
        private final WeakReference<Thread> ref;
        private final String[] keys;
        private final long[] counters;
        
        private final long id;
        
        private LinkedList<Throwable> errors = null;

        private boolean active = true;
        
        protected SingleThreadMonitor(String name, Thread thread, String... keys) {
            Preconditions.checkNotNull(name, "The parameter 'name' must not be null");
            Preconditions.checkNotNull(thread, "The parameter 'thread' must not be null");
            Preconditions.checkNotNull(keys, "The parameter 'keys' must not be null");
            this.name = name;
            this.ref = new WeakReference<Thread>(thread);
            this.id = thread.getId();
            this.keys = keys;
            this.counters = new long[keys.length];
            ThreadMonitor.register(this);
        }

        public final boolean isAlive() {
            return ref.get() != null;
        }

        public final boolean isActive() {
            return active;
        }
        
        public final void setActive(boolean value) {
            this.active = value;
        }

        public final String getName() {
            return name;
        }

        public final long getThreadId() {
            return id;
        }
        
        public final boolean hasErrors() {
            return (errors != null && errors.size() > 0);
        }
        
        public final int getNumErrors() {
            if (errors == null)
                return 0;
            return errors.size();
        }
        
        public final Throwable getLastError() {
            if (errors == null)
                return null;
            return errors.peekLast();
        }
        
        public final LinkedList<Throwable> getErrors() {
            return errors;
        }
        
        public final void addError(Throwable error) {
            if (errors == null)
                errors = new LinkedList<Throwable>();
            errors.add(error);
        }
        
        public final int getNumCounters() {
            return keys.length;
        }
        
        public final String getKey(int index) {
            return keys[index];
        }
        
        public final long getCounter(int index) {
            return counters[index];
        }
        
        public final void increment(int index) {
            counters[index]++;
        }
    }
}