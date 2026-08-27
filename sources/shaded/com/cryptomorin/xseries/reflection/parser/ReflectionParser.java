package com.cryptomorin.xseries.reflection.parser;

import com.cryptomorin.xseries.reflection.ReflectiveHandle;
import com.cryptomorin.xseries.reflection.ReflectiveNamespace;
import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.ConstructorMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.FieldMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.FlaggedNamedMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.MemberHandle;
import com.cryptomorin.xseries.reflection.jvm.MethodMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.DynamicClassHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.PackageHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ReflectionParser {
    private final String declaration;
    private Pattern pattern;
    private Matcher matcher;
    private ReflectiveNamespace namespace;
    private Map<String, Class<?>> cachedImports;
    private final Set<Flag> flags = EnumSet.noneOf(Flag.class);
    private static final PackageHandle[] PACKAGE_HANDLES = MinecraftPackage.values();
    @Language(value="RegExp")
    private static final String JAVA_TYPE_REGEX = "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(?:<[.\\w<>\\[\\], ]+>)?((?:\\[])*)";
    private static final Map<String, Class<?>> PREDEFINED_TYPES = new HashMap();
    @Language(value="RegExp")
    private static final String GENERIC;
    @Language(value="RegExp")
    private static final String PACKAGE_REGEX = "(?:package\\s+(?<package>(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\s*;\\s*)?";
    @Language(value="RegExp")
    private static final String CLASS_TYPES = "(?<classType>class|interface|enum)";
    @Language(value="RegExp")
    private static final String PARAMETERS = "\\s*\\(\\s*(?<parameters>[\\w$_,. ]+)?\\s*\\)";
    @Language(value="RegExp")
    private static final String END_DECL = "\\s*;?\\s*";
    private static final Pattern CLASS;
    private static final Pattern METHOD;
    private static final Pattern CONSTRUCTOR;
    private static final Pattern FIELD;

    public ReflectionParser(@Language(value="Java") String string) {
        this.declaration = string;
    }

    @Language(value="RegExp")
    private static String id(@Language(value="RegExp") String string) {
        if (string == null) {
            return "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
        }
        return "(?<" + string + '>' + "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*" + ')';
    }

    @Language(value="RegExp")
    private static String type(@Language(value="RegExp") String string) {
        if (string == null) {
            return JAVA_TYPE_REGEX;
        }
        return "(?<" + string + '>' + JAVA_TYPE_REGEX + ')';
    }

    private Class<?>[] parseTypes(String[] stringArray) {
        Class[] classArray = new Class[stringArray.length];
        for (int i = 0; i < stringArray.length; ++i) {
            String string = stringArray[i];
            string = string.trim().substring(0, string.lastIndexOf(32)).trim();
            classArray[i] = this.parseType(string);
        }
        return classArray;
    }

    private Class<?> parseType(String string) {
        Class clazz;
        if (this.cachedImports == null && this.namespace != null) {
            this.cachedImports = this.namespace.getImports();
        }
        String string2 = string;
        int n = 0;
        if (string.endsWith("[]")) {
            clazz = string.replace("[]", "");
            n = (string.length() - ((String)((Object)clazz)).length()) / 2;
            string = clazz;
        }
        if (string.endsWith(">")) {
            string = string.substring(0, string.indexOf(60));
        }
        clazz = null;
        if (!string.contains(".")) {
            if (this.cachedImports != null) {
                clazz = this.cachedImports.get(string);
            }
            if (clazz == null) {
                clazz = PREDEFINED_TYPES.get(string);
            }
        }
        if (clazz == null) {
            try {
                clazz = Class.forName(string);
            }
            catch (ClassNotFoundException classNotFoundException) {
                // empty catch block
            }
        }
        if (clazz == null) {
            this.error("Unknown type '" + string2 + "' -> '" + string + '\'');
        }
        if (n != 0) {
            clazz = (Class)XReflection.of(clazz).asArray(n).unreflect();
        }
        return clazz;
    }

    public ReflectionParser imports(ReflectiveNamespace reflectiveNamespace) {
        this.namespace = reflectiveNamespace;
        return this;
    }

    private void pattern(Pattern pattern, ReflectiveHandle<?> reflectiveHandle) {
        this.pattern = pattern;
        this.matcher = pattern.matcher(this.declaration);
        this.start(reflectiveHandle);
    }

    public <T extends DynamicClassHandle> T parseClass(T t) {
        this.pattern(CLASS, t);
        String string = this.group("package");
        if (string != null && !string.isEmpty()) {
            boolean bl = false;
            for (PackageHandle packageHandle : PACKAGE_HANDLES) {
                String string2 = packageHandle.packageId().toLowerCase(Locale.ENGLISH);
                if (!string.startsWith(string2)) continue;
                t.inPackage(packageHandle, string.substring(string2.length() + 1));
                bl = true;
                break;
            }
            if (!bl) {
                t.inPackage(string);
            }
        }
        t.named(this.group("className").split("\\$"));
        return t;
    }

    public <T extends ConstructorMemberHandle> T parseConstructor(T t) {
        this.pattern(CONSTRUCTOR, t);
        if (this.has("className") && !t.getClassHandle().getPossibleNames().contains(this.group("className"))) {
            this.error("Wrong class name associated to constructor, possible names: " + t.getClassHandle().getPossibleNames());
        }
        if (this.has("parameters")) {
            t.parameters(this.parseTypes(this.group("parameters").split(",")));
        }
        return t;
    }

    public <T extends MethodMemberHandle> T parseMethod(T t) {
        this.pattern(METHOD, t);
        t.named(this.group("methodName").split("\\$"));
        t.returns((Class)this.parseType(this.group("methodReturnType")));
        if (this.has("parameters")) {
            t.parameters(this.parseTypes(this.group("parameters").split(",")));
        }
        return t;
    }

    public <T extends FieldMemberHandle> T parseField(T t) {
        this.pattern(FIELD, t);
        t.named(this.group("fieldName").split("\\$"));
        t.returns((Class)this.parseType(this.group("fieldType")));
        return t;
    }

    private String group(String string) {
        return this.matcher.group(string);
    }

    private boolean has(String string) {
        String string2 = this.group(string);
        return string2 != null && !string2.isEmpty();
    }

    private void start(ReflectiveHandle<?> reflectiveHandle) {
        if (!this.matcher.matches()) {
            this.error("Not a " + reflectiveHandle + " declaration");
        }
        this.parseFlags();
        if (reflectiveHandle instanceof MemberHandle) {
            MemberHandle memberHandle = (MemberHandle)reflectiveHandle;
            if (!ReflectionParser.hasOneOf(this.flags, Flag.PUBLIC, Flag.PROTECTED, Flag.PRIVATE)) {
                Class clazz = (Class)memberHandle.getClassHandle().reflectOrNull();
                if (clazz != null && !clazz.isInterface()) {
                    memberHandle.makeAccessible();
                }
            } else if (ReflectionParser.hasOneOf(this.flags, Flag.PRIVATE, Flag.PROTECTED)) {
                memberHandle.makeAccessible();
            }
            if (reflectiveHandle instanceof FieldMemberHandle && this.flags.contains((Object)Flag.FINAL)) {
                ((FieldMemberHandle)reflectiveHandle).asFinal();
            }
            if (reflectiveHandle instanceof FlaggedNamedMemberHandle && this.flags.contains((Object)Flag.STATIC)) {
                ((FlaggedNamedMemberHandle)reflectiveHandle).asStatic();
            }
        }
    }

    private void parseFlags() {
        if (!this.has("flags")) {
            return;
        }
        String string = this.group("flags");
        for (String string2 : string.split("\\s+")) {
            if (this.flags.add(Flag.valueOf(string2.toUpperCase()))) continue;
            this.error("Repeated flag: " + string2);
        }
        if (ReflectionParser.containsDuplicates(this.flags, Flag.PUBLIC, Flag.PROTECTED, Flag.PRIVATE)) {
            this.error("Duplicate visibility flags");
        }
    }

    @SafeVarargs
    private static <T> boolean containsDuplicates(Collection<T> collection, T ... TArray) {
        boolean bl = false;
        for (T t : TArray) {
            if (!collection.contains(t)) continue;
            if (bl) {
                return true;
            }
            bl = true;
        }
        return false;
    }

    @SafeVarargs
    private static <T> boolean hasOneOf(Collection<T> collection, T ... TArray) {
        return Arrays.stream(TArray).anyMatch(collection::contains);
    }

    private void error(String string) {
        throw new RuntimeException(string + " in: " + this.declaration + " (RegEx: " + this.pattern.pattern() + "), (Imports: " + this.cachedImports + ')');
    }

    static {
        Arrays.asList(Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Boolean.TYPE, Character.TYPE, Void.TYPE, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Boolean.class, Character.class, Void.class, String.class, Optional.class, StringBuilder.class, StringBuffer.class, UUID.class, Map.class, HashMap.class, ConcurrentHashMap.class, LinkedHashMap.class, WeakHashMap.class, List.class, ArrayList.class, Set.class, HashSet.class, Deque.class, Queue.class, LinkedList.class, Date.class, Calendar.class, Duration.class).forEach(clazz -> PREDEFINED_TYPES.put(clazz.getSimpleName(), (Class<?>)clazz));
        GENERIC = "(?:<" + ReflectionParser.id(null) + ">)*";
        CLASS = Pattern.compile(PACKAGE_REGEX + Flag.FLAGS_REGEX + CLASS_TYPES + "\\s+" + ReflectionParser.id("className") + "(?:\\s+extends\\s+" + ReflectionParser.id("superclasses") + ")?\\s+(implements\\s+" + ReflectionParser.id("interfaces") + ")?(?:\\s*\\{\\s*})?\\s*");
        METHOD = Pattern.compile(Flag.FLAGS_REGEX + ReflectionParser.type("methodReturnType") + "\\s+" + ReflectionParser.id("methodName") + PARAMETERS + END_DECL);
        CONSTRUCTOR = Pattern.compile(Flag.FLAGS_REGEX + "\\s+" + ReflectionParser.id("className") + PARAMETERS + END_DECL);
        FIELD = Pattern.compile(Flag.FLAGS_REGEX + ReflectionParser.type("fieldType") + "\\s+" + ReflectionParser.id("fieldName") + END_DECL);
    }

    private static enum Flag {
        PUBLIC,
        PROTECTED,
        PRIVATE,
        FINAL,
        TRANSIENT,
        ABSTRACT,
        STATIC,
        NATIVE,
        SYNCHRONIZED,
        STRICTFP,
        VOLATILE;

        private static final String FLAGS_REGEX;

        static {
            FLAGS_REGEX = "(?<flags>(?:(?:" + Arrays.stream(Flag.values()).map(Enum::name).map(string -> string.toLowerCase(Locale.ENGLISH)).collect(Collectors.joining("|")) + ")\\s*)+)?";
        }
    }
}

