/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.util.Vector
 */
package org.holoeasy.util;

import java.util.Arrays;
import java.util.Objects;
import kotlin.Metadata;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001:\u0002\u0012\u0013B\u000f\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004B\u0015\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\bJ \u0010\t\u001a\u0004\u0018\u00010\u00062\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\rJ\u000e\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0006R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2={"Lorg/holoeasy/util/AABB;", "", "block", "Lorg/bukkit/Location;", "(Lorg/bukkit/Location;)V", "min", "Lorg/holoeasy/util/AABB$Vec3D;", "max", "(Lorg/holoeasy/util/AABB$Vec3D;Lorg/holoeasy/util/AABB$Vec3D;)V", "intersectsRay", "ray", "Lorg/holoeasy/util/AABB$Ray3D;", "minDist", "", "maxDist", "translate", "", "vec", "Ray3D", "Vec3D", "holoeasy-core"})
public final class AABB {
    @NotNull
    private Vec3D min;
    @NotNull
    private Vec3D max;

    public AABB(@NotNull Vec3D vec3D, @NotNull Vec3D vec3D2) {
        Intrinsics.checkNotNullParameter(vec3D, "min");
        Intrinsics.checkNotNullParameter(vec3D2, "max");
        this.min = vec3D;
        this.max = vec3D2;
    }

    public AABB(@NotNull Location location) {
        Intrinsics.checkNotNullParameter(location, "block");
        this(Vec3D.Companion.fromLocation(location), Vec3D.Companion.fromLocation(location).add(Vec3D.Companion.getUNIT_MAX()));
    }

    public final void translate(@NotNull Vec3D vec3D) {
        Intrinsics.checkNotNullParameter(vec3D, "vec");
        this.min = this.min.add(vec3D);
        this.max = this.max.add(vec3D);
    }

    @Nullable
    public final Vec3D intersectsRay(@NotNull Ray3D ray3D, float f, float f2) {
        Intrinsics.checkNotNullParameter(ray3D, "ray");
        Vec3D vec3D = new Vec3D((double)1.0f / ray3D.getDirection().getX(), (double)1.0f / ray3D.getDirection().getY(), (double)1.0f / ray3D.getDirection().getZ());
        boolean bl = vec3D.getX() < 0.0;
        boolean bl2 = vec3D.getY() < 0.0;
        boolean bl3 = vec3D.getZ() < 0.0;
        Vec3D vec3D2 = bl ? this.max : this.min;
        double d = (vec3D2.getX() - ray3D.getX()) * vec3D.getX();
        vec3D2 = bl ? this.min : this.max;
        double d2 = (vec3D2.getX() - ray3D.getX()) * vec3D.getX();
        vec3D2 = bl2 ? this.max : this.min;
        double d3 = (vec3D2.getY() - ray3D.getY()) * vec3D.getY();
        vec3D2 = bl2 ? this.min : this.max;
        double d4 = (vec3D2.getY() - ray3D.getY()) * vec3D.getY();
        if (d > d4 || d3 > d2) {
            return null;
        }
        if (d3 > d) {
            d = d3;
        }
        if (d4 < d2) {
            d2 = d4;
        }
        vec3D2 = bl3 ? this.max : this.min;
        double d5 = (vec3D2.getZ() - ray3D.getZ()) * vec3D.getZ();
        vec3D2 = bl3 ? this.min : this.max;
        double d6 = (vec3D2.getZ() - ray3D.getZ()) * vec3D.getZ();
        if (d > d6 || d5 > d2) {
            return null;
        }
        if (d5 > d) {
            d = d5;
        }
        if (d6 < d2) {
            d2 = d6;
        }
        if (d < (double)f2 && d2 > (double)f) {
            return ray3D.getPointAtDistance(d);
        }
        return null;
    }

    @Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\u000e\n\u0000\u0018\u00002\u00020\u0001B\u000f\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004B\u0015\u0012\u0006\u0010\u0005\u001a\u00020\u0001\u0012\u0006\u0010\u0006\u001a\u00020\u0001\u00a2\u0006\u0002\u0010\u0007J\u000e\u0010\n\u001a\u00020\u00012\u0006\u0010\u000b\u001a\u00020\fJ\b\u0010\r\u001a\u00020\u000eH\u0016R\u0011\u0010\u0006\u001a\u00020\u0001\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\u000f"}, d2={"Lorg/holoeasy/util/AABB$Ray3D;", "Lorg/holoeasy/util/AABB$Vec3D;", "loc", "Lorg/bukkit/Location;", "(Lorg/bukkit/Location;)V", "origin", "direction", "(Lorg/holoeasy/util/AABB$Vec3D;Lorg/holoeasy/util/AABB$Vec3D;)V", "getDirection", "()Lorg/holoeasy/util/AABB$Vec3D;", "getPointAtDistance", "dist", "", "toString", "", "holoeasy-core"})
    public static final class Ray3D
    extends Vec3D {
        @NotNull
        private final Vec3D direction;

        public Ray3D(@NotNull Vec3D vec3D, @NotNull Vec3D vec3D2) {
            Intrinsics.checkNotNullParameter(vec3D, "origin");
            Intrinsics.checkNotNullParameter(vec3D2, "direction");
            super(vec3D);
            this.direction = vec3D2.normalize();
        }

        @NotNull
        public final Vec3D getDirection() {
            return this.direction;
        }

        public Ray3D(@NotNull Location location) {
            Intrinsics.checkNotNullParameter(location, "loc");
            Vec3D vec3D = Vec3D.Companion.fromLocation(location);
            Vector vector = location.getDirection();
            Intrinsics.checkNotNullExpressionValue(vector, "getDirection(...)");
            this(vec3D, Vec3D.Companion.fromVector(vector));
        }

        @NotNull
        public final Vec3D getPointAtDistance(double d) {
            return this.add(this.direction.scale(d));
        }

        @Override
        @NotNull
        public String toString() {
            return "origin: " + super.toString() + " dir: " + this.direction;
        }
    }

    @Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0016\u0018\u0000 \u00182\u00020\u0001:\u0001\u0018B\u001f\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0006B\u000f\b\u0016\u0012\u0006\u0010\u0007\u001a\u00020\u0000\u00a2\u0006\u0002\u0010\bJ\u000e\u0010\r\u001a\u00020\u00002\u0006\u0010\u0007\u001a\u00020\u0000J\u0013\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0001H\u0096\u0002J\b\u0010\u0011\u001a\u00020\u0012H\u0016J\u0006\u0010\u0013\u001a\u00020\u0000J\u000e\u0010\u0014\u001a\u00020\u00002\u0006\u0010\u0015\u001a\u00020\u0003J\b\u0010\u0016\u001a\u00020\u0017H\u0016R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\nR\u0011\u0010\u0005\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\n\u00a8\u0006\u0019"}, d2={"Lorg/holoeasy/util/AABB$Vec3D;", "", "x", "", "y", "z", "(DDD)V", "v", "(Lorg/holoeasy/util/AABB$Vec3D;)V", "getX", "()D", "getY", "getZ", "add", "equals", "", "obj", "hashCode", "", "normalize", "scale", "s", "toString", "", "Companion", "holoeasy-core"})
    public static class Vec3D {
        @NotNull
        public static final Companion Companion = new Companion(null);
        private final double x;
        private final double y;
        private final double z;
        @NotNull
        private static final Vec3D UNIT_MAX = new Vec3D(1.0, 1.0, 1.0);

        public final double getX() {
            return this.x;
        }

        public final double getY() {
            return this.y;
        }

        public final double getZ() {
            return this.z;
        }

        public Vec3D(double d, double d2, double d3) {
            this.x = d;
            this.y = d2;
            this.z = d3;
        }

        public Vec3D(@NotNull Vec3D vec3D) {
            Intrinsics.checkNotNullParameter(vec3D, "v");
            this.x = vec3D.x;
            this.y = vec3D.y;
            this.z = vec3D.z;
        }

        @NotNull
        public final Vec3D add(@NotNull Vec3D vec3D) {
            Intrinsics.checkNotNullParameter(vec3D, "v");
            return new Vec3D(this.x + vec3D.x, this.y + vec3D.y, this.z + vec3D.z);
        }

        @NotNull
        public final Vec3D scale(double d) {
            return new Vec3D(this.x * d, this.y * d, this.z * d);
        }

        @NotNull
        public final Vec3D normalize() {
            double d = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
            if (d > 0.0) {
                return this.scale(1.0 / d);
            }
            return this;
        }

        public boolean equals(@Nullable Object object) {
            if (object instanceof Vec3D) {
                Object object2 = object;
                return this.x == ((Vec3D)object2).x && this.y == ((Vec3D)object2).y && this.z == ((Vec3D)object2).z;
            }
            return false;
        }

        public int hashCode() {
            Object[] objectArray = new Object[]{this.x, this.y, this.z};
            return Objects.hash(objectArray);
        }

        @NotNull
        public String toString() {
            String string = "{x: %g, y: %g, z: %g}";
            Object[] objectArray = new Object[]{this.x, this.y, this.z};
            String string2 = String.format(string, Arrays.copyOf(objectArray, objectArray.length));
            Intrinsics.checkNotNullExpressionValue(string2, "format(...)");
            return string2;
        }

        @JvmStatic
        @NotNull
        public static final Vec3D fromLocation(@NotNull Location location) {
            return Companion.fromLocation(location);
        }

        @Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\tH\u0007J\u000e\u0010\n\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\fR\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\r"}, d2={"Lorg/holoeasy/util/AABB$Vec3D$Companion;", "", "()V", "UNIT_MAX", "Lorg/holoeasy/util/AABB$Vec3D;", "getUNIT_MAX", "()Lorg/holoeasy/util/AABB$Vec3D;", "fromLocation", "loc", "Lorg/bukkit/Location;", "fromVector", "v", "Lorg/bukkit/util/Vector;", "holoeasy-core"})
        public static final class Companion {
            private Companion() {
            }

            @NotNull
            public final Vec3D getUNIT_MAX() {
                return UNIT_MAX;
            }

            @JvmStatic
            @NotNull
            public final Vec3D fromLocation(@NotNull Location location) {
                Intrinsics.checkNotNullParameter(location, "loc");
                return new Vec3D(location.getX(), location.getY(), location.getZ());
            }

            @NotNull
            public final Vec3D fromVector(@NotNull Vector vector) {
                Intrinsics.checkNotNullParameter(vector, "v");
                return new Vec3D(vector.getX(), vector.getY(), vector.getZ());
            }

            public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
                this();
            }
        }
    }
}

