package com.destroystokyo.paper.profile;

/**
 * Заглушка профиля Paper для тестов {@code PaperProfileStrategy}.
 */
public class PlayerProfile {

    private PlayerProfileTextures textures;

    public void setTextures(PlayerProfileTextures textures) {
        this.textures = textures;
    }

    public PlayerProfileTextures getTextures() {
        return textures;
    }
}
