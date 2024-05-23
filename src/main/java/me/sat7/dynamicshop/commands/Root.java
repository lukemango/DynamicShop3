package me.sat7.dynamicshop.commands;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.constants.Constants;
import me.sat7.dynamicshop.utilities.UserUtil;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.sat7.dynamicshop.utilities.LangUtil.t;

public class Root implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean senderIsPlayer = (sender instanceof Player);
        if(senderIsPlayer)
        {
            Player player = (Player) sender;

            if (!player.hasPermission(Constants.P_USE))
            {
                player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.NO_PERMISSION"));
                return true;
            }

            if (player.getGameMode() == GameMode.CREATIVE && !player.hasPermission(Constants.P_ADMIN_CREATIVE))
            {
                player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.CREATIVE"));
                return true;
            }

            // user.yml 에 player가 없으면 재생성 시도. 실패시 리턴.
            if (!DynaShopAPI.recreateUserData(player))
            {
                player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.NO_USER_ID"));
                return true;
            }

            // 스타트페이지
            if (args.length == 0)
            {
                DynaShopAPI.openStartPage(player);
                return true;
            }
            else if (args[0].equalsIgnoreCase("close"))
            {
                player.closeInventory();
                return true;
            }
            else if (args[0].equalsIgnoreCase("shop"))
            {
                Shop.shopCommand(args, player);
                return true;
            }
            else if (args[0].equalsIgnoreCase("qsell"))
            {
                if (sender.hasPermission(Constants.P_USE_QSELL))
                {
                    DynaShopAPI.openQuickSellGUI(player);
                    return true;
                }
            }
            else if (args[0].equalsIgnoreCase("dummyUUID") && DynamicShop.DEBUG_MODE && player.hasPermission(Constants.P_ADMIN_SHOP_EDIT))
            {
                UserUtil.CreateDummyPlayerData(player, 1000);
                return true;
            }
        }
        // 콘솔에서 실행했음.
        else
        {
            if (args.length > 0)
            {
                if (args[0].equalsIgnoreCase("dbgToggle"))
                {
                    DynamicShop.DEBUG_MODE = !DynamicShop.DEBUG_MODE;
                    sender.sendMessage(Constants.DYNAMIC_SHOP_PREFIX + "DebugMode " + DynamicShop.DEBUG_MODE);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("dbgLog"))
                {
                    DynamicShop.DEBUG_LOG_ENABLED = !DynamicShop.DEBUG_LOG_ENABLED;
                    sender.sendMessage(Constants.DYNAMIC_SHOP_PREFIX + "DebugLog " + DynamicShop.DEBUG_LOG_ENABLED);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("dbg") && DynamicShop.DEBUG_MODE)
                {
                    DynamicShop.DebugLog();
                    return true;
                }
                else if (args[0].equalsIgnoreCase("shop"))
                {
                    Shop.shopCommand(args, sender);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("qsell"))
                {
                    sender.sendMessage(Constants.DYNAMIC_SHOP_PREFIX + " You can't run this command in console");
                    return true;
                }
            }
        }

        if(args.length > 0)
            CMDManager.RunCMD(args[0].toLowerCase(), args, sender);
        else
            sender.sendMessage(Constants.DYNAMIC_SHOP_PREFIX + " You can't run this command in console");

        return true;
    }

}

