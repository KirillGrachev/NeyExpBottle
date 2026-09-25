package net.minecraft.util.com.mojang.authlib.properties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Заглушка карты свойств authlib: {@code GameProfileFactory.putTextureProperty}
 * кладёт текстуру через {@code put(Object, Object)}, найденный отражением.
 */
public class PropertyMap {

    private final Map<String, Object> entries = new LinkedHashMap<>();

    public void put(Object key, Object value) {
        entries.put(String.valueOf(key), value);
    }

    public Map<String, Object> entries() {
        return entries;
    }
}
