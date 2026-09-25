package net.minecraft.nbt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Заглушка NBT-тега для тестов: повторяет сигнатуры NMS {@code CompoundTag},
 * которые перебирает {@code SkullOwnerWriter.writeSkullOwner}
 * ({@code setString(String, String)} и {@code set(String, CompoundTag)}).
 */
public class CompoundTag {

    private final Map<String, Object> values = new LinkedHashMap<>();

    public void setString(String key, String value) {
        values.put(key, value);
    }

    public void set(String key, CompoundTag tag) {
        values.put(key, tag);
    }

    public Map<String, Object> values() {
        return values;
    }
}
