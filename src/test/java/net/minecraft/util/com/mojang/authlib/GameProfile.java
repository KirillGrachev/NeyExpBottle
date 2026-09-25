package net.minecraft.util.com.mojang.authlib;

import net.minecraft.util.com.mojang.authlib.properties.PropertyMap;

import java.util.UUID;

/**
 * Заглушка authlib-профиля для тестов: конструктор {@code (UUID, String)}
 * и {@code getProperties()}, как у настоящей {@code GameProfile}.
 *
 * <p>Дополнительный конструктор с {@code null}-картой свойств позволяет
 * проверить защитную ветку {@code GameProfileFactory.putTextureProperty}.</p>
 */
public class GameProfile {

    private final UUID id;
    private final String name;
    private final PropertyMap properties;

    public GameProfile(UUID id, String name) {
        this(id, name, new PropertyMap());
    }

    public GameProfile(UUID id, String name, PropertyMap properties) {

        this.id = id;
        this.name = name;
        this.properties = properties;

    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PropertyMap getProperties() {
        return properties;
    }
}
