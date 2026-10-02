package eu.neydev.expbottle.config;

import eu.neydev.expbottle.config.type.AmountSettings;
import eu.neydev.expbottle.config.type.MessageKey;
import eu.neydev.expbottle.config.type.SoundSettings;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Контракт config.yml: сервисы зависят от интерфейса, а не от Bukkit-конфига.
 *
 * <p>Все значения кешируются при загрузке, поэтому чтение конфига не происходит
 * на каждый клик по меню.</p>
 */
public interface PluginConfig {

    boolean isEnabled();

    boolean isDebugEnabled();

    @NotNull String getDefaultMenu();

    @NotNull String getAvailableText();

    @NotNull String getUnavailableText();

    @NotNull String getUnavailableBottlesText();

    boolean arePermissionsEnabled();

    @NotNull String getPermissionUse();

    @NotNull String getPermissionExchange();

    @NotNull String getPermissionAdmin();

    @NotNull String getPermissionBypassCooldown();

    @NotNull Material getBottleMaterial();

    @NotNull String getBottleName();

    @NotNull List<String> getBottleLore();

    boolean isBottleGlowEnabled();

    boolean isInstructionEnabled();

    @NotNull String getInstructionText();

    int getMaxBottleLevels();

    boolean isOpBypassEnabled();

    boolean isSafeMode();

    double getPickupRadius();

    @NotNull SoundSettings getBreakSound();

    @NotNull AmountSettings getAmount();

    boolean areEmptyBottlesRequired();

    @NotNull Material getEmptyBottleMaterial();

    @NotNull SoundSettings getExchangeSound();

    @NotNull SoundSettings getFailSound();

    boolean isCooldownEnabled();

    long getCooldownMillis();

    boolean isMessageEnabled(@NotNull MessageKey key);

    @NotNull List<String> getMessage(@NotNull MessageKey key);
}