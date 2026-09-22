package eu.neydev.expbottle.registry;

import eu.neydev.expbottle.gui.item.MenuItem;
import eu.neydev.expbottle.gui.item.MenuItemType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Реестр кнопок обмена (предметы типа {@code TIER}) по всем меню.
 *
 * <p>Нужен командам: {@code /neyexpbottle give <игрок> <id>} и tab-complete
 * не зависят от того, в каком меню объявлена кнопка.</p>
 */
public class BottleRegistry {

    private final MenuRegistry menuRegistry;
    private final Logger logger;

    private final List<BottleTier> tiers = new ArrayList<>();
    private final Map<String, BottleTier> tiersById = new ConcurrentHashMap<>();

    public BottleRegistry(@NotNull MenuRegistry menuRegistry, @NotNull Logger logger) {

        this.menuRegistry = menuRegistry;
        this.logger = logger;

        initializeTiers();

    }

    private void initializeTiers() {

        for (MenuDefinition definition : menuRegistry.getMenus()) {

            for (MenuItem item : definition.items()) {

                if (item.getType() != MenuItemType.TIER) {
                    continue;
                }

                register(definition.name(), item);

            }

        }

    }

    private void register(@NotNull String menuName, @NotNull MenuItem item) {

        if (item.getLevels() <= 0) {
            logger.warning("Button '" + item.getId() + "' in menu " + menuName
                    + ": levels must be greater than zero - skipping");
            return;
        }

        BottleTier tier = new BottleTier(item.getId(), item.getLevels(), menuName);

        if (tiersById.putIfAbsent(tier.id(), tier) != null) {

            BottleTier previous = tiersById.get(tier.id());
            logger.warning("Exchange button id '" + tier.id() + "' is already used in menu "
                    + (previous == null ? "?" : previous.menu()) + " - duplicate in " + menuName + " skipping");
            return;

        }

        tiers.add(tier);

    }

    /**
     * Перечитывает кнопки обмена после перезагрузки меню.
     */
    public void reload() {
        clearTiers();
        initializeTiers();
    }

    public void clearTiers() {
        tiers.clear();
        tiersById.clear();
    }

    public @NotNull List<BottleTier> getTiers() {
        return Collections.unmodifiableList(tiers);
    }

    public @NotNull Optional<BottleTier> byId(@NotNull String id) {
        return Optional.ofNullable(tiersById.get(id.toLowerCase(Locale.ROOT)));
    }

    public @NotNull List<String> getIds() {

        List<String> ids = new ArrayList<>(tiersById.keySet());
        Collections.sort(ids);
        return ids;

    }

    public int size() {
        return tiers.size();
    }
}
