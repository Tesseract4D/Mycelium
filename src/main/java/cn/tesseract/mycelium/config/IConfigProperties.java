package cn.tesseract.mycelium.config;

import java.lang.reflect.Field;
import java.util.HashMap;

public interface IConfigProperties {
    default void load(Field f, Class<?> c, String n, String v) {
        try {
            if (c == byte.class)
                f.set(this, Byte.valueOf(v));
            else if (c == short.class)
                f.set(this, Short.valueOf(v));
            else if (c == int.class)
                f.set(this, Integer.valueOf(v));
            else if (c == long.class)
                f.set(this, Long.valueOf(v));
            else if (c == float.class)
                f.set(this, Float.valueOf(v));
            else if (c == double.class)
                f.set(this, Double.valueOf(v));
            else if (c == boolean.class)
                f.set(this, Boolean.valueOf(v));
            else if (c == char.class)
                f.set(this, v.toCharArray()[0]);
            else if (c == String.class)
                f.set(this, v);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    default void loadProperties(String ct) {
        Field[] fields = this.getClass().getDeclaredFields();
        HashMap<String, Field> map = new HashMap<>();
        for (Field field : fields)
            map.put(field.getName(), field);
        for (String line : ct.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty() && t.charAt(0) != '#') {
                String[] b = t.split("=", -1);
                String k = b[0].trim();
                String v = b[1].trim();
                Field f;
                if ((f = map.get(k)) != null) {
                    this.load(f, f.getType(), f.getName(), v);
                }
            }
        }
    }

    default String toProperties() {
        Field[] fields = this.getClass().getDeclaredFields();
        StringBuilder ct = new StringBuilder();
        try {
            for (Field field : fields) {
                if (ct.length() != 0)
                    ct.append("\n");
                if (field.isAnnotationPresent(Comment.class))
                    ct.append("#").append(field.getAnnotation(Comment.class).value()).append("\n");
                ct.append(field.getName()).append("=").append(field.get(this));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return ct.toString();
    }
}
