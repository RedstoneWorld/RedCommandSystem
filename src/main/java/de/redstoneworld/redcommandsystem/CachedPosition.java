package de.redstoneworld.redcommandsystem;
/*
* Copyright (C) 2012
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
public class CachedPosition {
    private final String world;
    private final double[] senderCoordinates;
    private final String[] coordinateInput;
    private final float senderYaw;
    private final float senderPitch;
    private double[] targetCoordinates;

    public CachedPosition(String world, double[] senderCoordinates, float senderPitch, float senderYaw, String[] coordinateInput, double[] targetCoordinates) {
        this.world = world;
        this.senderCoordinates = senderCoordinates;
        this.coordinateInput = coordinateInput;
        this.senderYaw = senderYaw;
        this.senderPitch = senderPitch;
        this.targetCoordinates = targetCoordinates;
    }

    public String getWorld() {
        return world;
    }

    public double[] getSenderCoordinates() {
        return senderCoordinates;
    }

    public float getSenderYaw() {
        return senderYaw;
    }

    public float getSenderPitch() {
        return senderPitch;
    }

    public String[] getCoordinateInput() {
        return coordinateInput;
    }

    public double[] getTargetCoordinates() {
        return targetCoordinates;
    }
}
