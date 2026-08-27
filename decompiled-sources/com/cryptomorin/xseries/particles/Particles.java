/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.World
 *  org.bukkit.block.BlockFace
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.NumberConversions
 *  org.bukkit.util.Vector
 */
package com.cryptomorin.xseries.particles;

import com.cryptomorin.xseries.particles.ParticleDisplay;
import com.cryptomorin.xseries.particles.XParticle;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

public final class Particles {
    public static final double PII = Math.PI * 2;
    public static final double R270 = Math.toRadians(270.0);
    public static final double R90 = 1.5707963267948966;

    private Particles() {
    }

    public static Optional<XParticle> randomParticle(String ... stringArray) {
        int n = Particles.randInt(0, stringArray.length - 1);
        return XParticle.of(stringArray[n]);
    }

    public static double random(double d, double d2) {
        return ThreadLocalRandom.current().nextDouble(d, d2);
    }

    public static int randInt(int n, int n2) {
        return ThreadLocalRandom.current().nextInt(n, n2 + 1);
    }

    public static org.bukkit.Color randomColor() {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        int n = threadLocalRandom.nextInt(0, 256);
        int n2 = threadLocalRandom.nextInt(0, 256);
        int n3 = threadLocalRandom.nextInt(0, 256);
        return org.bukkit.Color.fromRGB((int)n, (int)n2, (int)n3);
    }

    public static Particle.DustOptions randomDust() {
        float f = (float)Particles.randInt(5, 10) / 10.0f;
        return new Particle.DustOptions(Particles.randomColor(), f);
    }

    public static void blackSun(double d, double d2, double d3, double d4, ParticleDisplay particleDisplay) {
        double d5 = 0.0;
        for (double d6 = 10.0; d6 > 0.0; d6 -= d2) {
            Particles.circle(d + d6, d3 - (d5 += d4), particleDisplay);
        }
    }

    public static void circle(double d, double d2, ParticleDisplay particleDisplay) {
        Particles.circle(d, d, 1.0, d2, 0.0, particleDisplay);
    }

    public static void circle(double d, double d2, double d3, double d4, double d5, ParticleDisplay particleDisplay) {
        double d6 = Math.PI / Math.abs(d4);
        if (d5 == 0.0) {
            d5 = Math.PI * 2;
        } else if (d5 == -1.0) {
            d5 = Math.PI * 2 / Math.abs(d3);
        }
        for (double d7 = 0.0; d7 <= d5; d7 += d6) {
            double d8 = d * Math.cos(d3 * d7);
            double d9 = d2 * Math.sin(d3 * d7);
            if (particleDisplay.isDirectional()) {
                double d10 = Math.atan2(d9, d8);
                double d11 = Math.cos(d3 * d10);
                double d12 = Math.sin(d3 * d10);
                particleDisplay.particleDirection(d11, particleDisplay.getOffset().getY(), d12);
            }
            particleDisplay.spawn(d8, 0.0, d9);
        }
    }

    public static void diamond(double d, double d2, double d3, ParticleDisplay particleDisplay) {
        double d4 = 0.0;
        for (double d5 = 0.0; d5 < d3 * 2.0; d5 += d2) {
            d4 = d5 < d3 ? (d4 += d) : (d4 -= d);
            for (double d6 = -d4; d6 < d4; d6 += d2) {
                particleDisplay.spawn(d6, d5, 0.0);
            }
        }
    }

    public static Runnable circularBeam(final double d, final double d2, final double d3, final double d4, final ParticleDisplay particleDisplay) {
        return new Runnable(){
            final double rateDiv;
            final double radiusDiv;
            final Vector dir;
            double dynamicRadius;
            {
                this.rateDiv = Math.PI / d2;
                this.radiusDiv = Math.PI / d3;
                this.dir = particleDisplay.getLocation().getDirection().normalize().multiply(d4);
                this.dynamicRadius = 0.0;
            }

            @Override
            public void run() {
                double d5 = d * Math.sin(this.dynamicRadius);
                for (double d22 = 0.0; d22 < Math.PI * 2; d22 += this.rateDiv) {
                    double d32 = d5 * Math.sin(d22);
                    double d42 = d5 * Math.cos(d22);
                    particleDisplay.spawn(d32, 0.0, d42);
                }
                this.dynamicRadius += this.radiusDiv;
                if (this.dynamicRadius > Math.PI) {
                    this.dynamicRadius = 0.0;
                }
                particleDisplay.getLocation().add(this.dir);
            }
        };
    }

    public static BukkitTask circularBeam(Plugin plugin, double d, double d2, double d3, double d4, ParticleDisplay particleDisplay) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Particles.circularBeam(d, d2, d3, d4, particleDisplay), 0L, 1L);
    }

    public static void flower(int n, double d, ParticleDisplay particleDisplay, Runnable runnable) {
        for (double d2 = 0.0; d2 < Math.PI * 2; d2 += Math.PI * 2 / (double)n) {
            double d3 = d * Math.cos(d2);
            double d4 = d * Math.sin(d2);
            particleDisplay.getLocation().add(d3, 0.0, d4);
            runnable.run();
            particleDisplay.getLocation().subtract(d3, 0.0, d4);
        }
    }

    public static void filledCircle(double d, double d2, double d3, ParticleDisplay particleDisplay) {
        double d4 = 0.0;
        for (double d5 = 0.1; d5 < d; d5 += d3) {
            if (d5 > d) {
                d5 = d;
            }
            Particles.circle(d5, d4 += d2 / (d / d3), particleDisplay);
        }
    }

    public static Runnable chaoticDoublePendulum(final double d, final double d2, final double d3, final double d4, final double d5, final double d6, final boolean bl, final int n, final ParticleDisplay particleDisplay) {
        return new Runnable(){
            double theta = 1.5707963267948966;
            double theta2 = 1.5707963267948966;
            double thetaPrime = 0.0;
            double thetaPrime2 = 0.0;

            @Override
            public void run() {
                int n2 = n;
                while (n2-- != 0) {
                    if (bl) {
                        particleDisplay.rotate(0.09519977738150888, 0.07139983303613166, 0.057119866428905326);
                    }
                    double d20 = d5 + d6;
                    double d22 = 2.0 * d20;
                    double d32 = this.theta - this.theta2;
                    double d42 = d22 - d6 * Math.cos(2.0 * this.theta - 2.0 * this.theta2);
                    double d52 = Math.cos(d32);
                    double d62 = Math.sin(d32);
                    double d7 = this.thetaPrime * this.thetaPrime * d3;
                    double d8 = this.thetaPrime2 * this.thetaPrime2 * d4;
                    double d9 = -d2 * d22 * Math.sin(this.theta);
                    double d10 = -d6 * d2 * Math.sin(this.theta - 2.0 * this.theta2);
                    double d11 = -2.0 * d62 * d6;
                    double d12 = d8 + d7 * d52;
                    double d13 = d3 * d42;
                    double d14 = (d9 + d10 + d11 * d12) / d13;
                    d9 = 2.0 * d62;
                    d10 = d7 * d20;
                    d11 = d2 * d20 * Math.cos(this.theta);
                    d12 = d8 * d6 * d52;
                    d13 = d4 * d42;
                    double d15 = d9 * (d10 + d11 + d12) / d13;
                    this.thetaPrime += d14;
                    this.thetaPrime2 += d15;
                    this.theta += this.thetaPrime;
                    this.theta2 += this.thetaPrime2;
                    double d16 = d * Math.sin(this.theta);
                    double d17 = d * Math.cos(this.theta);
                    double d18 = d16 + d * Math.sin(this.theta2);
                    double d19 = d17 + d * Math.cos(this.theta2);
                    particleDisplay.spawn(d18, d19, 0.0);
                }
            }
        };
    }

    public static BukkitTask chaoticDoublePendulum(Plugin plugin, double d, double d2, double d3, double d4, double d5, double d6, boolean bl, int n, ParticleDisplay particleDisplay) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Particles.chaoticDoublePendulum(d, d2, d3, d4, d5, d6, bl, n, particleDisplay), 0L, 1L);
    }

    public static Runnable magicCircles(final double d, final double d2, final double d3, final double d4, final ParticleDisplay particleDisplay) {
        return new Runnable(){
            final double radiusDiv;
            final Vector dir;
            double dynamicRadius;
            {
                this.radiusDiv = Math.PI / d3;
                this.dir = particleDisplay.getLocation().getDirection().normalize().multiply(d4);
                this.dynamicRadius = d;
            }

            @Override
            public void run() {
                double d5 = Math.PI / (d2 * this.dynamicRadius);
                for (double d22 = 0.0; d22 < Math.PI * 2; d22 += d5) {
                    double d32 = this.dynamicRadius * Math.sin(d22);
                    double d42 = this.dynamicRadius * Math.cos(d22);
                    particleDisplay.spawn(d32, 0.0, d42);
                }
                this.dynamicRadius += this.radiusDiv;
                particleDisplay.getLocation().add(this.dir);
            }
        };
    }

    public static BukkitTask magicCircles(Plugin plugin, double d, double d2, double d3, double d4, ParticleDisplay particleDisplay) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Particles.magicCircles(d, d2, d3, d4, particleDisplay), 0L, 1L);
    }

    public static void infinity(double d, double d2, ParticleDisplay particleDisplay) {
        double d3 = Math.PI / d2;
        for (double d4 = 0.0; d4 < Math.PI * 2; d4 += d3) {
            double d5 = Math.sin(d4);
            double d6 = Math.pow(d5, 2.0) + 1.0;
            double d7 = d * Math.cos(d4);
            double d8 = d7 / d6;
            double d9 = d7 * d5 / d6;
            Particles.circle(1.0, d2, particleDisplay.cloneWithLocation(d5, d9, d8));
        }
    }

    public static void cone(double d, double d2, double d3, double d4, ParticleDisplay particleDisplay) {
        double d5 = d2 / (d / d3);
        for (double d6 = 0.0; d6 < d; d6 += d3) {
            if ((d2 -= d5) < 0.0) {
                d2 = 0.0;
            }
            Particles.circle(d2, d4 - d6, particleDisplay.cloneWithLocation(0.0, d6, 0.0));
        }
    }

    public static void slash(double d, boolean bl, ParticleDisplay particleDisplay) {
        double d2 = bl ? 1.5707963267948966 : 0.0;
        double d3 = bl ? R270 : Math.PI;
        Particles.ellipse(d2, d3, 0.10471975511965977, d, d + 2.0, particleDisplay);
    }

    public static void slash(Plugin plugin, final double d, final boolean bl, final Supplier<Double> supplier, final Supplier<Double> supplier2, final ParticleDisplay particleDisplay) {
        new BukkitRunnable(){
            double distanceTraveled = 0.0;

            public void run() {
                Particles.slash((Double)supplier.get(), bl, particleDisplay);
                double d2 = (Double)supplier2.get();
                this.distanceTraveled += d2;
                if (this.distanceTraveled >= d) {
                    this.cancel();
                } else {
                    particleDisplay.advanceInDirection(d2);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 1L, 1L);
    }

    public static void ellipse(double d, double d2, double d3, double d4, double d5, ParticleDisplay particleDisplay) {
        for (double d6 = d; d6 <= d2; d6 += d3) {
            double d7 = d4 * Math.cos(d6);
            double d8 = d5 * Math.sin(d6);
            particleDisplay.spawn(d7, 0.0, d8);
        }
    }

    public static BooleanSupplier blackhole(final int n, final double d, final double d2, final int n2, final int n3, final ParticleDisplay particleDisplay) {
        particleDisplay.extra = 0.1;
        return new BooleanSupplier(){
            final double rateDiv;
            int timer;
            double theta;
            boolean done;
            {
                this.rateDiv = Math.PI / d2;
                this.timer = n3;
                this.theta = 0.0;
                this.done = false;
            }

            @Override
            public boolean getAsBoolean() {
                if (this.done) {
                    return false;
                }
                for (int i = 0; i < n; ++i) {
                    double d7 = Math.PI * 2 * ((double)i / (double)n);
                    double d22 = d * Math.cos(this.theta + d7);
                    double d3 = d * Math.sin(this.theta + d7);
                    double d4 = Math.atan2(d3, d22);
                    double d5 = -Math.cos(d4);
                    double d6 = -Math.sin(d4);
                    particleDisplay.particleDirection(d5, 0.0, d6);
                    particleDisplay.spawn(d22, 0.0, d3);
                    if (n2 <= 1) continue;
                    d22 = d * Math.cos(-this.theta + d7);
                    d3 = d * Math.sin(-this.theta + d7);
                    if (n2 == 2) {
                        d4 = Math.atan2(d3, d22);
                    } else if (n2 == 3) {
                        d4 = Math.atan2(d22, d3);
                    } else if (n2 == 4) {
                        Math.atan2(Math.log(d22), Math.log(d3));
                    }
                    d5 = -Math.cos(d4);
                    d6 = -Math.sin(d4);
                    particleDisplay.particleDirection(d5, 0.0, d6);
                    particleDisplay.spawn(d22, 0.0, d3);
                }
                this.theta += this.rateDiv;
                if (--this.timer <= 0) {
                    this.done = true;
                    return false;
                }
                return true;
            }
        };
    }

    public static BukkitTask blackhole(Plugin plugin, int n, double d, double d2, int n2, int n3, ParticleDisplay particleDisplay) {
        final BooleanSupplier booleanSupplier = Particles.blackhole(n, d, d2, n2, n3, particleDisplay);
        return new BukkitRunnable(){

            public void run() {
                if (!booleanSupplier.getAsBoolean()) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    public static void rainbow(double d, double d2, double d3, double d4, double d5, ParticleDisplay particleDisplay) {
        int[][] nArrayArray = new int[][]{{128, 0, 128}, {75, 0, 130}, {0, 0, 255}, {0, 255, 0}, {255, 255, 0}, {255, 140, 0}, {255, 0, 0}};
        double d6 = d * d3;
        for (int i = 0; i < 7; ++i) {
            int[] nArray = nArrayArray[i];
            particleDisplay = ParticleDisplay.of(XParticle.DUST).withLocation(particleDisplay.getLocation()).withColor(new Color(nArray[0], nArray[1], nArray[2]), 1.0f);
            int n = 0;
            while ((double)n < d4) {
                double d7 = Math.PI / (d2 * (double)(i + 2));
                for (double d8 = 0.0; d8 <= Math.PI; d8 += d7) {
                    double d9 = d * Math.cos(d8);
                    double d10 = d6 * Math.sin(d8);
                    particleDisplay.spawn(d9, d10, 0.0);
                }
                d += d5;
                ++n;
            }
        }
    }

    public static void crescent(double d, double d2, ParticleDisplay particleDisplay) {
        double d3 = Math.PI / d2;
        double d4 = Math.toRadians(325.0);
        for (double d5 = Math.toRadians(45.0); d5 <= d4; d5 += d3) {
            double d6 = Math.cos(d5);
            double d7 = Math.sin(d5);
            particleDisplay.spawn(d * d6, 0.0, d * d7);
            double d8 = d / 1.3;
            particleDisplay.spawn(d8 * d6 + 0.8, 0.0, d8 * d7);
        }
    }

    public static void waveFunction(double d, double d2, double d3, double d4, ParticleDisplay particleDisplay) {
        double d5 = d2 / 2.0;
        boolean bl = true;
        double d6 = Particles.random(d2 / 2.0, d2);
        double d7 = Math.PI / d4;
        d3 *= Math.PI * 2;
        for (double d8 = 0.0; d8 <= d3; d8 += d7) {
            double d9 = d * d8;
            double d10 = Math.sin(d8);
            if (d10 == 1.0) {
                bl = !bl;
                d6 = bl ? Particles.random(d2 / 2.0, d2) : Particles.random(-d2, -d2 / 2.0);
            }
            d5 += d6;
            for (double d11 = 0.0; d11 <= d3; d11 += d7) {
                double d12 = Math.cos(d11);
                double d13 = d5 * d10 * d12;
                double d14 = d * d11;
                particleDisplay.spawn(d9, d13, d14);
            }
        }
    }

    public static Runnable vortex(final int n, double d, final ParticleDisplay particleDisplay) {
        final double d2 = Math.PI / d;
        return new Runnable(){
            double theta = 0.0;

            @Override
            public void run() {
                this.theta += d2;
                for (int i = 0; i < n; ++i) {
                    double d = Math.PI * 2 * ((double)i / (double)n);
                    double d22 = Math.cos(this.theta + d);
                    double d3 = Math.sin(this.theta + d);
                    double d4 = Math.atan2(d3, d22);
                    double d5 = Math.cos(d4);
                    double d6 = Math.sin(d4);
                    particleDisplay.particleDirection(d5, 0.0, d6);
                    particleDisplay.spawn(d22, 0.0, d3);
                }
            }
        };
    }

    public static BukkitTask vortex(Plugin plugin, int n, double d, ParticleDisplay particleDisplay) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Particles.vortex(n, d, particleDisplay), 0L, 1L);
    }

    public static void cylinder(double d, double d2, double d3, ParticleDisplay particleDisplay) {
        Particles.filledCircle(d2, d3, 3.0, particleDisplay);
        Particles.filledCircle(d2, d3, 3.0, particleDisplay.cloneWithLocation(0.0, d, 0.0));
        for (double d4 = 0.0; d4 < d; d4 += 0.1) {
            Particles.circle(d2, d3, particleDisplay.cloneWithLocation(0.0, d4, 0.0));
        }
    }

    public static Runnable moveRotatingAround(final double d, final double d2, final double d3, final double d4, final Runnable runnable, final ParticleDisplay ... particleDisplayArray) {
        return new Runnable(){
            double rotation = 180.0;

            @Override
            public void run() {
                this.rotation += d;
                double d5 = Math.toRadians(90.0 + this.rotation);
                double d22 = Math.toRadians(60.0 + this.rotation);
                double d32 = Math.toRadians(30.0 + this.rotation);
                Vector vector = new Vector(d2 * Math.PI, d3 * Math.PI, d4 * Math.PI);
                if (d2 != 0.0) {
                    ParticleDisplay.rotateAround(vector, ParticleDisplay.Axis.X, d5);
                }
                if (d3 != 0.0) {
                    ParticleDisplay.rotateAround(vector, ParticleDisplay.Axis.Y, d22);
                }
                if (d4 != 0.0) {
                    ParticleDisplay.rotateAround(vector, ParticleDisplay.Axis.Z, d32);
                }
                for (ParticleDisplay particleDisplay : particleDisplayArray) {
                    particleDisplay.getLocation().add(vector);
                }
                runnable.run();
                for (ParticleDisplay particleDisplay : particleDisplayArray) {
                    particleDisplay.getLocation().subtract(vector);
                }
            }
        };
    }

    public static BukkitTask moveRotatingAround(Plugin plugin, long l, double d, double d2, double d3, double d4, Runnable runnable, ParticleDisplay ... particleDisplayArray) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Particles.moveRotatingAround(d, d2, d3, d4, runnable, particleDisplayArray), 0L, l);
    }

    public static Runnable moveAround(final double d, final double d2, final double d3, final double d4, final double d5, final Runnable runnable, final ParticleDisplay ... particleDisplayArray) {
        return new Runnable(){
            double multiplier = 0.0;
            boolean opposite = false;

            @Override
            public void run() {
                this.multiplier = this.opposite ? (this.multiplier -= d) : (this.multiplier += d);
                double d6 = this.multiplier * d3;
                double d22 = this.multiplier * d4;
                double d32 = this.multiplier * d5;
                for (ParticleDisplay particleDisplay : particleDisplayArray) {
                    particleDisplay.getLocation().add(d6, d22, d32);
                }
                runnable.run();
                for (ParticleDisplay particleDisplay : particleDisplayArray) {
                    particleDisplay.getLocation().subtract(d6, d22, d32);
                }
                if (this.opposite) {
                    if (this.multiplier <= 0.0) {
                        this.opposite = false;
                    }
                } else if (this.multiplier >= d2) {
                    this.opposite = true;
                }
            }
        };
    }

    public static BukkitTask moveAround(Plugin plugin, long l, double d, double d2, double d3, double d4, double d5, Runnable runnable, ParticleDisplay ... particleDisplayArray) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Particles.moveAround(d, d2, d3, d4, d5, runnable, particleDisplayArray), 0L, l);
    }

    public static BukkitTask testDisplay(Plugin plugin, Runnable runnable) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, 0L, 1L);
    }

    public static Runnable rotateAround(final double d, final double d2, final double d3, final double d4, final Runnable runnable, final ParticleDisplay ... particleDisplayArray) {
        return new Runnable(){
            double rotation = 180.0;

            @Override
            public void run() {
                this.rotation += d;
                double d5 = Math.toRadians((90.0 + this.rotation) * d2);
                double d22 = Math.toRadians((60.0 + this.rotation) * d3);
                double d32 = Math.toRadians((30.0 + this.rotation) * d4);
                for (ParticleDisplay particleDisplay : particleDisplayArray) {
                    particleDisplay.rotate(d5, d22, d32);
                }
                runnable.run();
            }
        };
    }

    public static BukkitTask rotateAround(Plugin plugin, long l, double d, double d2, double d3, double d4, Runnable runnable, ParticleDisplay ... particleDisplayArray) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Particles.rotateAround(d, d2, d3, d4, runnable, particleDisplayArray), 0L, l);
    }

    public static Runnable guard(final double d, final double d2, final double d3, final double d4, final Runnable runnable, final ParticleDisplay ... particleDisplayArray) {
        return new Runnable(){
            double rotation = 180.0;

            @Override
            public void run() {
                this.rotation += d;
                double d5 = Math.toRadians((90.0 + this.rotation) * d2);
                double d22 = Math.toRadians((60.0 + this.rotation) * d3);
                double d32 = Math.toRadians((30.0 + this.rotation) * d4);
                Vector vector = new Vector(d2 * Math.PI, d3 * Math.PI, d4 * Math.PI);
                ParticleDisplay.rotateAround(vector, d5, d22, d32);
                for (ParticleDisplay particleDisplay : particleDisplayArray) {
                    particleDisplay.rotate(d5, d22, d32);
                    particleDisplay.getLocation().add(vector);
                }
                runnable.run();
                for (ParticleDisplay particleDisplay : particleDisplayArray) {
                    particleDisplay.getLocation().subtract(vector);
                }
            }
        };
    }

    public static BukkitTask guard(Plugin plugin, long l, double d, double d2, double d3, double d4, Runnable runnable, ParticleDisplay ... particleDisplayArray) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Particles.guard(d, d2, d3, d4, runnable, particleDisplayArray), 0L, l);
    }

    public static void sphere(double d, double d2, ParticleDisplay particleDisplay) {
        double d3 = Math.PI / d2;
        for (double d4 = 0.0; d4 <= Math.PI; d4 += d3) {
            double d5 = d * Math.cos(d4);
            double d6 = d * Math.sin(d4);
            for (double d7 = 0.0; d7 <= Math.PI * 2; d7 += d3) {
                double d8 = Math.cos(d7) * d6;
                double d9 = Math.sin(d7) * d6;
                if (particleDisplay.isDirectional()) {
                    double d10 = Math.atan2(d9, d8);
                    double d11 = Math.cos(d10);
                    double d12 = Math.sin(Math.atan2(d6, d5));
                    double d13 = Math.sin(d10);
                    particleDisplay.particleDirection(d11, d12, d13);
                }
                particleDisplay.spawn(d8, d5, d9);
            }
        }
    }

    public static void spikeSphere(double d, double d2, int n, double d3, double d4, ParticleDisplay particleDisplay) {
        double d5 = Math.PI / d2;
        for (double d6 = 0.0; d6 <= Math.PI; d6 += d5) {
            double d7 = d * Math.cos(d6);
            double d8 = d * Math.sin(d6);
            for (double d9 = 0.0; d9 <= Math.PI * 2; d9 += d5) {
                double d10 = Math.cos(d9) * d8;
                double d11 = Math.sin(d9) * d8;
                if (n != 0 && Particles.randInt(0, n) != 1) continue;
                Location location = particleDisplay.cloneLocation(d10, d7, d11);
                Vector vector = location.clone().subtract(particleDisplay.getLocation()).toVector().multiply(Particles.random(d3, d4));
                Location location2 = location.clone().add(vector);
                Particles.line(location, location2, 0.1, particleDisplay);
            }
        }
    }

    public static void ring(double d, double d2, double d3, ParticleDisplay particleDisplay) {
        double d4 = Math.PI / d;
        double d5 = Math.PI / d3;
        for (double d6 = 0.0; d6 <= Math.PI * 2; d6 += d4) {
            double d7 = Math.cos(d6);
            double d8 = Math.sin(d6);
            for (double d9 = 0.0; d9 <= Math.PI * 2; d9 += d5) {
                double d10 = d2 + d3 * Math.cos(d9);
                double d11 = d10 * d7;
                double d12 = d10 * d8;
                double d13 = d3 * Math.sin(d9);
                particleDisplay.spawn(d11, d12, d13);
            }
        }
    }

    public static BooleanSupplier spread(final int n, final int n2, final Location location, final Location location2, final double d, final double d2, final double d3, final ParticleDisplay particleDisplay) {
        return new BooleanSupplier(){
            int count;
            boolean done;
            {
                this.count = n;
                this.done = false;
            }

            @Override
            public boolean getAsBoolean() {
                if (this.done) {
                    return false;
                }
                int n3 = n2;
                while (n3-- != 0) {
                    double d4 = Particles.random(-d, d);
                    double d22 = Particles.random(-d2, d2);
                    double d32 = Particles.random(-d3, d3);
                    Location location3 = location2.clone().add(d4, d22, d32);
                    Particles.line(location, location3, 0.1, particleDisplay);
                }
                if (this.count-- <= 0) {
                    this.done = true;
                    return false;
                }
                return true;
            }
        };
    }

    public static BukkitTask spread(Plugin plugin, int n, int n2, Location location, Location location2, double d, double d2, double d3, ParticleDisplay particleDisplay) {
        final BooleanSupplier booleanSupplier = Particles.spread(n, n2, location, location2, d, d2, d3, particleDisplay);
        return new BukkitRunnable(){

            public void run() {
                if (!booleanSupplier.getAsBoolean()) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    public static void heart(double d, double d2, double d3, double d4, double d5, ParticleDisplay particleDisplay) {
        for (double d6 = 0.0; d6 <= Math.PI * 2; d6 += Math.PI / d5) {
            double d7 = d6 / d;
            double d8 = Math.cos(d7);
            double d9 = Math.sin(d7);
            double d10 = Math.pow(Math.abs(Math.sin(2.0 * d2 * d7)) + d3 * Math.abs(Math.sin(d2 * d7)), 1.0 / d4);
            double d11 = d10 * (d9 + d8);
            double d12 = d10 * (d8 - d9);
            particleDisplay.spawn(0.0, d11, d12);
        }
    }

    public static Runnable atomic(final int n, final double d, final double d2, final ParticleDisplay particleDisplay) {
        return new Runnable(){
            final double rateDiv;
            final double dist;
            double theta;
            {
                this.rateDiv = Math.PI / d2;
                this.dist = Math.PI / (double)n;
                this.theta = 0.0;
            }

            @Override
            public void run() {
                int n2 = n;
                this.theta += this.rateDiv;
                double d4 = d * Math.cos(this.theta);
                double d22 = d * Math.sin(this.theta);
                double d3 = 0.0;
                while (n2 > 0) {
                    particleDisplay.rotate(ParticleDisplay.Rotation.of(d3, ParticleDisplay.Axis.Z));
                    particleDisplay.spawn(d4, 0.0, d22);
                    --n2;
                    d3 += this.dist;
                }
            }
        };
    }

    public static BukkitTask atomic(Plugin plugin, int n, double d, double d2, ParticleDisplay particleDisplay) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Particles.atomic(n, d, d2, particleDisplay), 0L, 1L);
    }

    public static BooleanSupplier helix(final int n, final double d, final double d2, final double d3, final double d4, final double d5, final double d6, final boolean bl, final boolean bl2, final ParticleDisplay particleDisplay) {
        return new BooleanSupplier(){
            final double distanceBetweenEachCirclePoints;
            final double radiusDiv;
            final double radiusDiv2;
            double dynamicRadius;
            boolean center;
            final double calculatedRotRate;
            double rotation;
            double currentDistance;
            {
                this.distanceBetweenEachCirclePoints = Math.PI * 2 / (double)n;
                this.radiusDiv = d / (d4 / d2);
                this.radiusDiv2 = bl && bl2 ? this.radiusDiv * 2.0 : this.radiusDiv;
                this.dynamicRadius = bl2 ? 0.0 : d;
                this.center = !bl2;
                this.calculatedRotRate = this.distanceBetweenEachCirclePoints / d6;
                this.rotation = 0.0;
                this.currentDistance = 0.0;
            }

            @Override
            public boolean getAsBoolean() {
                if (this.currentDistance >= d4) {
                    return false;
                }
                if (!this.center) {
                    this.dynamicRadius += this.radiusDiv2;
                    if (this.dynamicRadius >= d) {
                        this.center = true;
                    }
                } else if (bl) {
                    this.dynamicRadius -= this.radiusDiv2;
                }
                for (double d7 = 0.0; d7 < (double)n; d7 += 1.0) {
                    double d22 = d7 * this.distanceBetweenEachCirclePoints * d3 + this.rotation;
                    double d32 = this.dynamicRadius * Math.cos(d22);
                    double d42 = this.dynamicRadius * Math.sin(d22);
                    particleDisplay.spawn(d32, 0.0, d42);
                }
                this.currentDistance += d5;
                if (this.currentDistance < d4) {
                    particleDisplay.advanceInDirection(d5);
                } else {
                    particleDisplay.advanceInDirection(d5 - (this.currentDistance - d4));
                }
                this.rotation += this.calculatedRotRate;
                return true;
            }
        };
    }

    public static BukkitTask helix(Plugin plugin, int n, double d, double d2, double d3, double d4, double d5, double d6, boolean bl, boolean bl2, ParticleDisplay particleDisplay) {
        final BooleanSupplier booleanSupplier = Particles.helix(n, d, d2, d3, d4, d5, d6, bl, bl2, particleDisplay);
        return new BukkitRunnable(){

            public void run() {
                if (!booleanSupplier.getAsBoolean()) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    public static void lightning(Location location, Vector vector, int n, int n2, double d, double d2, double d3, double d4, double d5, double d6, double d7, ParticleDisplay particleDisplay) {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        if (n <= 0) {
            return;
        }
        boolean bl = true;
        while (threadLocalRandom.nextDouble() < d6 || bl) {
            Vector vector2 = new Vector(threadLocalRandom.nextDouble(-d, d), threadLocalRandom.nextDouble(-d, d), threadLocalRandom.nextDouble(-d, d)).normalize().multiply(threadLocalRandom.nextDouble(-d, d) * d2);
            Vector vector3 = location.clone().toVector().add(vector.clone().multiply(d4)).add(vector2);
            Location location2 = vector3.toLocation(location.getWorld());
            if (location2.distance(location) <= d4) {
                bl = true;
                continue;
            }
            bl = false;
            int n3 = (int)(location.distance(location2) / 0.1);
            Vector vector4 = vector3.clone().subtract(location.toVector()).normalize().multiply(0.1);
            for (int i = 0; i < n3; ++i) {
                Location location3 = location.clone().add(vector4.clone().multiply(i));
                particleDisplay.spawn(location3);
            }
            Particles.lightning(location2.clone(), vector, n - 1, n2 - 1, d, d2 * d3, d3, d4 * d5, d5, d6 * d7, d7, particleDisplay);
            if (n2 > 0) continue;
            break;
        }
    }

    public static void dna(double d, double d2, double d3, int n, int n2, ParticleDisplay particleDisplay, ParticleDisplay particleDisplay2) {
        int n3 = 0;
        for (double d4 = 0.0; d4 <= (double)n; d4 += d2) {
            ++n3;
            double d5 = d * Math.cos(d3 * d4);
            double d6 = d * Math.sin(d3 * d4);
            Location location = particleDisplay.getLocation().clone().add(d5, d4, d6);
            particleDisplay.spawn(d5, d4, d6);
            Location location2 = particleDisplay.getLocation().clone().subtract(d5, -d4, d6);
            particleDisplay.spawn(-d5, d4, -d6);
            if (n3 < n2) continue;
            n3 = 0;
            Particles.line(location, location2, d2 * 2.0, particleDisplay2);
        }
    }

    public static BooleanSupplier dnaReplication(final double d, final double d2, final int n, final double d3, final int n2, final int n3, final ParticleDisplay particleDisplay) {
        final ParticleDisplay particleDisplay2 = ParticleDisplay.of(XParticle.DUST).withColor(Color.BLUE, 1.0f);
        final ParticleDisplay particleDisplay3 = ParticleDisplay.of(XParticle.DUST).withColor(Color.YELLOW, 1.0f);
        final ParticleDisplay particleDisplay4 = ParticleDisplay.of(XParticle.DUST).withColor(Color.GREEN, 1.0f);
        final ParticleDisplay particleDisplay5 = ParticleDisplay.of(XParticle.DUST).withColor(Color.RED, 1.0f);
        return new BooleanSupplier(){
            double y = 0.0;
            int nucleotideDist = 0;
            boolean done = false;

            @Override
            public boolean getAsBoolean() {
                if (this.done) {
                    return false;
                }
                int n4 = n;
                while (n4-- != 0) {
                    this.y += d2;
                    ++this.nucleotideDist;
                    double d4 = d * Math.cos(d3 * this.y);
                    double d22 = d * Math.sin(d3 * this.y);
                    Location location = particleDisplay.getLocation().clone().add(d4, this.y, d22);
                    Particles.circle(0.1, 10.0, particleDisplay.cloneWithLocation(d4, this.y, d22));
                    Location location2 = particleDisplay.getLocation().clone().subtract(d4, -this.y, d22);
                    Particles.circle(0.1, 10.0, particleDisplay.cloneWithLocation(-d4, this.y, -d22));
                    Location location3 = location.toVector().midpoint(location2.toVector()).toLocation(location.getWorld());
                    if (this.nucleotideDist >= n3) {
                        this.nucleotideDist = 0;
                        if (Particles.randInt(0, 1) == 1) {
                            Particles.line(location, location3, d2 - 0.1, particleDisplay2);
                            Particles.line(location2, location3, d2 - 0.1, particleDisplay3);
                        } else {
                            Particles.line(location, location3, d2 - 0.1, particleDisplay5);
                            Particles.line(location2, location3, d2 - 0.1, particleDisplay4);
                        }
                    }
                    if (!(this.y > (double)n2)) continue;
                    this.done = true;
                    return false;
                }
                return true;
            }
        };
    }

    public static BukkitTask dnaReplication(Plugin plugin, double d, double d2, int n, double d3, int n2, int n3, ParticleDisplay particleDisplay) {
        final BooleanSupplier booleanSupplier = Particles.dnaReplication(d, d2, n, d3, n2, n3, particleDisplay);
        return new BukkitRunnable(){

            public void run() {
                if (!booleanSupplier.getAsBoolean()) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    public static void drawLine(Player player, double d, double d2, ParticleDisplay particleDisplay) {
        Location location = player.getEyeLocation();
        Particles.line(location, location.clone().add(location.getDirection().multiply(d)), d2, particleDisplay);
    }

    public static Runnable cloud(ParticleDisplay particleDisplay, ParticleDisplay particleDisplay2) {
        return () -> {
            particleDisplay.spawn();
            particleDisplay2.spawn();
        };
    }

    public static BukkitTask cloud(Plugin plugin, ParticleDisplay particleDisplay, ParticleDisplay particleDisplay2) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Particles.cloud(particleDisplay, particleDisplay2), 0L, 1L);
    }

    public static void line(Location location, Location location2, double d, ParticleDisplay particleDisplay) {
        d = Math.abs(d);
        double d2 = location2.getX() - location.getX();
        double d3 = location2.getY() - location.getY();
        double d4 = location2.getZ() - location.getZ();
        double d5 = Math.sqrt(NumberConversions.square((double)d2) + NumberConversions.square((double)d3) + NumberConversions.square((double)d4));
        d2 /= d5;
        d3 /= d5;
        d4 /= d5;
        ParticleDisplay particleDisplay2 = particleDisplay.clone();
        particleDisplay2.withLocation(location);
        for (double d6 = 0.0; d6 < d5; d6 += d) {
            if (d6 > d5) {
                d6 = d5;
            }
            particleDisplay2.spawn(d2 * d6, d3 * d6, d4 * d6);
        }
    }

    public static void rectangle(Location location, Location location2, double d, ParticleDisplay particleDisplay) {
        particleDisplay.withLocation(location);
        double d2 = Math.max(location.getX(), location2.getX());
        double d3 = Math.min(location.getX(), location2.getX());
        double d4 = Math.max(location.getY(), location2.getY());
        double d5 = Math.min(location.getY(), location2.getY());
        for (double d6 = d3; d6 <= d2; d6 += d) {
            for (double d7 = d5; d7 <= d4; d7 += d) {
                particleDisplay.spawn(d6 - d3, d7 - d5, 0.0);
            }
        }
    }

    public static void cage(Location location, Location location2, double d, double d2, ParticleDisplay particleDisplay) {
        double d3 = Math.max(location.getX(), location2.getX());
        double d4 = Math.min(location.getX(), location2.getX());
        double d5 = Math.max(location.getZ(), location2.getZ());
        double d6 = Math.min(location.getZ(), location2.getZ());
        double d7 = 0.0;
        for (double d8 = d4; d8 <= d3; d8 += d) {
            for (double d9 = d6; d9 <= d5; d9 += d) {
                Location location3 = particleDisplay.spawn(d8 - d4, 0.0, d9 - d6);
                Location location4 = particleDisplay.spawn(d8 - d4, 3.0, d9 - d6);
                if (d8 != d4 && !(d8 + d > d3) && d9 != d6 && !(d9 + d > d5) || !((d7 += 1.0) >= d2)) continue;
                d7 = 0.0;
                Particles.line(location3, location4, d, particleDisplay);
            }
        }
    }

    public static void filledCube(Location location, Location location2, double d, ParticleDisplay particleDisplay) {
        particleDisplay.withLocation(location);
        double d2 = Math.max(location.getX(), location2.getX());
        double d3 = Math.min(location.getX(), location2.getX());
        double d4 = Math.max(location.getY(), location2.getY());
        double d5 = Math.min(location.getY(), location2.getY());
        double d6 = Math.max(location.getZ(), location2.getZ());
        double d7 = Math.min(location.getZ(), location2.getZ());
        for (double d8 = d3; d8 <= d2; d8 += d) {
            for (double d9 = d5; d9 <= d4; d9 += d) {
                for (double d10 = d7; d10 <= d6; d10 += d) {
                    particleDisplay.spawn(d8 - d3, d9 - d5, d10 - d7);
                }
            }
        }
    }

    public static void cube(Location location, Location location2, double d, ParticleDisplay particleDisplay) {
        particleDisplay.withLocation(location);
        double d2 = Math.max(location.getX(), location2.getX());
        double d3 = Math.min(location.getX(), location2.getX());
        double d4 = Math.max(location.getY(), location2.getY());
        double d5 = Math.min(location.getY(), location2.getY());
        double d6 = Math.max(location.getZ(), location2.getZ());
        double d7 = Math.min(location.getZ(), location2.getZ());
        for (double d8 = d3; d8 <= d2; d8 += d) {
            for (double d9 = d5; d9 <= d4; d9 += d) {
                for (double d10 = d7; d10 <= d6; d10 += d) {
                    if (d9 != d5 && !(d9 + d > d4) && d8 != d3 && !(d8 + d > d2) && d10 != d7 && !(d10 + d > d6)) continue;
                    particleDisplay.spawn(d8 - d3, d9 - d5, d10 - d7);
                }
            }
        }
    }

    public static void structuredCube(Location location, Location location2, double d, ParticleDisplay particleDisplay) {
        particleDisplay.withLocation(location);
        double d2 = Math.max(location.getX(), location2.getX());
        double d3 = Math.min(location.getX(), location2.getX());
        double d4 = Math.max(location.getY(), location2.getY());
        double d5 = Math.min(location.getY(), location2.getY());
        double d6 = Math.max(location.getZ(), location2.getZ());
        double d7 = Math.min(location.getZ(), location2.getZ());
        for (double d8 = d3; d8 <= d2; d8 += d) {
            for (double d9 = d5; d9 <= d4; d9 += d) {
                for (double d10 = d7; d10 <= d6; d10 += d) {
                    int n = 0;
                    if (d8 == d3 || d8 + d > d2) {
                        ++n;
                    }
                    if (d9 == d5 || d9 + d > d4) {
                        ++n;
                    }
                    if (d10 == d7 || d10 + d > d6) {
                        ++n;
                    }
                    if (n < 2) continue;
                    particleDisplay.spawn(d8 - d3, d9 - d5, d10 - d7);
                }
            }
        }
    }

    public static void hypercube(Location location, Location location2, double d, double d2, int n, ParticleDisplay particleDisplay) {
        ArrayList<Location> arrayList = null;
        for (int i = 0; i < n + 1; ++i) {
            ArrayList<Location> arrayList2 = new ArrayList<Location>(8);
            Location location3 = location.clone().subtract((double)i * d2, (double)i * d2, (double)i * d2);
            Location location4 = location2.clone().add((double)i * d2, (double)i * d2, (double)i * d2);
            particleDisplay.withLocation(location3);
            double d3 = Math.max(location3.getX(), location4.getX());
            double d4 = Math.min(location3.getX(), location4.getX());
            double d5 = Math.max(location3.getY(), location4.getY());
            double d6 = Math.min(location3.getY(), location4.getY());
            double d7 = Math.max(location3.getZ(), location4.getZ());
            double d8 = Math.min(location3.getZ(), location4.getZ());
            arrayList2.add(new Location(location3.getWorld(), d3, d5, d7));
            arrayList2.add(new Location(location3.getWorld(), d4, d6, d8));
            arrayList2.add(new Location(location3.getWorld(), d3, d6, d7));
            arrayList2.add(new Location(location3.getWorld(), d4, d5, d8));
            arrayList2.add(new Location(location3.getWorld(), d4, d6, d7));
            arrayList2.add(new Location(location3.getWorld(), d3, d6, d8));
            arrayList2.add(new Location(location3.getWorld(), d3, d5, d8));
            arrayList2.add(new Location(location3.getWorld(), d4, d5, d7));
            if (arrayList != null) {
                for (int j = 0; j < 8; ++j) {
                    Location location5 = (Location)arrayList2.get(j);
                    Location location6 = (Location)arrayList.get(j);
                    Particles.line(location6, location5, d, particleDisplay);
                }
            }
            arrayList = arrayList2;
            for (double d9 = d4; d9 <= d3; d9 += d) {
                for (double d10 = d6; d10 <= d5; d10 += d) {
                    for (double d11 = d8; d11 <= d7; d11 += d) {
                        int n2 = 0;
                        if (d9 == d4 || d9 + d > d3) {
                            ++n2;
                        }
                        if (d10 == d6 || d10 + d > d5) {
                            ++n2;
                        }
                        if (d11 == d8 || d11 + d > d7) {
                            ++n2;
                        }
                        if (n2 < 2) continue;
                        particleDisplay.spawn(d9 - d4, d10 - d6, d11 - d8);
                    }
                }
            }
        }
    }

    public static BooleanSupplier tesseract(final double d, final double d2, final double d3, final long l, final ParticleDisplay particleDisplay) {
        int n;
        final double[][] dArrayArray = new double[][]{{-1.0, -1.0, -1.0, 1.0}, {1.0, -1.0, -1.0, 1.0}, {1.0, 1.0, -1.0, 1.0}, {-1.0, 1.0, -1.0, 1.0}, {-1.0, -1.0, 1.0, 1.0}, {1.0, -1.0, 1.0, 1.0}, {1.0, 1.0, 1.0, 1.0}, {-1.0, 1.0, 1.0, 1.0}, {-1.0, -1.0, -1.0, -1.0}, {1.0, -1.0, -1.0, -1.0}, {1.0, 1.0, -1.0, -1.0}, {-1.0, 1.0, -1.0, -1.0}, {-1.0, -1.0, 1.0, -1.0}, {1.0, -1.0, 1.0, -1.0}, {1.0, 1.0, 1.0, -1.0}, {-1.0, 1.0, 1.0, -1.0}};
        final ArrayList<int[]> arrayList = new ArrayList<int[]>();
        int n2 = 1;
        for (n = 0; n <= n2; ++n) {
            int n3;
            for (int i = n3 = 8 * n; i < n3 + 4; ++i) {
                arrayList.add(new int[]{i, (i + 1) % 4 + n3});
                arrayList.add(new int[]{i + 4, (i + 1) % 4 + 4 + n3});
                arrayList.add(new int[]{i, i + 4});
            }
        }
        for (n = 0; n < (n2 + 1) * 4; ++n) {
            arrayList.add(new int[]{n, n + 8});
        }
        return new BooleanSupplier(){
            double angle = 0.0;
            long repeat = 0L;
            boolean done = false;

            @Override
            public boolean getAsBoolean() {
                double[] dArray;
                Object[] objectArray;
                if (this.done) {
                    return false;
                }
                double d4 = Math.cos(this.angle);
                double d22 = Math.sin(this.angle);
                double[][] dArrayArray4 = new double[][]{{d4, -d22, 0.0, 0.0}, {d22, d4, 0.0, 0.0}, {0.0, 0.0, 1.0, 0.0}, {0.0, 0.0, 0.0, 1.0}};
                double[][] dArrayArray2 = new double[][]{{1.0, 0.0, 0.0, 0.0}, {0.0, 1.0, 0.0, 0.0}, {0.0, 0.0, d4, -d22}, {0.0, 0.0, d22, d4}};
                double[][] dArray2 = new double[dArrayArray.length][4];
                for (int i = 0; i < dArrayArray.length; ++i) {
                    objectArray = dArrayArray[i];
                    dArray = Particles.matrix(dArrayArray4, objectArray);
                    dArray = Particles.matrix(dArrayArray2, dArray);
                    int n = 2;
                    double d32 = 1.0 / ((double)n - dArray[3]);
                    double[][] dArrayArray3 = new double[][]{{d32, 0.0, 0.0, 0.0}, {0.0, d32, 0.0, 0.0}, {0.0, 0.0, d32, 0.0}};
                    double[] dArray3 = Particles.matrix(dArrayArray3, dArray);
                    int n2 = 0;
                    while (n2 < dArray3.length) {
                        int n3 = n2++;
                        dArray3[n3] = dArray3[n3] * d;
                    }
                    dArray2[i] = dArray3;
                    particleDisplay.spawn(dArray3[0], dArray3[1], dArray3[2]);
                }
                Iterator iterator2 = arrayList.iterator();
                while (iterator2.hasNext()) {
                    objectArray = (int[])iterator2.next();
                    dArray = dArray2[objectArray[0]];
                    double[] dArray4 = dArray2[objectArray[1]];
                    Location location = particleDisplay.cloneLocation(dArray[0], dArray[1], dArray[2]);
                    Location location2 = particleDisplay.cloneLocation(dArray4[0], dArray4[1], dArray4[2]);
                    Particles.line(location, location2, d2, particleDisplay);
                }
                if (++this.repeat > l) {
                    this.done = true;
                    return false;
                }
                this.angle += d3;
                return true;
            }
        };
    }

    public static BukkitTask tesseract(Plugin plugin, double d, double d2, double d3, long l, ParticleDisplay particleDisplay) {
        final BooleanSupplier booleanSupplier = Particles.tesseract(d, d2, d3, l, particleDisplay);
        return new BukkitRunnable(){

            public void run() {
                if (!booleanSupplier.getAsBoolean()) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    private static double[] matrix(double[][] dArray, double[] dArray2) {
        double[][] dArray3 = new double[4][1];
        dArray3[0][0] = dArray2[0];
        dArray3[1][0] = dArray2[1];
        dArray3[2][0] = dArray2[2];
        dArray3[3][0] = dArray2[3];
        int n = dArray[0].length;
        int n2 = dArray.length;
        int n3 = dArray3[0].length;
        int n4 = dArray3.length;
        double[][] dArray4 = new double[n2][n4];
        for (int i = 0; i < n2; ++i) {
            for (int j = 0; j < n3; ++j) {
                float f = 0.0f;
                for (int k = 0; k < n; ++k) {
                    f += (float)(dArray[i][k] * dArray3[k][j]);
                }
                dArray4[i][j] = f;
            }
        }
        double[] dArray5 = new double[4];
        dArray5[0] = dArray4[0][0];
        dArray5[1] = dArray4[1][0];
        dArray5[2] = dArray4[2][0];
        if (dArray4.length > 3) {
            dArray5[3] = dArray4[3][0];
        }
        return dArray5;
    }

    public static void mandelbrot(double d, double d2, double d3, double d4, double d5, int n, ParticleDisplay particleDisplay) {
        for (double d6 = -d; d6 < d; d6 += d3) {
            for (double d7 = -d; d7 < d; d7 += d3) {
                int n2;
                double d8 = 0.0;
                double d9 = 0.0;
                double d10 = (d7 - d4) / d2;
                double d11 = (d6 - d5) / d2;
                for (n2 = n; d9 * d9 + d8 * d8 <= 4.0 && n2 > 0; --n2) {
                    double d12 = d9 * d9 - d8 * d8 + d10;
                    d8 = 2.0 * d9 * d8 + d11;
                    d9 = d12;
                }
                if (n2 != 0) continue;
                particleDisplay.spawn(d7, d6, 0.0);
            }
        }
    }

    public static void julia(double d, double d2, int n, double d3, double d4, ParticleDisplay particleDisplay) {
        double d5 = -0.7;
        double d6 = 0.27015;
        for (double d7 = -d; d7 < d; d7 += 0.1) {
            for (double d8 = -d; d8 < d; d8 += 0.1) {
                int n2;
                double d9 = 1.5 * (d - d / 2.0) / (0.5 * d2 * d) + d3;
                double d10 = (d8 - d / 2.0) / (0.5 * d2 * d) + d4;
                for (n2 = n; d9 * d9 + d10 * d10 < 4.0 && n2 > 0; --n2) {
                    double d11 = d9 * d9 - d10 * d10 + d5;
                    d10 = 2.0 * d9 * d10 + d6;
                    d9 = d11;
                }
                Color color = new Color((n2 << 21) + (n2 << 10) + n2 * 8);
                particleDisplay.withColor(color, 0.8f).spawn(d7, d8, 0.0);
            }
        }
    }

    public static List<BooleanSupplier> star(final int n, int n2, double d, final double d2, final double d3, final double d4, final boolean bl, final int n3, final ParticleDisplay particleDisplay) {
        final double d5 = Math.PI * 2 / (double)n;
        final double d6 = Math.PI / d;
        final ThreadLocalRandom threadLocalRandom = bl ? null : ThreadLocalRandom.current();
        ArrayList<BooleanSupplier> arrayList = new ArrayList<BooleanSupplier>();
        for (int i = 0; i < n2 * 2; ++i) {
            final double d7 = (double)i * Math.PI / (double)n2;
            arrayList.add(new BooleanSupplier(){
                double vein = 0.0;
                double theta = 0.0;
                boolean done = false;

                @Override
                public boolean getAsBoolean() {
                    if (this.done) {
                        return false;
                    }
                    int n2 = n3;
                    while (n2-- != 0) {
                        this.theta += d6;
                        double d = (bl ? this.vein : threadLocalRandom.nextDouble(0.0, d4)) * d2;
                        if (bl) {
                            this.vein += d4;
                        }
                        Vector vector = new Vector(Math.cos(this.theta), 0.0, Math.sin(this.theta));
                        vector.multiply((d2 - d) * d3 / d2);
                        vector.setY(d3 + d);
                        ParticleDisplay.rotateAround(vector, ParticleDisplay.Axis.X, d7);
                        for (int i = 0; i < n; ++i) {
                            ParticleDisplay.rotateAround(vector, ParticleDisplay.Axis.Y, d5);
                            particleDisplay.spawn(vector);
                        }
                    }
                    if (this.theta >= Math.PI * 2) {
                        this.done = true;
                        return false;
                    }
                    return true;
                }
            });
        }
        return arrayList;
    }

    public static List<BukkitTask> star(Plugin plugin, int n, int n2, double d, double d2, double d3, double d4, boolean bl, int n3, ParticleDisplay particleDisplay) {
        ArrayList<BukkitTask> arrayList = new ArrayList<BukkitTask>();
        for (final BooleanSupplier booleanSupplier : Particles.star(n, n2, d, d2, d3, d4, bl, n3, particleDisplay)) {
            arrayList.add(new BukkitRunnable(){

                public void run() {
                    if (!booleanSupplier.getAsBoolean()) {
                        this.cancel();
                    }
                }
            }.runTaskTimerAsynchronously(plugin, 0L, 1L));
        }
        return arrayList;
    }

    public static void eye(double d, double d2, double d3, double d4, ParticleDisplay particleDisplay) {
        double d5 = Math.PI / d3;
        double d6 = Math.PI / d4;
        double d7 = 0.0;
        for (double d8 = 0.0; d8 < d6; d8 += d5) {
            double d9 = d * Math.sin(d4 * d8);
            double d10 = d2 * Math.sin(d4 * -d8);
            particleDisplay.spawn(d7, d9, 0.0);
            particleDisplay.spawn(d7, d10, 0.0);
            d7 += 0.1;
        }
    }

    public static void illuminati(double d, double d2, ParticleDisplay particleDisplay) {
        Particles.polygon(3, 1, d, 1.0 / (d * 30.0), 0.0, particleDisplay);
        Particles.eye(d / 4.0, d / 4.0, 30.0, d2, particleDisplay.cloneWithLocation(0.3, 0.0, d / 1.8).rotate(1.5707963267948966, 1.5707963267948966, 0.0));
        Particles.circle(d / 5.0, d * 5.0, particleDisplay.cloneWithLocation(0.3, 0.0, 0.0));
    }

    public static void polygon(int n, int n2, double d, double d2, double d3, ParticleDisplay particleDisplay) {
        for (int i = 0; i < n; ++i) {
            double d4 = Math.toRadians(360.0 / (double)n * (double)i);
            double d5 = Math.toRadians(360.0 / (double)n * (double)(i + n2));
            double d6 = Math.cos(d4) * d;
            double d7 = Math.sin(d4) * d;
            double d8 = Math.cos(d5) * d;
            double d9 = Math.sin(d5) * d;
            double d10 = d8 - d6;
            double d11 = d9 - d7;
            for (double d12 = 0.0; d12 < 1.0 + d3; d12 += d2) {
                double d13 = d6 + d10 * d12;
                double d14 = d7 + d11 * d12;
                particleDisplay.spawn(d13, 0.0, d14);
            }
        }
    }

    public static void neopaganPentagram(double d, double d2, double d3, ParticleDisplay particleDisplay, ParticleDisplay particleDisplay2) {
        Particles.polygon(5, 2, d, d2, d3, particleDisplay);
        Particles.circle(d + 0.5, d2 * 1000.0, particleDisplay2);
    }

    public static void atom(int n, double d, double d2, ParticleDisplay particleDisplay, ParticleDisplay particleDisplay2) {
        double d3 = Math.PI / (double)n;
        double d4 = 0.0;
        while (n > 0) {
            particleDisplay.rotate(ParticleDisplay.Rotation.of(d4, ParticleDisplay.Axis.Z));
            Particles.circle(d, d2, particleDisplay);
            --n;
            d4 += d3;
        }
        Particles.sphere(d / 3.0, d2 / 2.0, particleDisplay2);
    }

    public static BooleanSupplier meguminExplosion(final double d, final ParticleDisplay particleDisplay) {
        final BooleanSupplier booleanSupplier = Particles.spread(30, 2, particleDisplay.getLocation(), particleDisplay.getLocation().clone().add(0.0, 10.0, 0.0), 5.0, 5.0, 5.0, particleDisplay);
        return new BooleanSupplier(){
            boolean first = true;

            @Override
            public boolean getAsBoolean() {
                if (this.first) {
                    this.first = false;
                    Particles.polygon(10, 4, d, 0.02, 0.3, particleDisplay);
                    Particles.polygon(10, 3, d / (d - 1.0), 0.5, 0.0, particleDisplay);
                    Particles.circle(d, 40.0, particleDisplay);
                }
                return booleanSupplier.getAsBoolean();
            }
        };
    }

    public static BukkitTask meguminExplosion(Plugin plugin, double d, ParticleDisplay particleDisplay) {
        final BooleanSupplier booleanSupplier = Particles.meguminExplosion(d, particleDisplay);
        return new BukkitRunnable(){

            public void run() {
                if (!booleanSupplier.getAsBoolean()) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    public static BooleanSupplier explosionWave(final double d, final ParticleDisplay particleDisplay, final ParticleDisplay particleDisplay2) {
        return new BooleanSupplier(){
            static final double addition = 0.3141592653589793;
            final double rateDiv;
            double times;
            boolean done;
            {
                this.rateDiv = Math.PI / d;
                this.times = 0.7853981633974483;
                this.done = false;
            }

            @Override
            public boolean getAsBoolean() {
                if (this.done) {
                    return false;
                }
                this.times += 0.3141592653589793;
                for (double d5 = 0.0; d5 <= Math.PI * 2; d5 += this.rateDiv) {
                    double d2 = this.times * Math.cos(d5);
                    double d3 = 2.0 * Math.exp(-0.1 * this.times) * Math.sin(this.times) + 1.5;
                    double d4 = this.times * Math.sin(d5);
                    particleDisplay.spawn(d2, d3, d4);
                    d2 = this.times * Math.cos(d5 += 0.04908738521234052);
                    d4 = this.times * Math.sin(d5);
                    particleDisplay2.spawn(d2, d3, d4);
                }
                if (this.times > 20.0) {
                    this.done = true;
                    return false;
                }
                return true;
            }
        };
    }

    public static BukkitTask explosionWave(Plugin plugin, double d, ParticleDisplay particleDisplay, ParticleDisplay particleDisplay2) {
        final BooleanSupplier booleanSupplier = Particles.explosionWave(d, particleDisplay, particleDisplay2);
        return new BukkitRunnable(){

            public void run() {
                if (!booleanSupplier.getAsBoolean()) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    private static BufferedImage getImage(Path path) {
        if (!Files.exists(path, new LinkOption[0])) {
            return null;
        }
        try {
            return ImageIO.read(Files.newInputStream(path, StandardOpenOption.READ));
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
            return null;
        }
    }

    private static CompletableFuture<BufferedImage> getScaledImage(Path path, int n, int n2) {
        return CompletableFuture.supplyAsync(() -> {
            BufferedImage bufferedImage = Particles.getImage(path);
            if (bufferedImage == null) {
                return null;
            }
            int n3 = n2;
            int n4 = n;
            if (bufferedImage.getWidth() > bufferedImage.getHeight()) {
                n3 = n * bufferedImage.getHeight() / bufferedImage.getWidth();
            } else {
                n4 = n2 * bufferedImage.getWidth() / bufferedImage.getHeight();
            }
            BufferedImage bufferedImage2 = new BufferedImage(n, n2, 2);
            Graphics2D graphics2D = bufferedImage2.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.drawImage(bufferedImage, 0, 0, n4, n3, null);
            graphics2D.dispose();
            return bufferedImage2;
        });
    }

    public static CompletableFuture<Map<double[], org.bukkit.Color>> renderImage(Path path, int n, int n2, double d) {
        return Particles.getScaledImage(path, n, n2).thenCompose(bufferedImage -> Particles.renderImage(bufferedImage, n, n2, d));
    }

    public static CompletableFuture<Map<double[], org.bukkit.Color>> renderImage(BufferedImage bufferedImage, int n, int n2, double d) {
        return CompletableFuture.supplyAsync(() -> {
            if (bufferedImage == null) {
                return null;
            }
            int n = bufferedImage.getWidth();
            int n2 = bufferedImage.getHeight();
            double d2 = (double)n / 2.0;
            double d3 = (double)n2 / 2.0;
            HashMap<double[], org.bukkit.Color> hashMap = new HashMap<double[], org.bukkit.Color>();
            for (int i = 0; i < n2; ++i) {
                for (int j = 0; j < n; ++j) {
                    int n3 = bufferedImage.getRGB(j, i);
                    if (n3 >> 24 == 0) continue;
                    Color color = new Color(n3);
                    int n4 = color.getRed();
                    int n5 = color.getGreen();
                    int n6 = color.getBlue();
                    double[] dArray = new double[]{((double)j - d2) * d, ((double)i - d3) * d};
                    org.bukkit.Color color2 = org.bukkit.Color.fromRGB((int)n4, (int)n5, (int)n6);
                    hashMap.put(dArray, color2);
                }
            }
            return hashMap;
        });
    }

    public static BooleanSupplier displayRenderedImage(final Map<double[], org.bukkit.Color> map, final Callable<Location> callable, final int n, final int n2, final int n3, final float f) {
        return new BooleanSupplier(){
            int times;
            boolean done;
            {
                this.times = n;
                this.done = false;
            }

            @Override
            public boolean getAsBoolean() {
                if (this.done) {
                    return false;
                }
                try {
                    Particles.displayRenderedImage(map, (Location)callable.call(), n2, n3, f);
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
                if (this.times-- <= 0) {
                    this.done = true;
                    return false;
                }
                return true;
            }
        };
    }

    public static BukkitTask displayRenderedImage(Plugin plugin, Map<double[], org.bukkit.Color> map, Callable<Location> callable, int n, long l, int n2, int n3, float f) {
        final BooleanSupplier booleanSupplier = Particles.displayRenderedImage(map, callable, n, n2, n3, f);
        return new BukkitRunnable(){

            public void run() {
                if (!booleanSupplier.getAsBoolean()) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, l);
    }

    public static void displayRenderedImage(Map<double[], org.bukkit.Color> map, Location location, int n, int n2, float f) {
        BlockFace blockFace;
        World world = location.getWorld();
        double d = location.getYaw();
        if (d >= 135.0 || d < -135.0) {
            blockFace = BlockFace.NORTH;
        } else if (d >= -135.0 && d < -45.0) {
            blockFace = BlockFace.EAST;
        } else if (d >= -45.0 && d < 45.0) {
            blockFace = BlockFace.SOUTH;
        } else if (d >= 45.0 && d < 135.0) {
            blockFace = BlockFace.WEST;
        } else {
            throw new IllegalArgumentException("Unknown rotation yaw: " + d);
        }
        for (Map.Entry<double[], org.bukkit.Color> entry : map.entrySet()) {
            double d2;
            double d3;
            double d4;
            Particle.DustOptions dustOptions = new Particle.DustOptions(entry.getValue(), f);
            double[] dArray = entry.getKey();
            switch (blockFace) {
                case NORTH: {
                    d4 = location.getX() - dArray[0];
                    d3 = location.getY() - dArray[1];
                    d2 = location.getZ();
                    break;
                }
                case EAST: {
                    d4 = location.getX();
                    d3 = location.getY() - dArray[0];
                    d2 = location.getZ() - dArray[1];
                    break;
                }
                case SOUTH: {
                    d4 = location.getX() - dArray[1];
                    d3 = location.getY() - dArray[0];
                    d2 = location.getZ();
                    break;
                }
                case WEST: {
                    d4 = location.getX();
                    d3 = location.getY() - dArray[1];
                    d2 = location.getZ() - dArray[0];
                    break;
                }
                default: {
                    throw new AssertionError((Object)("Invalid facing: " + blockFace));
                }
            }
            Location location2 = new Location(world, d4, d3, d2);
            world.spawnParticle(XParticle.DUST.get(), location2, n, 0.0, 0.0, 0.0, (double)n2, (Object)dustOptions);
        }
    }

    public static void saveImage(BufferedImage bufferedImage, Path path) {
        try {
            ImageIO.write((RenderedImage)bufferedImage, "png", Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE));
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    public static CompletableFuture<BufferedImage> stringToImage(Font font, Color color, String string) {
        return CompletableFuture.supplyAsync(() -> {
            BufferedImage bufferedImage = new BufferedImage(1, 1, 2);
            Graphics2D graphics2D = bufferedImage.createGraphics();
            graphics2D.setFont(font);
            FontRenderContext fontRenderContext = graphics2D.getFontMetrics().getFontRenderContext();
            Rectangle2D rectangle2D = font.getStringBounds(string, fontRenderContext);
            graphics2D.dispose();
            bufferedImage = new BufferedImage((int)Math.ceil(rectangle2D.getWidth()), (int)Math.ceil(rectangle2D.getHeight()), 2);
            graphics2D = bufferedImage.createGraphics();
            graphics2D.setColor(color);
            graphics2D.setFont(font);
            graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
            graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            FontMetrics fontMetrics = graphics2D.getFontMetrics();
            graphics2D.drawString(string, 0, fontMetrics.getAscent());
            graphics2D.dispose();
            return bufferedImage;
        });
    }
}

