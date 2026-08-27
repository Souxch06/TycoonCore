package com.cryptomorin.xseries.particles;

import com.cryptomorin.xseries.particles.XParticle;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

public class ParticleDisplay
implements Cloneable {
    private static final boolean ISFLAT;
    private static final boolean SUPPORTS_ALPHA_COLORS;
    public static final Color[] NOTE_COLORS;
    @Nonnull
    private static final XParticle DEFAULT_PARTICLE;
    public int count = 1;
    public double extra;
    public boolean force;
    @Nonnull
    private XParticle particle = DEFAULT_PARTICLE;
    @Nullable
    private Location location;
    @Nullable
    private Location lastLocation;
    @Nonnull
    private Vector offset = new Vector();
    @Nullable
    private Vector particleDirection;
    @Nonnull
    private Vector direction = new Vector(0, 1, 0);
    @Nonnull
    public List<List<Rotation>> rotations = new ArrayList<List<Rotation>>();
    @Nullable
    private List<Quaternion> cachedFinalRotationQuaternions;
    @Nullable
    private ParticleData data;
    @Nullable
    private Consumer<CalculationContext> preCalculation;
    @Nullable
    private Consumer<CalculationContext> postCalculation;
    @Nullable
    private Function<Double, Double> onAdvance;
    @Nullable
    private Set<Player> players;

    @Nonnull
    @Deprecated
    public static ParticleDisplay colored(@Nullable Location location, int n, int n2, int n3, float f) {
        return ParticleDisplay.of(XParticle.DUST).withLocation(location).withColor(n, n2, n3, f);
    }

    @Nullable
    public Set<Player> getPlayers() {
        return this.players;
    }

    public ParticleDisplay onlyVisibleTo(Collection<Player> collection) {
        if (collection.isEmpty()) {
            return this;
        }
        if (this.players == null) {
            this.players = Collections.newSetFromMap(new WeakHashMap());
        }
        this.players.addAll(collection);
        return this;
    }

    public ParticleDisplay onlyVisibleTo(Player ... playerArray) {
        if (playerArray.length == 0) {
            return this;
        }
        if (this.players == null) {
            this.players = Collections.newSetFromMap(new WeakHashMap());
        }
        Collections.addAll(this.players, playerArray);
        return this;
    }

    @Nonnull
    @Deprecated
    public static ParticleDisplay colored(Location location, @Nonnull Color color, float f) {
        return ParticleDisplay.of(XParticle.DUST).withLocation(location).withColor(color, f);
    }

    @Nonnull
    @Deprecated
    public static ParticleDisplay simple(@Nullable Location location, @Nonnull Particle particle) {
        Objects.requireNonNull(particle, "Cannot build ParticleDisplay with null particle");
        ParticleDisplay particleDisplay = new ParticleDisplay();
        particleDisplay.particle = XParticle.of(particle);
        particleDisplay.location = location;
        return particleDisplay;
    }

    @Nonnull
    @Deprecated
    public static ParticleDisplay of(@Nonnull Particle particle) {
        return ParticleDisplay.of(XParticle.of(particle));
    }

    @Nonnull
    public static ParticleDisplay of(@Nonnull XParticle xParticle) {
        ParticleDisplay particleDisplay = new ParticleDisplay();
        particleDisplay.particle = xParticle;
        return particleDisplay;
    }

    @Nonnull
    @Deprecated
    public static ParticleDisplay display(@Nonnull Location location, @Nonnull Particle particle) {
        Objects.requireNonNull(location, "Cannot display particle in null location");
        ParticleDisplay particleDisplay = ParticleDisplay.simple(location, particle);
        particleDisplay.spawn();
        return particleDisplay;
    }

    public static ParticleDisplay fromConfig(@Nonnull ConfigurationSection configurationSection) {
        return ParticleDisplay.edit(new ParticleDisplay(), configurationSection);
    }

    private static int toInt(String string) {
        try {
            return Integer.parseInt(string);
        }
        catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }

    private static double toDouble(String string) {
        try {
            return Double.parseDouble(string);
        }
        catch (NumberFormatException numberFormatException) {
            return 0.0;
        }
    }

    private static List<String> split(@Nonnull String string, char c) {
        ArrayList<String> arrayList = new ArrayList<String>(5);
        boolean bl = false;
        boolean bl2 = false;
        int n = string.length();
        int n2 = 0;
        for (int i = 0; i < n; ++i) {
            if (string.charAt(i) == c) {
                if (bl) {
                    arrayList.add(string.substring(n2, i));
                    bl = false;
                    bl2 = true;
                }
                n2 = i + 1;
                continue;
            }
            bl2 = false;
            bl = true;
        }
        if (bl || bl2) {
            arrayList.add(string.substring(n2, n));
        }
        return arrayList;
    }

    @Nonnull
    public static ParticleDisplay edit(@Nonnull ParticleDisplay particleDisplay, @Nonnull ConfigurationSection configurationSection) {
        Object object;
        ConfigurationSection configurationSection2;
        double d;
        List<String> list;
        List<String> list2;
        String string;
        Objects.requireNonNull(particleDisplay, "Cannot edit a null particle display");
        Objects.requireNonNull(configurationSection, "Cannot parse ParticleDisplay from a null config section");
        String string2 = configurationSection.getString("particle");
        Optional<Object> optional = string2 == null ? Optional.empty() : XParticle.of(string2);
        optional.ifPresent(xParticle -> {
            particleDisplay.particle = xParticle;
        });
        if (configurationSection.isSet("count")) {
            particleDisplay.withCount(configurationSection.getInt("count"));
        }
        if (configurationSection.isSet("extra")) {
            particleDisplay.withExtra(configurationSection.getDouble("extra"));
        }
        if (configurationSection.isSet("force")) {
            particleDisplay.forceSpawn(configurationSection.getBoolean("force"));
        }
        if ((string = configurationSection.getString("offset")) != null) {
            double d2;
            list2 = ParticleDisplay.split(string.replace(" ", ""), ',');
            if (list2.size() >= 3) {
                d2 = ParticleDisplay.toDouble((String)list2.get(0));
                double d3 = ParticleDisplay.toDouble((String)list2.get(1));
                double d4 = ParticleDisplay.toDouble((String)list2.get(2));
                particleDisplay.offset(d2, d3, d4);
            } else {
                d2 = ParticleDisplay.toDouble((String)list2.get(0));
                particleDisplay.offset(d2);
            }
        }
        if ((list2 = configurationSection.getString("direction")) != null && (list = ParticleDisplay.split(((String)((Object)list2)).replace(" ", ""), ',')).size() >= 3) {
            double d5 = ParticleDisplay.toDouble(list.get(0));
            double d6 = ParticleDisplay.toDouble(list.get(1));
            d = ParticleDisplay.toDouble(list.get(2));
            particleDisplay.particleDirection(d5, d6, d);
        }
        if ((configurationSection2 = configurationSection.getConfigurationSection("rotations")) != null) {
            for (String string3 : configurationSection2.getKeys(false)) {
                ConfigurationSection configurationSection3 = configurationSection2.getConfigurationSection(string3);
                ArrayList<Rotation> arrayList = new ArrayList<Rotation>();
                for (String string4 : configurationSection3.getKeys(false)) {
                    Vector vector;
                    object = configurationSection3.getConfigurationSection(string4);
                    double d7 = object.getDouble("angle");
                    String string5 = object.getString("vector").toUpperCase(Locale.ENGLISH).replace(" ", "");
                    if (string5.length() == 1) {
                        vector = Axis.valueOf(string5).vector;
                    } else {
                        String[] stringArray = string5.split(",");
                        vector = new Vector(Math.toRadians(Double.parseDouble(stringArray[0])), Math.toRadians(Double.parseDouble(stringArray[1])), Math.toRadians(Double.parseDouble(stringArray[2])));
                    }
                    arrayList.add(Rotation.of(d7, vector));
                }
                particleDisplay.rotations.add(arrayList);
            }
        }
        String string6 = configurationSection.getString("color");
        String string7 = configurationSection.getString("blockdata");
        String string8 = configurationSection.getString("itemstack");
        String string9 = configurationSection.getString("materialdata");
        if (configurationSection.isSet("size")) {
            particleDisplay.extra = d = configurationSection.getDouble("size");
        } else {
            d = 1.0;
        }
        if (string6 != null) {
            object = ParticleDisplay.split(string6.replace(" ", ""), ',');
            if (object.size() <= 3 || object.size() == 6) {
                Color color = Color.white;
                Color color2 = null;
                if (object.size() <= 2) {
                    try {
                        color = Color.decode((String)object.get(0));
                        if (object.size() == 2) {
                            color2 = Color.decode((String)object.get(1));
                        }
                    }
                    catch (NumberFormatException numberFormatException) {}
                } else {
                    color = new Color(ParticleDisplay.toInt((String)object.get(0)), ParticleDisplay.toInt((String)object.get(1)), ParticleDisplay.toInt((String)object.get(2)));
                    if (object.size() == 6) {
                        color2 = new Color(ParticleDisplay.toInt((String)object.get(3)), ParticleDisplay.toInt((String)object.get(4)), ParticleDisplay.toInt((String)object.get(5)));
                    }
                }
                particleDisplay.data = color2 != null ? new DustTransitionParticleColor(color, color2, d) : new RGBParticleColor(color);
            }
        } else if (string7 != null) {
            object = Material.getMaterial((String)string7);
            if (object != null && object.isBlock()) {
                particleDisplay.data = new ParticleBlockData(object.createBlockData());
            }
        } else if (string8 != null) {
            object = Material.getMaterial((String)string8);
            if (object != null && object.isItem()) {
                particleDisplay.data = new ParticleItemData(new ItemStack((Material)object, 1));
            }
        } else if (string9 != null && (object = Material.getMaterial((String)string9)) != null && object.isBlock()) {
            particleDisplay.data = new ParticleMaterialData(object.getNewData((byte)0));
        }
        return particleDisplay;
    }

    public static void serialize(ParticleDisplay particleDisplay, ConfigurationSection configurationSection) {
        Vector vector;
        configurationSection.set("particle", (Object)particleDisplay.particle.name());
        if (particleDisplay.count != 1) {
            configurationSection.set("count", (Object)particleDisplay.count);
        }
        if (particleDisplay.extra != 0.0) {
            configurationSection.set("extra", (Object)particleDisplay.extra);
        }
        if (particleDisplay.force) {
            configurationSection.set("force", (Object)true);
        }
        if (!ParticleDisplay.isZero(particleDisplay.offset)) {
            vector = particleDisplay.offset;
            configurationSection.set("offset", (Object)(vector.getX() + ", " + vector.getY() + ", " + vector.getZ()));
        }
        if (particleDisplay.particleDirection != null) {
            vector = particleDisplay.particleDirection;
            configurationSection.set("direction", (Object)(vector.getX() + ", " + vector.getY() + ", " + vector.getZ()));
        }
        if (!particleDisplay.rotations.isEmpty()) {
            vector = configurationSection.createSection("rotations");
            int n = 1;
            for (List<Rotation> list : particleDisplay.rotations) {
                ConfigurationSection configurationSection2 = vector.createSection("group-" + n++);
                int n2 = 1;
                for (Rotation rotation : list) {
                    ConfigurationSection configurationSection3 = configurationSection2.createSection(String.valueOf(n2++));
                    configurationSection3.set("angle", (Object)rotation.angle);
                    Vector vector2 = rotation.axis;
                    Optional<Axis> optional = Arrays.stream(Axis.values()).filter(axis -> ((Axis)axis).vector.equals((Object)vector2)).findFirst();
                    if (optional.isPresent()) {
                        configurationSection3.set("axis", (Object)optional.get().name());
                        continue;
                    }
                    configurationSection3.set("axis", (Object)(vector2.getX() + ", " + vector2.getY() + ", " + vector2.getZ()));
                }
            }
        }
        if (particleDisplay.data != null) {
            particleDisplay.data.serialize(configurationSection);
        }
    }

    public static Vector rotateAround(@Nonnull Vector vector, @Nonnull Axis axis, @Nonnull Vector vector2) {
        Objects.requireNonNull(axis, "Cannot rotate around null axis");
        Objects.requireNonNull(vector2, "Rotation vector cannot be null");
        switch (axis.ordinal()) {
            case 0: {
                return ParticleDisplay.rotateAround(vector, axis, vector2.getX());
            }
            case 1: {
                return ParticleDisplay.rotateAround(vector, axis, vector2.getY());
            }
            case 2: {
                return ParticleDisplay.rotateAround(vector, axis, vector2.getZ());
            }
        }
        throw new AssertionError((Object)("Unknown rotation axis: " + (Object)((Object)axis)));
    }

    public static Vector rotateAround(@Nonnull Vector vector, double d, double d2, double d3) {
        ParticleDisplay.rotateAround(vector, Axis.X, d);
        ParticleDisplay.rotateAround(vector, Axis.Y, d2);
        ParticleDisplay.rotateAround(vector, Axis.Z, d3);
        return vector;
    }

    public static Vector rotateAround(@Nonnull Vector vector, @Nonnull Axis axis, double d) {
        Objects.requireNonNull(vector, "Cannot rotate a null location");
        Objects.requireNonNull(axis, "Cannot rotate around null axis");
        if (d == 0.0) {
            return vector;
        }
        double d2 = Math.cos(d);
        double d3 = Math.sin(d);
        switch (axis.ordinal()) {
            case 0: {
                double d4 = vector.getY() * d2 - vector.getZ() * d3;
                double d5 = vector.getY() * d3 + vector.getZ() * d2;
                return vector.setY(d4).setZ(d5);
            }
            case 1: {
                double d6 = vector.getX() * d2 + vector.getZ() * d3;
                double d7 = vector.getX() * -d3 + vector.getZ() * d2;
                return vector.setX(d6).setZ(d7);
            }
            case 2: {
                double d8 = vector.getX() * d2 - vector.getY() * d3;
                double d9 = vector.getX() * d3 + vector.getY() * d2;
                return vector.setX(d8).setY(d9);
            }
        }
        throw new AssertionError((Object)("Unknown rotation axis: " + (Object)((Object)axis)));
    }

    public ParticleDisplay preCalculation(@Nullable Consumer<CalculationContext> consumer) {
        this.preCalculation = consumer;
        return this;
    }

    public ParticleDisplay postCalculation(@Nullable Consumer<CalculationContext> consumer) {
        this.postCalculation = consumer;
        return this;
    }

    public ParticleDisplay onAdvance(@Nullable Function<Double, Double> function) {
        this.onAdvance = function;
        return this;
    }

    public ParticleDisplay withParticle(@Nonnull Particle particle) {
        return this.withParticle(XParticle.of(Objects.requireNonNull(particle, "Particle cannot be null")));
    }

    public ParticleDisplay withParticle(@Nonnull XParticle xParticle) {
        this.particle = Objects.requireNonNull(xParticle, "Particle cannot be null");
        return this;
    }

    @Nonnull
    public Vector getDirection() {
        return this.direction;
    }

    public void advanceInDirection(double d) {
        Objects.requireNonNull(this.direction, "Cannot advance with null direction");
        if (d == 0.0) {
            return;
        }
        if (this.onAdvance != null) {
            d = this.onAdvance.apply(d);
        }
        this.location.add(this.direction.clone().multiply(d));
    }

    public ParticleDisplay withDirection(@Nullable Vector vector) {
        this.direction = vector.clone().normalize();
        return this;
    }

    @Nonnull
    public XParticle getParticle() {
        return this.particle;
    }

    public int getCount() {
        return this.count;
    }

    public double getExtra() {
        return this.extra;
    }

    @Nullable
    public ParticleData getData() {
        return this.data;
    }

    public ParticleDisplay withData(ParticleData particleData) {
        this.data = particleData;
        return this;
    }

    public String toString() {
        return "ParticleDisplay:[Particle=" + (Object)((Object)this.particle) + ", Count=" + this.count + ", Offset:{" + this.offset.getX() + ", " + this.offset.getY() + ", " + this.offset.getZ() + "}, " + (this.location != null ? "Location:{" + this.location.getWorld().getName() + this.location.getX() + ", " + this.location.getY() + ", " + this.location.getZ() + "}, " : "") + "Rotation:" + this.rotations + ", Extra=" + this.extra + ", Force=" + this.force + ", Data=" + this.data;
    }

    @Nonnull
    public ParticleDisplay withCount(int n) {
        this.count = n;
        return this;
    }

    @Nonnull
    public ParticleDisplay withExtra(double d) {
        this.extra = d;
        return this;
    }

    @Nonnull
    public ParticleDisplay forceSpawn(boolean bl) {
        this.force = bl;
        return this;
    }

    @Nonnull
    public ParticleDisplay withColor(@Nonnull Color color, float f) {
        return this.withColor(color.getRed(), color.getGreen(), color.getBlue(), f);
    }

    @Nonnull
    public ParticleDisplay withColor(@Nonnull Color color) {
        return this.withColor(color, 1.0f);
    }

    @Nonnull
    public ParticleDisplay withNoteColor(int n) {
        this.data = new NoteParticleColor(n);
        return this;
    }

    @Nonnull
    public ParticleDisplay withNoteColor(Note note) {
        return this.withNoteColor(note.getId());
    }

    @Nonnull
    @Deprecated
    public ParticleDisplay withColor(float f, float f2, float f3, float f4) {
        this.data = new RGBParticleColor((int)f, (int)f2, (int)f3);
        this.extra = f4;
        return this;
    }

    @Nonnull
    public ParticleDisplay withTransitionColor(@Nonnull Color color, float f, @Nonnull Color color2) {
        this.data = new DustTransitionParticleColor(color, color2, f);
        this.extra = f;
        return this;
    }

    @Nonnull
    @Deprecated
    public ParticleDisplay withTransitionColor(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        return this.withTransitionColor(new Color((int)f, (int)f2, (int)f3), f4, new Color((int)f5, (int)f6, (int)f7));
    }

    @Nonnull
    public ParticleDisplay withBlock(@Nonnull BlockData blockData) {
        this.data = new ParticleBlockData(blockData);
        return this;
    }

    @Nonnull
    public ParticleDisplay withBlock(@Nonnull MaterialData materialData) {
        this.data = new ParticleMaterialData(materialData);
        return this;
    }

    @Nonnull
    public ParticleDisplay withItem(@Nonnull ItemStack itemStack) {
        this.data = new ParticleItemData(itemStack);
        return this;
    }

    @Nonnull
    public Vector getOffset() {
        return this.offset;
    }

    @Nonnull
    public Vector getParticleDirection() {
        return this.direction;
    }

    @Nonnull
    public ParticleDisplay withEntity(@Nonnull Entity entity) {
        return this.withLocationCaller(() -> ((Entity)entity).getLocation());
    }

    @Nonnull
    public ParticleDisplay withLocationCaller(@Nullable Callable<Location> callable) {
        this.preCalculation = calculationContext -> {
            try {
                ((CalculationContext)calculationContext).location = (Location)callable.call();
            }
            catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        };
        return this;
    }

    @Nullable
    public Location getLocation() {
        return this.location;
    }

    public ParticleDisplay withLocation(@Nullable Location location) {
        this.location = location;
        return this;
    }

    @Nonnull
    public ParticleDisplay face(@Nonnull Entity entity) {
        return this.face(Objects.requireNonNull(entity, "Cannot face null entity").getLocation());
    }

    @Nonnull
    public ParticleDisplay face(@Nonnull Location location) {
        Objects.requireNonNull(location, "Cannot face null location");
        this.rotate(Rotation.of(Math.toRadians(location.getYaw()), Axis.Y), Rotation.of(Math.toRadians(-location.getPitch()), Axis.X));
        this.direction = location.getDirection().clone().normalize();
        return this;
    }

    @Nullable
    public Location cloneLocation(double d, double d2, double d3) {
        return this.location == null ? null : ParticleDisplay.cloneLocation(this.location).add(d, d2, d3);
    }

    @Nonnull
    private static Location cloneLocation(@Nonnull Location location) {
        return new Location(location.getWorld(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    private static boolean isZero(@Nonnull Vector vector) {
        return vector.getX() == 0.0 && vector.getY() == 0.0 && vector.getZ() == 0.0;
    }

    @Nonnull
    public ParticleDisplay cloneWithLocation(double d, double d2, double d3) {
        ParticleDisplay particleDisplay = this.clone();
        if (this.location == null) {
            return particleDisplay;
        }
        particleDisplay.location.add(d, d2, d3);
        return particleDisplay;
    }

    @Nonnull
    public ParticleDisplay clone() {
        ParticleDisplay particleDisplay = ParticleDisplay.of(this.particle).withDirection(this.direction).withCount(this.count).offset(this.offset.clone()).forceSpawn(this.force).preCalculation(this.preCalculation).postCalculation(this.postCalculation);
        if (this.location != null) {
            particleDisplay.location = ParticleDisplay.cloneLocation(this.location);
        }
        if (!this.rotations.isEmpty()) {
            particleDisplay.rotations = new ArrayList<List<Rotation>>(this.rotations);
        }
        particleDisplay.data = this.data;
        return particleDisplay;
    }

    public static Vector getPrincipalAxesRotation(Location location) {
        return ParticleDisplay.getPrincipalAxesRotation(location.getPitch(), location.getYaw(), 0.0f);
    }

    public static Vector getPrincipalAxesRotation(float f, float f2, float f3) {
        return new Vector(Math.toRadians(f + 90.0f), Math.toRadians(-f2), (double)f3);
    }

    public static float[] getYawPitch(Vector vector) {
        float f;
        float f2;
        double d = Math.PI * 2;
        double d2 = vector.getX();
        double d3 = vector.getZ();
        if (d2 == 0.0 && d3 == 0.0) {
            f2 = 0.0f;
            f = vector.getY() > 0.0 ? -90.0f : 90.0f;
        } else {
            double d4 = Math.atan2(-d2, d3);
            f2 = (float)Math.toDegrees((d4 + Math.PI * 2) % (Math.PI * 2));
            double d5 = NumberConversions.square((double)d2);
            double d6 = NumberConversions.square((double)d3);
            double d7 = Math.sqrt(d5 + d6);
            f = (float)Math.toDegrees(Math.atan(-vector.getY() / d7));
        }
        return new float[]{f2, f};
    }

    @Nonnull
    public List<Quaternion> getRotation(boolean bl) {
        if (this.rotations.isEmpty()) {
            return new ArrayList<Quaternion>();
        }
        if (bl) {
            this.cachedFinalRotationQuaternions = null;
        }
        if (this.cachedFinalRotationQuaternions == null) {
            this.cachedFinalRotationQuaternions = new ArrayList<Quaternion>();
            for (List<Rotation> list : this.rotations) {
                Quaternion quaternion = null;
                for (Rotation rotation : list) {
                    Quaternion quaternion2 = Quaternion.rotation(rotation.angle, rotation.axis);
                    if (quaternion == null) {
                        quaternion = quaternion2;
                        continue;
                    }
                    quaternion = quaternion.mul(quaternion2);
                }
                this.cachedFinalRotationQuaternions.add(quaternion);
            }
        }
        return this.cachedFinalRotationQuaternions;
    }

    @Nonnull
    public ParticleDisplay rotate(double d, double d2, double d3) {
        return this.rotate(Rotation.of(d, Axis.X), Rotation.of(d2, Axis.Y), Rotation.of(d3, Axis.Z));
    }

    public ParticleDisplay rotate(Rotation ... rotationArray) {
        List list;
        Objects.requireNonNull(rotationArray, "Null rotations");
        if (rotationArray.length != 0 && !(list = Arrays.stream(rotationArray).filter(rotation -> rotation.angle != 0.0).collect(Collectors.toList())).isEmpty()) {
            this.rotations.add(list);
            if (this.cachedFinalRotationQuaternions != null) {
                this.cachedFinalRotationQuaternions.clear();
            }
        }
        return this;
    }

    public ParticleDisplay rotate(Rotation rotation) {
        Objects.requireNonNull(rotation, "Null rotation");
        if (rotation.angle != 0.0) {
            this.rotations.add(Collections.singletonList(rotation));
            if (this.cachedFinalRotationQuaternions != null) {
                this.cachedFinalRotationQuaternions.clear();
            }
        }
        return this;
    }

    @Nullable
    public Location getLastLocation() {
        return this.lastLocation == null ? this.getLocation() : this.lastLocation;
    }

    @Nullable
    public Location finalizeLocation(@Nullable Vector vector) {
        Object object;
        CalculationContext calculationContext = new CalculationContext(this.location, vector);
        if (this.preCalculation != null) {
            this.preCalculation.accept(calculationContext);
        }
        if (!calculationContext.shouldSpawn) {
            return null;
        }
        Location location = calculationContext.location;
        if (location == null) {
            throw new IllegalStateException("Attempting to spawn particle when no location is set");
        }
        vector = calculationContext.local;
        if (vector != null && !this.rotations.isEmpty()) {
            object = this.getRotation(false);
            Iterator<Quaternion> iterator2 = object.iterator();
            while (iterator2.hasNext()) {
                Quaternion quaternion = iterator2.next();
                vector = Quaternion.rotate(vector, quaternion);
            }
        }
        location = ParticleDisplay.cloneLocation(location);
        if (vector != null) {
            location.add(vector);
        }
        object = new CalculationContext(location, vector);
        if (this.postCalculation != null) {
            this.postCalculation.accept((CalculationContext)object);
        }
        if (!((CalculationContext)object).shouldSpawn) {
            return null;
        }
        return location;
    }

    @Nonnull
    public ParticleDisplay offset(double d, double d2, double d3) {
        return this.offset(new Vector(d, d2, d3));
    }

    @Nonnull
    public ParticleDisplay offset(@Nonnull Vector vector) {
        this.offset = Objects.requireNonNull(vector, "Particle offset cannot be null");
        return this;
    }

    @Nonnull
    public ParticleDisplay offset(double d) {
        return this.offset(d, d, d);
    }

    @Nonnull
    public ParticleDisplay particleDirection(double d, double d2, double d3) {
        return this.particleDirection(new Vector(d, d2, d3));
    }

    @Nonnull
    public ParticleDisplay particleDirection(@Nullable Vector vector) {
        this.particleDirection = vector;
        if (vector != null && this.extra == 0.0) {
            this.extra = 1.0;
        }
        return this;
    }

    @Nonnull
    public ParticleDisplay directional() {
        this.particleDirection = new Vector();
        return this;
    }

    public boolean isDirectional() {
        return this.particleDirection != null;
    }

    @Nullable
    public Location spawn() {
        return this.spawn(this.finalizeLocation(null));
    }

    @Nullable
    public Location spawn(@Nullable Vector vector) {
        return this.spawn(this.finalizeLocation(vector));
    }

    @Nullable
    public Location spawn(double d, double d2, double d3) {
        return this.spawn(this.finalizeLocation(new Vector(d, d2, d3)));
    }

    @Nullable
    public Location spawn(Location location) {
        if (location == null) {
            return null;
        }
        this.lastLocation = location;
        Particle particle = this.particle.get();
        Objects.requireNonNull(particle, () -> "Cannot spawn unsupported particle: " + particle);
        if (this.count == 0) {
            this.count = 1;
        }
        Object object = null;
        if (this.data != null) {
            this.data = this.data.transform(this);
            Vector vector = this.data.offsetValues(this);
            if (vector != null) {
                this.spawnWithDataInOffset(particle, location, vector, null);
                return location;
            }
            object = this.data.data(this);
            if (!particle.getDataType().isInstance(object)) {
                object = null;
            }
        }
        if (this.particleDirection != null) {
            this.spawnWithDataInOffset(particle, location, this.particleDirection, object);
            return location;
        }
        this.spawnRaw(particle, location, this.count, this.offset, object);
        return location;
    }

    private void spawnWithDataInOffset(Particle particle, Location location, Vector vector, Object object) {
        if (ParticleDisplay.isZero(this.offset) && this.count < 2) {
            this.spawnRaw(particle, location, 0, vector, object);
            return;
        }
        double d = this.offset.getX();
        double d2 = this.offset.getY();
        double d3 = this.offset.getZ();
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        for (int i = 0; i < this.count; ++i) {
            double d4 = d == 0.0 ? 0.0 : threadLocalRandom.nextGaussian() * 4.0 * d;
            double d5 = d2 == 0.0 ? 0.0 : threadLocalRandom.nextGaussian() * 4.0 * d2;
            double d6 = d3 == 0.0 ? 0.0 : threadLocalRandom.nextGaussian() * 4.0 * d3;
            Location location2 = ParticleDisplay.cloneLocation(location).add(d4, d5, d6);
            this.spawnRaw(particle, location2, 0, vector, object);
        }
    }

    private void spawnRaw(Particle particle, Location location, int n, Vector vector, Object object) {
        double d;
        double d2 = vector.getX();
        double d3 = vector.getY();
        double d4 = vector.getZ();
        double d5 = d = this.particle == XParticle.DUST ? 1.0 : this.extra;
        if (this.players == null) {
            if (ISFLAT) {
                location.getWorld().spawnParticle(particle, location, n, d2, d3, d4, d, object, this.force);
            } else {
                location.getWorld().spawnParticle(particle, location, n, d2, d3, d4, d, object);
            }
        } else {
            for (Player player : this.players) {
                player.spawnParticle(particle, location, n, d2, d3, d4, d, object);
            }
        }
    }

    public static int findNearestNoteColor(Color color) {
        double d = ParticleDisplay.colorDistanceSquared(color, NOTE_COLORS[0]);
        int n = 0;
        for (int i = 1; i < NOTE_COLORS.length; ++i) {
            double d2 = ParticleDisplay.colorDistanceSquared(color, NOTE_COLORS[i]);
            if (!(d2 < d)) continue;
            d = d2;
            n = i;
        }
        return n;
    }

    public static double colorDistanceSquared(Color color, Color color2) {
        int n = color.getRed();
        int n2 = color2.getRed();
        int n3 = n + n2 >> 1;
        int n4 = n - n2;
        int n5 = color.getGreen() - color2.getGreen();
        int n6 = color.getBlue() - color2.getBlue();
        return ((512 + n3) * n4 * n4 >> 8) + 4 * n5 * n5 + ((767 - n3) * n6 * n6 >> 8);
    }

    static {
        boolean bl;
        boolean bl2;
        try {
            World.class.getDeclaredMethod("spawnParticle", Particle.class, Location.class, Integer.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Object.class, Boolean.TYPE);
            bl2 = true;
        }
        catch (NoSuchMethodException noSuchMethodException) {
            bl2 = false;
        }
        ISFLAT = bl2;
        try {
            org.bukkit.Color.fromARGB((int)0);
            bl = true;
        }
        catch (NoSuchMethodError noSuchMethodError) {
            bl = false;
        }
        SUPPORTS_ALPHA_COLORS = bl;
        NOTE_COLORS = new Color[]{new Color(0x77D700), new Color(9814016), new Color(11707648), new Color(13403648), new Color(14836992), new Color(15941888), new Color(16522752), new Color(0xFE000F), new Color(16187443), new Color(15204442), new Color(13566083), new Color(11403433), new Color(8782028), new Color(5964007), new Color(2949369), new Color(133886), new Color(14326), new Color(26848), new Color(39612), new Color(50829), new Color(59736), new Color(64545), new Color(2096128), new Color(5892096), new Color(9748736)};
        DEFAULT_PARTICLE = XParticle.FLAME;
    }

    public static enum Axis {
        X(new Vector(1, 0, 0)),
        Y(new Vector(0, 1, 0)),
        Z(new Vector(0, 0, 1));

        private final Vector vector;

        private Axis(Vector vector) {
            this.vector = vector;
        }

        public Vector getVector() {
            return this.vector;
        }
    }

    public static class Rotation
    implements Cloneable {
        public double angle;
        public Vector axis;

        public Rotation(double d, Vector vector) {
            this.angle = d;
            this.axis = vector;
        }

        public Object clone() {
            return new Rotation(this.angle, this.axis.clone());
        }

        public static Rotation of(double d, Vector vector) {
            return new Rotation(d, vector);
        }

        public static Rotation of(double d, Axis axis) {
            return new Rotation(d, axis.vector);
        }
    }

    public static class DustTransitionParticleColor
    implements ParticleData {
        private final Particle.DustTransition dustTransition;

        public DustTransitionParticleColor(Color color, Color color2, double d) {
            this.dustTransition = new Particle.DustTransition(org.bukkit.Color.fromRGB((int)color.getRed(), (int)color.getGreen(), (int)color.getBlue()), org.bukkit.Color.fromRGB((int)color2.getRed(), (int)color2.getGreen(), (int)color2.getBlue()), (float)d);
        }

        @Override
        public Object data(ParticleDisplay particleDisplay) {
            return this.dustTransition;
        }

        @Override
        public void serialize(ConfigurationSection configurationSection) {
            StringJoiner stringJoiner = new StringJoiner(", ");
            org.bukkit.Color color = this.dustTransition.getColor();
            org.bukkit.Color color2 = this.dustTransition.getToColor();
            stringJoiner.add(Integer.toString(color.getRed()));
            stringJoiner.add(Integer.toString(color.getGreen()));
            stringJoiner.add(Integer.toString(color.getBlue()));
            stringJoiner.add(Integer.toString(color2.getRed()));
            stringJoiner.add(Integer.toString(color2.getGreen()));
            stringJoiner.add(Integer.toString(color2.getBlue()));
            configurationSection.set("color", (Object)stringJoiner.toString());
        }
    }

    public static interface ParticleData {
        default public Vector offsetValues(ParticleDisplay display) {
            return null;
        }

        public Object data(ParticleDisplay var1);

        public void serialize(ConfigurationSection var1);

        default public ParticleData transform(ParticleDisplay display) {
            return this;
        }
    }

    public static class RGBParticleColor
    implements ParticleData {
        private final Color color;

        public RGBParticleColor(Color color) {
            this.color = color;
        }

        public RGBParticleColor(int n, int n2, int n3) {
            this(new Color(n, n2, n3));
        }

        @Override
        public Vector offsetValues(ParticleDisplay particleDisplay) {
            if (!ISFLAT || particleDisplay.particle == XParticle.ENTITY_EFFECT && particleDisplay.particle.isSupported() && particleDisplay.particle.get().getDataType() == Void.class) {
                double d = this.color.getRed() == 0 ? (double)1.4E-45f : (double)this.color.getRed() / 255.0;
                return new Vector(d, (double)this.color.getGreen() / 255.0, (double)this.color.getBlue() / 255.0);
            }
            return null;
        }

        @Override
        public Object data(ParticleDisplay particleDisplay) {
            if (particleDisplay.particle == XParticle.DUST) {
                return new Particle.DustOptions(org.bukkit.Color.fromRGB((int)this.color.getRed(), (int)this.color.getGreen(), (int)this.color.getBlue()), (float)particleDisplay.extra);
            }
            if (particleDisplay.particle == XParticle.DUST_COLOR_TRANSITION) {
                org.bukkit.Color color = org.bukkit.Color.fromRGB((int)this.color.getRed(), (int)this.color.getGreen(), (int)this.color.getBlue());
                return new Particle.DustTransition(color, color, (float)particleDisplay.extra);
            }
            if (SUPPORTS_ALPHA_COLORS) {
                return org.bukkit.Color.fromARGB((int)this.color.getAlpha(), (int)this.color.getRed(), (int)this.color.getGreen(), (int)this.color.getBlue());
            }
            return org.bukkit.Color.fromRGB((int)this.color.getRed(), (int)this.color.getGreen(), (int)this.color.getBlue());
        }

        @Override
        public void serialize(ConfigurationSection configurationSection) {
            StringJoiner stringJoiner = new StringJoiner(", ");
            stringJoiner.add(Integer.toString(this.color.getRed()));
            stringJoiner.add(Integer.toString(this.color.getGreen()));
            stringJoiner.add(Integer.toString(this.color.getBlue()));
            configurationSection.set("color", (Object)stringJoiner.toString());
        }

        @Override
        public ParticleData transform(ParticleDisplay particleDisplay) {
            if (particleDisplay.particle == XParticle.NOTE) {
                return new NoteParticleColor(ParticleDisplay.findNearestNoteColor(this.color));
            }
            return this;
        }
    }

    public static class ParticleBlockData
    implements ParticleData {
        private final BlockData blockData;

        public ParticleBlockData(BlockData blockData) {
            this.blockData = blockData;
        }

        @Override
        public Object data(ParticleDisplay particleDisplay) {
            return this.blockData;
        }

        @Override
        public void serialize(ConfigurationSection configurationSection) {
            configurationSection.set("blockdata", (Object)this.blockData.getMaterial().name());
        }
    }

    public static class ParticleItemData
    implements ParticleData {
        private final ItemStack item;

        public ParticleItemData(ItemStack itemStack) {
            this.item = itemStack;
        }

        @Override
        public Object data(ParticleDisplay particleDisplay) {
            return this.item;
        }

        @Override
        public void serialize(ConfigurationSection configurationSection) {
            configurationSection.set("itemstack", (Object)this.item.getType());
        }
    }

    public static class ParticleMaterialData
    implements ParticleData {
        private final MaterialData materialData;

        public ParticleMaterialData(MaterialData materialData) {
            this.materialData = materialData;
        }

        @Override
        public Object data(ParticleDisplay particleDisplay) {
            return this.materialData;
        }

        @Override
        public void serialize(ConfigurationSection configurationSection) {
            configurationSection.set("materialdata", (Object)this.materialData.getItemType().name());
        }
    }

    public static class NoteParticleColor
    implements ParticleData {
        private final int note;

        public NoteParticleColor(int n) {
            this.note = n;
        }

        public NoteParticleColor(Note note) {
            this(note.getId());
        }

        @Override
        public Vector offsetValues(ParticleDisplay particleDisplay) {
            return new Vector((double)this.note / 24.0, 0.0, 0.0);
        }

        @Override
        public Object data(ParticleDisplay particleDisplay) {
            return null;
        }

        @Override
        public void serialize(ConfigurationSection configurationSection) {
            configurationSection.set("color", (Object)this.note);
        }

        @Override
        public ParticleData transform(ParticleDisplay particleDisplay) {
            if (particleDisplay.particle == XParticle.NOTE) {
                return this;
            }
            return new RGBParticleColor(NOTE_COLORS[this.note]);
        }
    }

    public static class Quaternion
    implements Cloneable {
        public final double w;
        public final double x;
        public final double y;
        public final double z;

        public Quaternion(double d, double d2, double d3, double d4) {
            this.w = d;
            this.x = d2;
            this.y = d3;
            this.z = d4;
        }

        public Quaternion clone() {
            return new Quaternion(this.w, this.x, this.y, this.z);
        }

        public static Vector rotate(Vector vector, Quaternion quaternion) {
            return quaternion.mul(Quaternion.from(vector)).mul(quaternion.inverse()).toVector();
        }

        public static Vector rotate(Vector vector, Vector vector2, double d) {
            return Quaternion.rotate(vector, Quaternion.rotation(d, vector2));
        }

        public static Quaternion from(Vector vector) {
            return new Quaternion(0.0, vector.getX(), vector.getY(), vector.getZ());
        }

        public static Quaternion rotation(double d, Vector vector) {
            vector = vector.normalize();
            double d2 = Math.sin(d /= 2.0);
            return new Quaternion(Math.cos(d), vector.getX() * d2, vector.getY() * d2, vector.getZ() * d2);
        }

        public String getInverseString() {
            double d = Math.acos(this.w);
            double d2 = Math.toDegrees(d) * 2.0;
            double d3 = Math.sin(d);
            Vector vector = new Vector(this.x / d3, this.y / d3, this.z / d3);
            return d2 + ", " + vector.getX() + ", " + vector.getY() + ", " + vector.getZ();
        }

        public Vector toVector() {
            return new Vector(this.x, this.y, this.z);
        }

        public Quaternion inverse() {
            double d = this.w * this.w + this.x * this.x + this.y * this.y + this.z * this.z;
            return new Quaternion(this.w / d, -this.x / d, -this.y / d, -this.z / d);
        }

        public Quaternion conjugate() {
            return new Quaternion(this.w, -this.x, -this.y, -this.z);
        }

        public Quaternion mul(Quaternion quaternion) {
            double d = quaternion.w * this.w - quaternion.x * this.x - quaternion.y * this.y - quaternion.z * this.z;
            double d2 = quaternion.w * this.x + quaternion.x * this.w + quaternion.y * this.z - quaternion.z * this.y;
            double d3 = quaternion.w * this.y - quaternion.x * this.z + quaternion.y * this.w + quaternion.z * this.x;
            double d4 = quaternion.w * this.z + quaternion.x * this.y - quaternion.y * this.x + quaternion.z * this.w;
            return new Quaternion(d, d2, d3, d4);
        }

        public Vector mul(Vector vector) {
            double d = this.x * 2.0;
            double d2 = this.y * 2.0;
            double d3 = this.z * 2.0;
            double d4 = this.x * d;
            double d5 = this.y * d2;
            double d6 = this.z * d3;
            double d7 = this.x * d2;
            double d8 = this.x * d3;
            double d9 = this.y * d3;
            double d10 = this.w * d;
            double d11 = this.w * d2;
            double d12 = this.w * d3;
            double d13 = (1.0 - (d5 + d6)) * vector.getX() + (d7 - d12) * vector.getY() + (d8 + d11) * vector.getZ();
            double d14 = (d7 + d12) * vector.getX() + (1.0 - (d4 + d6)) * vector.getY() + (d9 - d10) * vector.getZ();
            double d15 = (d8 - d11) * vector.getX() + (d9 + d10) * vector.getY() + (1.0 - (d4 + d5)) * vector.getZ();
            return new Vector(d13, d14, d15);
        }
    }

    public final class CalculationContext {
        private Location location;
        private Vector local;
        private boolean shouldSpawn = true;

        public CalculationContext(Location location, Vector vector) {
            this.location = location;
            this.local = vector;
        }

        @Nullable
        public Location getLocation() {
            return this.location;
        }

        @Nullable
        public Vector getLocal() {
            return this.local;
        }

        public void setLocal(Vector vector) {
            this.local = vector;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public void dontSpawn() {
            this.shouldSpawn = false;
        }

        public ParticleDisplay getDisplay() {
            return ParticleDisplay.this;
        }
    }
}

