package org.btik.espidf.util;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lustre
 * @since 2024/9/9 1:21
 */
public class ClassMetaUtils {
    public static final String GETTER_PREFIX = "get";
    public static final String SETTER_PREFIX = "set";
    public static final String IS_PREFIX = "is";

    public record PropOptMeta(String propName, Field field, Method getter, Method setter) {
    }

    public static List<PropOptMeta> parseFieldsByAnnotation(Class<?> clazz, Class<? extends Annotation> filedFilter) {
        Field[] declaredFields = clazz.getDeclaredFields();
        Map<String, Method> methodMap = Arrays.stream(clazz.getDeclaredMethods())
                .collect(Collectors.toMap(Method::getName, m -> m));
        List<PropOptMeta> propOptMetas = new ArrayList<>();
        for (Field declaredField : declaredFields) {
            if (!declaredField.isAnnotationPresent(filedFilter)) {
                continue;
            }
            String name = declaredField.getName();
            Method getter = methodMap.get(GETTER_PREFIX + name);
            if (getter == null) {
                getter = methodMap.get(IS_PREFIX + name);
            }
            Method setter = methodMap.get(SETTER_PREFIX + name);
            propOptMetas.add(new PropOptMeta(declaredField.getName(), declaredField, getter, setter));
        }
        return propOptMetas;
    }

    public static boolean isMod(Member member, int mask) {
        return (member.getModifiers() & mask) != 0;
    }

    public static Class<?> propType(PropOptMeta propOptMeta) {
        return propOptMeta.field.getType();
    }

    public static <T> T get(@NotNull PropOptMeta propOptMeta, Object object) {
        try {
            return (T) propOptMeta.getter.invoke(object);
        } catch (InvocationTargetException | IllegalAccessException | NullPointerException e) {
            Field field = propOptMeta.field;
            field.setAccessible(true);
            try {
                return (T) field.get(object);
            } catch (IllegalAccessException ex) {
                return null;
            }
        }
    }

    public static void set(@NotNull PropOptMeta propOptMeta, Object object, Object value) {
        try {
            propOptMeta.setter.invoke(object, value);
        } catch (InvocationTargetException | IllegalAccessException | NullPointerException e) {
            Field field = propOptMeta.field;
            field.setAccessible(true);
            try {
                field.set(object, value);
            } catch (IllegalAccessException ignored) {

            }
        }
    }
}
