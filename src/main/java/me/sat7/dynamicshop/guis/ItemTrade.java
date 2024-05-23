package me.sat7.dynamicshop.guis;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.constants.Constants;
import me.sat7.dynamicshop.economyhook.JobsHook;
import me.sat7.dynamicshop.economyhook.PlayerpointHook;
import me.sat7.dynamicshop.events.OnChat;
import me.sat7.dynamicshop.files.CustomConfig;
import me.sat7.dynamicshop.transactions.Buy;
import me.sat7.dynamicshop.transactions.Calc;
import me.sat7.dynamicshop.transactions.Sell;
import me.sat7.dynamicshop.utilities.ConfigUtil;
import me.sat7.dynamicshop.utilities.HashUtil;
import me.sat7.dynamicshop.utilities.ShopUtil;
import me.sat7.dynamicshop.utilities.UserUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

import static me.sat7.dynamicshop.constants.Constants.P_ADMIN_SHOP_EDIT;
import static me.sat7.dynamicshop.utilities.LangUtil.n;
import static me.sat7.dynamicshop.utilities.LangUtil.t;
import static me.sat7.dynamicshop.utilities.LayoutUtil.l;

public final class ItemTrade extends InGameUI
{
    public ItemTrade()
    {
        uiType = UI_TYPE.ItemTrade;
    }

    private final int CLOSE = 9;
    private final int SELL_ONLY_TOGGLE = 1;
    private final int BUY_ONLY_TOGGLE = 10;
    private final int CHECK_BALANCE = 0;

    private Player player;
    private String shopName;
    private String tradeIdx;
    private int deliveryCharge;
    private FileConfiguration shopData;
    private String sellBuyOnly;
    private String material;
    private ItemMeta itemMeta;

    private int[] tradeUI_default;

    public Inventory getGui(Player player, String shopName, String tradeIdx)
    {
        this.player = player;
        this.shopName = shopName;
        this.tradeIdx = tradeIdx;
        this.deliveryCharge = ShopUtil.CalcShipping(shopName, player);
        this.shopData = ShopUtil.shopConfigFiles.get(shopName).get();
        this.sellBuyOnly = shopData.getString(this.tradeIdx + ".tradeType", "");
        this.material = shopData.getString(tradeIdx + ".mat");
        this.itemMeta = (ItemMeta) shopData.get(tradeIdx + ".itemStack");

        UserUtil.userInteractItem.put(player.getUniqueId(), shopName + "/" + tradeIdx);

        String uiTitle = shopData.getBoolean("Options.enable", true) ? "" : t(player, "SHOP.DISABLED");
        uiTitle += t(player, "TRADE_TITLE");
        inventory = Bukkit.createInventory(player, 18, uiTitle);

        if (shopData.contains("Options.tradeUI"))
        {
            tradeUI_default = new int[]{1, 2, 4, 8, 16, 32, 64};
            try
            {
                String amountArrayData = shopData.getString("Options.tradeUI");
                String[] strArray = amountArrayData.split(",");
                int count = Math.min(strArray.length, 7);
                for (int i = 0; i < count; i++)
                {
                    tradeUI_default[i] = Integer.parseInt(strArray[i]);
                }
            }
            catch (Exception e)
            {
                tradeUI_default = null;
                DynamicShop.console.sendMessage(Constants.DYNAMIC_SHOP_PREFIX + "Data parsing error. ShopName: " + shopName + ", tradeIdx: " + tradeIdx);
            }
        }

        CreateBalanceButton();
        CreateSellBuyOnlyToggle();
        CreateTradeButtons();
        CreateCloseButton(player, CLOSE);

        return inventory;
    }

    @Override
    public void OnClickUpperInventory(InventoryClickEvent e)
    {
        player = (Player) e.getWhoClicked();

        if(!CheckShopIsEnable())
            return;

        CustomConfig data = ShopUtil.shopConfigFiles.get(shopName);

        if (e.getCurrentItem() != null && e.getCurrentItem().getItemMeta() != null)
        {
            if (e.getSlot() == CLOSE)
            {
                // 표지판을 클릭해서 거래화면에 진입한 경우에는 상점UI로 돌아가는 대신 인벤토리를 닫음
                if (UserUtil.userTempData.get(player.getUniqueId()).equalsIgnoreCase("sign"))
                {
                    UserUtil.userTempData.put(player.getUniqueId(), "");
                    player.closeInventory();
                } else
                {
                    DynaShopAPI.openShopGui(player, shopName, Integer.parseInt(tradeIdx) / 45 + 1);
                }
            } else if (e.getSlot() == CHECK_BALANCE)
            {
                if (ShopUtil.GetCurrency(data).equalsIgnoreCase(Constants.S_JOBPOINT))
                {
                    player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "TRADE.BALANCE") + ":§f " + n(JobsHook.getCurJobPoints(player)) + t(player, "JOB_POINTS"));
                } else if (ShopUtil.GetCurrency(data).equalsIgnoreCase(Constants.S_PLAYERPOINT))
                {
                    player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "TRADE.BALANCE") + ":§f " + n(PlayerpointHook.getCurrentPP(player)) + t(player, "PLAYER_POINTS"));
                } else if (ShopUtil.GetCurrency(data).equalsIgnoreCase(Constants.S_EXP))
                {
                    player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "TRADE.BALANCE") + ":§f " + n(player.getTotalExperience()) + t(player, "EXP_POINTS"));
                } else
                {
                    player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "TRADE.BALANCE") + ":§f " + n(DynamicShop.getEconomy().getBalance(player)));
                }
            } else if (e.getSlot() == SELL_ONLY_TOGGLE)
            {
                if (player.hasPermission(P_ADMIN_SHOP_EDIT))
                {
                    String path = tradeIdx + ".tradeType";
                    if (sellBuyOnly == null || !sellBuyOnly.equalsIgnoreCase("SellOnly"))
                    {
                        sellBuyOnly = "SellOnly";
                        data.get().set(path, "SellOnly");
                    } else
                    {
                        sellBuyOnly = "";
                        data.get().set(path, null);
                    }

                    data.save();
                    RefreshUI();
                }
            } else if (e.getSlot() == BUY_ONLY_TOGGLE)
            {
                if (player.hasPermission(P_ADMIN_SHOP_EDIT))
                {
                    String path = tradeIdx + ".tradeType";
                    if (sellBuyOnly == null || !sellBuyOnly.equalsIgnoreCase("BuyOnly"))
                    {
                        sellBuyOnly = "BuyOnly";
                        data.get().set(path, "BuyOnly");
                    } else
                    {
                        sellBuyOnly = "";
                        data.get().set(path, null);
                    }

                    data.save();
                    RefreshUI();
                }
            } else
            {
                if (player.hasPermission(P_ADMIN_SHOP_EDIT) && e.isShiftClick() && e.isRightClick())
                {
                    player.closeInventory();
                    player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "TRADE.WAIT_FOR_INPUT"));

                    UserUtil.userInteractItem.put(player.getUniqueId(), shopName + "/" + tradeIdx);
                    UserUtil.userTempData.put(player.getUniqueId(), "waitForTradeUI");

                    OnChat.WaitForInput(player);
                }
                else
                {
                    ItemStack tempIS = new ItemStack(e.getCurrentItem().getType(), e.getCurrentItem().getAmount());
                    tempIS.setItemMeta((ItemMeta) data.get().get(tradeIdx + ".itemStack"));

                    // 무한재고&고정가격
                    boolean infiniteStock = data.get().getInt(tradeIdx + ".stock") <= 0;

                    // 배달비 계산
                    ConfigurationSection optionS = data.get().getConfigurationSection("Options");
                    if (optionS.contains("world") && optionS.contains("pos1") && optionS.contains("pos2") && optionS.contains("flag.deliverycharge"))
                    {
                        deliveryCharge = ShopUtil.CalcShipping(shopName, player);
                        if (deliveryCharge == -1)
                        {
                            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "MESSAGE.DELIVERY_CHARGE_NA")); // 다른 월드로 배달 불가능
                            return;
                        }
                    }

                    if (e.getSlot() <= 10)
                        Sell(optionS, tempIS, deliveryCharge, infiniteStock);
                    else
                        Buy(optionS, tempIS, deliveryCharge, infiniteStock);
                }
            }
        }
    }

    private void CreateBalanceButton()
    {
        String moneyLore = l("TRADE_VIEW.BALANCE");
        String myBalanceString;

        if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_JOBPOINT))
        {
            myBalanceString = "§f" + n(JobsHook.getCurJobPoints(player)) + t(player,"JOB_POINTS");
        }
        else if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_PLAYERPOINT))
        {
            myBalanceString = "§f" + n(PlayerpointHook.getCurrentPP(player)) + t(player,"PLAYER_POINTS");
        }
        else if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_EXP))
        {
            myBalanceString = "§f" + n(player.getTotalExperience()) + t(player,"EXP_POINTS");
        }
        else
        {
            myBalanceString = "§f" + n(DynamicShop.getEconomy().getBalance(player));
        }
        String balStr;
        if (ShopUtil.getShopBalance(shopName) >= 0)
        {
            double d = ShopUtil.getShopBalance(shopName);

            if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_JOBPOINT))
                balStr = n(d) + t(player, "JOB_POINTS");
            else if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_PLAYERPOINT))
                balStr = n(d, true) + t(player, "PLAYER_POINTS");
            else if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_EXP))
                balStr = n(d, true) + t(player, "EXP_POINTS");
            else
                balStr = n(d);
        } else
        {
            balStr = t(player, "TRADE.SHOP_BAL_INF");
        }

        String shopBalanceString = "";
        if (!shopData.contains("Options.flag.hideshopbalance"))
            shopBalanceString = t(player, "TRADE.SHOP_BAL").replace("{num}", balStr);

        moneyLore = moneyLore.replace("{\\nPlayerBalance}", "\n" + myBalanceString);
        moneyLore = moneyLore.replace("{\\nShopBalance}", shopBalanceString.isEmpty() ? "" : "\n" + shopBalanceString);
        moneyLore = moneyLore.replace("{PlayerBalance}", myBalanceString);
        moneyLore = moneyLore.replace("{ShopBalance}", shopBalanceString);

        String temp = moneyLore.replace(" ", "");
        if (ChatColor.stripColor(temp).startsWith("\n"))
            moneyLore = moneyLore.replaceFirst("\n", "");

        CreateButton(CHECK_BALANCE, InGameUI.GetBalanceButtonIconMat(), t(player, "TRADE.BALANCE"), moneyLore);
    }

    private void CreateSellBuyOnlyToggle()
    {
        ArrayList<String> sellLore = new ArrayList<>();
        if (sellBuyOnly.equalsIgnoreCase("SellOnly")) sellLore.add(t(player, "TRADE.SELL_ONLY_LORE"));
        else if (sellBuyOnly.equalsIgnoreCase("BuyOnly")) sellLore.add(t(player,"TRADE.BUY_ONLY_LORE"));

        if (player.hasPermission(P_ADMIN_SHOP_EDIT))
            sellLore.add(t(player,"TRADE.TOGGLE_SELLABLE"));

        ArrayList<String> buyLore = new ArrayList<>();
        if (sellBuyOnly.equalsIgnoreCase("SellOnly")) buyLore.add(t(player,"TRADE.SELL_ONLY_LORE"));
        else if (sellBuyOnly.equalsIgnoreCase("BuyOnly")) buyLore.add(t(player,"TRADE.BUY_ONLY_LORE"));

        if (player.hasPermission(P_ADMIN_SHOP_EDIT))
            buyLore.add(t(player,"TRADE.TOGGLE_BUYABLE"));

        CreateButton(SELL_ONLY_TOGGLE, InGameUI.GetSellToggleButtonIconMat(), t(player, "TRADE.SELL"), sellLore);
        CreateButton(BUY_ONLY_TOGGLE, InGameUI.GetBuyToggleButtonIconMat(), t(player, "TRADE.BUY"), buyLore);
    }

    private void CreateTradeButtons()
    {
        if (!sellBuyOnly.equalsIgnoreCase("BuyOnly"))
            CreateTradeButtons(true);
        if (!sellBuyOnly.equalsIgnoreCase("SellOnly"))
            CreateTradeButtons(false);
    }

    private void CreateTradeButtons(boolean sell)
    {
        // 플레이어당 거래 제한
        String tradeLimitString = "";
        int tradeIdxInt = Integer.parseInt(tradeIdx);
        int tradeLimitLeft = UserUtil.GetTradingLimitLeft(player, shopName, tradeIdxInt, HashUtil.GetItemHash(new ItemStack(Material.getMaterial(material))), sell);
        if (tradeLimitLeft != Integer.MAX_VALUE)
        {
            String limitString = sell ? t(player, "TRADE.SALES_LIMIT_PER_PLAYER") : t(player, "TRADE.PURCHASE_LIMIT_PER_PLAYER");
            String tradeLimitResetTime = ShopUtil.GetTradeLimitNextResetTime(shopName, tradeIdxInt);
            tradeLimitString = limitString.replace("{num}", String.valueOf(tradeLimitLeft)).replace("{time}", tradeLimitResetTime);
        }

        int[] amountArray;
        Material mat = Material.getMaterial(material);
        if (tradeUI_default != null)
        {
            amountArray = tradeUI_default;
        }
        else
        {
            if (mat.getMaxStackSize() <= 1)
            {
                amountArray = new int[]{1, 2, 3, 4, 5, 6, 7};
            }
            else
            {
                amountArray = new int[]{1, 2, 4, 8, 16, 32, 64};
            }
        }

        if (shopData.contains(tradeIdx + ".tradeUI"))
        {
            try
            {
                String amountArrayData = shopData.getString(tradeIdx + ".tradeUI");
                String[] strArray = amountArrayData.split(",");
                int count = Math.min(strArray.length, 7);
                for (int i = 0; i < count; i++)
                {
                    amountArray[i] = Integer.parseInt(strArray[i]);
                }
            }
            catch (Exception e)
            {
                //DynamicShop.console.sendMessage(Constants.DYNAMIC_SHOP_PREFIX + "Data parsing error. ShopName: " + shopName + ", tradeIdx: " + tradeIdx);
            }
        }

        int idx = sell ? 2 : 11;
        for (int i = 1; i < 8; i++)
        {
            int amount = Math.min(64, amountArray[i - 1]);

            ItemStack itemStack = new ItemStack(Material.getMaterial(material), amount);
            itemStack.setItemMeta((ItemMeta) shopData.get(tradeIdx + ".itemStack"));
            ItemMeta meta = itemStack.getItemMeta();

            int stock = shopData.getInt(tradeIdx + ".stock");
            int maxStock = shopData.getInt(tradeIdx + ".maxStock", -1);

            double price = Calc.calcTotalCost(shopName, tradeIdx, sell ? -amount : amount)[0];
            String lore;
            String priceText;

            boolean isIntTypeCurrency = false;
            String currencyKey = "";
            if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_JOBPOINT))
            {
                currencyKey = "_JP";
            }
            else if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_PLAYERPOINT))
            {
                currencyKey = "_PP";
                isIntTypeCurrency = true;
            }
            else if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_EXP))
            {
                currencyKey = "_EXP";
                isIntTypeCurrency = true;
            }

            if (sell)
            {
                lore = l("TRADE_VIEW.SELL");

                if (shopData.contains(tradeIdx + ".discount"))
                {
                    String original = n(price * 100 / (double) (100 - shopData.getInt(tradeIdx + ".discount")), isIntTypeCurrency);
                    priceText = t(player, "TRADE.SELL_PRICE_DISCOUNTED" + currencyKey).replace("{num}", original).replace("{num2}", n(price, isIntTypeCurrency));
                }
                else
                {
                    priceText = t(player, "TRADE.SELL_PRICE" + currencyKey).replace("{num}", n(price, isIntTypeCurrency));
                }
            }
            else
            {
                lore = l("TRADE_VIEW.BUY");

                if (shopData.contains(tradeIdx + ".discount"))
                {
                    String original = n(price * 100 / (double) (100 - shopData.getInt(tradeIdx + ".discount")), isIntTypeCurrency);
                    priceText = t(player, "TRADE.PRICE_DISCOUNTED" + currencyKey).replace("{num}", original).replace("{num2}", n(price, isIntTypeCurrency));
                }
                else
                {
                    priceText = t(player, "TRADE.PRICE" + currencyKey).replace("{num}", n(price, isIntTypeCurrency));
                }
            }

            if (!sell)
            {
                if (stock != -1 && stock <= amount) // stock은 1이거나 그보다 작을 수 없음. 단 -1은 무한재고를 의미함.
                    continue;
            }

            String stockText = "";
            if (!shopData.contains("Options.flag.hidestock"))
            {
                if (stock <= 0)
                {
                    stockText = t(player, "TRADE.INF_STOCK");
                } else if (ConfigUtil.GetDisplayStockAsStack())
                {
                    stockText = t(player, "TRADE.STACKS").replace("{num}", n(stock / 64));
                } else
                {
                    stockText = n(stock);
                }

                String maxStockText;
                if (shopData.contains("Options.flag.showmaxstock") && maxStock != -1)
                {
                    if (ConfigUtil.GetDisplayStockAsStack())
                    {
                        maxStockText = t(player, "TRADE.STACKS").replace("{num}", n(maxStock / 64));
                    } else
                    {
                        maxStockText = n(maxStock);
                    }

                    stockText = t(player, "SHOP.STOCK_2").replace("{stock}", stockText).replace("{max_stock}", maxStockText);
                } else
                {
                    stockText = t(player, "SHOP.STOCK").replace("{num}", stockText);
                }
            }

            // 플레이어당 거래 제한
            if (tradeLimitLeft != Integer.MAX_VALUE)
            {
                if (!stockText.isEmpty())
                    stockText += "\n";
                stockText += tradeLimitString;
            }

            // 배달비
            String deliveryChargeText = "";
            if (deliveryCharge > 0)
            {
                if (sell && price < deliveryCharge)
                {
                    deliveryChargeText = "§c" + ChatColor.stripColor(t(player,"MESSAGE.DELIVERY_CHARGE")).replace("{fee}", n(deliveryCharge));
                } else
                {
                    deliveryChargeText = t(player, "MESSAGE.DELIVERY_CHARGE").replace("{fee}", n(deliveryCharge));
                }
            }

            String tradeLoreText = sell ? t(player, "TRADE.CLICK_TO_SELL") : t(player, "TRADE.CLICK_TO_BUY");
            tradeLoreText = tradeLoreText.replace("{amount}", n(amount));

            if (player.hasPermission(P_ADMIN_SHOP_EDIT))
            {
                tradeLoreText += "\n" + t(player, "TRADE.QUANTITY_LORE");
            }

            lore = lore.replace("{\\nPrice}", priceText.isEmpty() ? "" : "\n" + priceText);
            lore = lore.replace("{\\nStock}", stockText.isEmpty() ? "" : "\n" + stockText);
            lore = lore.replace("{\\nDeliveryCharge}", deliveryChargeText.isEmpty() ? "" : "\n" + deliveryChargeText);
            lore = lore.replace("{\\nTradeLore}", "\n" + tradeLoreText);

            lore = lore.replace("{Price}", priceText);
            lore = lore.replace("{Stock}", stockText);
            lore = lore.replace("{DeliveryCharge}", deliveryChargeText);
            lore = lore.replace("{TradeLore}", tradeLoreText);

            String temp = lore.replace(" ", "");
            if (ChatColor.stripColor(temp).startsWith("\n"))
                lore = lore.replaceFirst("\n", "");

            meta.setLore(new ArrayList<>(Arrays.asList(lore.split("\n"))));

            itemStack.setItemMeta(meta);
            inventory.setItem(idx, itemStack);

            idx++;
        }
    }

    private void Sell(ConfigurationSection options, ItemStack itemStack, int deliveryCharge, boolean infiniteStock)
    {
        String permission = options.getString("permission");
        if (permission != null && permission.length() > 0 && !player.hasPermission(permission) && !player.hasPermission(permission + ".sell"))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.NO_PERMISSION"));
            return;
        }

        Sell.sell(ShopUtil.GetCurrency(shopData), player, shopName, tradeIdx, itemStack, -deliveryCharge, infiniteStock);
    }

    private void Buy(ConfigurationSection options, ItemStack itemStack, int deliveryCharge, boolean infiniteStock)
    {
        String permission = options.getString("permission");
        if (permission != null && permission.length() > 0 && !player.hasPermission(permission) && !player.hasPermission(permission + ".buy"))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.NO_PERMISSION"));
            return;
        }

        Buy.buy(ShopUtil.GetCurrency(shopData), player, shopName, tradeIdx, itemStack, deliveryCharge, infiniteStock);
    }

    @Override
    public void RefreshUI()
    {
        if(!CheckShopIsEnable())
            return;

        for (int i = 2; i < 9; i++)
            inventory.setItem(i, null);
        for (int i = 11; i < 18; i++)
            inventory.setItem(i, null);

        CreateBalanceButton();
        CreateSellBuyOnlyToggle();
        CreateTradeButtons();
    }

    public boolean CheckShopIsEnable()
    {
        if (!ShopUtil.shopConfigFiles.containsKey(shopName)
            || shopData == null
            || !shopData.contains(tradeIdx)
            || !shopData.getString(tradeIdx + ".mat").equals(material))
        {
            ItemMeta otherMeta = (ItemMeta) shopData.get(tradeIdx + ".itemStack");
            if(itemMeta == null || !itemMeta.equals(otherMeta))
            {
                player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.INVALID_TRANSACTION"));
                player.closeInventory();
                return false;
            }
        }

        boolean ret = DynaShopAPI.IsShopEnable(shopName) || player.hasPermission(P_ADMIN_SHOP_EDIT);

        if(!ret)
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "MESSAGE.SHOP_IS_CLOSED_BY_ADMIN"));
            player.closeInventory();
        }

        return ret;
    }
}
