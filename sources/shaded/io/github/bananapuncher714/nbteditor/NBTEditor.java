package io.github.bananapuncher714.nbteditor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Pont de compatibilité NBT de ValoriaTycoon.
 *
 * <p>La bibliothèque embarquée d'origine identifie les générateurs en écrivant des composés NBT dans
 * l'item, en résolvant par réflexion des noms de classes et de méthodes <em>obfusqués</em>
 * ({@code net.minecraft.nbt.NBTTagCompound}, {@code MinecraftServer.bc()}, {@code ItemStack.a(...)},
 * {@code net.minecraft.core.HolderLookup$a}). Ces noms ont disparu des serveurs modernes : la
 * relocation du paquet CraftBukkit a été supprimée en Paper 1.20.6, et depuis 26.1 Mojang ne fournit
 * plus de jar obfusqué tout en abandonnant le remapper interne de Paper. Sur 26.x la bibliothèque ne
 * trouvait donc plus ses cibles : sans lever d'erreur, mais sans écrire ni relire le NBT, ce qui
 * rendait tous les générateurs inertes.</p>
 *
 * <p>Cette classe ne remplace que la surface appelée par le plugin — le champ {@code CUSTOM_DATA} et
 * les méthodes {@code set}, {@code contains}, {@code getInt}, {@code getString} — et s'appuie sur le
 * {@code PersistentDataContainer}, API Bukkit stable depuis 1.14 et indépendante des noms internes.
 * Les données atterrissent dans le composant {@code minecraft:custom_data}, c'est-à-dire l'endroit où
 * l'ancienne implémentation les écrivait déjà. Les signatures doivent rester binaire-compatibles :
 * les classes du plugin sont précompilées et appellent ces descripteurs exacts.</p>
 *
 * <p>L'implémentation historique est conservée sous {@code LegacyNbtBridge} et sert de repli : blocs
 * et entités (hors items), serveurs sans {@code PersistentDataContainer}, et données écrites avant
 * cette correction. Cet arbre ne déclare aucune dépendance de build : tout accès à Bukkit passe par la
 * réflexion, ce qui garde {@code mvn package} exécutable sans artifact serveur.</p>
 */
public final class NBTEditor {

    private static final String PLUGIN_NAMESPACE = "valoriatycoon";
    private static final int MAX_KEY_LENGTH = 128;

    /** Marqueurs de chemin repris de l'API d'origine ; les appelants les comparent par identité. */
    public enum Type {
        COMPOUND,
        LIST,
        NEW_ELEMENT,
        DELETE,
        CUSTOM_DATA,
        ITEMSTACK_COMPONENTS;
    }

    public static final Type COMPOUND = Type.COMPOUND;
    public static final Type LIST = Type.LIST;
    public static final Type NEW_ELEMENT = Type.NEW_ELEMENT;
    public static final Type DELETE = Type.DELETE;
    public static final Type CUSTOM_DATA = Type.CUSTOM_DATA;
    public static final Type ITEMSTACK_COMPONENTS = Type.ITEMSTACK_COMPONENTS;

    private NBTEditor() {
    }

    // ------------------------------------------------------------------ surface appelée par le plugin

    public static boolean contains(Object object, Object ... objectArray) {
        Keys keys = Keys.of(objectArray);
        if (keys != null && Bukkit.isItem(object)) {
            Object container = Bukkit.container(object);
            if (container != null) {
                try {
                    if (Bukkit.get(container, keys) != null) {
                        return true;
                    }
                }
                catch (Throwable throwable) {
                    // repli historique ci-dessous
                }
            }
        }
        return Legacy.contains(object, objectArray);
    }

    public static int getInt(Object object, Object ... objectArray) {
        Keys keys = Keys.of(objectArray);
        if (keys != null && Bukkit.isItem(object)) {
            Object container = Bukkit.container(object);
            if (container != null) {
                try {
                    Object value = Bukkit.get(container, keys);
                    if (value instanceof Number) {
                        return ((Number)value).intValue();
                    }
                    if (value instanceof String) {
                        return Integer.parseInt(((String)value).trim());
                    }
                }
                catch (Throwable throwable) {
                    // repli historique ci-dessous
                }
            }
        }
        Object legacy = Legacy.getInt(object, objectArray);
        return legacy instanceof Number ? ((Number)legacy).intValue() : 0;
    }

    public static String getString(Object object, Object ... objectArray) {
        Keys keys = Keys.of(objectArray);
        if (keys != null && Bukkit.isItem(object)) {
            Object container = Bukkit.container(object);
            if (container != null) {
                try {
                    Object value = Bukkit.get(container, keys);
                    if (value != null) {
                        return String.valueOf(value);
                    }
                }
                catch (Throwable throwable) {
                    // repli historique ci-dessous
                }
            }
        }
        Object legacy = Legacy.getString(object, objectArray);
        return legacy == null ? null : String.valueOf(legacy);
    }

    /**
     * Ecrit (ou supprime, si {@code value} vaut {@code null} ou {@link Type#DELETE}) une valeur, puis
     * renvoie l'item à réutiliser : le même objet quand il est modifiable, une copie sinon (un
     * {@code CraftItemStack} refuse {@code setItemMeta}).
     */
    public static Object set(Object object, Object object2, Object ... objectArray) {
        Keys keys = Keys.of(objectArray);
        if (keys != null && Bukkit.isItem(object)) {
            try {
                Object meta = Bukkit.invoke(Bukkit.GET_ITEM_META, object);
                if (meta != null) {
                    Object container = Bukkit.invoke(Bukkit.GET_PDC, meta);
                    if (container != null) {
                        if (object2 == null || object2 == Type.DELETE) {
                            Bukkit.remove(container, keys);
                        } else {
                            Object primitive = primitive(object2);
                            Object dataType = Bukkit.dataType(object2);
                            if (primitive != null && dataType != null) {
                                Bukkit.set(container, keys, dataType, primitive);
                            }
                        }
                        return Bukkit.applyMeta(object, meta);
                    }
                }
            }
            catch (Throwable throwable) {
                // repli historique ci-dessous
            }
        }
        Object legacy = Legacy.set(object, object2, objectArray);
        return legacy != null ? legacy : object;
    }

    // ------------------------------------------------------------------ encodage du chemin

    private static boolean isPresent(Object object) {
        if (object instanceof Boolean) {
            return ((Boolean)object).booleanValue();
        }
        if (object instanceof Number) {
            return ((Number)object).intValue() != 0;
        }
        if (object instanceof String) {
            return !((String)object).isEmpty();
        }
        return true;
    }

    private static Object primitive(Object object) {
        if (object instanceof Boolean) {
            return ((Boolean)object).booleanValue() ? Byte.valueOf((byte)1) : Byte.valueOf((byte)0);
        }
        if (object instanceof Short) {
            return Integer.valueOf(((Short)object).intValue());
        }
        if (object instanceof char[]) {
            return new String((char[])object);
        }
        if (object instanceof Byte || object instanceof Integer || object instanceof Long || object instanceof Float || object instanceof Double || object instanceof String || object instanceof byte[] || object instanceof int[]) {
            return object;
        }
        return null;
    }

    /** Espace de noms et clé d'une entrée de {@code PersistentDataContainer}. */
    private static final class Keys {
        private final String namespace;
        private final String key;

        private Keys(String string, String string2) {
            this.namespace = string;
            this.key = string2;
        }

        /**
         * Convertit un chemin façon NBTEditor en couple espace de noms / clé. Les deux formes utilisées
         * par le plugin convergent : {@code ("valoriatycoon", "spawnitem", "tier")} et
         * {@code (CUSTOM_DATA, "valoriatycoon", "spawnitem", "tier")} donnent toutes deux
         * {@code valoriatycoon:spawnitem.tier}. Un chemin à segment unique est rattaché à l'espace de
         * noms du plugin, ce qui rend par exemple {@code valoriatycoon:sell-wand-uuid}.
         *
         * @return {@code null} si le chemin n'est pas transposable (segments non textuels, marqueurs seuls)
         */
        static Keys of(Object[] objectArray) {
            if (objectArray == null || objectArray.length == 0) {
                return null;
            }
            String namespace = null;
            StringBuilder builder = new StringBuilder();
            for (int n = 0; n < objectArray.length; ++n) {
                Object object = objectArray[n];
                if (object instanceof Type) {
                    continue;
                }
                if (!(object instanceof String)) {
                    return null;
                }
                String string = (String)object;
                if (string.isEmpty()) {
                    return null;
                }
                if (namespace == null) {
                    namespace = string;
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append('.');
                }
                builder.append(string);
            }
            if (namespace == null) {
                return null;
            }
            if (builder.length() == 0) {
                return new Keys(PLUGIN_NAMESPACE, sanitize(namespace));
            }
            return new Keys(sanitize(namespace), sanitize(builder.toString()));
        }

        /** {@code NamespacedKey} n'accepte que [a-z0-9._-] (et '/' côténamespace). */
        private static String sanitize(String string) {
            StringBuilder builder = new StringBuilder(string.length());
            for (int n = 0; n < string.length(); ++n) {
                char c = Character.toLowerCase(string.charAt(n));
                boolean valid = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-';
                builder.append(valid ? c : '_');
            }
            if (builder.length() > MAX_KEY_LENGTH) {
                builder.setLength(MAX_KEY_LENGTH);
            }
            if (builder.length() == 0) {
                builder.append(PLUGIN_NAMESPACE);
            }
            return builder.toString();
        }
    }

    // ------------------------------------------------------------------ accès Bukkit par réflexion

    /**
     * Les méthodes sont cherchées par nom et arité plutôt que par types exacts : les signatures du PDC
     * ont évolué (generics effacés, méthodes déplacées de {@code PersistentDataHolder} vers
     * {@code PersistentDataContainer}) et une résolution stricte casserait silencieusement sur les
     * versions anciennes. Tout membre introuvable vaut {@code null} et fait basculer l'appel vers le repli.
     */
    private static final class Bukkit {
        private static final Class<?> ITEM_STACK = lookup("org.bukkit.inventory.ItemStack");
        private static final Class<?> NAMESPACED_KEY = lookup("org.bukkit.NamespacedKey");
        private static final Class<?> PERSISTENT_DATA_TYPE = lookup("org.bukkit.persistence.PersistentDataType");
        private static final Class<?> CONTAINER = lookup("org.bukkit.persistence.PersistentDataContainer");
        private static final Class<?> ITEM_META = lookup("org.bukkit.inventory.meta.ItemMeta");

        private static final Method GET_ITEM_META = method(ITEM_STACK, "getItemMeta", 0);
        private static final Method SET_ITEM_META = method(ITEM_STACK, "setItemMeta", 1);
        private static final Method CLONE = method(ITEM_STACK, "clone", 0);
        private static final Method GET_PDC = method(ITEM_META, "getPersistentDataContainer", 0);
        private static final Method GET = method(CONTAINER, "get", 1);
        private static final Method SET = method(CONTAINER, "set", 3);
        private static final Method REMOVE = method(CONTAINER, "remove", 1);
        private static final Constructor<?> KEY_CTOR = constructor(NAMESPACED_KEY);

        private static boolean available() {
            return GET != null && KEY_CTOR != null && GET_ITEM_META != null && GET_PDC != null;
        }

        private static boolean isItem(Object object) {
            return object != null && ITEM_STACK != null && ITEM_STACK.isInstance(object) && available();
        }

        static Object container(Object object) {
            try {
                Object meta = invoke(GET_ITEM_META, object);
                return meta == null ? null : invoke(GET_PDC, meta);
            }
            catch (Throwable throwable) {
                return null;
            }
        }

        static Object get(Object container, Keys keys) throws Throwable {
            return invoke(GET, container, key(keys));
        }

        static void set(Object container, Keys keys, Object dataType, Object value) throws Throwable {
            invoke(SET, container, key(keys), dataType, value);
        }

        static void remove(Object container, Keys keys) throws Throwable {
            if (REMOVE == null) {
                return;
            }
            invoke(REMOVE, container, key(keys));
        }

        /** Applique les méta-données, en passant par une copie si l'objet source est immuable. */
        static Object applyMeta(Object object, Object meta) throws Throwable {
            if (SET_ITEM_META != null) {
                try {
                    SET_ITEM_META.invoke(object, meta);
                    return object;
                }
                catch (InvocationTargetException invocationTargetException) {
                    if (CLONE == null) {
                        throw invocationTargetException.getCause() != null ? invocationTargetException.getCause() : invocationTargetException;
                    }
                    Object copy = CLONE.invoke(object, new Object[0]);
                    SET_ITEM_META.invoke(copy, meta);
                    return copy;
                }
            }
            return object;
        }

        static Object invoke(Method method, Object target, Object ... args) throws Throwable {
            if (method == null || target == null) {
                return null;
            }
            try {
                return method.invoke(target, args);
            }
            catch (InvocationTargetException invocationTargetException) {
                Throwable cause = invocationTargetException.getCause();
                throw cause != null ? cause : invocationTargetException;
            }
        }

        private static Object key(Keys keys) throws Throwable {
            return KEY_CTOR.newInstance(keys.namespace, keys.key);
        }

        /** Champ {@code PersistentDataType} adapté à la valeur à écrire. */
        static Object dataType(Object object) {
            if (PERSISTENT_DATA_TYPE == null || object == null) {
                return null;
            }
            String string;
            if (object instanceof String || object instanceof char[]) {
                string = "STRING";
            } else if (object instanceof Boolean || object instanceof Byte) {
                string = "BYTE";
            } else if (object instanceof Short || object instanceof Integer) {
                string = "INTEGER";
            } else if (object instanceof Long) {
                string = "LONG";
            } else if (object instanceof Float) {
                string = "FLOAT";
            } else if (object instanceof Double) {
                string = "DOUBLE";
            } else if (object instanceof int[]) {
                string = "INTEGER_ARRAY";
            } else if (object instanceof byte[]) {
                string = "BYTE_ARRAY";
            } else {
                return null;
            }
            try {
                Field field = PERSISTENT_DATA_TYPE.getField(string);
                field.setAccessible(true);
                return field.get(null);
            }
            catch (Throwable throwable) {
                return null;
            }
        }

        private static Class<?> lookup(String string) {
            try {
                return Class.forName(string, false, NBTEditor.class.getClassLoader());
            }
            catch (Throwable throwable) {
                return null;
            }
        }

        private static Method method(Class<?> clazz, String name, int arity) {
            if (clazz == null) {
                return null;
            }
            Method[] methods = clazz.getMethods();
            for (int n = 0; n < methods.length; ++n) {
                Method method = methods[n];
                if (!method.getName().equals(name) || method.getParameterTypes().length != arity) continue;
                try {
                    method.setAccessible(true);
                }
                catch (Throwable throwable) {
                    // accessible de toute façon via l'interface publique
                }
                return method;
            }
            return null;
        }

        private static Constructor<?> constructor(Class<?> clazz) {
            if (clazz == null) {
                return null;
            }
            try {
                Constructor<?> ctor = clazz.getConstructor(String.class, String.class);
                ctor.setAccessible(true);
                return ctor;
            }
            catch (Throwable throwable) {
                return null;
            }
        }
    }

    // ------------------------------------------------------------------ repli vers l'implémentation d'origine

    /**
     * Délégation vers l'implémentation historique (chargée par réflexion, jamais liée à la compilation) :
     * utilisée pour les blocs et entités, quand le PDC manque, ou quand la donnée n'a pas encore été
     * migrée vers une clé {@code PersistentDataContainer}.
     */
    private static final class Legacy {
        private static final Class<?> BRIDGE = Bukkit.lookup("io.github.bananapuncher714.nbteditor.LegacyNbtBridge");

        private static boolean contains(Object object, Object[] path) {
            Object result = call("contains", 2, object, null, path);
            if (result instanceof Boolean) {
                return ((Boolean)result).booleanValue();
            }
            return result != null && NBTEditor.isPresent(result);
        }

        private static Object getInt(Object object, Object[] path) {
            return call("getInt", 2, object, null, path);
        }

        private static Object getString(Object object, Object[] path) {
            return call("getString", 2, object, null, path);
        }

        private static Object set(Object object, Object value, Object[] path) {
            return call("set", 3, object, value, path);
        }

        /**
         * @param arity 2 pour {@code m(Object, Object[])}, 3 pour {@code set(Object, Object, Object[])}
         * @return la valeur renvoyée par l'implémentation d'origine, ou {@code null} si indisponible
         */
        private static Object call(String name, int arity, Object object, Object value, Object[] path) {
            if (BRIDGE == null || object == null) {
                return null;
            }
            Method method = Bukkit.method(BRIDGE, name, arity);
            if (method == null) {
                return null;
            }
            try {
                Object[] translated = translate(path);
                if (arity == 3) {
                    return method.invoke(null, object, value, translated);
                }
                return method.invoke(null, object, translated);
            }
            catch (InvocationTargetException invocationTargetException) {
                return null;
            }
            catch (Throwable throwable) {
                return null;
            }
        }

        /** Remplace les marqueurs de ce pont par ceux de la bibliothèque d'origine. */
        private static Object[] translate(Object[] objectArray) {
            if (objectArray == null) {
                return new Object[0];
            }
            Object[] copy = new Object[objectArray.length];
            for (int n = 0; n < objectArray.length; ++n) {
                Object object = objectArray[n];
                copy[n] = object;
                if (!(object instanceof Type) || BRIDGE == null) continue;
                try {
                    Field field = BRIDGE.getField(((Type)object).name());
                    field.setAccessible(true);
                    Object legacy = field.get(null);
                    if (legacy != null) {
                        copy[n] = legacy;
                    }
                }
                catch (Throwable throwable) {
                    // marqueur inconnu côté legacy : gardé tel quel
                }
            }
            return copy;
        }
    }

    static {
        // Force l'initialisation de Bukkit avant le premier appel distant, et journalise une fois
        // l'absence de PDC (serveur < 1.14) pour qu'un administrateur comprenne le repli engagé.
        boolean available = Bukkit.available();
        if (!available && Bukkit.ITEM_STACK != null) {
            try {
                Object logger = Class.forName("org.bukkit.Bukkit").getMethod("getLogger", new Class[0]).invoke(null, new Object[0]);
                if (logger != null) {
                    logger.getClass().getMethod("warning", String.class).invoke(logger, "[ValoriaTycoon] PersistentDataContainer indisponible : repli sur l'ancienne lecture NBT (serveur trop ancien ?).");
                }
            }
            catch (Throwable throwable) {
                // aucun log possible : rien à faire de plus
            }
        }
    }
}
