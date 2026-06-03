package dev.aero.tpa.gui;

import dev.aero.tpa.AeroTPA;
import dev.aero.tpa.model.TPARequest;
import dev.aero.tpa.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

// Builds and manages GUI menus
public class GUIManager {

    public static final String GUI_ID_PREFIX = "§8§lAeroTPA§r ";

    private final AeroTPA plugin;

    // Track GUI state for click handling
    // Player UUID → the target player name they're confirming a request to
    private final Map<UUID, String> confirmTargets = new HashMap<>();
    private final Map<UUID, TPARequest.Type> confirmTypes = new HashMap<>();

    // Dashboard page state
    private final Map<UUID, Integer> dashboardPages = new HashMap<>();

    public GUIManager(AeroTPA plugin) {
        this.plugin = plugin;
    }

    // -- Confirm GUI --

    // Opens confirmation GUI
    public void openConfirmGUI(Player sender, Player target, TPARequest.Type type) {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("gui.confirm");
        if (cfg == null) return;

        int rows = cfg.getInt("rows", 3);
        String title = GUI_ID_PREFIX + MessageUtil.colorize(
                MessageUtil.replacePlaceholders(cfg.getString("title", "Confirm Teleport"),
                        Map.of("player", target.getName())));

        Inventory inv = Bukkit.createInventory(null, rows * 9, MessageUtil.toComponent(title));

        // Fill with filler
        if (cfg.getBoolean("filler.enabled", true)) {
            ItemStack filler = createItem(
                    Material.valueOf(cfg.getString("filler.material", "GRAY_STAINED_GLASS_PANE")),
                    cfg.getString("filler.name", " "),
                    null, null);
            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, filler);
            }
        }

        Map<String, String> placeholders = Map.of(
                "player", target.getName(),
                "type", type == TPARequest.Type.TPA ? "TPA" : "TPAHere"
        );

        // Player head info item
        ConfigurationSection infoSec = cfg.getConfigurationSection("info-item");
        if (infoSec != null) {
            int slot = infoSec.getInt("slot", 4);
            String mat = infoSec.getString("material", "PLAYER_HEAD");
            ItemStack infoItem = createItem(
                    Material.valueOf(mat),
                    MessageUtil.replacePlaceholders(infoSec.getString("name", ""), placeholders),
                    replacePlaceholdersInList(infoSec.getStringList("lore"), placeholders),
                    null);

            // Fill with background items
            if (mat.equals("PLAYER_HEAD") && infoItem.getItemMeta() instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(target);
                infoItem.setItemMeta(skullMeta);
            }
            inv.setItem(slot, infoItem);
        }

        // Confirm button
        ConfigurationSection confirmSec = cfg.getConfigurationSection("confirm-item");
        if (confirmSec != null) {
            int slot = confirmSec.getInt("slot", 11);
            inv.setItem(slot, createItem(
                    Material.valueOf(confirmSec.getString("material", "LIME_WOOL")),
                    MessageUtil.replacePlaceholders(confirmSec.getString("name", "Confirm"), placeholders),
                    replacePlaceholdersInList(confirmSec.getStringList("lore"), placeholders),
                    null));
        }

        // Cancel button
        ConfigurationSection cancelSec = cfg.getConfigurationSection("cancel-item");
        if (cancelSec != null) {
            int slot = cancelSec.getInt("slot", 15);
            inv.setItem(slot, createItem(
                    Material.valueOf(cancelSec.getString("material", "RED_WOOL")),
                    MessageUtil.replacePlaceholders(cancelSec.getString("name", "Cancel"), placeholders),
                    replacePlaceholdersInList(cancelSec.getStringList("lore"), placeholders),
                    null));
        }

        // Track confirmation target
        confirmTargets.put(sender.getUniqueId(), target.getName());
        confirmTypes.put(sender.getUniqueId(), type);

        sender.openInventory(inv);
    }

    // -- Dashboard GUI --

    // Opens pending request dashboard
    public void openDashboardGUI(Player player, int page) {
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("gui.dashboard");
        if (cfg == null) return;

        int rows = cfg.getInt("rows", 6);
        String title = GUI_ID_PREFIX + MessageUtil.colorize(cfg.getString("title", "Pending Requests"));

        Inventory inv = Bukkit.createInventory(null, rows * 9, MessageUtil.toComponent(title));

        // Fill with filler
        if (cfg.getBoolean("filler.enabled", true)) {
            ItemStack filler = createItem(
                    Material.valueOf(cfg.getString("filler.material", "GRAY_STAINED_GLASS_PANE")),
                    cfg.getString("filler.name", " "),
                    null, null);
            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, filler);
            }
        }

        // Fetch requests
        List<TPARequest> requests = plugin.getRequestManager().getRequestsFor(player.getUniqueId());
        int perPage = cfg.getInt("requests-per-page", 21);
        int startSlot = cfg.getInt("request-start-slot", 10);

        int totalPages = Math.max(1, (int) Math.ceil((double) requests.size() / perPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        dashboardPages.put(player.getUniqueId(), page);

        if (requests.isEmpty()) {
            // Show empty state item
            ConfigurationSection noReqSec = cfg.getConfigurationSection("no-requests-item");
            if (noReqSec != null) {
                int slot = noReqSec.getInt("slot", 22);
                inv.setItem(slot, createItem(
                        Material.valueOf(noReqSec.getString("material", "LIGHT_GRAY_STAINED_GLASS_PANE")),
                        noReqSec.getString("name", "&7No pending requests"),
                        noReqSec.getStringList("lore"),
                        null));
            }
        } else {
            // Populate request items
            ConfigurationSection reqItemSec = cfg.getConfigurationSection("request-item");
            int startIndex = page * perPage;
            int endIndex = Math.min(startIndex + perPage, requests.size());

            // Fill empty slots with filler, skipping borders
            List<Integer> availableSlots = getInteriorSlots(rows, startSlot, perPage);
            int slotIdx = 0;

            for (int i = startIndex; i < endIndex && slotIdx < availableSlots.size(); i++) {
                TPARequest req = requests.get(i);
                Player sender = Bukkit.getPlayer(req.getSender());
                String senderName = sender != null ? sender.getName() : "Unknown";

                Map<String, String> placeholders = Map.of(
                        "player", senderName,
                        "type", req.getTypeDisplay(),
                        "time_ago", req.getTimeAgo()
                );

                String mat = reqItemSec != null ? reqItemSec.getString("material", "PLAYER_HEAD") : "PLAYER_HEAD";
                String name = reqItemSec != null ? reqItemSec.getString("name", "{player}") : "{player}";
                List<String> lore = reqItemSec != null ? reqItemSec.getStringList("lore") : Collections.emptyList();

                ItemStack item = createItem(
                        Material.valueOf(mat),
                        MessageUtil.replacePlaceholders(name, placeholders),
                        replacePlaceholdersInList(lore, placeholders),
                        null);

                // Set skull texture
                if (mat.equals("PLAYER_HEAD") && sender != null && item.getItemMeta() instanceof SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(sender);
                    item.setItemMeta(skullMeta);
                }

                inv.setItem(availableSlots.get(slotIdx), item);
                slotIdx++;
            }
        }

        // Previous page button
        if (page > 0) {
            ConfigurationSection prevSec = cfg.getConfigurationSection("prev-page-item");
            if (prevSec != null) {
                inv.setItem(prevSec.getInt("slot", 45), createItem(
                        Material.valueOf(prevSec.getString("material", "ARROW")),
                        prevSec.getString("name", "← Previous Page"),
                        null, null));
            }
        }

        // Next page button
        if (page < totalPages - 1) {
            ConfigurationSection nextSec = cfg.getConfigurationSection("next-page-item");
            if (nextSec != null) {
                inv.setItem(nextSec.getInt("slot", 53), createItem(
                        Material.valueOf(nextSec.getString("material", "ARROW")),
                        nextSec.getString("name", "Next Page →"),
                        null, null));
            }
        }

        // Close button
        ConfigurationSection closeSec = cfg.getConfigurationSection("close-item");
        if (closeSec != null) {
            inv.setItem(closeSec.getInt("slot", 49), createItem(
                    Material.valueOf(closeSec.getString("material", "BARRIER")),
                    closeSec.getString("name", "&c&lClose"),
                    closeSec.getStringList("lore"),
                    null));
        }

        player.openInventory(inv);
    }

    // Opens page 0
    public void openDashboardGUI(Player player) {
        openDashboardGUI(player, 0);
    }

    // -- Getters --

    public String getConfirmTarget(UUID uuid) {
        return confirmTargets.get(uuid);
    }

    public TPARequest.Type getConfirmType(UUID uuid) {
        return confirmTypes.get(uuid);
    }

    public int getDashboardPage(UUID uuid) {
        return dashboardPages.getOrDefault(uuid, 0);
    }

    public void clearPlayerData(UUID uuid) {
        confirmTargets.remove(uuid);
        confirmTypes.remove(uuid);
        dashboardPages.remove(uuid);
    }

    // ── Config Slot Accessors ────────────────────────────────

    public int getConfirmSlot() {
        return plugin.getConfig().getInt("gui.confirm.confirm-item.slot", 11);
    }

    public int getCancelSlot() {
        return plugin.getConfig().getInt("gui.confirm.cancel-item.slot", 15);
    }

    public int getPrevPageSlot() {
        return plugin.getConfig().getInt("gui.dashboard.prev-page-item.slot", 45);
    }

    public int getNextPageSlot() {
        return plugin.getConfig().getInt("gui.dashboard.next-page-item.slot", 53);
    }

    public int getCloseSlot() {
        return plugin.getConfig().getInt("gui.dashboard.close-item.slot", 49);
    }

    public int getDashboardStartSlot() {
        return plugin.getConfig().getInt("gui.dashboard.request-start-slot", 10);
    }

    public int getDashboardPerPage() {
        return plugin.getConfig().getInt("gui.dashboard.requests-per-page", 21);
    }

    public int getDashboardRows() {
        return plugin.getConfig().getInt("gui.dashboard.rows", 6);
    }

    // ── Helpers ──────────────────────────────────────────────

    private ItemStack createItem(Material material, String name, List<String> lore, Player skullOwner) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (name != null && !name.isEmpty()) {
            meta.displayName(MessageUtil.toComponent(name));
        }

        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream()
                    .map(MessageUtil::toComponent)
                    .collect(Collectors.toList()));
        }

        item.setItemMeta(meta);
        return item;
    }

    private List<String> replacePlaceholdersInList(List<String> list, Map<String, String> placeholders) {
        if (list == null) return Collections.emptyList();
        return list.stream()
                .map(line -> MessageUtil.replacePlaceholders(line, placeholders))
                .collect(Collectors.toList());
    }

    /**
     * Get interior inventory slots (skip borders) starting from startSlot.
     */
    private List<Integer> getInteriorSlots(int rows, int startSlot, int maxSlots) {
        List<Integer> slots = new ArrayList<>();
        int size = rows * 9;
        for (int i = startSlot; i < size && slots.size() < maxSlots; i++) {
            int col = i % 9;
            // Skip first and last column (border)
            if (col == 0 || col == 8) continue;
            slots.add(i);
        }
        return slots;
    }
}
