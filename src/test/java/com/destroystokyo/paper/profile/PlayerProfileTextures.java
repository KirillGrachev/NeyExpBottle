package com.destroystokyo.paper.profile;

/**
 * Заглушка текстур профиля Paper: {@code PaperProfileStrategy} доходит до
 * {@code Bukkit.createProfile}, которого в spigot-api 1.16.5 нет, — но сами
 * классы Paper отражение находит уже здесь.
 */
public class PlayerProfileTextures {

    private String skin;

    public PlayerProfileTextures() {
    }

    public void setSkin(String url) {
        this.skin = url;
    }

    public String getSkin() {
        return skin;
    }
}
