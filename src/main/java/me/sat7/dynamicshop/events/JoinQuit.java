package me.sat7.dynamicshop.events;

import me.sat7.dynamicshop.guis.UIManager;
import me.sat7.dynamicshop.utilities.UserUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuit implements Listener {

    @EventHandler
    public void OnPlayerQuit(PlayerQuitEvent e) {
        UIManager.OnPlayerQuit(e.getPlayer());
        UserUtil.userTempData.remove(e.getPlayer().getUniqueId());
        UserUtil.userInteractItem.remove(e.getPlayer().getUniqueId());
    }
}
