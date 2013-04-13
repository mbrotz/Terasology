/*
 * Copyright 2012 Benjamin Glatzel <benjamin.glatzel@me.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.world.chunks.provider;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.vecmath.Vector3f;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.components.world.LocationComponent;
import org.terasology.config.AdvancedConfig;
import org.terasology.entitySystem.EntityRef;
import org.terasology.game.CoreRegistry;
import org.terasology.math.Region3i;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.monitoring.SingleThreadMonitor;
import org.terasology.monitoring.ThreadMonitor;
import org.terasology.world.lighting.LightPropagator;
import org.terasology.world.ClassicWorldView;
import org.terasology.world.chunks.Chunk;
import org.terasology.world.chunks.ChunkState;
import org.terasology.world.chunks.ChunkType;
import org.terasology.world.chunks.storage.ChunkStore;
import org.terasology.world.generator.core.ChunkGeneratorManager;
import org.terasology.world.lighting.InternalLightProcessor;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

/**
 * @author Immortius
 */
public class LocalChunkProvider implements ChunkProvider {
    private static final int CACHE_SIZE = (int) (2 * Runtime.getRuntime().maxMemory() / 1048576);
    private static final int REQUEST_CHUNK_THREADS = 4;
    private static final int CHUNK_PROCESSING_THREADS = 8;
    
    private final Vector3i LOCAL_REGION_EXTENTS;
    private final Vector3i LOCAL_REGION_EXTENTS_2;
    private final Vector3i LOCAL_REGION_EXTENTS_4;

    private static final Logger logger = LoggerFactory.getLogger(LocalChunkProvider.class);

    private final ChunkType chunkType;
    private final ChunkStore farStore;
    private final ChunkGeneratorManager generator;

    private BlockingQueue<ChunkTask> chunkTasksQueue;
    private BlockingQueue<ChunkRequest> reviewChunkQueue;
    private ExecutorService reviewThreads;
    private ExecutorService chunkProcessingThreads;
    
    private Set<CacheRegion> regions = Sets.newHashSet();

    private ConcurrentMap<Vector3i, Chunk> nearCache = Maps.newConcurrentMap();
    private final Set<Vector3i> preparingChunks = Sets.newSetFromMap(Maps.<Vector3i, Boolean>newConcurrentMap());

    private EntityRef worldEntity = EntityRef.NULL;

    private ReadWriteLock regionLock = new ReentrantReadWriteLock();

    public LocalChunkProvider(ChunkType chunkType, ChunkStore farStore, ChunkGeneratorManager generator) {
        this.chunkType = Preconditions.checkNotNull(chunkType, "The parameter 'chunkType' must not be null");
        this.farStore = Preconditions.checkNotNull(farStore, "The parameter 'farStore' must not be null");
        this.generator = Preconditions.checkNotNull(generator, "The parameter 'generator' must not be null");
        
        this.LOCAL_REGION_EXTENTS = chunkType.getChunkExtents(1);
        this.LOCAL_REGION_EXTENTS_2 = chunkType.getChunkExtents(2);
        this.LOCAL_REGION_EXTENTS_4 = chunkType.getChunkExtents(4);
        
        logger.info("CACHE_SIZE = {} for nearby chunks", CACHE_SIZE);

        reviewChunkQueue = new PriorityBlockingQueue<ChunkRequest>(32);
        reviewThreads = Executors.newFixedThreadPool(REQUEST_CHUNK_THREADS);
        for (int i = 0; i < REQUEST_CHUNK_THREADS; ++i) {
            reviewThreads.execute(new Runnable() {
                @Override
                public void run() {
                    final SingleThreadMonitor monitor = ThreadMonitor.create("Terasology.Chunks.Requests", "Review", "Produce");
                    try {
                        boolean running = true;
                        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                        while (running) {
                            try {
                                ChunkRequest request = reviewChunkQueue.take();
                                switch (request.getType()) {
                                    case REVIEW:
                                        for (Vector3i pos : request.getRegion()) {
                                            checkState(pos);
                                        }
                                        monitor.increment(0);
                                        break;
                                    case PRODUCE:
                                        for (Vector3i pos : request.getRegion()) {
                                            checkOrCreateChunk(pos);
                                        }
                                        monitor.increment(1);
                                        break;
                                    case EXIT:
                                        running = false;
                                        break;
                                }
                            } catch (InterruptedException e) {
                                monitor.addError(e);
                                logger.error("Thread interrupted", e);
                            } catch (Exception e) {
                                monitor.addError(e);
                                logger.error("Error in thread", e);
                            }
                        }
                        logger.debug("Thread shutdown safely");
                    } finally {
                        monitor.setActive(false);
                    }
                }
            });
        }

        chunkTasksQueue = new PriorityBlockingQueue<ChunkTask>(128, new ChunkTaskRelevanceComparator());
        chunkProcessingThreads = Executors.newFixedThreadPool(CHUNK_PROCESSING_THREADS);
        for (int i = 0; i < CHUNK_PROCESSING_THREADS; ++i) {
            chunkProcessingThreads.submit(new Runnable() {
                @Override
                public void run() {
                    final SingleThreadMonitor monitor = ThreadMonitor.create("Terasology.Chunks.Processing", "Tasks");
                    try {
                        boolean running = true;
                        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                        while (running) {
                            try {
                                ChunkTask request = chunkTasksQueue.take();
                                if (request.isShutdownRequest()) {
                                    running = false;
                                    break;
                                }
                                request.enact();
                                monitor.increment(0);
                            } catch (InterruptedException e) {
                                monitor.addError(e);
                                logger.error("Thread interrupted", e);
                            } catch (Exception e) {
                                monitor.addError(e);
                                logger.error("Error in thread", e);
                            }
                        }
                        logger.debug("Thread shutdown safely");
                    } finally {
                        monitor.setActive(false);
                    }
                }
            });
        }
    }
    
    @Override
    public ChunkType getChunkType() {
        return chunkType;
    }

    @Override
    public void setWorldEntity(EntityRef worldEntity) {
        if (worldEntity == null)
            this.worldEntity = EntityRef.NULL;
        else
            this.worldEntity = worldEntity;
    }

    @Override
    public void addRegionEntity(EntityRef entity, int distance) {
        CacheRegion region = new CacheRegion(this, entity, distance);
        regionLock.writeLock().lock();
        try {
            regions.remove(region);
            regions.add(region);
        } finally  {
            regionLock.writeLock().unlock();
        }
        reviewChunkQueue.offer(new ChunkRequest(ChunkRequest.RequestType.PRODUCE, region.getRegion().expand(LOCAL_REGION_EXTENTS_2)));
    }

    @Override
    public void removeRegionEntity(EntityRef entity) {
        regionLock.writeLock().lock();
        try {
            regions.remove(new CacheRegion(this, entity, 0));
        } finally {
            regionLock.writeLock().unlock();
        }
    }

    @Override
    public void update() {
        regionLock.readLock().lock();
        try {
            for (CacheRegion cacheRegion : regions) {
                cacheRegion.update();
                if (cacheRegion.isDirty()) {
                    cacheRegion.setUpToDate();
                    reviewChunkQueue.offer(new ChunkRequest(ChunkRequest.RequestType.PRODUCE, cacheRegion.getRegion().expand(LOCAL_REGION_EXTENTS_2)));
                }
            }

            if (nearCache.size() > CACHE_SIZE) {
                PerformanceMonitor.startActivity("Review cache size");
                logger.debug("Compacting cache");
                Iterator<Vector3i> iterator = nearCache.keySet().iterator();
                while (iterator.hasNext()) {
                    Vector3i pos = iterator.next();
                    boolean keep = false;
                    for (CacheRegion region : regions) {
                        if (region.getRegion().expand(LOCAL_REGION_EXTENTS_4).encompasses(pos)) {
                            keep = true;
                            break;
                        }
                    }
                    if (!keep) {
                        // TODO: need some way to not dispose chunks being edited or processed (or do so safely)
                        Chunk chunk = nearCache.get(pos);
                        if (chunk.isLocked()) {
                            continue;
                        }
                        chunk.lock();
                        try {
                            farStore.put(chunk);
                            iterator.remove();
                            chunk.dispose();
                        } finally {
                            chunk.unlock();
                        }
                    }

                }
                PerformanceMonitor.endActivity();
            }
        } finally {
            regionLock.readLock().unlock();
        }
    }

    @Override
    public boolean isChunkAvailable(Vector3i pos) {
        return nearCache.containsKey(pos);
    }

    @Override
    public Chunk getChunk(int x, int y, int z) {
        return getChunk(new Vector3i(x, y, z));
    }

    @Override
    public Chunk getChunk(Vector3i pos) {
        return nearCache.get(pos);
    }

    @Override
    public void dispose() {
        for (int i = 0; i < REQUEST_CHUNK_THREADS; ++i) {
            reviewChunkQueue.offer(new ChunkRequest(ChunkRequest.RequestType.EXIT, Region3i.EMPTY));
        }
        for (int i = 0; i < CHUNK_PROCESSING_THREADS; ++i) {
            chunkTasksQueue.offer(new ShutdownTask());
        }
        reviewThreads.shutdown();
        chunkProcessingThreads.shutdown();
        try {
            if (!reviewThreads.awaitTermination(1, TimeUnit.SECONDS)) {
                logger.warn("Timed out awaiting chunk review thread termination");
            }
            if (!chunkProcessingThreads.awaitTermination(1, TimeUnit.SECONDS)) {
                logger.warn("Timed out awaiting chunk processing thread termination");
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted awaiting chunk thread termination");
        }

        for (Chunk chunk : nearCache.values()) {
            farStore.put(chunk);
            chunk.dispose();
        }
        nearCache.clear();
    }

    @Override
    public float size() {
        return farStore.size();
    }
    
    private void checkOrCreateChunk(Vector3i chunkPos) {
        if (!chunkType.isStackable && (chunkPos.y != 0)) {
            logger.error("The chunk type {} is not stackable. The requested chunk {} will not be generated.", chunkType, chunkPos);
            return;
        }
        Chunk chunk = getChunk(chunkPos);
        if (chunk == null) {
            PerformanceMonitor.startActivity("Check chunk in cache");
            if (preparingChunks.add(chunkPos)) {
                if (farStore.contains(chunkPos)) {
                    chunkTasksQueue.offer(new AbstractChunkTask(chunkPos, this) {
                        @Override
                        public void enact() {
                            Chunk chunk = farStore.get(getPosition());
                            if (nearCache.putIfAbsent(getPosition(), chunk) != null) {
                                logger.warn("Chunk {} is already in the near cache", getPosition());
                            }
                            preparingChunks.remove(getPosition());
                            if (chunk.getChunkState() == ChunkState.COMPLETE) {
                                for (Vector3i adjPos : Region3i.createFromCenterExtents(getPosition(), LOCAL_REGION_EXTENTS)) {
                                    checkChunkReady(adjPos);
                                }
                                reviewChunkQueue.offer(new ChunkRequest(ChunkRequest.RequestType.REVIEW, Region3i.createFromCenterExtents(getPosition(), LOCAL_REGION_EXTENTS)));
                            }
                        }
                    });
                } else {
                    chunkTasksQueue.offer(new AbstractChunkTask(chunkPos, this) {
                        @Override
                        public void enact() {
                            Chunk chunk = generator.generateChunk(getPosition());
                            if (nearCache.putIfAbsent(getPosition(), chunk) != null) {
                                logger.warn("Chunk {} is already in the near cache", getPosition());
                            }
                            preparingChunks.remove(getPosition());
                            reviewChunkQueue.offer(new ChunkRequest(ChunkRequest.RequestType.REVIEW, Region3i.createFromCenterExtents(getPosition(), LOCAL_REGION_EXTENTS)));
                        }
                    });
                }
            }
            PerformanceMonitor.endActivity();
        } else {
            checkState(chunk);
        }
    }

    private void checkState(Vector3i pos) {
        Chunk chunk = getChunk(pos);
        if (chunk != null) {
            checkState(chunk);
        }
    }

    private void checkState(Chunk chunk) {
        switch (chunk.getChunkState()) {
            case ADJACENCY_GENERATION_PENDING:
                checkReadyForSecondPass(chunk.getPos());
                break;
            case INTERNAL_LIGHT_GENERATION_PENDING:
                checkReadyToDoInternalLighting(chunk.getPos());
                break;
            case LIGHT_PROPAGATION_PENDING:
                checkReadyToPropagateLighting(chunk.getPos());
                break;
            case FULL_LIGHT_CONNECTIVITY_PENDING:
                checkComplete(chunk.getPos());
                break;
            default:
                break;
        }
    }

    private void checkReadyForSecondPass(Vector3i pos) {
        Chunk chunk = getChunk(pos);
        if (chunk != null && chunk.getChunkState() == ChunkState.ADJACENCY_GENERATION_PENDING) {
            for (Vector3i adjPos : Region3i.createFromCenterExtents(pos, LOCAL_REGION_EXTENTS)) {
                if (!adjPos.equals(pos)) {
                    Chunk adjChunk = getChunk(adjPos);
                    if (adjChunk == null) {
                        return;
                    }
                }
            }
            logger.debug("Queueing for adjacency generation {}", pos);
            chunkTasksQueue.offer(new AbstractChunkTask(pos, this) {
                @Override
                public void enact() {
                    ClassicWorldView view = ClassicWorldView.createLocalView(getPosition(), getProvider());
                    if (view == null) {
                        return;
                    }
                    view.lock();
                    try {
                        if (!view.isValidView()) {
                            return;
                        }
                        Chunk chunk = getProvider().getChunk(getPosition());
                        if (chunk.getChunkState() != ChunkState.ADJACENCY_GENERATION_PENDING) {
                            return;
                        }

                        generator.secondPassChunk(getPosition(), view);
                        chunk.setChunkState(ChunkState.INTERNAL_LIGHT_GENERATION_PENDING);
                        reviewChunkQueue.offer(new ChunkRequest(ChunkRequest.RequestType.REVIEW, Region3i.createFromCenterExtents(getPosition(), LOCAL_REGION_EXTENTS)));
                    } finally {
                        view.unlock();
                    }
                }
            });
        }
    }

    private void checkReadyToDoInternalLighting(Vector3i pos) {
        Chunk chunk = getChunk(pos);
        if (chunk != null && chunk.getChunkState() == ChunkState.INTERNAL_LIGHT_GENERATION_PENDING) {
            for (Vector3i adjPos : Region3i.createFromCenterExtents(pos, LOCAL_REGION_EXTENTS)) {
                if (!adjPos.equals(pos)) {
                    Chunk adjChunk = getChunk(adjPos);
                    if (adjChunk == null || adjChunk.getChunkState().compareTo(ChunkState.INTERNAL_LIGHT_GENERATION_PENDING) < 0) {
                        return;
                    }
                }
            }
            logger.debug("Queueing for internal light generation {}", pos);
            chunkTasksQueue.offer(new AbstractChunkTask(pos, this) {
                @Override
                public void enact() {
                    Chunk chunk = getProvider().getChunk(getPosition());
                    if (chunk == null) {
                        return;
                    }

                    chunk.lock();
                    try {
                        if (chunk.isDisposed() || chunk.getChunkState() != ChunkState.INTERNAL_LIGHT_GENERATION_PENDING) {
                            return;
                        }
                        InternalLightProcessor.generateInternalLighting(chunk);
                        chunk.setChunkState(ChunkState.LIGHT_PROPAGATION_PENDING);
                        reviewChunkQueue.offer(new ChunkRequest(ChunkRequest.RequestType.REVIEW, Region3i.createFromCenterExtents(getPosition(), LOCAL_REGION_EXTENTS)));
                    } finally {
                        chunk.unlock();
                    }
                }
            });
        }
    }

    private void checkReadyToPropagateLighting(Vector3i pos) {
        Chunk chunk = getChunk(pos);
        if (chunk != null && chunk.getChunkState() == ChunkState.LIGHT_PROPAGATION_PENDING) {
            for (Vector3i adjPos : Region3i.createFromCenterExtents(pos, LOCAL_REGION_EXTENTS)) {
                if (!adjPos.equals(pos)) {
                    Chunk adjChunk = getChunk(adjPos);
                    if (adjChunk == null || adjChunk.getChunkState().compareTo(ChunkState.LIGHT_PROPAGATION_PENDING) < 0) {
                        return;
                    }
                }
            }
            logger.debug("Queueing for light propagation pass {}", pos);
            chunkTasksQueue.offer(new AbstractChunkTask(pos, this) {
                @Override
                public void enact() {
                    ClassicWorldView worldView = ClassicWorldView.createLocalView(getPosition(), getProvider());
                    if (worldView == null) {
                        return;
                    }
                    worldView.lock();
                    try {
                        if (!worldView.isValidView()) {
                            return;
                        }
                        Chunk chunk = getProvider().getChunk(getPosition());
                        if (chunk.getChunkState() != ChunkState.LIGHT_PROPAGATION_PENDING) {
                            return;
                        }

                        new LightPropagator(worldView).propagateOutOfTargetChunk();
                        chunk.setChunkState(ChunkState.FULL_LIGHT_CONNECTIVITY_PENDING);
                        reviewChunkQueue.offer(new ChunkRequest(ChunkRequest.RequestType.REVIEW, Region3i.createFromCenterExtents(getPosition(), LOCAL_REGION_EXTENTS)));
                    } finally {
                        worldView.unlock();
                    }
                }
            });
        }
    }

    private void checkComplete(Vector3i pos) {
        Chunk chunk = getChunk(pos);
        if (chunk != null && chunk.getChunkState() == ChunkState.FULL_LIGHT_CONNECTIVITY_PENDING) {
            for (Vector3i adjPos : Region3i.createFromCenterExtents(pos, LOCAL_REGION_EXTENTS)) {
                if (!adjPos.equals(pos)) {
                    Chunk adjChunk = getChunk(adjPos);
                    if (adjChunk == null || adjChunk.getChunkState().compareTo(ChunkState.FULL_LIGHT_CONNECTIVITY_PENDING) < 0) {
                        return;
                    }
                }
            }
            logger.debug("Now complete {}", pos);
            chunk.setChunkState(ChunkState.COMPLETE);
            AdvancedConfig config = CoreRegistry.get(org.terasology.config.Config.class).getAdvancedConfig();
            if (config.isChunkDeflationEnabled()) {
                if (!chunkTasksQueue.offer(new AbstractChunkTask(pos, this) {
                    @Override
                    public void enact() {
                        Chunk chunk = getChunk(getPosition());
                        if (chunk != null) {
                            chunk.deflate();
                        }
                    }
                })) {
                    logger.warn("LocalChunkProvider.chunkTasksQueue rejected deflation task for chunk {}", pos);
                }
            }
            for (Vector3i adjPos : Region3i.createFromCenterExtents(pos, LOCAL_REGION_EXTENTS)) {
                checkChunkReady(adjPos);
            }
        }
    }

    private void checkChunkReady(Vector3i pos) {
        if (worldEntity.exists()) {
            for (Vector3i adjPos : Region3i.createFromCenterExtents(pos, LOCAL_REGION_EXTENTS)) {
                Chunk chunk = getChunk(adjPos);
                if (chunk == null || chunk.getChunkState() != ChunkState.COMPLETE) {
                    return;
                }
            }
            worldEntity.send(new ChunkReadyEvent(pos));
        }
    }

    private static class CacheRegion {
        private final LocalChunkProvider owner;
        private final EntityRef entity;
        private final int halfDistance;
        private boolean dirty;
        
        private final Vector3i center = new Vector3i();
        private final Vector3i halfDistanceExtents = new Vector3i();

        public CacheRegion(LocalChunkProvider owner, EntityRef entity, int distance) {
            this.owner = Preconditions.checkNotNull(owner, "The parameter 'owner' must not be null");
            this.entity = Preconditions.checkNotNull(entity, "The parameter 'entity' must not be null");
            this.halfDistance = TeraMath.ceilToInt(distance / 2.0f);
            this.halfDistanceExtents.set(halfDistance, halfDistance * owner.getChunkType().fStackable, halfDistance);

            LocationComponent loc = entity.getComponent(LocationComponent.class);
            if (loc == null) {
                dirty = false;
            } else {
                center.set(worldToChunkPos(loc.getWorldPosition()));
                dirty = true;
            }
        }

        public boolean isValid() {
            return entity.hasComponent(LocationComponent.class);
        }

        public boolean isDirty() {
            return dirty;
        }

        public void setUpToDate() {
            dirty = false;
        }

        public void update() {
            if (!isValid()) {
                dirty = false;
            } else {
                Vector3i newCenter = getCenter();
                if (!newCenter.equals(center)) {
                    dirty = true;
                    center.set(newCenter);
                }
            }
        }

        public Region3i getRegion() {
            LocationComponent loc = entity.getComponent(LocationComponent.class);
            if (loc != null) {
                return Region3i.createFromCenterExtents(worldToChunkPos(loc.getWorldPosition()), halfDistanceExtents);
            }
            return Region3i.EMPTY;
        }

        private Vector3i getCenter() {
            LocationComponent loc = entity.getComponent(LocationComponent.class);
            if (loc != null) {
                return worldToChunkPos(loc.getWorldPosition());
            }
            return new Vector3i();
        }

        private Vector3i worldToChunkPos(Vector3f worldPos) {
            final ChunkType chunkType = owner.getChunkType();
            return new Vector3i(worldPos.x / chunkType.sizeX, worldPos.y / chunkType.sizeY, worldPos.z / chunkType.sizeZ);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof CacheRegion) {
                CacheRegion other = (CacheRegion) o;
                return Objects.equal(other.entity, entity);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(entity);
        }
    }

    private class ChunkTaskRelevanceComparator implements Comparator<ChunkTask> {

        @Override
        public int compare(ChunkTask o1, ChunkTask o2) {
            return score(o1.getPosition()) - score(o2.getPosition());
        }

        private int score(Vector3i chunk) {
            int score = Integer.MAX_VALUE;
            // TODO: This isn't thread safe. Fix me

            regionLock.readLock().lock();
            try {
                for (CacheRegion region : regions) {
                    int dist = distFromRegion(chunk, region.center);
                    if (dist < score) {
                        score = dist;
                    }
                }
                return score;
            } finally {
                regionLock.readLock().unlock();
            }
        }

        private int distFromRegion(Vector3i pos, Vector3i regionCenter) {
            return pos.gridDistance(regionCenter);
        }
    }
}
