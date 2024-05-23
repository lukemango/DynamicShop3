package me.sat7.dynamicshop.events;

import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.UpdateChecker;
import me.sat7.dynamicshop.guis.UIManager;
import me.sat7.dynamicshop.utilities.UserUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static me.sat7.dynamicshop.constants.Constants.P_ADMIN_RELOAD;
import static me.sat7.dynamicshop.constants.Constants.P_ADMIN_SHOP_EDIT;

public class JoinQuit implements Listener
{

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        Player player = e.getPlayer();
        UserUtil.CreateNewPlayerData(player);

        boolean isSnapshot = DynamicShop.yourVersion.contains("snapshot");
        if (DynamicShop.updateAvailable || isSnapshot)
        {
            if (e.getPlayer().hasPermission(P_ADMIN_SHOP_EDIT) || e.getPlayer().hasPermission(P_ADMIN_RELOAD))
            {
                TextComponent text = new TextComponent("");
                text.addExtra(DynamicShop.CreateLink("DShop3", false, ChatColor.DARK_AQUA, UpdateChecker.getResourceUrl()));
                text.addExtra(" ");
                text.addExtra(DynamicShop.CreateLink("Download", false, ChatColor.WHITE, UpdateChecker.getResourceUrl()));
                text.addExtra(" ");
                text.addExtra(DynamicShop.CreateLink("Premium", false, ChatColor.WHITE, "https://spigotmc.org/resources/100058"));
                text.addExtra(" ");
                text.addExtra(DynamicShop.CreateLink("Donate", false, ChatColor.WHITE, "https://www.paypal.com/paypalme/7sat"));

                e.getPlayer().sendMessage("");
                e.getPlayer().spigot().sendMessage(text);

                if(isSnapshot)
                {
                    e.getPlayer().sendMessage("§cYou are currently using a snapshot build.");
                }
                else
                {
                    e.getPlayer().sendMessage("New Update available");
                }

                e.getPlayer().sendMessage("§7Latest version: §f" + DynamicShop.lastVersion);
                e.getPlayer().sendMessage("§7Your version: §f" + DynamicShop.yourVersion);
                e.getPlayer().sendMessage("");
            }
        }
    }

    @EventHandler
    public void OnPlayerQuit(PlayerQuitEvent e)
    {
        UIManager.OnPlayerQuit(e.getPlayer());
        UserUtil.userTempData.remove(e.getPlayer().getUniqueId());
        UserUtil.userInteractItem.remove(e.getPlayer().getUniqueId());
    }
}
