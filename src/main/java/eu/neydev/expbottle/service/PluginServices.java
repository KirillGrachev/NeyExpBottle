package eu.neydev.expbottle.service;

import eu.neydev.expbottle.NeyExpBottle;
import eu.neydev.expbottle.config.ConfigManager;
import eu.neydev.expbottle.gui.MenuItemFactory;
import eu.neydev.expbottle.gui.MenuRenderer;
import eu.neydev.expbottle.registry.BottleRegistry;
import eu.neydev.expbottle.registry.MenuRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Точка сборки зависимостей (composition root).
 *
 * <p>Все сервисы создаются один раз при включении плагина и передаются друг другу
 * через конструкторы. Статического {@code getInstance()} здесь нет намеренно:
 * скрытое глобальное состояние — главная причина, по которой старый код
 * было невозможно тестировать.</p>
 *
 * <p>Порядок создания важен: {@link ActionExecutor} и {@link MenuService} получают
 * ссылку на весь контекст, потому что действия меню обращаются к открытию меню,
 * а открытие меню выполняет действия. Жёсткие ссылки образовали бы цикл.</p>
 */
public class PluginServices {

    private final NeyExpBottle plugin;

    private final ConfigManager configManager;
    private final MenuRegistry menuRegistry;
    private final BottleRegistry bottleRegistry;

    private final DiagnosticsService diagnosticsService;
    private final MessageService messageService;
    private final PermissionService permissionService;
    private final SoundService soundService;
    private final InventoryService inventoryService;
    private final ExperienceService experienceService;
    private final CooldownService cooldownService;
    private final AmountSelectionService amountSelectionService;
    private final PlaceholderService placeholderService;
    private final SkullTextureService skullTextureService;
    private final BottleTagService bottleTagService;
    private final BottleFactory bottleFactory;
    private final ExchangeService exchangeService;
    private final MenuItemFactory menuItemFactory;
    private final MenuRenderer menuRenderer;
    private final ActionExecutor actionExecutor;
    private final MenuService menuService;

    public PluginServices(@NotNull NeyExpBottle plugin) {

        this.plugin = plugin;

        this.configManager = new ConfigManager(plugin);
        this.menuRegistry = new MenuRegistry(plugin);
        this.bottleRegistry = new BottleRegistry(menuRegistry, plugin.getLogger());

        this.diagnosticsService = new DiagnosticsService(plugin.getLogger(), configManager);
        this.messageService = new MessageService(configManager);
        this.permissionService = new PermissionService(configManager);
        this.soundService = new SoundService(diagnosticsService);
        this.inventoryService = new InventoryService(diagnosticsService);
        this.experienceService = new ExperienceService();
        this.cooldownService = new CooldownService(configManager, permissionService);
        this.amountSelectionService = new AmountSelectionService(configManager, diagnosticsService);
        this.skullTextureService = new SkullTextureService(plugin.getLogger());
        this.placeholderService = new PlaceholderService(
                configManager, experienceService, diagnosticsService, amountSelectionService,
                cooldownService, menuRegistry);
        this.bottleTagService = new BottleTagService(plugin);
        this.bottleFactory = new BottleFactory(configManager, bottleTagService, placeholderService);
        this.exchangeService = new ExchangeService(
                configManager, bottleFactory, experienceService, inventoryService,
                permissionService, cooldownService, diagnosticsService);

        this.menuItemFactory = new MenuItemFactory(configManager, placeholderService, exchangeService,
                amountSelectionService, skullTextureService);
        this.menuRenderer = new MenuRenderer(placeholderService, exchangeService, amountSelectionService,
                menuItemFactory);
        this.actionExecutor = new ActionExecutor(configManager, messageService, soundService, exchangeService,
                placeholderService, bottleRegistry, diagnosticsService, cooldownService);
        this.menuService = new MenuService(plugin, configManager, menuRegistry, messageService, permissionService,
                placeholderService, actionExecutor, diagnosticsService, menuRenderer);
        this.actionExecutor.bindMenuNavigation(menuService);

    }

    /**
     * Перезагружает конфигурацию, меню и реестр кнопок обмена.
     *
     * @return время перезагрузки в миллисекундах
     */
    public long reload() {

        return diagnosticsService.measure(() -> {

            configManager.reload();
            menuRegistry.reload();
            bottleRegistry.reload();

            cooldownService.purgeExpired();

            diagnosticsService.debug("Reload finished: menus " + menuRegistry.size()
                    + ", exchange buttons " + bottleRegistry.size());

        });

    }

    /**
     * Освобождает состояние при выключении плагина.
     */
    public void shutdown() {
        cooldownService.clear();
    }

    public @NotNull NeyExpBottle getPlugin() {
        return plugin;
    }

    public @NotNull ConfigManager getConfigManager() {
        return configManager;
    }

    public @NotNull MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    public @NotNull BottleRegistry getBottleRegistry() {
        return bottleRegistry;
    }

    public @NotNull DiagnosticsService getDiagnosticsService() {
        return diagnosticsService;
    }

    public @NotNull MessageService getMessageService() {
        return messageService;
    }

    public @NotNull PermissionService getPermissionService() {
        return permissionService;
    }

    public @NotNull SoundService getSoundService() {
        return soundService;
    }

    public @NotNull InventoryService getInventoryService() {
        return inventoryService;
    }

    public @NotNull ExperienceService getExperienceService() {
        return experienceService;
    }

    public @NotNull CooldownService getCooldownService() {
        return cooldownService;
    }

    public @NotNull AmountSelectionService getAmountSelectionService() {
        return amountSelectionService;
    }

    public @NotNull SkullTextureService getSkullTextureService() {
        return skullTextureService;
    }

    public @NotNull PlaceholderService getPlaceholderService() {
        return placeholderService;
    }

    public @NotNull BottleTagService getBottleTagService() {
        return bottleTagService;
    }

    public @NotNull BottleFactory getBottleFactory() {
        return bottleFactory;
    }

    public @NotNull ExchangeService getExchangeService() {
        return exchangeService;
    }

    public @NotNull MenuItemFactory getMenuItemFactory() {
        return menuItemFactory;
    }

    public @NotNull MenuRenderer getMenuRenderer() {
        return menuRenderer;
    }

    public @NotNull ActionExecutor getActionExecutor() {
        return actionExecutor;
    }

    public @NotNull MenuService getMenuService() {
        return menuService;
    }
}