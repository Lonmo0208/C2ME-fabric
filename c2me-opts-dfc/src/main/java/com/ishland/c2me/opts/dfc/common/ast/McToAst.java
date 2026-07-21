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

package com.ishland.c2me.opts.dfc.common.ast;

import com.ishland.c2me.opts.dfc.common.Config;
import com.ishland.c2me.opts.dfc.common.ast.binary.AddNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.DivNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MaxShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MinShortNode;
import com.ishland.c2me.opts.dfc.common.ast.binary.MulNode;
import com.ishland.c2me.opts.dfc.common.ast.integration.tectonic.ConfigClampBindings;
import com.ishland.c2me.opts.dfc.common.ast.integration.tectonic.ConfigNoiseBindings;
import com.ishland.c2me.opts.dfc.common.ast.misc.BeardifierNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.CoordinateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.DelegateNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.EndIslandsNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.FindTopSurfaceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.FocusedDensityNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.HollowHillNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.BoxDensityNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.TanhHillNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.InterpolatedNoiseSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.RangeChoiceNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.YClampedGradientNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.DFTWeirdScaledSamplerNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.GenericShiftedNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.noise.SinglePerlinNoiseNode;
import com.ishland.c2me.opts.dfc.common.ast.opto.OptoPasses;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineAstNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.AbsNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.CubeNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.NegMulNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqrtNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SquareNode;
import com.ishland.c2me.opts.dfc.common.ast.unary.SqueezeNode;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.gen.jvm.CompiledDensityFunction;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class McToAst {

    private static final Logger LOGGER = LoggerFactory.getLogger(McToAst.class);
    private static final ConcurrentHashMap<Class<?>, AtomicLong> delegateStatistics = new ConcurrentHashMap<>();

    public static AstNode toAst(DensityFunction df) {
        Objects.requireNonNull(df);
        return switch (df) {
            case ChunkNoiseSampler.BlendAlphaDensityFunction f -> new ConstantNode(1.0);
            case ChunkNoiseSampler.BlendOffsetDensityFunction f -> new ConstantNode(0.0);
            case DensityFunctionTypes.BlendAlpha f -> new ConstantNode(1.0);
            case DensityFunctionTypes.BlendOffset f -> new ConstantNode(0.0);
            case DensityFunctionTypes.BinaryOperationLike f -> switch (f.type()) {
                case ADD -> new AddNode(toAst(f.argument1()), toAst(f.argument2()));
                case MUL -> new MulNode(toAst(f.argument1()), toAst(f.argument2()));
                case MIN -> {
                    double rightMin = f.argument2().minValue();
                    if (f.argument1().minValue() < rightMin) {
                        yield new MinShortNode(toAst(f.argument1()), toAst(f.argument2()), rightMin);
                    } else {
                        yield new MinNode(toAst(f.argument1()), toAst(f.argument2()));
                    }
                }
                case MAX -> {
                    double rightMax = f.argument2().maxValue();
                    if (f.argument1().maxValue() > rightMax) {
                        yield new MaxShortNode(toAst(f.argument1()), toAst(f.argument2()), rightMax);
                    } else {
                        yield new MaxNode(toAst(f.argument1()), toAst(f.argument2()));
                    }
                }
            };
            case DensityFunctionTypes.BlendDensity f -> toAst(f.input());
            case DensityFunctionTypes.Clamp f -> new MaxNode(new ConstantNode(f.minValue()), new MinNode(new ConstantNode(f.maxValue()), toAst(f.input())));
            case DensityFunctionTypes.Constant f -> new ConstantNode(f.value());
            case DensityFunctionTypes.RegistryEntryHolder f -> toAst(f.function().value());
            case DensityFunctionTypes.UnaryOperation f -> switch (f.type()) {
                case ABS -> new AbsNode(toAst(f.input()));
                case SQUARE -> new SquareNode(toAst(f.input()));
                case CUBE -> new CubeNode(toAst(f.input()));
                case HALF_NEGATIVE -> new NegMulNode(toAst(f.input()), 0.5);
                case QUARTER_NEGATIVE -> new NegMulNode(toAst(f.input()), 0.25);
                case INVERT -> new DivNode(new ConstantNode(1.0), toAst(f.input()));
                case SQUEEZE -> new SqueezeNode(toAst(f.input()));
            };
            case DensityFunctionTypes.RangeChoice f -> new RangeChoiceNode(toAst(f.input()), f.minInclusive(), f.maxExclusive(), toAst(f.whenInRange()), toAst(f.whenOutOfRange()));
            case IFastCacheLike f -> new CacheLikeNode(f, toAst(f.c2me$getDelegate()));
            case DensityFunctionTypes.Wrapping f -> {
//                if ((Object) f instanceof IFastCacheLike fastCacheLike && f.type() != DensityFunctionTypes.Wrapping.Type.INTERPOLATED) {
//                    yield new CacheLikeNode(fastCacheLike, toAst(fastCacheLike.c2me$getDelegate()));
//                }
                DensityFunctionTypes.Wrapping wrapping = new DensityFunctionTypes.Wrapping(f.type(), new CompiledDensityFunction(BytecodeGen.compile0("unknown", OptoPasses.AstPair.ofOptimizedOnly(toAst(f.wrapped()))), null));
                yield new DelegateNode(wrapping);
            }
            case DensityFunctionTypes.ShiftedNoise f -> new GenericShiftedNoiseNode(
                    new AddNode(new MulNode(CoordinateNode.AXIS_X, new ConstantNode(f.xzScale())), toAst(f.shiftX())),
                    new AddNode(new MulNode(CoordinateNode.AXIS_Y, new ConstantNode(f.yScale())), toAst(f.shiftY())),
                    new AddNode(new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(f.xzScale())), toAst(f.shiftZ())),
                    f.noise()
            );
            case DensityFunctionTypes.Noise f -> new GenericShiftedNoiseNode(
                    new MulNode(CoordinateNode.AXIS_X, new ConstantNode(f.xzScale())),
                    new MulNode(CoordinateNode.AXIS_Y, new ConstantNode(f.yScale())),
                    new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(f.xzScale())),
                    f.noise()
            );
            case DensityFunctionTypes.Shift f -> new MulNode(
                    new GenericShiftedNoiseNode(
                            new MulNode(CoordinateNode.AXIS_X, new ConstantNode(0.25)),
                            new MulNode(CoordinateNode.AXIS_Y, new ConstantNode(0.25)),
                            new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(0.25)),
                            f.offsetNoise()
                    ),
                    new ConstantNode(4.0)
            );
            case DensityFunctionTypes.ShiftA f -> new MulNode(
                    new GenericShiftedNoiseNode(
                            new MulNode(CoordinateNode.AXIS_X, new ConstantNode(0.25)),
                            new ConstantNode(0.0),
                            new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(0.25)),
                            f.offsetNoise()
                    ),
                    new ConstantNode(4.0)
            );
            case DensityFunctionTypes.ShiftB f -> new MulNode(
                    new GenericShiftedNoiseNode(
                            new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(0.25)),
                            new MulNode(CoordinateNode.AXIS_X, new ConstantNode(0.25)),
                            new ConstantNode(0.0),
                            f.offsetNoise()
                    ),
                    new ConstantNode(4.0)
            );
            case DensityFunctionTypes.YClampedGradient f -> new YClampedGradientNode(f.fromY(), f.toY(), f.fromValue(), f.toValue());
            case DensityFunctionTypes.WeirdScaledSampler f -> new DFTWeirdScaledSamplerNode(toAst(f.input()), f.noise(), f.rarityValueMapper());
            case DensityFunctionTypes.Spline f -> new SplineAstNode(f.spline());
            case DensityFunctionTypes.FindTopSurface f -> new FindTopSurfaceNode(toAst(f.density()), toAst(f.upperBound()), new ConstantNode(f.lowerBound()), f.cellHeight());

            // delegate nodes that have specialized OpenCL gen
            case DensityFunctionTypes.EndIslands f -> new EndIslandsNode(f);
            case InterpolatedNoiseSampler f -> new InterpolatedNoiseSamplerNode(f);
            case DensityFunctionTypes.Beardifier f -> new BeardifierNode(f);

            // Aether II PerlinNoiseFunction integration
            default -> {
                if (df.getClass().getName().equals("com.aetherteam.aetherii.world.density.PerlinNoiseFunction")) {
                    try {
                        var cls = df.getClass();
                        var noise = cls.getField("noise").get(df);
                        var xzScaleField = cls.getDeclaredField("xzScale");
                        xzScaleField.setAccessible(true);
                        var xzScale = xzScaleField.getDouble(df);
                        var yScaleField = cls.getDeclaredField("yScale");
                        yScaleField.setAccessible(true);
                        var yScale = yScaleField.getDouble(df);
                        // PerlinNoiseFunction has lazy initialization - use fakeNoise as fallback (private field)
                        if (noise == null) {
                            var fakeNoiseField = cls.getDeclaredField("fakeNoise");
                            fakeNoiseField.setAccessible(true);
                            noise = fakeNoiseField.get(df);
                        }
                        if (noise != null) {
                            yield new SinglePerlinNoiseNode(
                                    new MulNode(CoordinateNode.AXIS_X, new ConstantNode(xzScale)),
                                    new MulNode(CoordinateNode.AXIS_Y, new ConstantNode(yScale)),
                                    new MulNode(CoordinateNode.AXIS_Z, new ConstantNode(xzScale)),
                                    noise
                            );
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse PerlinNoiseFunction", e);
                    }
                }

                if (Config.enableBuiltinIntegrations) {
                    {
                        AstNode node = ConfigClampBindings.tryParse(df);
                        if (node != null) yield node;
                    }

                    {
                        AstNode node = ConfigNoiseBindings.tryParse(df);
                        if (node != null) yield node;
                    }
                }

                // Twilight Forest integrations
                {
                    AstNode node = tryParseTwilightForest(df);
                    if (node != null) yield node;
                }

                long known = delegateStatistics.computeIfAbsent(df.getClass(), unused -> new AtomicLong(0L)).getAndIncrement();
                if (known == 0) {
                    LOGGER.warn("warn_once: Generating DelegateNode for type: {}", df.getClass().toString());
                }
                yield new DelegateNode(df);
            }
        };

    }

    private static AstNode tryParseTwilightForest(DensityFunction df) {
        String className = df.getClass().getName();
        return switch (className) {
            case "twilightforest.world.components.chunkgenerators.SqrtDensityFunction" -> {
                try {
                    var cls = df.getClass();
                    var input = (DensityFunction) cls.getField("input").get(df);
                    yield new SqrtNode(toAst(input));
                } catch (Exception e) {
                    yield null;
                }
            }
            case "twilightforest.world.components.chunkgenerators.FocusedDensityFunction" -> {
                try {
                    var cls = df.getClass();
                    double centerX = cls.getField("centerX").getDouble(df);
                    double bottomY = cls.getField("bottomY").getDouble(df);
                    double centerZ = cls.getField("centerZ").getDouble(df);
                    double radius = cls.getField("radius").getDouble(df);
                    double nearValue = cls.getField("nearValue").getDouble(df);
                    double farValue = cls.getField("farValue").getDouble(df);
                    yield new FocusedDensityNode(df, centerX, bottomY, centerZ, radius, nearValue, farValue);
                } catch (Exception e) {
                    yield null;
                }
            }
            case "twilightforest.world.components.chunkgenerators.HollowHillFunction" -> {
                try {
                    var cls = df.getClass();
                    double centerX = cls.getField("centerX").getDouble(df);
                    double bottomY = cls.getField("bottomY").getDouble(df);
                    double centerZ = cls.getField("centerZ").getDouble(df);
                    double radius = cls.getField("radius").getDouble(df);
                    double heightScale = cls.getField("heightScale").getDouble(df);
                    yield new HollowHillNode(df, centerX, bottomY, centerZ, radius, heightScale);
                } catch (Exception e) {
                    yield null;
                }
            }
            case "twilightforest.world.components.chunkgenerators.TanhHillFunction" -> {
                try {
                    var cls = df.getClass();
                    double centerX = cls.getField("centerX").getDouble(df);
                    double bottomY = cls.getField("bottomY").getDouble(df);
                    double centerZ = cls.getField("centerZ").getDouble(df);
                    double radius = cls.getField("radius").getDouble(df);
                    double heightScale = cls.getField("heightScale").getDouble(df);
                    double cosAngleBiasDirection = cls.getField("cosAngleBiasDirection").getDouble(df);
                    double sinAngleBiasDirection = cls.getField("sinAngleBiasDirection").getDouble(df);
                    boolean isXOriented = cls.getField("isXOriented").getBoolean(df);
                    boolean isOnRightSide = cls.getField("isOnRightSide").getBoolean(df);
                    yield new TanhHillNode(df, centerX, bottomY, centerZ, radius, heightScale, cosAngleBiasDirection, sinAngleBiasDirection, isXOriented, isOnRightSide);
                } catch (Exception e) {
                    yield null;
                }
            }
            case "twilightforest.world.components.chunkgenerators.BoxDensityFunction" -> {
                try {
                    var cls = df.getClass();
                    double minX = cls.getField("minX").getDouble(df);
                    double minY = cls.getField("minY").getDouble(df);
                    double minZ = cls.getField("minZ").getDouble(df);
                    double maxX = cls.getField("maxX").getDouble(df);
                    double maxY = cls.getField("maxY").getDouble(df);
                    double maxZ = cls.getField("maxZ").getDouble(df);
                    double minValue = cls.getField("minValue").getDouble(df);
                    double maxValue = cls.getField("maxValue").getDouble(df);
                    double terrainAdjustment = cls.getField("terrainAdjustment").getDouble(df);
                    yield new BoxDensityNode(df, minX, minY, minZ, maxX, maxY, maxZ, minValue, maxValue, terrainAdjustment);
                } catch (Exception e) {
                    yield null;
                }
            }
            case "twilightforest.world.components.chunkgenerators.AbsoluteDifferenceFunction$Min" -> {
                try {
                    var cls = df.getClass();
                    double max = cls.getField("max").getDouble(df);
                    double centerX = cls.getField("centerX").getDouble(df);
                    double centerZ = cls.getField("centerZ").getDouble(df);
                    // Math.min(max, Math.max(Math.abs(x - centerX), Math.abs(z - centerZ)))
                    yield new MinNode(
                            new ConstantNode(max),
                            new MaxNode(
                                    new AbsNode(new AddNode(CoordinateNode.AXIS_X, new ConstantNode(-centerX))),
                                    new AbsNode(new AddNode(CoordinateNode.AXIS_Z, new ConstantNode(-centerZ)))
                            )
                    );
                } catch (Exception e) {
                    yield null;
                }
            }
            case "twilightforest.world.components.chunkgenerators.AbsoluteDifferenceFunction$Max" -> {
                try {
                    var cls = df.getClass();
                    double max = cls.getField("max").getDouble(df);
                    double centerX = cls.getField("centerX").getDouble(df);
                    double centerZ = cls.getField("centerZ").getDouble(df);
                    // Math.min(max, Math.max(Math.abs(x - centerX), Math.abs(z - centerZ)))
                    yield new MinNode(
                            new ConstantNode(max),
                            new MaxNode(
                                    new AbsNode(new AddNode(CoordinateNode.AXIS_X, new ConstantNode(-centerX))),
                                    new AbsNode(new AddNode(CoordinateNode.AXIS_Z, new ConstantNode(-centerZ)))
                            )
                    );
                } catch (Exception e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

}
