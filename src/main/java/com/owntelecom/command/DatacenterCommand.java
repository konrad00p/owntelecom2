package com.owntelecom.command;

import com.owntelecom.OwnTelecomPlugin;
import com.owntelecom.database.model.Datacenter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DatacenterCommand extends BaseCommand {

    public DatacenterCommand(OwnTelecomPlugin plugin) {
        super(plugin, "serwerownia");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("/serwerownia <utworz|ulepsz|napraw|usun|info|lista>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "utworz" -> handleCreate(sender);
            case "ulepsz" -> handleUpgrade(sender, args);
            case "napraw" -> handleRepair(sender, args);
            case "usun" -> handleDelete(sender, args);
            case "info" -> handleInfo(sender, args);
            case "lista" -> handleList(sender);
            default -> sender.sendMessage("Nieznana podkomenda.");
        }
        return true;
    }

    private void handleCreate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }

        Location loc = player.getTargetBlockExact(5).getLocation();
        if (loc == null) {
            player.sendMessage("Musisz patrzeć na blok.");
            return;
        }

        String requiredBlock = plugin.getConfigManager().getConfig().getString("datacenter.required-block", "IRON_BLOCK");
        Material requiredMat = Material.matchMaterial(requiredBlock);
        if (requiredMat == null || player.getTargetBlockExact(5).getType() != requiredMat) {
            player.sendMessage("Musisz patrzeć na blok: " + requiredBlock);
            return;
        }

        Optional<Datacenter> existing = plugin.getDatabaseManager().datacenters()
                .findAtLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (existing.isPresent()) {
            player.sendMessage("W tej lokalizacji już istnieje serwerownia.");
            return;
        }

        double cost = plugin.getConfigManager().getConfig().getDouble("datacenter.create-cost", 5000);
        if (cost > 0 && !plugin.getEconomyService().withdraw(player, cost)) {
            plugin.getMessageService().send(player, "errors.insufficient-funds", 
                    Map.of("cost", String.valueOf(cost), "currency", "$"));
            return;
        }

        Datacenter datacenter = new Datacenter(
                0,
                player.getUniqueId(),
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ(),
                1,
                false
        );

        int id = plugin.getDatabaseManager().datacenters().create(datacenter);
        player.sendMessage("Serwerownia #" + id + " utworzona. Koszt: " + cost + "$");
    }

    private void handleUpgrade(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }

        Location loc = player.getTargetBlockExact(5).getLocation();
        if (loc == null) {
            player.sendMessage("Musisz patrzeć na blok serwerowni.");
            return;
        }

        Optional<Datacenter> opt = plugin.getDatabaseManager().datacenters()
                .findAtLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (opt.isEmpty()) {
            player.sendMessage("Nie znaleziono serwerowni w tej lokalizacji.");
            return;
        }

        Datacenter dc = opt.get();
        if (!dc.ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        int maxLevel = plugin.getConfigManager().getConfig().getInt("datacenter.max-level", 3);
        if (dc.level() >= maxLevel) {
            player.sendMessage("Serwerownia jest już na maksymalnym poziomie.");
            return;
        }

        double cost = plugin.getConfigManager().getConfig().getDouble("datacenter.upgrade-cost", 2000);
        if (cost > 0 && !plugin.getEconomyService().withdraw(player, cost)) {
            plugin.getMessageService().send(player, "errors.insufficient-funds",
                    Map.of("cost", String.valueOf(cost), "currency", "$"));
            return;
        }

        plugin.getDatabaseManager().datacenters().updateLevel(dc.id(), dc.level() + 1);
        player.sendMessage("Serwerownia ulepszona do poziomu " + (dc.level() + 1) + ". Koszt: " + cost + "$");
    }

    private void handleRepair(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }

        Location loc = player.getTargetBlockExact(5).getLocation();
        if (loc == null) {
            player.sendMessage("Musisz patrzeć na blok serwerowni.");
            return;
        }

        Optional<Datacenter> opt = plugin.getDatabaseManager().datacenters()
                .findAtLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (opt.isEmpty()) {
            player.sendMessage("Nie znaleziono serwerowni w tej lokalizacji.");
            return;
        }

        Datacenter dc = opt.get();
        if (!dc.ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        if (!dc.broken()) {
            player.sendMessage("Serwerownia nie jest uszkodzona.");
            return;
        }

        double cost = plugin.getConfigManager().getConfig().getDouble("datacenter.repair-cost", 500);
        if (cost > 0 && !plugin.getEconomyService().withdraw(player, cost)) {
            plugin.getMessageService().send(player, "errors.insufficient-funds",
                    Map.of("cost", String.valueOf(cost), "currency", "$"));
            return;
        }

        plugin.getDatabaseManager().datacenters().setBroken(dc.id(), false);
        player.sendMessage("Serwerownia naprawiona. Koszt: " + cost + "$");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }

        Location loc = player.getTargetBlockExact(5).getLocation();
        if (loc == null) {
            player.sendMessage("Musisz patrzeć na blok serwerowni.");
            return;
        }

        Optional<Datacenter> opt = plugin.getDatabaseManager().datacenters()
                .findAtLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (opt.isEmpty()) {
            player.sendMessage("Nie znaleziono serwerowni w tej lokalizacji.");
            return;
        }

        Datacenter dc = opt.get();
        if (!dc.ownerUuid().equals(player.getUniqueId()) && !player.hasPermission("owntelecom.admin")) {
            plugin.getMessageService().send(player, "operator.not-owner");
            return;
        }

        plugin.getDatabaseManager().datacenters().delete(dc.id());
        player.sendMessage("Serwerownia usunięta.");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }

        Location loc = player.getTargetBlockExact(5).getLocation();
        if (loc == null) {
            player.sendMessage("Musisz patrzeć na blok serwerowni.");
            return;
        }

        Optional<Datacenter> opt = plugin.getDatabaseManager().datacenters()
                .findAtLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (opt.isEmpty()) {
            player.sendMessage("Nie znaleziono serwerowni w tej lokalizacji.");
            return;
        }

        Datacenter dc = opt.get();
        player.sendMessage("=== Serwerownia #" + dc.id() + " ===");
        player.sendMessage("Właściciel: " + plugin.getServer().getOfflinePlayer(dc.ownerUuid()).getName());
        player.sendMessage("Poziom: " + dc.level());
        player.sendMessage("Status: " + (dc.broken() ? "&cUSZKODZONA" : "&aAKTYWNA"));
        player.sendMessage("Lokalizacja: " + dc.world() + " " + dc.x() + "," + dc.y() + "," + dc.z());
    }

    private void handleList(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "errors.player-only");
            return;
        }

        List<Datacenter> list = plugin.getDatabaseManager().datacenters().findByOwner(player.getUniqueId());
        player.sendMessage("=== Twoje serwerownie (" + list.size() + ") ===");
        for (Datacenter dc : list) {
            player.sendMessage("#" + dc.id() + " - Poziom " + dc.level() + 
                    (dc.broken() ? " &c[USZKODZONA]" : " &a[AKTYWNA]"));
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("utworz", "ulepsz", "napraw", "usun", "info", "lista"), args[0]);
        }
        return super.tabComplete(sender, args);
    }
}
