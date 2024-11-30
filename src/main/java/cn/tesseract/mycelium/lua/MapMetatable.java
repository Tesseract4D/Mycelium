package cn.tesseract.mycelium.lua;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.Map;

public class MapMetatable extends LuaTable {
    public MapMetatable(final Map<String, Object> map) {
        this.rawset(LuaValue.INDEX, new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key) {
                if (key.isstring())
                    return toLua(map.get(key.tojstring()));
                else
                    return this.rawget(key);
            }
        });
        this.rawset(LuaValue.NEWINDEX, new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
                if (key.isstring()) {
                    final String k = key.tojstring();
                    final Object v = toJava(value);
                    if (v == null)
                        map.remove(k);
                    else
                        map.put(k, v);
                } else {
                    this.rawset(key, value);
                }
                return LuaValue.NONE;
            }
        });
    }

    public static Object toJava(Varargs v) {
        final int n = v.narg();
        switch (n) {
            case 0:
                return null;
            case 1:
                return toJava(v.arg1());
            default:
                Object[] o = new Object[n];
                for (int i = 0; i < n; ++i)
                    o[i] = toJava(v.arg(i + 1));
                return o;
        }
    }


    public static LuaValue toLua(Object javaValue) {
        return javaValue == null ? LuaValue.NIL
                : javaValue instanceof LuaValue ? (LuaValue) javaValue : CoerceJavaToLua.coerce(javaValue);
    }
}
