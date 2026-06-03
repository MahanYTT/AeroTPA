package dev.aero.tpa.gui;

import dev.aero.tpa.AeroTPA;
import dev.aero.tpa.model.TPARequest;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

import java.util.List;
import java.util.UUID;

// Prevents item theft and processes GUI button clicks.
// Cancels events first, then executes logic.
public class GUIListener implements Listener {

    private final AeroTPA plugin;

    public GUIListener(AeroTPA plugin) {
        this.plugin = plugin;
    }

    // -- Inventory Click --

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = getPlainTitle(event.getView());
        if (title == null || !title.startsWith("AeroTPA")) return;

        // Cancel click event to prevent item taking
        event.setCancelled(true);

        // Ignore clicks outside the top inventory
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        GUIManager guiManager = plugin.getGuiManager();

        // Determine which GUI this is
        if (isConfirmGUI(title)) {
            handleConfirmClick(player, slot, guiManager);
        } else if (isDashboardGUI(title)) {
            handleDashboardClick(player, slot, event.getClick(), guiManager);
        }
    }

    // -- Prevent Drag --

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = getPlainTitle(event.getView());
        if (title != null && title.startsWith("AeroTPA")) {
            event.setCancelled(true);
        }
    }

    // -- Prevent Move --

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        String destTitle = getPlainInventoryTitle(event.getDestination());
        String srcTitle = getPlainInventoryTitle(event.getSource());
        if ((destTitle != null && destTitle.startsWith("AeroTPA"))
                || (srcTitle != null && srcTitle.startsWith("AeroTPA"))) {
            event.setCancelled(true);
        }
    }

    // -- Clean up on close --

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = getPlainTitle(event.getView());
        if (title != null && title.startsWith("AeroTPA")) {
            plugin.getGuiManager().clearPlayerData(player.getUniqueId());
        }
    }

    // -- Confirm GUI Logic --

    private void handleConfirmClick(Player player, int slot, GUIManager guiManager) {
        if (slot == guiManager.getConfirmSlot()) {
            // Confirm button logic
            String targetName = guiManager.getConfirmTarget(player.getUniqueId());
            TPARequest.Type type = guiManager.getConfirmType(player.getUniqueId());

            if (targetName != null && type != null) {
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null && target.isOnline()) {
                    plugin.getRequestManager().createRequest(player, target, type);
                }
            }
            player.closeInventory();

        } else if (slot == guiManager.getCancelSlot()) {
            // Cancel button logic
            player.closeInventory();
        }
    }

    // -- Dashboard GUI Logic --

    private void handleDashboardClick(Player player, int slot, ClickType clickType, GUIManager guiManager) {
        UUID uuid = player.getUniqueId();

        // Close button logic
        if (slot == guiManager.getCloseSlot()) {
            player.closeInventory();
            return;
        }

        // Previous page logic
        if (slot == guiManager.getPrevPageSlot()) {
            int currentPage = guiManager.getDashboardPage(uuid);
            if (currentPage > 0) {
                guiManager.openDashboardGUI(player, currentPage - 1);
            }
            return;
        }

        // Next page logic
        if (slot == guiManager.getNextPageSlot()) {
            int currentPage = guiManager.getDashboardPage(uuid);
            guiManager.openDashboardGUI(player, currentPage + 1);
            return;
        }

        // Handle request item click
        int startSlot = guiManager.getDashboardStartSlot();
        int perPage = guiManager.getDashboardPerPage();
        int dashboardRows = guiManager.getDashboardRows();

        List<Integer> interiorSlots = getInteriorSlots(dashboardRows, startSlot, perPage);
        int slotIndex = interiorSlots.indexOf(slot);
        if (slotIndex < 0) return; // Clicked a non-request slot

        int page = guiManager.getDashboardPage(uuid);
        List<TPARequest> requests = plugin.getRequestManager().getRequestsFor(uuid);
        int requestIndex = (page * perPage) + slotIndex;

        if (requestIndex >= requests.size()) return;

        TPARequest request = requests.get(requestIndex);

        if (clickType.isLeftClick()) {
            // Accept logic
            plugin.getRequestManager().acceptRequest(player, request.getSender());
            // Refresh or close inventory
            player.closeInventory();
        } else if (clickType.isRightClick()) {
            // Deny logic
            plugin.getRequestManager().denyRequest(player, request.getSender());
            // Refresh the dashboard GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    guiManager.openDashboardGUI(player, page);
                }
            }, 1L);
        }
    }

    // -- Utility --

    private boolean isConfirmGUI(String title) {
        return title.startsWith("AeroTPA") && title.contains("Confirm");
    }

    private boolean isDashboardGUI(String title) {
        return title.startsWith("AeroTPA") && title.contains("Pending");
    }

    // Extract plain text inventory title safely
    private String getPlainTitle(org.bukkit.inventory.InventoryView view) {
        try {
            return PlainTextComponentSerializer.plainText().serialize(view.title());
        } catch (Exception e) {
            return null;
        }
    }

    // Extract title for move events
    private String getPlainInventoryTitle(org.bukkit.inventory.Inventory inventory) {
        try {
            // Cannot reliably get title from Inventory object alone
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Get interior inventory slots
    private java.util.List<Integer> getInteriorSlots(int rows, int startSlot, int maxSlots) {
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        int size = rows * 9;
        for (int i = startSlot; i < size && slots.size() < maxSlots; i++) {
            int col = i % 9;
            if (col == 0 || col == 8) continue;
            slots.add(i);
        }
        return slots;
    }
}
