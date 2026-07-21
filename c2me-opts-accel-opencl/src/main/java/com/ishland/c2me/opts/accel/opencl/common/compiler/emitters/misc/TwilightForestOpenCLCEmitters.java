/*
 * All Rights Reserved
 *
 * Copyright (c) 2025-2026 ishland
 *
 * All rights reserved. Do not redistribute.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ishland.c2me.opts.accel.opencl.common.compiler.emitters.misc;

import com.ishland.c2me.opts.dfc.common.ast.misc.FocusedDensityNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.HollowHillNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.TanhHillNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.BoxDensityNode;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCEmitter;
import com.ishland.c2me.opts.dfc.common.gen.opencl.OpenCLCGenContext;

public class TwilightForestOpenCLCEmitters {

    public static final OpenCLCEmitter<FocusedDensityNode> FOCUSED_DENSITY = (node, context) ->
        String.format(
            "return math_focused_density(ctx.x, ctx.y, ctx.z, %d, %d, %d, %.17e, %.17e, %.17e);\n",
            (long) node.getCenterX(), (long) node.getBottomY(), (long) node.getCenterZ(),
            node.getRadius(), node.getNearValue(), node.getFarValue()
        );

    public static final OpenCLCEmitter<HollowHillNode> HOLLOW_HILL = (node, context) ->
        String.format(
            "return math_hollow_hill(ctx.x, ctx.y, ctx.z, %d, %d, %d, %.17e, %.17e);\n",
            (long) node.getCenterX(), (long) node.getBottomY(), (long) node.getCenterZ(),
            node.getRadius(), node.getHeightScale()
        );

    public static final OpenCLCEmitter<TanhHillNode> TANH_HILL = (node, context) ->
        String.format(
            "return math_tanh_hill(ctx.x, ctx.y, ctx.z, %d, %d, %d, %.17e, %.17e, %.17e, %.17e, %d, %d);\n",
            (long) node.getCenterX(), (long) node.getBottomY(), (long) node.getCenterZ(),
            node.getRadius(), node.getHeightScale(),
            node.getCosAngleBiasDirection(), node.getSinAngleBiasDirection(),
            node.isXOriented() ? 1 : 0, node.isOnRightSide() ? 1 : 0
        );

    public static final OpenCLCEmitter<BoxDensityNode> BOX_DENSITY = (node, context) ->
        String.format(
            "return math_box_density(ctx.x, ctx.y, ctx.z, %d, %d, %d, %d, %d, %d, %.17e, %.17e, %.17e);\n",
            (long) node.getMinX(), (long) node.getMinY(), (long) node.getMinZ(),
            (long) node.getMaxX(), (long) node.getMaxY(), (long) node.getMaxZ(),
            node.getMinValue(), node.getMaxValue(), node.getTerrainAdjustment()
        );

    public static void register(com.ishland.c2me.opts.dfc.common.gen.CodeGenRegistry<OpenCLCEmitter<? extends com.ishland.c2me.opts.dfc.common.ast.AstNode>> registry) {
        registry.registerExactMatch(FocusedDensityNode.class, FOCUSED_DENSITY);
        registry.registerExactMatch(HollowHillNode.class, HOLLOW_HILL);
        registry.registerExactMatch(TanhHillNode.class, TANH_HILL);
        registry.registerExactMatch(BoxDensityNode.class, BOX_DENSITY);
    }

}