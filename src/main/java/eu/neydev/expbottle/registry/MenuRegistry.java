package eu.neydev.expbottle.registry;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.util.ValueResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Реестр меню: хранит загруженные {@link MenuDefinition} и ищет их по имени.
 *
 * <p>Один файл — одно меню. Имя файла становится именем меню,
 * поэтому {@code /exp exchange} откроет {@code menus/exchange.yml}.
 * Чтением с диска занимается {@link MenuLoader}; реестр отвечает только
 * за хранение и поиск.</p>
 */
public class MenuRegistry {

    private final Logger logger;
    private final MenuLoader loader;

    private final Map<String, MenuDefinition> menus = new ConcurrentHashMap<>();

    public MenuRegistry(@NotNull NeyExpBottle plugin) {

        this.logger = plugin.getLogger();
        this.loader = new MenuLoader(plugin);

        loader.load(menus);

    }

    /**
     * Перечитывает папку menus/.
     */
    public void reload() {
        loader.load(menus);
    }

    public @NotNull Optional<MenuDefinition> byName(@NotNull String name) {
        return Optional.ofNullable(menus.get(name.toLowerCase(Locale.ROOT)));
    }

    public @NotNull Collection<MenuDefinition> getMenus() {
        return Collections.unmodifiableCollection(menus.values());
    }

    public @NotNull List<String> getNames() {

        List<String> names = new ArrayList<>(menus.keySet());
        Collections.sort(names);
        return names;

    }

    /**
     * Меню по умолчанию: из config.yml, иначе первое по алфавиту.
     *
     * @param defaultName имя из конфига
     * @return определение меню или {@code null}, если меню не загружены
     */
    public @Nullable MenuDefinition getDefault(@Nullable String defaultName) {

        if (!ValueResolver.isBlank(defaultName)) {

            Optional<MenuDefinition> menu = byName(defaultName);

            if (menu.isPresent()) {
                return menu.get();
            }

            logger.warning("Default menu '" + defaultName + "' not found in folder menus/");

        }

        List<String> names = getNames();
        return names.isEmpty() ? null : menus.get(names.get(0));

    }

    public int size() {
        return menus.size();
    }
}