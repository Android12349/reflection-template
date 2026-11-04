package org.example.generator;

import org.example.classes.Rectangle;
import org.example.classes.Triangle;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Generator {
    private final Random random;
    private final List<Class<?>> implementations;

    public Generator() {
        this(new Random());
    }

    public Generator(Random random, Class<?>... additionalImplementations) {
        if (random == null || additionalImplementations == null) {
            throw new IllegalArgumentException("Random and implementation list must not be null");
        }
        this.random = random;
        implementations = new ArrayList<>(List.of(Rectangle.class, Triangle.class));
        for (Class<?> implementation : additionalImplementations) {
            if (implementation == null || !implementation.isAnnotationPresent(Generatable.class)
                    || implementation.isInterface() || Modifier.isAbstract(implementation.getModifiers())) {
                throw new IllegalArgumentException("Implementation must be a concrete @Generatable class: " + implementation);
            }
            implementations.add(implementation);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T generateValueOfType(Class<T> type) {
        if (type == null) {
            throw new GenerationException("Requested type must not be null");
        }
        return (T) generate(type, new ArrayList<>());
    }

    private Object generate(Type type, List<Class<?>> ancestors) {
        if (type instanceof ParameterizedType parameterized) {
            return generateParameterized(parameterized, ancestors);
        }
        if (type instanceof GenericArrayType arrayType) {
            if (arrayType.getGenericComponentType() instanceof Class<?> component) {
                return generateArray(component, ancestors);
            }
            throw new GenerationException("Unsupported array component: " + arrayType);
        }
        if (!(type instanceof Class<?> clazz)) {
            throw new GenerationException("Unsupported type: " + type);
        }
        if (clazz == String.class) return randomString();
        Object simple = generateSimple(clazz);
        if (simple != null) return simple;
        if (clazz.isEnum()) {
            Object[] constants = clazz.getEnumConstants();
            if (constants.length == 0) throw new GenerationException("Enum has no constants: " + clazz.getName());
            return constants[random.nextInt(constants.length)];
        }
        if (clazz.isArray()) return generateArray(clazz.getComponentType(), ancestors);
        if (List.class.isAssignableFrom(clazz)) return generateList(clazz, String.class, ancestors);
        if (Map.class.isAssignableFrom(clazz)) return generateMap(clazz, String.class, Integer.class, ancestors);
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return generate(resolveImplementation(clazz), ancestors);
        }
        if (clazz == Object.class || clazz.isPrimitive()) {
            throw new GenerationException("Unsupported type: " + clazz.getName());
        }
        if (ancestors.contains(clazz)) return null;
        return generateObject(clazz, ancestors);
    }

    private Object generateParameterized(ParameterizedType type, List<Class<?>> ancestors) {
        if (!(type.getRawType() instanceof Class<?> raw)) {
            throw new GenerationException("Unsupported parameterized type: " + type);
        }
        Type[] arguments = type.getActualTypeArguments();
        if (List.class.isAssignableFrom(raw) && arguments.length == 1) {
            return generateList(raw, arguments[0], ancestors);
        }
        if (Map.class.isAssignableFrom(raw) && arguments.length == 2) {
            return generateMap(raw, arguments[0], arguments[1], ancestors);
        }
        throw new GenerationException("Unsupported parameterized type: " + type);
    }

    private Object generateSimple(Class<?> type) {
        if (type == int.class || type == Integer.class) return random.nextInt(1, 101);
        if (type == long.class || type == Long.class) return random.nextLong(1, 101);
        if (type == short.class || type == Short.class) return (short) random.nextInt(1, 101);
        if (type == byte.class || type == Byte.class) return (byte) random.nextInt(1, 101);
        if (type == double.class || type == Double.class) return 1.0 + random.nextDouble() * 99.0;
        if (type == float.class || type == Float.class) return 1.0f + random.nextFloat() * 99.0f;
        if (type == boolean.class || type == Boolean.class) return random.nextBoolean();
        if (type == char.class || type == Character.class) return (char) ('a' + random.nextInt(26));
        return null;
    }

    private String randomString() {
        int length = random.nextInt(5, 13);
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append((char) ('a' + random.nextInt(26)));
        }
        return result.toString();
    }

    private Object generateArray(Class<?> component, List<Class<?>> ancestors) {
        Object array = Array.newInstance(component, random.nextInt(1, 4));
        for (int i = 0; i < Array.getLength(array); i++) {
            Array.set(array, i, generate(component, ancestors));
        }
        return array;
    }

    private Object generateList(Class<?> requested, Type element, List<Class<?>> ancestors) {
        if (!requested.isAssignableFrom(ArrayList.class)) {
            throw new GenerationException("Unsupported List implementation: " + requested.getName());
        }
        List<Object> result = new ArrayList<>();
        int size = random.nextInt(1, 4);
        for (int i = 0; i < size; i++) result.add(generate(element, ancestors));
        return result;
    }

    private Object generateMap(Class<?> requested, Type key, Type value, List<Class<?>> ancestors) {
        if (!requested.isAssignableFrom(HashMap.class)) {
            throw new GenerationException("Unsupported Map implementation: " + requested.getName());
        }
        Map<Object, Object> result = new HashMap<>();
        int size = random.nextInt(1, 4);
        for (int i = 0; i < size; i++) result.put(generate(key, ancestors), generate(value, ancestors));
        return result;
    }

    private Class<?> resolveImplementation(Class<?> requested) {
        if (!requested.isAnnotationPresent(Generatable.class)) {
            throw new GenerationException("Abstract type requires @Generatable: " + requested.getName());
        }
        List<Class<?>> candidates = implementations.stream()
                .filter(requested::isAssignableFrom)
                .filter(candidate -> candidate.isAnnotationPresent(Generatable.class))
                .toList();
        if (candidates.isEmpty()) {
            throw new GenerationException("No registered @Generatable implementation for " + requested.getName());
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private Object generateObject(Class<?> clazz, List<Class<?>> ancestors) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Arrays.sort(constructors, Comparator.comparingInt((Constructor<?> c) -> c.getParameterCount()).reversed());
        if (constructors.length == 0) throw new GenerationException("No constructor for " + clazz.getName());
        ancestors.add(clazz);
        GenerationException failure = null;
        try {
            for (Constructor<?> constructor : constructors) {
                try {
                    Type[] parameterTypes = constructor.getGenericParameterTypes();
                    Object[] arguments = new Object[parameterTypes.length];
                    for (int i = 0; i < arguments.length; i++) {
                        arguments[i] = generate(parameterTypes[i], ancestors);
                    }
                    constructor.setAccessible(true);
                    return constructor.newInstance(arguments);
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    failure = new GenerationException("Cannot use constructor " + constructor + ": " + ex.getMessage(), ex);
                }
            }
        } finally {
            ancestors.remove(ancestors.size() - 1);
        }
        throw new GenerationException("Cannot generate " + clazz.getName(), failure);
    }
}
