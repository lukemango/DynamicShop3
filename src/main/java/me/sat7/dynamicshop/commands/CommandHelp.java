package me.sat7.dynamicshop.commands;

import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.utilities.UserUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static me.sat7.dynamicshop.utilities.LangUtil.t;

public final class CommandHelp extends DSCMD
{
    public CommandHelp()
    {
        permission = "";
        validArgCount.add(2);
    }

    @Override
    public void SendHelpMessage(Player player)
    {
        player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "HELP.TITLE").replace("{command}", "cmdHelp"));
        player.sendMessage(" - " + t(player, "HELP.USAGE") + ": /ds cmdHelp <on | off>");
        player.sendMessage(" - " + t(player, "HELP.CMD"));

        player.sendMessage("");
    }

    @Override
    public void RunCMD(String[] args, CommandSender sender)
    {
        if(!CheckValid(args, sender))
            return;

        Player player = (Player) sender;

        UUID uuid = player.getUniqueId();

        if (args[1].equalsIgnoreCase("on"))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ON"));
            UserUtil.userTempData.put(uuid, "");
            UserUtil.ccUser.get().set(player.getUniqueId() + ".cmdHelp", true);
        }
        else if (args[1].equalsIgnoreCase("off"))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "OFF"));
            UserUtil.userTempData.put(uuid, "");
            UserUtil.ccUser.get().set(player.getUniqueId() + ".cmdHelp", false);
        }
        else
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.WRONG_USAGE"));
        }
    }
}
