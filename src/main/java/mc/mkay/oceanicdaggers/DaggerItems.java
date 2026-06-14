package mc.mkay.oceanicdaggers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class DaggerItems {

    public static final NamespacedKey DAGGER_KEY = new NamespacedKey("oceanicdaggers", "is_dagger");
    public static final NamespacedKey DAGGER_HAND_KEY = new NamespacedKey("oceanicdaggers", "dagger_hand");
    // Custom model data values — match these in your CIT resource pack
    public static final int MAINHAND_CMD = 1001;
    public static final int OFFHAND_CMD  = 1002;

    public static ItemStack makeMainhand() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Oceanic Dagger", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(List.of(
            Component.text("≋ Dual-Wield Oceanic Daggers ≋", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Right Click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" ▸ Dagger Dash", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)),
            Component.text("  Cross daggers & lunge through your target", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("  Deals ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("✦ 2 Hearts", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .append(Component.text("  (40s cooldown)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.empty(),
            Component.text("Shift + Right Click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" ▸ Oceanic Tide", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)),
            Component.text("  Ride the tide, then smash down like a mace", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("  Deals ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("✦ Smash damage on landing", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false))
                .append(Component.text("  (60s cooldown)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
        ));

        // Match netherite sword attack damage (8) but label it mid
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
            new AttributeModifier(new NamespacedKey("oceanicdaggers", "damage_main"),
                7.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.addAttributeModifier(Attribute.ATTACK_SPEED,
            new AttributeModifier(new NamespacedKey("oceanicdaggers", "speed_main"),
                -2.4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));

        meta.addEnchant(Enchantment.UNBREAKING, 10, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);
        meta.setCustomModelData(MAINHAND_CMD);

        meta.getPersistentDataContainer().set(DAGGER_KEY, PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(DAGGER_HAND_KEY, PersistentDataType.STRING, "main");

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack makeOffhand() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Oceanic Dagger (Offhand)", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(List.of(
            Component.text("≋ Dual-Wield Oceanic Daggers ≋", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Hold in offhand alongside main dagger", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("to unlock Dagger Dash & Oceanic Tide", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));

        meta.addEnchant(Enchantment.UNBREAKING, 10, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);
        meta.setCustomModelData(OFFHAND_CMD);

        meta.getPersistentDataContainer().set(DAGGER_KEY, PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(DAGGER_HAND_KEY, PersistentDataType.STRING, "off");

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isDagger(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(DAGGER_KEY, PersistentDataType.BOOLEAN);
    }

    public static boolean isDualWielding(org.bukkit.entity.Player player) {
        return isDagger(player.getInventory().getItemInMainHand())
            && isDagger(player.getInventory().getItemInOffHand());
    }
}
