package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DisconnectCommand extends BaseCommand {

    public DisconnectCommand(OwnTelecomPlugin plugin) {
        super(plugin, "rozlacz");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }

        var sub = plugin.getDatabaseManager().subscribers().findByPlayer(player.getUniqueId());
        if (sub.isEmpty()) {
            plugin.getMessageService().send(player, "errors.no-operator");
            return true;
        }

        plugin.getDatabaseManager().subscribers().clearOperator(player.getUniqueId());
        plugin.getMessageService().send(player, "operator.disconnected");
        return true;
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return super.tabComplete(sender, args);
    }
}
