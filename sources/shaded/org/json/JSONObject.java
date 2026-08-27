package org.json;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONPointer;
import org.json.JSONPointerException;
import org.json.JSONPropertyIgnore;
import org.json.JSONPropertyName;
import org.json.JSONString;
import org.json.JSONTokener;
import org.json.JSONWriter;

public class JSONObject {
    static final Pattern NUMBER_PATTERN = Pattern.compile("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");
    private final Map<String, Object> map;
    public static final Object NULL = new Null();

    public Class<? extends Map> getMapType() {
        return this.map.getClass();
    }

    public JSONObject() {
        this.map = new HashMap<String, Object>();
    }

    public JSONObject(JSONObject jSONObject, String ... stringArray) {
        this(stringArray.length);
        for (int i = 0; i < stringArray.length; ++i) {
            try {
                this.putOnce(stringArray[i], jSONObject.opt(stringArray[i]));
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public JSONObject(JSONTokener jSONTokener) {
        this();
        if (jSONTokener.nextClean() != '{') {
            throw jSONTokener.syntaxError("A JSONObject text must begin with '{'");
        }
        block8: while (true) {
            char c = jSONTokener.nextClean();
            switch (c) {
                case '\u0000': {
                    throw jSONTokener.syntaxError("A JSONObject text must end with '}'");
                }
                case '}': {
                    return;
                }
            }
            String string = jSONTokener.nextSimpleValue(c).toString();
            c = jSONTokener.nextClean();
            if (c != ':') {
                throw jSONTokener.syntaxError("Expected a ':' after a key");
            }
            if (string != null) {
                if (this.opt(string) != null) {
                    throw jSONTokener.syntaxError("Duplicate key \"" + string + "\"");
                }
                Object object = jSONTokener.nextValue();
                if (object != null) {
                    this.put(string, object);
                }
            }
            switch (jSONTokener.nextClean()) {
                case ',': 
                case ';': {
                    if (jSONTokener.nextClean() == '}') {
                        return;
                    }
                    if (jSONTokener.end()) {
                        throw jSONTokener.syntaxError("A JSONObject text must end with '}'");
                    }
                    jSONTokener.back();
                    continue block8;
                }
                case '}': {
                    return;
                }
            }
            break;
        }
        throw jSONTokener.syntaxError("Expected a ',' or '}'");
    }

    public JSONObject(Map<?, ?> map) {
        if (map == null) {
            this.map = new HashMap<String, Object>();
        } else {
            this.map = new HashMap<String, Object>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    throw new NullPointerException("Null key.");
                }
                Object obj = entry.getValue();
                if (obj == null) continue;
                JSONObject.testValidity(obj);
                this.map.put(String.valueOf(entry.getKey()), JSONObject.wrap(obj));
            }
        }
    }

    public JSONObject(Object object) {
        this();
        this.populateMap(object);
    }

    private JSONObject(Object object, Set<Object> set) {
        this();
        this.populateMap(object, set);
    }

    public JSONObject(Object object, String ... stringArray) {
        this(stringArray.length);
        Class<?> clazz = object.getClass();
        for (int i = 0; i < stringArray.length; ++i) {
            String string = stringArray[i];
            try {
                this.putOpt(string, clazz.getField(string).get(object));
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public JSONObject(String string) {
        this(new JSONTokener(string));
    }

    public JSONObject(String string, Locale locale) {
        this();
        ResourceBundle resourceBundle = ResourceBundle.getBundle(string, locale, Thread.currentThread().getContextClassLoader());
        Enumeration<String> enumeration = resourceBundle.getKeys();
        while (enumeration.hasMoreElements()) {
            String string2 = enumeration.nextElement();
            if (string2 == null) continue;
            String[] stringArray = string2.split("\\.");
            int n = stringArray.length - 1;
            JSONObject jSONObject = this;
            for (int i = 0; i < n; ++i) {
                String string3 = stringArray[i];
                JSONObject jSONObject2 = jSONObject.optJSONObject(string3);
                if (jSONObject2 == null) {
                    jSONObject2 = new JSONObject();
                    jSONObject.put(string3, jSONObject2);
                }
                jSONObject = jSONObject2;
            }
            jSONObject.put(stringArray[n], resourceBundle.getString(string2));
        }
    }

    protected JSONObject(int n) {
        this.map = new HashMap<String, Object>(n);
    }

    public JSONObject accumulate(String string, Object object) {
        JSONObject.testValidity(object);
        Object object2 = this.opt(string);
        if (object2 == null) {
            this.put(string, object instanceof JSONArray ? new JSONArray().put(object) : object);
        } else if (object2 instanceof JSONArray) {
            ((JSONArray)object2).put(object);
        } else {
            this.put(string, new JSONArray().put(object2).put(object));
        }
        return this;
    }

    public JSONObject append(String string, Object object) {
        JSONObject.testValidity(object);
        Object object2 = this.opt(string);
        if (object2 == null) {
            this.put(string, new JSONArray().put(object));
        } else if (object2 instanceof JSONArray) {
            this.put(string, ((JSONArray)object2).put(object));
        } else {
            throw JSONObject.wrongValueFormatException(string, "JSONArray", null, null);
        }
        return this;
    }

    public static String doubleToString(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return "null";
        }
        String string = Double.toString(d);
        if (string.indexOf(46) > 0 && string.indexOf(101) < 0 && string.indexOf(69) < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }

    public Object get(String string) {
        if (string == null) {
            throw new JSONException("Null key.");
        }
        Object object = this.opt(string);
        if (object == null) {
            throw new JSONException("JSONObject[" + JSONObject.quote(string) + "] not found.");
        }
        return object;
    }

    public <E extends Enum<E>> E getEnum(Class<E> clazz, String string) {
        E e = this.optEnum(clazz, string);
        if (e == null) {
            throw JSONObject.wrongValueFormatException(string, "enum of type " + JSONObject.quote(clazz.getSimpleName()), this.opt(string), null);
        }
        return e;
    }

    public boolean getBoolean(String string) {
        Object object = this.get(string);
        if (object.equals(Boolean.FALSE) || object instanceof String && ((String)object).equalsIgnoreCase("false")) {
            return false;
        }
        if (object.equals(Boolean.TRUE) || object instanceof String && ((String)object).equalsIgnoreCase("true")) {
            return true;
        }
        throw JSONObject.wrongValueFormatException(string, "Boolean", object, null);
    }

    public BigInteger getBigInteger(String string) {
        Object object = this.get(string);
        BigInteger bigInteger = JSONObject.objectToBigInteger(object, null);
        if (bigInteger != null) {
            return bigInteger;
        }
        throw JSONObject.wrongValueFormatException(string, "BigInteger", object, null);
    }

    public BigDecimal getBigDecimal(String string) {
        Object object = this.get(string);
        BigDecimal bigDecimal = JSONObject.objectToBigDecimal(object, null);
        if (bigDecimal != null) {
            return bigDecimal;
        }
        throw JSONObject.wrongValueFormatException(string, "BigDecimal", object, null);
    }

    public double getDouble(String string) {
        Object object = this.get(string);
        if (object instanceof Number) {
            return ((Number)object).doubleValue();
        }
        try {
            return Double.parseDouble(object.toString());
        }
        catch (Exception exception) {
            throw JSONObject.wrongValueFormatException(string, "double", object, exception);
        }
    }

    public float getFloat(String string) {
        Object object = this.get(string);
        if (object instanceof Number) {
            return ((Number)object).floatValue();
        }
        try {
            return Float.parseFloat(object.toString());
        }
        catch (Exception exception) {
            throw JSONObject.wrongValueFormatException(string, "float", object, exception);
        }
    }

    public Number getNumber(String string) {
        Object object = this.get(string);
        try {
            if (object instanceof Number) {
                return (Number)object;
            }
            return JSONObject.stringToNumber(object.toString());
        }
        catch (Exception exception) {
            throw JSONObject.wrongValueFormatException(string, "number", object, exception);
        }
    }

    public int getInt(String string) {
        Object object = this.get(string);
        if (object instanceof Number) {
            return ((Number)object).intValue();
        }
        try {
            return Integer.parseInt(object.toString());
        }
        catch (Exception exception) {
            throw JSONObject.wrongValueFormatException(string, "int", object, exception);
        }
    }

    public JSONArray getJSONArray(String string) {
        Object object = this.get(string);
        if (object instanceof JSONArray) {
            return (JSONArray)object;
        }
        throw JSONObject.wrongValueFormatException(string, "JSONArray", object, null);
    }

    public JSONObject getJSONObject(String string) {
        Object object = this.get(string);
        if (object instanceof JSONObject) {
            return (JSONObject)object;
        }
        throw JSONObject.wrongValueFormatException(string, "JSONObject", object, null);
    }

    public long getLong(String string) {
        Object object = this.get(string);
        if (object instanceof Number) {
            return ((Number)object).longValue();
        }
        try {
            return Long.parseLong(object.toString());
        }
        catch (Exception exception) {
            throw JSONObject.wrongValueFormatException(string, "long", object, exception);
        }
    }

    public static String[] getNames(JSONObject jSONObject) {
        if (jSONObject.isEmpty()) {
            return null;
        }
        return jSONObject.keySet().toArray(new String[jSONObject.length()]);
    }

    public static String[] getNames(Object object) {
        if (object == null) {
            return null;
        }
        Class<?> clazz = object.getClass();
        Field[] fieldArray = clazz.getFields();
        int n = fieldArray.length;
        if (n == 0) {
            return null;
        }
        String[] stringArray = new String[n];
        for (int i = 0; i < n; ++i) {
            stringArray[i] = fieldArray[i].getName();
        }
        return stringArray;
    }

    public String getString(String string) {
        Object object = this.get(string);
        if (object instanceof String) {
            return (String)object;
        }
        throw JSONObject.wrongValueFormatException(string, "string", object, null);
    }

    public boolean has(String string) {
        return this.map.containsKey(string);
    }

    public JSONObject increment(String string) {
        Object object = this.opt(string);
        if (object == null) {
            this.put(string, 1);
        } else if (object instanceof Integer) {
            this.put(string, (Integer)object + 1);
        } else if (object instanceof Long) {
            this.put(string, (Long)object + 1L);
        } else if (object instanceof BigInteger) {
            this.put(string, ((BigInteger)object).add(BigInteger.ONE));
        } else if (object instanceof Float) {
            this.put(string, ((Float)object).floatValue() + 1.0f);
        } else if (object instanceof Double) {
            this.put(string, (Double)object + 1.0);
        } else if (object instanceof BigDecimal) {
            this.put(string, ((BigDecimal)object).add(BigDecimal.ONE));
        } else {
            throw new JSONException("Unable to increment [" + JSONObject.quote(string) + "].");
        }
        return this;
    }

    public boolean isNull(String string) {
        return NULL.equals(this.opt(string));
    }

    public Iterator<String> keys() {
        return this.keySet().iterator();
    }

    public Set<String> keySet() {
        return this.map.keySet();
    }

    protected Set<Map.Entry<String, Object>> entrySet() {
        return this.map.entrySet();
    }

    public int length() {
        return this.map.size();
    }

    public void clear() {
        this.map.clear();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public JSONArray names() {
        if (this.map.isEmpty()) {
            return null;
        }
        return new JSONArray((Collection<?>)this.map.keySet());
    }

    public static String numberToString(Number number) {
        if (number == null) {
            throw new JSONException("Null pointer");
        }
        JSONObject.testValidity(number);
        String string = number.toString();
        if (string.indexOf(46) > 0 && string.indexOf(101) < 0 && string.indexOf(69) < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }

    public Object opt(String string) {
        return string == null ? null : this.map.get(string);
    }

    public <E extends Enum<E>> E optEnum(Class<E> clazz, String string) {
        return this.optEnum(clazz, string, null);
    }

    public <E extends Enum<E>> E optEnum(Class<E> clazz, String string, E e) {
        try {
            Object object = this.opt(string);
            if (NULL.equals(object)) {
                return e;
            }
            if (clazz.isAssignableFrom(object.getClass())) {
                Enum enum_ = (Enum)object;
                return (E)enum_;
            }
            return Enum.valueOf(clazz, object.toString());
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return e;
        }
        catch (NullPointerException nullPointerException) {
            return e;
        }
    }

    public boolean optBoolean(String string) {
        return this.optBoolean(string, false);
    }

    public boolean optBoolean(String string, boolean bl) {
        Object object = this.opt(string);
        if (NULL.equals(object)) {
            return bl;
        }
        if (object instanceof Boolean) {
            return (Boolean)object;
        }
        try {
            return this.getBoolean(string);
        }
        catch (Exception exception) {
            return bl;
        }
    }

    public Boolean optBooleanObject(String string) {
        return this.optBooleanObject(string, false);
    }

    public Boolean optBooleanObject(String string, Boolean bl) {
        Object object = this.opt(string);
        if (NULL.equals(object)) {
            return bl;
        }
        if (object instanceof Boolean) {
            return (boolean)((Boolean)object);
        }
        try {
            return this.getBoolean(string);
        }
        catch (Exception exception) {
            return bl;
        }
    }

    public BigDecimal optBigDecimal(String string, BigDecimal bigDecimal) {
        Object object = this.opt(string);
        return JSONObject.objectToBigDecimal(object, bigDecimal);
    }

    static BigDecimal objectToBigDecimal(Object object, BigDecimal bigDecimal) {
        return JSONObject.objectToBigDecimal(object, bigDecimal, true);
    }

    static BigDecimal objectToBigDecimal(Object object, BigDecimal bigDecimal, boolean bl) {
        if (NULL.equals(object)) {
            return bigDecimal;
        }
        if (object instanceof BigDecimal) {
            return (BigDecimal)object;
        }
        if (object instanceof BigInteger) {
            return new BigDecimal((BigInteger)object);
        }
        if (object instanceof Double || object instanceof Float) {
            if (!JSONObject.numberIsFinite((Number)object)) {
                return bigDecimal;
            }
            if (bl) {
                return new BigDecimal(((Number)object).doubleValue());
            }
            return new BigDecimal(object.toString());
        }
        if (object instanceof Long || object instanceof Integer || object instanceof Short || object instanceof Byte) {
            return new BigDecimal(((Number)object).longValue());
        }
        try {
            return new BigDecimal(object.toString());
        }
        catch (Exception exception) {
            return bigDecimal;
        }
    }

    public BigInteger optBigInteger(String string, BigInteger bigInteger) {
        Object object = this.opt(string);
        return JSONObject.objectToBigInteger(object, bigInteger);
    }

    static BigInteger objectToBigInteger(Object object, BigInteger bigInteger) {
        if (NULL.equals(object)) {
            return bigInteger;
        }
        if (object instanceof BigInteger) {
            return (BigInteger)object;
        }
        if (object instanceof BigDecimal) {
            return ((BigDecimal)object).toBigInteger();
        }
        if (object instanceof Double || object instanceof Float) {
            if (!JSONObject.numberIsFinite((Number)object)) {
                return bigInteger;
            }
            return new BigDecimal(((Number)object).doubleValue()).toBigInteger();
        }
        if (object instanceof Long || object instanceof Integer || object instanceof Short || object instanceof Byte) {
            return BigInteger.valueOf(((Number)object).longValue());
        }
        try {
            String string = object.toString();
            if (JSONObject.isDecimalNotation(string)) {
                return new BigDecimal(string).toBigInteger();
            }
            return new BigInteger(string);
        }
        catch (Exception exception) {
            return bigInteger;
        }
    }

    public double optDouble(String string) {
        return this.optDouble(string, Double.NaN);
    }

    public double optDouble(String string, double d) {
        Number number = this.optNumber(string);
        if (number == null) {
            return d;
        }
        return number.doubleValue();
    }

    public Double optDoubleObject(String string) {
        return this.optDoubleObject(string, Double.NaN);
    }

    public Double optDoubleObject(String string, Double d) {
        Number number = this.optNumber(string);
        if (number == null) {
            return d;
        }
        return number.doubleValue();
    }

    public float optFloat(String string) {
        return this.optFloat(string, Float.NaN);
    }

    public float optFloat(String string, float f) {
        Number number = this.optNumber(string);
        if (number == null) {
            return f;
        }
        float f2 = number.floatValue();
        return f2;
    }

    public Float optFloatObject(String string) {
        return this.optFloatObject(string, Float.valueOf(Float.NaN));
    }

    public Float optFloatObject(String string, Float f) {
        Number number = this.optNumber(string);
        if (number == null) {
            return f;
        }
        Float f2 = Float.valueOf(number.floatValue());
        return f2;
    }

    public int optInt(String string) {
        return this.optInt(string, 0);
    }

    public int optInt(String string, int n) {
        Number number = this.optNumber(string, null);
        if (number == null) {
            return n;
        }
        return number.intValue();
    }

    public Integer optIntegerObject(String string) {
        return this.optIntegerObject(string, 0);
    }

    public Integer optIntegerObject(String string, Integer n) {
        Number number = this.optNumber(string, null);
        if (number == null) {
            return n;
        }
        return number.intValue();
    }

    public JSONArray optJSONArray(String string) {
        return this.optJSONArray(string, null);
    }

    public JSONArray optJSONArray(String string, JSONArray jSONArray) {
        Object object = this.opt(string);
        return object instanceof JSONArray ? (JSONArray)object : jSONArray;
    }

    public JSONObject optJSONObject(String string) {
        return this.optJSONObject(string, null);
    }

    public JSONObject optJSONObject(String string, JSONObject jSONObject) {
        Object object = this.opt(string);
        return object instanceof JSONObject ? (JSONObject)object : jSONObject;
    }

    public long optLong(String string) {
        return this.optLong(string, 0L);
    }

    public long optLong(String string, long l) {
        Number number = this.optNumber(string, null);
        if (number == null) {
            return l;
        }
        return number.longValue();
    }

    public Long optLongObject(String string) {
        return this.optLongObject(string, 0L);
    }

    public Long optLongObject(String string, Long l) {
        Number number = this.optNumber(string, null);
        if (number == null) {
            return l;
        }
        return number.longValue();
    }

    public Number optNumber(String string) {
        return this.optNumber(string, null);
    }

    public Number optNumber(String string, Number number) {
        Object object = this.opt(string);
        if (NULL.equals(object)) {
            return number;
        }
        if (object instanceof Number) {
            return (Number)object;
        }
        try {
            return JSONObject.stringToNumber(object.toString());
        }
        catch (Exception exception) {
            return number;
        }
    }

    public String optString(String string) {
        return this.optString(string, "");
    }

    public String optString(String string, String string2) {
        Object object = this.opt(string);
        return NULL.equals(object) ? string2 : object.toString();
    }

    private void populateMap(Object object) {
        this.populateMap(object, Collections.newSetFromMap(new IdentityHashMap()));
    }

    private void populateMap(Object object, Set<Object> set) {
        Method[] methodArray;
        Class<?> clazz = object.getClass();
        boolean bl = clazz.getClassLoader() != null;
        for (Method method : methodArray = bl ? clazz.getMethods() : clazz.getDeclaredMethods()) {
            String string;
            int n = method.getModifiers();
            if (!Modifier.isPublic(n) || Modifier.isStatic(n) || method.getParameterTypes().length != 0 || method.isBridge() || method.getReturnType() == Void.TYPE || !JSONObject.isValidMethodName(method.getName()) || (string = JSONObject.getKeyNameFromMethod(method)) == null || string.isEmpty()) continue;
            try {
                Object object2 = method.invoke(object, new Object[0]);
                if (object2 == null) continue;
                if (set.contains(object2)) {
                    throw JSONObject.recursivelyDefinedObjectException(string);
                }
                set.add(object2);
                JSONObject.testValidity(object2);
                this.map.put(string, JSONObject.wrap(object2, set));
                set.remove(object2);
                if (!(object2 instanceof Closeable)) continue;
                try {
                    ((Closeable)object2).close();
                }
                catch (IOException iOException) {}
            }
            catch (IllegalAccessException illegalAccessException) {
            }
            catch (IllegalArgumentException illegalArgumentException) {
            }
            catch (InvocationTargetException invocationTargetException) {
                // empty catch block
            }
        }
    }

    private static boolean isValidMethodName(String string) {
        return !"getClass".equals(string) && !"getDeclaringClass".equals(string);
    }

    private static String getKeyNameFromMethod(Method method) {
        String string;
        int n;
        int n2 = JSONObject.getAnnotationDepth(method, JSONPropertyIgnore.class);
        if (n2 > 0 && ((n = JSONObject.getAnnotationDepth(method, JSONPropertyName.class)) < 0 || n2 <= n)) {
            return null;
        }
        JSONPropertyName jSONPropertyName = JSONObject.getAnnotation(method, JSONPropertyName.class);
        if (jSONPropertyName != null && jSONPropertyName.value() != null && !jSONPropertyName.value().isEmpty()) {
            return jSONPropertyName.value();
        }
        String string2 = method.getName();
        if (string2.startsWith("get") && string2.length() > 3) {
            string = string2.substring(3);
        } else if (string2.startsWith("is") && string2.length() > 2) {
            string = string2.substring(2);
        } else {
            return null;
        }
        if (string.length() == 0 || Character.isLowerCase(string.charAt(0))) {
            return null;
        }
        if (string.length() == 1) {
            string = string.toLowerCase(Locale.ROOT);
        } else if (!Character.isUpperCase(string.charAt(1))) {
            string = string.substring(0, 1).toLowerCase(Locale.ROOT) + string.substring(1);
        }
        return string;
    }

    private static <A extends Annotation> A getAnnotation(Method method, Class<A> clazz) {
        if (method == null || clazz == null) {
            return null;
        }
        if (method.isAnnotationPresent(clazz)) {
            return method.getAnnotation(clazz);
        }
        Class<?> clazz2 = method.getDeclaringClass();
        if (clazz2.getSuperclass() == null) {
            return null;
        }
        for (Class<?> clazz3 : clazz2.getInterfaces()) {
            try {
                Method method2 = clazz3.getMethod(method.getName(), method.getParameterTypes());
                return JSONObject.getAnnotation(method2, clazz);
            }
            catch (SecurityException securityException) {
            }
            catch (NoSuchMethodException noSuchMethodException) {
                // empty catch block
            }
        }
        try {
            return JSONObject.getAnnotation(clazz2.getSuperclass().getMethod(method.getName(), method.getParameterTypes()), clazz);
        }
        catch (SecurityException securityException) {
            return null;
        }
        catch (NoSuchMethodException noSuchMethodException) {
            return null;
        }
    }

    private static int getAnnotationDepth(Method method, Class<? extends Annotation> clazz) {
        if (method == null || clazz == null) {
            return -1;
        }
        if (method.isAnnotationPresent(clazz)) {
            return 1;
        }
        Class<?> clazz2 = method.getDeclaringClass();
        if (clazz2.getSuperclass() == null) {
            return -1;
        }
        for (Class<?> clazz3 : clazz2.getInterfaces()) {
            try {
                Method method2 = clazz3.getMethod(method.getName(), method.getParameterTypes());
                int n = JSONObject.getAnnotationDepth(method2, clazz);
                if (n <= 0) continue;
                return n + 1;
            }
            catch (SecurityException securityException) {
            }
            catch (NoSuchMethodException noSuchMethodException) {
                // empty catch block
            }
        }
        try {
            int n = JSONObject.getAnnotationDepth(clazz2.getSuperclass().getMethod(method.getName(), method.getParameterTypes()), clazz);
            if (n > 0) {
                return n + 1;
            }
            return -1;
        }
        catch (SecurityException securityException) {
            return -1;
        }
        catch (NoSuchMethodException noSuchMethodException) {
            return -1;
        }
    }

    public JSONObject put(String string, boolean bl) {
        return this.put(string, bl ? Boolean.TRUE : Boolean.FALSE);
    }

    public JSONObject put(String string, Collection<?> collection) {
        return this.put(string, new JSONArray(collection));
    }

    public JSONObject put(String string, double d) {
        return this.put(string, (Object)d);
    }

    public JSONObject put(String string, float f) {
        return this.put(string, Float.valueOf(f));
    }

    public JSONObject put(String string, int n) {
        return this.put(string, (Object)n);
    }

    public JSONObject put(String string, long l) {
        return this.put(string, (Object)l);
    }

    public JSONObject put(String string, Map<?, ?> map) {
        return this.put(string, new JSONObject(map));
    }

    public JSONObject put(String string, Object object) {
        if (string == null) {
            throw new NullPointerException("Null key.");
        }
        if (object != null) {
            JSONObject.testValidity(object);
            this.map.put(string, object);
        } else {
            this.remove(string);
        }
        return this;
    }

    public JSONObject putOnce(String string, Object object) {
        if (string != null && object != null) {
            if (this.opt(string) != null) {
                throw new JSONException("Duplicate key \"" + string + "\"");
            }
            return this.put(string, object);
        }
        return this;
    }

    public JSONObject putOpt(String string, Object object) {
        if (string != null && object != null) {
            return this.put(string, object);
        }
        return this;
    }

    public Object query(String string) {
        return this.query(new JSONPointer(string));
    }

    public Object query(JSONPointer jSONPointer) {
        return jSONPointer.queryFrom(this);
    }

    public Object optQuery(String string) {
        return this.optQuery(new JSONPointer(string));
    }

    public Object optQuery(JSONPointer jSONPointer) {
        try {
            return jSONPointer.queryFrom(this);
        }
        catch (JSONPointerException jSONPointerException) {
            return null;
        }
    }

    public static String quote(String string) {
        StringWriter stringWriter = new StringWriter();
        try {
            return JSONObject.quote(string, stringWriter).toString();
        }
        catch (IOException iOException) {
            return "";
        }
    }

    public static Writer quote(String string, Writer writer) {
        if (string == null || string.isEmpty()) {
            writer.write("\"\"");
            return writer;
        }
        char c = '\u0000';
        int n = string.length();
        writer.write(34);
        block9: for (int i = 0; i < n; ++i) {
            char c2 = c;
            c = string.charAt(i);
            switch (c) {
                case '\"': 
                case '\\': {
                    writer.write(92);
                    writer.write(c);
                    continue block9;
                }
                case '/': {
                    if (c2 == '<') {
                        writer.write(92);
                    }
                    writer.write(c);
                    continue block9;
                }
                case '\b': {
                    writer.write("\\b");
                    continue block9;
                }
                case '\t': {
                    writer.write("\\t");
                    continue block9;
                }
                case '\n': {
                    writer.write("\\n");
                    continue block9;
                }
                case '\f': {
                    writer.write("\\f");
                    continue block9;
                }
                case '\r': {
                    writer.write("\\r");
                    continue block9;
                }
                default: {
                    if (c < ' ' || c >= '\u0080' && c < '\u00a0' || c >= '\u2000' && c < '\u2100') {
                        writer.write("\\u");
                        String string2 = Integer.toHexString(c);
                        writer.write("0000", 0, 4 - string2.length());
                        writer.write(string2);
                        continue block9;
                    }
                    writer.write(c);
                }
            }
        }
        writer.write(34);
        return writer;
    }

    public Object remove(String string) {
        return this.map.remove(string);
    }

    public boolean similar(Object object) {
        try {
            if (!(object instanceof JSONObject)) {
                return false;
            }
            if (!this.keySet().equals(((JSONObject)object).keySet())) {
                return false;
            }
            for (Map.Entry<String, Object> entry : this.entrySet()) {
                Object object2;
                String string = entry.getKey();
                Object object3 = entry.getValue();
                if (object3 == (object2 = ((JSONObject)object).get(string))) continue;
                if (object3 == null) {
                    return false;
                }
                if (!(object3 instanceof JSONObject ? !((JSONObject)object3).similar(object2) : (object3 instanceof JSONArray ? !((JSONArray)object3).similar(object2) : (object3 instanceof Number && object2 instanceof Number ? !JSONObject.isNumberSimilar((Number)object3, (Number)object2) : (object3 instanceof JSONString && object2 instanceof JSONString ? !((JSONString)object3).toJSONString().equals(((JSONString)object2).toJSONString()) : !object3.equals(object2)))))) continue;
                return false;
            }
            return true;
        }
        catch (Throwable throwable) {
            return false;
        }
    }

    static boolean isNumberSimilar(Number number, Number number2) {
        if (!JSONObject.numberIsFinite(number) || !JSONObject.numberIsFinite(number2)) {
            return false;
        }
        if (number.getClass().equals(number2.getClass()) && number instanceof Comparable) {
            int n = ((Comparable)((Object)number)).compareTo(number2);
            return n == 0;
        }
        BigDecimal bigDecimal = JSONObject.objectToBigDecimal(number, null, false);
        BigDecimal bigDecimal2 = JSONObject.objectToBigDecimal(number2, null, false);
        if (bigDecimal == null || bigDecimal2 == null) {
            return false;
        }
        return bigDecimal.compareTo(bigDecimal2) == 0;
    }

    private static boolean numberIsFinite(Number number) {
        if (number instanceof Double && (((Double)number).isInfinite() || ((Double)number).isNaN())) {
            return false;
        }
        return !(number instanceof Float) || !((Float)number).isInfinite() && !((Float)number).isNaN();
    }

    protected static boolean isDecimalNotation(String string) {
        return string.indexOf(46) > -1 || string.indexOf(101) > -1 || string.indexOf(69) > -1 || "-0".equals(string);
    }

    protected static Number stringToNumber(String string) {
        char c;
        String string2 = string;
        if (string2.startsWith(".")) {
            string2 = "0" + string2;
        }
        if (string2.startsWith("-.")) {
            string2 = "-0." + string2.substring(2);
        }
        if ((c = string2.charAt(0)) >= '0' && c <= '9' || c == '-') {
            BigInteger bigInteger;
            char c2;
            if (JSONObject.isDecimalNotation(string2)) {
                try {
                    BigDecimal bigDecimal = new BigDecimal(string2);
                    if (c == '-' && BigDecimal.ZERO.compareTo(bigDecimal) == 0) {
                        return -0.0;
                    }
                    return bigDecimal;
                }
                catch (NumberFormatException numberFormatException) {
                    try {
                        Double d = Double.valueOf(string2);
                        if (d.isNaN() || d.isInfinite()) {
                            throw new NumberFormatException("val [" + string + "] is not a valid number.");
                        }
                        return d;
                    }
                    catch (NumberFormatException numberFormatException2) {
                        throw new NumberFormatException("val [" + string + "] is not a valid number.");
                    }
                }
            }
            string2 = JSONObject.removeLeadingZerosOfNumber(string);
            c = string2.charAt(0);
            if (c == '0' && string2.length() > 1) {
                c2 = string2.charAt(1);
                if (c2 >= '0' && c2 <= '9') {
                    throw new NumberFormatException("val [" + string + "] is not a valid number.");
                }
            } else if (c == '-' && string2.length() > 2) {
                c2 = string2.charAt(1);
                char c3 = string2.charAt(2);
                if (c2 == '0' && c3 >= '0' && c3 <= '9') {
                    throw new NumberFormatException("val [" + string + "] is not a valid number.");
                }
            }
            if ((bigInteger = new BigInteger(string2)).bitLength() <= 31) {
                return bigInteger.intValue();
            }
            if (bigInteger.bitLength() <= 63) {
                return bigInteger.longValue();
            }
            return bigInteger;
        }
        throw new NumberFormatException("val [" + string + "] is not a valid number.");
    }

    public static Object stringToValue(String string) {
        if ("".equals(string)) {
            return string;
        }
        if ("true".equalsIgnoreCase(string)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(string)) {
            return Boolean.FALSE;
        }
        if ("null".equalsIgnoreCase(string)) {
            return NULL;
        }
        if (JSONObject.potentialNumber(string)) {
            try {
                return JSONObject.stringToNumber(string);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return string;
    }

    private static boolean potentialNumber(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        return JSONObject.potentialPositiveNumberStartingAtIndex(string, string.charAt(0) == '-' ? 1 : 0);
    }

    private static boolean potentialPositiveNumberStartingAtIndex(String string, int n) {
        if (n >= string.length()) {
            return false;
        }
        return JSONObject.digitAtIndex(string, string.charAt(n) == '.' ? n + 1 : n);
    }

    private static boolean digitAtIndex(String string, int n) {
        if (n >= string.length()) {
            return false;
        }
        return string.charAt(n) >= '0' && string.charAt(n) <= '9';
    }

    public static void testValidity(Object object) {
        if (object instanceof Number && !JSONObject.numberIsFinite((Number)object)) {
            throw new JSONException("JSON does not allow non-finite numbers.");
        }
    }

    public JSONArray toJSONArray(JSONArray jSONArray) {
        if (jSONArray == null || jSONArray.isEmpty()) {
            return null;
        }
        JSONArray jSONArray2 = new JSONArray();
        for (int i = 0; i < jSONArray.length(); ++i) {
            jSONArray2.put(this.opt(jSONArray.getString(i)));
        }
        return jSONArray2;
    }

    public String toString() {
        try {
            return this.toString(0);
        }
        catch (Exception exception) {
            return null;
        }
    }

    public String toString(int n) {
        StringWriter stringWriter = new StringWriter();
        return this.write(stringWriter, n, 0).toString();
    }

    public static String valueToString(Object object) {
        return JSONWriter.valueToString(object);
    }

    public static Object wrap(Object object) {
        return JSONObject.wrap(object, null);
    }

    private static Object wrap(Object object, Set<Object> set) {
        try {
            String string;
            if (NULL.equals(object)) {
                return NULL;
            }
            if (object instanceof JSONObject || object instanceof JSONArray || NULL.equals(object) || object instanceof JSONString || object instanceof Byte || object instanceof Character || object instanceof Short || object instanceof Integer || object instanceof Long || object instanceof Boolean || object instanceof Float || object instanceof Double || object instanceof String || object instanceof BigInteger || object instanceof BigDecimal || object instanceof Enum) {
                return object;
            }
            if (object instanceof Collection) {
                Collection collection = (Collection)object;
                return new JSONArray(collection);
            }
            if (object.getClass().isArray()) {
                return new JSONArray(object);
            }
            if (object instanceof Map) {
                Map map = (Map)object;
                return new JSONObject(map);
            }
            Package package_ = object.getClass().getPackage();
            String string2 = string = package_ != null ? package_.getName() : "";
            if (string.startsWith("java.") || string.startsWith("javax.") || object.getClass().getClassLoader() == null) {
                return object.toString();
            }
            if (set != null) {
                return new JSONObject(object, set);
            }
            return new JSONObject(object);
        }
        catch (JSONException jSONException) {
            throw jSONException;
        }
        catch (Exception exception) {
            return null;
        }
    }

    public Writer write(Writer writer) {
        return this.write(writer, 0, 0);
    }

    static final Writer writeValue(Writer writer, Object object, int n, int n2) {
        if (object == null || object.equals(null)) {
            writer.write("null");
        } else if (object instanceof JSONString) {
            String string;
            try {
                string = ((JSONString)object).toJSONString();
            }
            catch (Exception exception) {
                throw new JSONException(exception);
            }
            writer.write(string != null ? string.toString() : JSONObject.quote(object.toString()));
        } else if (object instanceof Number) {
            String string = JSONObject.numberToString((Number)object);
            if (NUMBER_PATTERN.matcher(string).matches()) {
                writer.write(string);
            } else {
                JSONObject.quote(string, writer);
            }
        } else if (object instanceof Boolean) {
            writer.write(object.toString());
        } else if (object instanceof Enum) {
            writer.write(JSONObject.quote(((Enum)object).name()));
        } else if (object instanceof JSONObject) {
            ((JSONObject)object).write(writer, n, n2);
        } else if (object instanceof JSONArray) {
            ((JSONArray)object).write(writer, n, n2);
        } else if (object instanceof Map) {
            Map map = (Map)object;
            new JSONObject(map).write(writer, n, n2);
        } else if (object instanceof Collection) {
            Collection collection = (Collection)object;
            new JSONArray(collection).write(writer, n, n2);
        } else if (object.getClass().isArray()) {
            new JSONArray(object).write(writer, n, n2);
        } else {
            JSONObject.quote(object.toString(), writer);
        }
        return writer;
    }

    static final void indent(Writer writer, int n) {
        for (int i = 0; i < n; ++i) {
            writer.write(32);
        }
    }

    public Writer write(Writer writer, int n, int n2) {
        try {
            boolean bl = false;
            int n3 = this.length();
            writer.write(123);
            if (n3 == 1) {
                Map.Entry<String, Object> entry = this.entrySet().iterator().next();
                String string = entry.getKey();
                writer.write(JSONObject.quote(string));
                writer.write(58);
                if (n > 0) {
                    writer.write(32);
                }
                try {
                    JSONObject.writeValue(writer, entry.getValue(), n, n2);
                }
                catch (Exception exception) {
                    throw new JSONException("Unable to write JSONObject value for key: " + string, exception);
                }
            }
            if (n3 != 0) {
                int n4 = n2 + n;
                for (Map.Entry<String, Object> entry : this.entrySet()) {
                    if (bl) {
                        writer.write(44);
                    }
                    if (n > 0) {
                        writer.write(10);
                    }
                    JSONObject.indent(writer, n4);
                    String string = entry.getKey();
                    writer.write(JSONObject.quote(string));
                    writer.write(58);
                    if (n > 0) {
                        writer.write(32);
                    }
                    try {
                        JSONObject.writeValue(writer, entry.getValue(), n, n4);
                    }
                    catch (Exception exception) {
                        throw new JSONException("Unable to write JSONObject value for key: " + string, exception);
                    }
                    bl = true;
                }
                if (n > 0) {
                    writer.write(10);
                }
                JSONObject.indent(writer, n2);
            }
            writer.write(125);
            return writer;
        }
        catch (IOException iOException) {
            throw new JSONException(iOException);
        }
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : this.entrySet()) {
            Object object = entry.getValue() == null || NULL.equals(entry.getValue()) ? null : (entry.getValue() instanceof JSONObject ? ((JSONObject)entry.getValue()).toMap() : (entry.getValue() instanceof JSONArray ? ((JSONArray)entry.getValue()).toList() : entry.getValue()));
            hashMap.put(entry.getKey(), object);
        }
        return hashMap;
    }

    private static JSONException wrongValueFormatException(String string, String string2, Object object, Throwable throwable) {
        if (object == null) {
            return new JSONException("JSONObject[" + JSONObject.quote(string) + "] is not a " + string2 + " (null).", throwable);
        }
        if (object instanceof Map || object instanceof Iterable || object instanceof JSONObject) {
            return new JSONException("JSONObject[" + JSONObject.quote(string) + "] is not a " + string2 + " (" + object.getClass() + ").", throwable);
        }
        return new JSONException("JSONObject[" + JSONObject.quote(string) + "] is not a " + string2 + " (" + object.getClass() + " : " + object + ").", throwable);
    }

    private static JSONException recursivelyDefinedObjectException(String string) {
        return new JSONException("JavaBean object contains recursively defined member variable of key " + JSONObject.quote(string));
    }

    private static String removeLeadingZerosOfNumber(String string) {
        int n;
        if (string.equals("-")) {
            return string;
        }
        boolean bl = string.charAt(0) == '-';
        int n2 = n = bl ? 1 : 0;
        while (n < string.length()) {
            if (string.charAt(n) != '0') {
                if (bl) {
                    return "-".concat(string.substring(n));
                }
                return string.substring(n);
            }
            ++n;
        }
        if (bl) {
            return "-0";
        }
        return "0";
    }

    private static final class Null {
        private Null() {
        }

        protected final Object clone() {
            return this;
        }

        public boolean equals(Object object) {
            return object == null || object == this;
        }

        public int hashCode() {
            return 0;
        }

        public String toString() {
            return "null";
        }
    }
}

