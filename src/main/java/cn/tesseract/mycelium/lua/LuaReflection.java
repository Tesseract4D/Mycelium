package cn.tesseract.mycelium.lua;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LuaReflection {
    public Method getDeclaredMethod(Class<?> clazz, String name) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(name);
    }

    public Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... types) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(name, types);
    }

    public Method[] getDeclaredMethods(Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    public Field getDeclaredField(Class<?> clazz, String name) throws NoSuchFieldException {
        return clazz.getDeclaredField(name);
    }

    public Field[] getDeclaredField(Class<?> clazz) {
        return clazz.getDeclaredFields();
    }

    public Method getMethod(Class<?> clazz, String name) throws NoSuchMethodException {
        return clazz.getMethod(name);
    }

    public Method getMethod(Class<?> clazz, String name, Class<?>... types) throws NoSuchMethodException {
        return clazz.getMethod(name, types);
    }

    public Method[] getMethods(Class<?> clazz) {
        return clazz.getMethods();
    }

    public Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
        return clazz.getField(name);
    }

    public Field[] getField(Class<?> clazz) {
        return clazz.getFields();
    }
}
