package mc.mkay.oceanicdaggers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DaggerCommand implements CommandExecutor {

    private final OceanicDaggers plugin;

    public DaggerCommand(OceanicDaggers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;

        if (args.length > 0) {
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Specify a player.", NamedTextColor.RED));
            return true;
        }

        if (command.getName().equalsIgnoreCase("givedaggers")) {
            target.getInventory().addItem(DaggerItems.makeMainhand());
            target.sendMessage(Component.text("≋ You received the Oceanic Dagger (mainhand)!", NamedTextColor.AQUA));
        } else if (command.getName().equalsIgnoreCase("givedaggeroff")) {
            target.getInventory().addItem(DaggerItems.makeOffhand());
            target.sendMessage(Component.text("≋ You received the Oceanic Dagger (offhand)!", NamedTextColor.AQUA));
        }

        return true;
    }
}
