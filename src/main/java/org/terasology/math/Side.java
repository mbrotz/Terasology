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
package org.terasology.math;

import javax.vecmath.Vector3f;

/**
 * The six sides of a block and a slew of related utility
 *
 * @author Immortius <immortius@gmail.com>
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 * @author Rasmus 'Cervator' Praestholm <cervator@gmail.com>
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 */
public enum Side {
    TOP(Vector3i.up()) {
        @Override
        public boolean isHorizontal() {
            return false;
        }

        @Override
        public Side getReverse() {
            return BOTTOM;
        }

        @Override
        public Side getClockwise() {
            return TOP;
        }

        @Override
        public Side getAntiClockwise() {
            return TOP;
        }
        
        @Override
        public Side rotateClockwise(int steps) {
            return this;
        }
    },
    LEFT(new Vector3i(-1, 0, 0)) {
        @Override
        public boolean isHorizontal() {
            return true;
        }

        @Override
        public Side getReverse() {
            return RIGHT;
        }

        @Override
        public Side getClockwise() {
            return FRONT;
        }

        @Override
        public Side getAntiClockwise() {
            return BACK;
        }
    },
    RIGHT(new Vector3i(1, 0, 0)) {
        @Override
        public boolean isHorizontal() {
            return true;
        }

        @Override
        public Side getReverse() {
            return LEFT;
        }

        @Override
        public Side getClockwise() {
            return BACK;
        }

        @Override
        public Side getAntiClockwise() {
            return FRONT;
        }
    },
    FRONT(new Vector3i(0, 0, -1)) {
        @Override
        public boolean isHorizontal() {
            return true;
        }

        @Override
        public Side getReverse() {
            return BACK;
        }

        @Override
        public Side getClockwise() {
            return RIGHT;
        }

        @Override
        public Side getAntiClockwise() {
            return LEFT;
        }
    },
    BACK(new Vector3i(0, 0, 1)) {
        @Override
        public boolean isHorizontal() {
            return true;
        }

        @Override
        public Side getReverse() {
            return FRONT;
        }

        @Override
        public Side getClockwise() {
            return LEFT;
        }

        @Override
        public Side getAntiClockwise() {
            return RIGHT;
        }
    },
    BOTTOM(Vector3i.down()) {
        @Override
        public boolean isHorizontal() {
            return false;
        }

        @Override
        public Side getReverse() {
            return TOP;
        }

        @Override
        public Side getClockwise() {
            return BOTTOM;
        }

        @Override
        public Side getAntiClockwise() {
            return BOTTOM;
        }
        
        @Override
        public Side rotateClockwise(int steps) {
            return this;
        }
    };

    private static Side[] horizontalSides;

    static {
        horizontalSides = new Side[]{LEFT, RIGHT, FRONT, BACK};
    }

    /**
     * @return The horizontal sides, for iteration
     */
    public static Side[] getHorizontalSides() {
        return horizontalSides;
    }

    /**
     * Determines which direction the player is facing
     *
     * @param x right/left
     * @param y top/bottom
     * @param z back/front
     * @return Side enum with the appropriate direction
     */
    public static Side inDirection(int x, int y, int z) {
        if (TeraMath.fastAbs(x) > TeraMath.fastAbs(y)) {
            if (TeraMath.fastAbs(x) > TeraMath.fastAbs(z)) {
                return (x > 0) ? RIGHT : LEFT;
            }
        } else if (TeraMath.fastAbs(y) > TeraMath.fastAbs(z)) {
            return (y > 0) ? TOP : BOTTOM;
        }
        return (z > 0) ? BACK : FRONT;
    }

    /**
     * Determines which direction the player is facing
     *
     * @param dir Direction
     * @return Side enum with the appropriate direction
     */
    public static Side inDirection(Vector3f dir) {
        return inDirection(dir.x, dir.y, dir.z);
    }

    /**
     * Determines which direction the player is facing
     *
     * @param x right/left
     * @param y top/bottom
     * @param z back/front
     * @return Side enum with the appropriate direction
     */
    public static Side inDirection(double x, double y, double z) {
        if (TeraMath.fastAbs(x) > TeraMath.fastAbs(y)) {
            if (TeraMath.fastAbs(x) > TeraMath.fastAbs(z)) {
                return (x > 0) ? RIGHT : LEFT;
            }
        } else if (TeraMath.fastAbs(y) > TeraMath.fastAbs(z)) {
            return (y > 0) ? TOP : BOTTOM;
        }
        return (z > 0) ? BACK : FRONT;
    }

    /**
     * Determines which horizontal direction the player is facing
     *
     * @param x right/left
     * @param z back/front
     * @return Side enum with the appropriate direction
     */
    public static Side inHorizontalDirection(double x, double z) {
        if (TeraMath.fastAbs(x) > TeraMath.fastAbs(z)) {
            return (x > 0) ? RIGHT : LEFT;
        }
        return (z > 0) ? BACK : FRONT;
    }

    private final Vector3i vector3iDir;

    private Side(Vector3i vector3i) {
        this.vector3iDir = vector3i;
    }

    /**
     * @return The vector3i in the direction of the side. Do not modify.
     */
    public Vector3i getVector3i() {
        return vector3iDir;
    }

    /**
     * @return Whether this is one of the horizontal directions.
     */
    public abstract boolean isHorizontal();

    /**
     * @return The opposite side to this side.
     */
    public abstract Side getReverse();
    
    /**
     * @return The clockwise rotated side to this side.
     */
    public abstract Side getClockwise();
    
    /**
     * @return The anti clockwise rotated side to this side.
     */
    public abstract Side getAntiClockwise();

    /**
     * Rotates this side clockwise by the specified number of steps.
     * @param steps The number of rotation steps
     * @return The rotated side
     */
    public Side rotateClockwise(int steps) {
        if (steps < 0)
            steps = -steps + 2;
        steps = steps % 4;
        switch (steps) {
            case 1:
                return getClockwise();
            case 2:
                return getReverse();
            case 3:
                return getAntiClockwise();
            default:
                return this;
        }
    }
}
