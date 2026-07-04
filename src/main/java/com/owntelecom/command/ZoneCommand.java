package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Zone;
import com.owntelecom.database.model.ZoneType;
import com.owntelecom.util.IdUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ZoneCommand extends BaseCommand {

    public ZoneCommand(OwnTelecomPlugin plugin) {
        super(plugin, "strefa");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("/strefa <utworz|usun|dodaj|usunoperator|lista|info>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "utworz" -> handleCreate(sender, args);
            case "usun" -> handleDelete(sender, args);
            case "dodaj" -> handleAddMember(sender, args);
            case "usunoperator" -> handleRemoveMember(sender, args);
            case "lista" -> handleList(sender, args);
            case "info" -> handleInfo(sender, args);
            default -> sender.sendMessage("Nieznana podkomenda.");
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }
        if (args.length < 4) {
            player.sendMessage("/strefa utworz <roaming|polaczenia> <Nazwa> <id>");
            player.sendMessage("Przykład: /strefa utworz roaming Strefa Home phonifyhome");
            return;
        }

        ZoneType type = args[1].equalsIgnoreCase("roaming") ? ZoneType.ROAMING : ZoneType.CALLS;
        String displayName = args[2];
        String id = args[3];

        String normalizedId = IdUtil.normalizeId(id);
        if (!IdUtil.isValidId(normalizedId)) {
            player.sendMessage("Nieprawidłowe ID. Użyj tylko małych liter i cyfr (3-16 znaków).");
            return;
        }

        var owned = plugin.getDatabaseManager().operators().findByOwner(player.getUniqueId());
        if (owned.isEmpty()) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        if (plugin.getDatabaseManager().zones().findById(normalizedId).isPresent()) {
            player.sendMessage("Strefa o tym ID już istnieje.");
            return;
        }

        Zone zone = new Zone(
                normalizedId,
                owned.get(0).id(),
                type,
                displayName,
                0, 0, 0
        );
        plugin.getDatabaseManager().zones().create(zone);
        player.sendMessage("Strefa '" + displayName + "' (" + normalizedId + ") utworzona.");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("/strefa usun <id>");
            return;
        }

        var owned = plugin.getDatabaseManager().operators().findByOwner(player.getUniqueId());
        if (owned.isEmpty()) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        String id = args[1];
        var zone = plugin.getDatabaseManager().zones().findById(id);
        if (zone.isEmpty()) {
            player.sendMessage("Strefa nie istnieje.");
            return;
        }

        if (!zone.get().operatorId().equals(owned.get(0).id()) && !player.hasPermission("owntelecom.admin")) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        plugin.getDatabaseManager().zones().delete(id);
        player.sendMessage("Strefa usunięta.");
    }

    private void handleAddMember(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }
        if (args.length < 3) {
            player.sendMessage("/strefa dodaj <id_strefy> <operator>");
            return;
        }

        var owned = plugin.getDatabaseManager().operators().findByOwner(player.getUniqueId());
        if (owned.isEmpty()) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        String zoneId = args[1];
        String targetOperator = args[2];

        var zone = plugin.getDatabaseManager().zones().findById(zoneId);
        if (zone.isEmpty()) {
            player.sendMessage("Strefa nie istnieje.");
            return;
        }

        if (!zone.get().operatorId().equals(owned.get(0).id()) && !player.hasPermission("owntelecom.admin")) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        var targetOp = plugin.getDatabaseManager().operators().findById(targetOperator);
        if (targetOp.isEmpty()) {
            player.sendMessage("Operator docelowy nie istnieje.");
            return;
        }

        plugin.getDatabaseManager().zones().addMember(zoneId, targetOperator);
        player.sendMessage("Operator " + targetOperator + " dodany do strefy.");
    }

    private void handleRemoveMember(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }
        if (args.length < 3) {
            player.sendMessage("/strefa usunoperator <id_strefy> <operator>");
            return;
        }

        var owned = plugin.getDatabaseManager().operators().findByOwner(player.getUniqueId());
        if (owned.isEmpty()) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        String zoneId = args[1];
        String targetOperator = args[2];

        var zone = plugin.getDatabaseManager().zones().findById(zoneId);
        if (zone.isEmpty()) {
            player.sendMessage("Strefa nie istnieje.");
            return;
        }

        if (!zone.get().operatorId().equals(owned.get(0).id()) && !player.hasPermission("owntelecom.admin")) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        plugin.getDatabaseManager().zones().removeMember(zoneId, targetOperator);
        player.sendMessage("Operator " + targetOperator + " usunięty ze strefy.");
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }

        var owned = plugin.getDatabaseManager().operators().findByOwner(player.getUniqueId());
        if (owned.isEmpty()) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        ZoneType type = args.length >= 2 && args[1].equalsIgnoreCase("polaczenia") ? ZoneType.CALLS : ZoneType.ROAMING;
        List<Zone> zones = plugin.getDatabaseManager().zones().findByOperatorAndType(owned.get(0).id(), type);

        player.sendMessage("=== Strefy " + type.name() + " ===");
        for (Zone z : zones) {
            List<String> members = plugin.getDatabaseManager().zones().getZoneMembers(z.id());
            player.sendMessage(z.displayName() + " (" + z.id() + ") - " + members.size() + " operatorów");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/strefa info <id>");
            return;
        }

        String id = args[1];
        var zone = plugin.getDatabaseManager().zones().findById(id);
        if (zone.isEmpty()) {
            sender.sendMessage("Strefa nie istnieje.");
            return;
        }

        Zone z = zone.get();
        List<String> members = plugin.getDatabaseManager().zones().getZoneMembers(z.id());

        sender.sendMessage("=== Strefa: " + z.displayName() + " ===");
        sender.sendMessage("ID: " + z.id());
        sender.sendMessage("Typ: " + z.type().name());
        sender.sendMessage("Dodatkowe stawki: " + z.extraMinute() + "/min, " + z.extraSms() + "/SMS, " + z.extraMb() + "/MB");
        sender.sendMessage("Operatorzy w strefie (" + members.size() + "): " + String.join(", ", members));
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("utworz", "usun", "dodaj", "usunoperator", "lista", "info"), args[0]);
        }
        return super.tabComplete(sender, args);
    }
}
