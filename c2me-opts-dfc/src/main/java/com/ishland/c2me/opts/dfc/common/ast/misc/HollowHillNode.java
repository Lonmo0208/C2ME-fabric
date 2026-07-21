/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021-2026 ishland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ishland.c2me.opts.dfc.common.ast.misc;

import net.minecraft.world.gen.densityfunction.DensityFunction;

public class HollowHillNode extends DelegateNode {

    private final double centerX, bottomY, centerZ, radius, heightScale;

    public HollowHillNode(DensityFunction delegate, double centerX, double bottomY, double centerZ, double radius, double heightScale) {
        super(delegate);
        this.centerX = centerX;
        this.bottomY = bottomY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.heightScale = heightScale;
    }

    public double getCenterX() { return centerX; }
    public double getBottomY() { return bottomY; }
    public double getCenterZ() { return centerZ; }
    public double getRadius() { return radius; }
    public double getHeightScale() { return heightScale; }

}