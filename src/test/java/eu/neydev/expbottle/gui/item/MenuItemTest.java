package eu.neydev.expbottle.gui.item;

import eu.neydev.expbottle.config.type.FillMode;
import eu.neydev.expbottle.gui.action.ActionType;
import eu.neydev.expbottle.gui.condition.Condition;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Разбор предмета меню из YAML.
 *
 * <p>Тесты работают без сервера: {@code YamlConfiguration} и {@code Material}
 * не требуют запущенного Bukkit.</p>
 */
class MenuItemTest {

    private static final Logger LOGGER = Logger.getLogger("MenuItemTest");
    private static final int SIZE = 54;

    private static ConfigurationSection section(String yaml) {

        YamlConfiguration configuration = new YamlConfiguration();

        try {
            configuration.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new IllegalStateException("The test YAML is invalid", exception);
        }

        ConfigurationSection section = configuration.getConfigurationSection("item");

        if (section == null) {
            throw new IllegalStateException("The test YAML has no item section");
        }

        return section;

    }

    @Test
    @DisplayName("The exchange button reads in full")
    void parsesTierItem() {

        MenuItem item = MenuItemParser.parse("tier_5", section("""
                item:
                  type: TIER
                  slot: 21
                  levels: 5
                  material: EXPERIENCE_BOTTLE
                  glow: true
                  name: "&fExchange &e{levels}"
                  lore:
                    - "&7line"
                """), LOGGER, SIZE);

        assertEquals("tier_5", item.getId());
        assertEquals(MenuItemType.TIER, item.getType());
        assertEquals(Material.EXPERIENCE_BOTTLE, item.getMaterial());
        assertEquals(5, item.getLevels());
        assertTrue(item.isGlow());
        assertEquals(FillMode.SLOTS, item.getFillMode());
        assertEquals(1, item.getSlots().size());
        assertEquals(21, item.getSlots().get(0));
        assertTrue(item.isRefresh(), "TIER must refresh by default");

    }

    @Test
    @DisplayName("The slot list supports ranges and removes duplicates")
    void parsesSlotRanges() {

        MenuItem item = MenuItemParser.parse("border", section("""
                item:
                  type: DECORATION
                  material: GRAY_STAINED_GLASS_PANE
                  slots:
                    - "0-2"
                    - 5
                    - 5
                """), LOGGER, SIZE);

        assertEquals(FillMode.SLOTS, item.getFillMode());
        assertEquals(java.util.List.of(0, 1, 2, 5), item.getSlots());

    }

    @Test
    @DisplayName("The all and empty service tokens set the fill mode")
    void parsesFillModes() {

        MenuItem all = MenuItemParser.parse("bg_all", section("""
                item:
                  material: BLACK_STAINED_GLASS_PANE
                  slots: ["all"]
                """), LOGGER, SIZE);

        MenuItem empty = MenuItemParser.parse("bg_empty", section("""
                item:
                  material: BLACK_STAINED_GLASS_PANE
                  slots: ["empty"]
                """), LOGGER, SIZE);

        assertEquals(FillMode.ALL, all.getFillMode());
        assertTrue(all.isFill());
        assertTrue(all.getSlots().isEmpty(), "In ALL mode the slot list is not needed");

        assertEquals(FillMode.EMPTY, empty.getFillMode());
        assertTrue(empty.getSlots().isEmpty());

    }

    @Test
    @DisplayName("Slots outside the menu range are dropped")
    void rejectsOutOfRangeSlots() {

        MenuItem item = MenuItemParser.parse("bad_slot", section("""
                item:
                  type: CUSTOM
                  material: STONE
                  slots: [70, 12, -3]
                """), LOGGER, SIZE);

        assertEquals(java.util.List.of(12), item.getSlots());

    }

    @Test
    @DisplayName("An unknown material falls back without a crash")
    void fallsBackOnUnknownMaterial() {

        MenuItem item = MenuItemParser.parse("weird", section("""
                item:
                  type: DECORATION
                  material: NO_SUCH
                  slot: 0
                """), LOGGER, SIZE);

        assertEquals(Material.STONE, item.getMaterial());

    }

    @Test
    @DisplayName("A broken condition does not break the item load")
    void brokenConditionIsIgnored() {

        MenuItem item = MenuItemParser.parse("cond", section("""
                item:
                  type: CUSTOM
                  material: STONE
                  slot: 3
                  view_requirement: "{player_level} >="
                """), LOGGER, SIZE);

        assertInstanceOf(Condition.AlwaysTrue.class, item.getViewRequirement());

    }

    @Test
    @DisplayName("A working condition parses into a tree")
    void workingConditionIsParsed() {

        MenuItem item = MenuItemParser.parse("cond", section("""
                item:
                  type: TIER
                  material: STONE
                  slot: 3
                  levels: 10
                  click_requirement: "{player_level} >= 10"
                """), LOGGER, SIZE);

        assertFalse(item.getClickRequirement() instanceof Condition.AlwaysTrue);

    }

    @Test
    @DisplayName("Item flags and custom_model_data")
    void parsesItemFlagsAndModelData() {

        MenuItem item = MenuItemParser.parse("flags", section("""
                item:
                  type: CUSTOM
                  material: DIAMOND_SWORD
                  slot: 10
                  custom_model_data: 1001
                  unbreakable: true
                  item_flags:
                    - HIDE_ENCHANTS
                    - HIDE_ATTRIBUTES
                """), LOGGER, SIZE);

        assertEquals(1001, item.getCustomModelData());
        assertTrue(item.isUnbreakable());
        assertEquals(java.util.List.of(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES), item.getItemFlags());

    }

    @Test
    @DisplayName("Click actions are read as a list")
    void parsesClickActions() {

        MenuItem item = MenuItemParser.parse("custom", section("""
                item:
                  type: CUSTOM
                  material: NETHER_STAR
                  slot: 30
                  click:
                    - "[message] &cHello"
                    - "[sound] ENTITY_PLAYER_LEVELUP:1:1.5"
                    - "[close]"
                """), LOGGER, SIZE);

        assertEquals(3, item.getClickActions().size());
        assertEquals(ActionType.MESSAGE, item.getClickActions().get(0).type());
        assertEquals(ActionType.SOUND, item.getClickActions().get(1).type());
        assertEquals(ActionType.CLOSE, item.getClickActions().get(2).type());
        assertEquals("&cHello", item.getClickActions().get(0).argument());

    }

    @Test
    @DisplayName("the default refresh depends on the item type")
    void refreshDefaultDependsOnType() {

        MenuItem info = MenuItemParser.parse("info", section("""
                item:
                  type: INFO
                  material: BOOK
                  slot: 4
                """), LOGGER, SIZE);

        MenuItem decoration = MenuItemParser.parse("deco", section("""
                item:
                  type: DECORATION
                  material: BOOK
                  slot: 4
                """), LOGGER, SIZE);

        MenuItem forced = MenuItemParser.parse("forced", section("""
                item:
                  type: DECORATION
                  material: BOOK
                  slot: 4
                  refresh: true
                """), LOGGER, SIZE);

        assertTrue(info.isRefresh());
        assertFalse(decoration.isRefresh());
        assertTrue(forced.isRefresh());

    }

    @Test
    @DisplayName("Heads: the owner and the texture are read from the config")
    void parsesSkullFields() {

        MenuItem item = MenuItemParser.parse("head", section("""
                item:
                  type: DECORATION
                  material: PLAYER_HEAD
                  slot: 8
                  skull_owner: Ney
                  skull_texture: eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvIn19fQ==
                """), LOGGER, SIZE);

        assertEquals("Ney", item.getSkullOwner());
        assertTrue(item.getSkullTexture().startsWith("eyJ0ZXh0dXJlcyI6"));

    }

    @Test
    @DisplayName("Without a slot the item gets no positions but does not break the load")
    void missingSlotIsSurvivable() {

        MenuItem item = MenuItemParser.parse("no_slot", section("""
                item:
                  type: DECORATION
                  material: STONE
                """), LOGGER, SIZE);

        assertTrue(item.getSlots().isEmpty());
        assertEquals(FillMode.SLOTS, item.getFillMode());

    }
}