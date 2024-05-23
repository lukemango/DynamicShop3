package me.sat7.dynamicshop.guis;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.constants.Constants;
import me.sat7.dynamicshop.economyhook.JobsHook;
import me.sat7.dynamicshop.economyhook.PlayerpointHook;
import me.sat7.dynamicshop.models.DSItem;
import me.sat7.dynamicshop.transactions.Calc;
import me.sat7.dynamicshop.utilities.ConfigUtil;
import me.sat7.dynamicshop.utilities.HashUtil;
import me.sat7.dynamicshop.utilities.MathUtil;
import me.sat7.dynamicshop.utilities.ShopUtil;
import me.sat7.dynamicshop.utilities.SoundUtil;
import me.sat7.dynamicshop.utilities.UserUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import static me.sat7.dynamicshop.constants.Constants.P_ADMIN_SHOP_EDIT;
import static me.sat7.dynamicshop.utilities.LangUtil.n;
import static me.sat7.dynamicshop.utilities.LangUtil.t;
import static me.sat7.dynamicshop.utilities.LayoutUtil.l;
import static me.sat7.dynamicshop.utilities.MathUtil.Clamp;
import static me.sat7.dynamicshop.utilities.ShopUtil.GetShopMaxPage;

public final class Shop extends InGameUI
{
    public Shop()
    {
        uiType = UI_TYPE.Shop;
    }

    private final int CLOSE = 45;
    private final int PAGE = 49;
    private final int SHOP_INFO = 53;

    private Player player;
    private String shopName;
    private int page;
    private int maxPage;
    FileConfiguration shopData;

    private int selectedSlot = -1;

    public Inventory getGui(Player player, String shopName, int page)
    {
        shopData = ShopUtil.shopConfigFiles.get(shopName).get();

        if (!JobsHook.jobsRebornActive && ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_JOBPOINT))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.JOBS_REBORN_NOT_FOUND"));
            return null;
        }
        if (!PlayerpointHook.isPPActive && ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_PLAYERPOINT))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.PLAYER_POINT_NOT_FOUND"));
            return null;
        }

        this.player = player;
        this.shopName = shopName;
        this.page = page;
        this.selectedSlot = -1;

        maxPage = GetShopMaxPage(shopName);
        this.page = Clamp(page,1,maxPage);

        UserUtil.userInteractItem.put(player.getUniqueId(), shopName + "/" + this.page);

        String uiName = shopData.getBoolean("Options.enable", true) ? "" : t(player, "SHOP.DISABLED");
        uiName += "§3" + shopData.getString("Options.title", shopName);
        inventory = Bukkit.createInventory(player, 54, uiName);

        CreateCloseButton(player, CLOSE);
        CreateButton(PAGE, InGameUI.GetPageButtonIconMat(), CreatePageButtonName(), CreatePageButtonLore(), this.page);
        CreateButton(SHOP_INFO, InGameUI.GetShopInfoButtonIconMat(), "§3" + shopName, CreateShopInfoText());

        ShowItems();

        return inventory;
    }

    @Override
    public void OnClickUpperInventory(InventoryClickEvent e)
    {
        player = (Player) e.getWhoClicked();

        if(!CheckShopIsEnable())
            return;

        if (e.getSlot() == CLOSE)
            CloseUI();
        else if (e.getSlot() == PAGE)
            OnClickPageButton(e.isLeftClick(), e.isRightClick(), e.isShiftClick());
        else if (e.getSlot() == SHOP_INFO && e.isRightClick())
            OnClickShopSettingsButton();
        else if (e.getSlot() <= 45)
        {
            int idx = e.getSlot() + (45 * (page - 1));
            OnClickItemSlot(idx, e);
        }
    }

    @Override
    public void OnClickLowerInventory(InventoryClickEvent e)
    {
        if(!CheckShopIsEnable())
            return;

        if(!ConfigUtil.GetEnableInventoryClickSearch_Shop())
            return;

        player = (Player) e.getWhoClicked();

        int idx = ShopUtil.findItemFromShop(shopName, e.getCurrentItem());
        if(idx != -1)
        {
            page = idx / 45 + 1;
            RefreshUI();
        }
    }

    private void ShowItems()
    {
        int idx = -1;
        for (String s : shopData.getKeys(false))
        {
            try
            {
                // 현재 페이지에 해당하는 것들만 출력
                idx = Integer.parseInt(s);
                idx -= ((page - 1) * 45);
                if (!(idx < 45 && idx >= 0)) continue;

                // 아이탬 생성
                String itemName = shopData.getString(s + ".mat"); // 메테리얼
                ItemStack itemStack = new ItemStack(Material.getMaterial(itemName), 1); // 아이탬 생성
                itemStack.setItemMeta((ItemMeta) shopData.get(s + ".itemStack")); // 저장된 메타 적용

                // 커스텀 메타 설정
                ItemMeta meta = itemStack.getItemMeta();
                String lore = "";

                // 상품
                if (shopData.contains(s + ".value"))
                {
                    int tradeIdx = Integer.parseInt(s);

                    lore = l("SHOP.ITEM_INFO");

                    int stock = shopData.getInt(s + ".stock");
                    int maxStock = shopData.getInt(s + ".maxStock", -1);
                    String stockStr;
                    String maxStockStr = "";

                    if (stock <= 0)
                    {
                        stockStr = t(player, "SHOP.INF_STOCK");
                    } else if (ConfigUtil.GetDisplayStockAsStack())
                    {
                        stockStr = t(player, "SHOP.STACKS").replace("{num}", n(stock / 64));
                    } else
                    {
                        stockStr = n(stock);
                    }

                    if (maxStock != -1)
                    {
                        if (ConfigUtil.GetDisplayStockAsStack())
                        {
                            maxStockStr = t(player, "SHOP.STACKS").replace("{num}", n(maxStock / 64));
                        }
                        else
                        {
                            maxStockStr = n(maxStock);
                        }
                    }

                    double buyPrice = Calc.getCurrentPrice(shopName, s, true);
                    double sellPrice = Calc.getCurrentPrice(shopName, s, false);

                    double buyPrice2 = shopData.getDouble(s + ".value");
                    if (shopData.contains("Options.flag.integeronly"))
                    {
                        buyPrice2 = Math.ceil(buyPrice2);
                    }

                    double priceSave1 = (buyPrice / buyPrice2) - 1;
                    double priceSave2 = 1 - (buyPrice / buyPrice2);

                    String valueChanged_Buy;
                    String valueChanged_Sell;

                    if (buyPrice - buyPrice2 > 0.005)
                    {
                        valueChanged_Buy = t(player, "ARROW.UP_2") + n(priceSave1 * 100) + "%";
                        valueChanged_Sell = t(player, "ARROW.UP") + n(priceSave1 * 100) + "%";
                    } else if (buyPrice - buyPrice2 < -0.005)
                    {
                        valueChanged_Buy = t(player, "ARROW.DOWN_2") + n(priceSave2 * 100) + "%";
                        valueChanged_Sell = t(player, "ARROW.DOWN") + n(priceSave2 * 100) + "%";
                    } else
                    {
                        valueChanged_Buy = "";
                        valueChanged_Sell = "";
                    }

                    String tradeType = "default";
                    if (shopData.contains(s + ".tradeType"))
                        tradeType = shopData.getString(s + ".tradeType");

                    boolean showValueChange = shopData.contains("Options.flag.showvaluechange");

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

                    String buyText = "";
                    String sellText = "";
                    if (!tradeType.equalsIgnoreCase("SellOnly"))
                    {
                        if(shopData.contains(s + ".discount"))
                        {
                            String original = n(buyPrice * 100 / (double) (100 - shopData.getInt(s + ".discount")),isIntTypeCurrency);
                            buyText = t(player, "SHOP.BUY_PRICE_DISCOUNTED" + currencyKey).replace("{num}", original).replace("{num2}", n(buyPrice,isIntTypeCurrency));
                        }
                        else
                        {
                            buyText = t(player, "SHOP.BUY_PRICE" + currencyKey).replace("{num}", n(buyPrice,isIntTypeCurrency));
                        }
                        buyText += showValueChange ? " " + valueChanged_Buy : "";
                    }

                    if (!tradeType.equalsIgnoreCase("BuyOnly"))
                    {
                        if(shopData.contains(s + ".discount"))
                        {
                            String original = n(sellPrice * 100 / (double) (100 - shopData.getInt(s + ".discount")),isIntTypeCurrency);
                            sellText = t(player, "SHOP.SELL_PRICE_DISCOUNTED" + currencyKey).replace("{num}", original).replace("{num2}", n(sellPrice,isIntTypeCurrency));
                        }
                        else
                        {
                            sellText = t(player, "SHOP.SELL_PRICE" + currencyKey).replace("{num}", n(sellPrice,isIntTypeCurrency));
                        }

                        sellText += showValueChange ? " " + valueChanged_Sell : "";
                    }

                    String pricingTypeText = "";
                    if (shopData.getInt(s + ".stock") <= 0 || shopData.getInt(s + ".median") <= 0)
                    {
                        if (!shopData.contains("Options.flag.hidepricingtype"))
                        {
                            pricingTypeText = t(player, "SHOP.STATIC_PRICE");
                        }
                    }

                    String stockText = "";
                    if (!shopData.contains("Options.flag.hidestock"))
                    {
                        if (maxStock != -1 && shopData.contains("Options.flag.showmaxstock"))
                            stockText = t(player, "SHOP.STOCK_2").replace("{stock}", stockStr).replace("{max_stock}", maxStockStr);
                        else
                            stockText = t(player, "SHOP.STOCK").replace("{num}", stockStr);
                    }

                    int sellLimitLeft = UserUtil.GetTradingLimitLeft(player, shopName, tradeIdx, HashUtil.GetItemHash(itemStack), true);
                    if (sellLimitLeft != Integer.MAX_VALUE)
                    {
                        if (!stockText.isEmpty())
                            stockText += "\n";
                        stockText += t(player, "SHOP.TRADE_LIMIT_SELL").replace("{num}", String.valueOf(sellLimitLeft));
                    }
                    int buyLimitLeft = UserUtil.GetTradingLimitLeft(player, shopName, tradeIdx, HashUtil.GetItemHash(itemStack), false);
                    if (buyLimitLeft != Integer.MAX_VALUE)
                    {
                        if (!stockText.isEmpty())
                            stockText += "\n";
                        stockText += t(player, "SHOP.TRADE_LIMIT_BUY").replace("{num}", String.valueOf(buyLimitLeft));
                    }

                    if (sellLimitLeft != Integer.MAX_VALUE || buyLimitLeft != Integer.MAX_VALUE)
                    {
                        String tradeLimitResetTime = ShopUtil.GetTradeLimitNextResetTime(shopName, tradeIdx);
                        stockText += "\n" + t(player, "SHOP.TRADE_LIMIT_TIMER").replace("{time}", tradeLimitResetTime);
                    }

                    String tradeLoreText = "";
                    if (t(player, "SHOP.TRADE_LORE").length() > 0)
                        tradeLoreText = t(player, "SHOP.TRADE_LORE");

                    StringBuilder itemMetaLoreText = new StringBuilder();
                    if(meta != null && meta.hasLore())
                    {
                        for(String tempLore : meta.getLore())
                        {
                            itemMetaLoreText.append(tempLore).append("\n");
                        }
                        itemMetaLoreText = new StringBuilder(itemMetaLoreText.substring(0, itemMetaLoreText.length() - 1));
                    }

                    lore = lore.replace("{\\nBuy}", buyText.isEmpty() ? "" : "\n" + buyText);
                    lore = lore.replace("{\\nSell}", sellText.isEmpty() ? "" : "\n" + sellText);
                    lore = lore.replace("{\\nStock}", stockText.isEmpty() ? "" : "\n" + stockText);
                    lore = lore.replace("{\\nPricingType}", pricingTypeText.isEmpty() ? "" : "\n" + pricingTypeText);
                    lore = lore.replace("{\\nTradeLore}", tradeLoreText.isEmpty() ? "" : "\n" + tradeLoreText);
                    lore = lore.replace("{\\nItemMetaLore}", (itemMetaLoreText.length() == 0) ? "" : "\n" + itemMetaLoreText);

                    lore = lore.replace("{Buy}", buyText);
                    lore = lore.replace("{Sell}", sellText);
                    lore = lore.replace("{Stock}", stockText);
                    lore = lore.replace("{PricingType}", pricingTypeText);
                    lore = lore.replace("{TradeLore}", tradeLoreText);
                    lore = lore.replace("{ItemMetaLore}", itemMetaLoreText.toString());

                    String temp = lore.replace(" ","");
                    if(ChatColor.stripColor(temp).startsWith("\n"))
                        lore = lore.replaceFirst("\n","");

                    if (player.hasPermission(P_ADMIN_SHOP_EDIT))
                    {
                        lore += "\n" + t(player, "SHOP.ITEM_MOVE_LORE");
                        lore += "\n" + t(player, "SHOP.ITEM_EDIT_LORE");
                    }
                }
                // 장식용
                else
                {
                    if (player.hasPermission(P_ADMIN_SHOP_EDIT))
                    {
                        lore += t(player, "SHOP.ITEM_COPY_LORE");
                        lore += "\n" + t(player, "SHOP.DECO_DELETE_LORE");
                    }

                    meta.setDisplayName(" ");
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                }

                meta.setLore(new ArrayList<>(Arrays.asList(lore.split("\n"))));
                itemStack.setItemMeta(meta);
                inventory.setItem(idx, itemStack);
            } catch (Exception e)
            {
                if (!s.equalsIgnoreCase("Options") && player.hasPermission(P_ADMIN_SHOP_EDIT) && idx != -1)
                {
                    CreateButton(idx, Material.BARRIER, t(player, "SHOP.INCOMPLETE_DATA"), t(null, "SHOP.INCOMPLETE_DATA_Lore") + idx);

                    if(DynamicShop.DEBUG_MODE)
                    {
                        for (StackTraceElement stackTraceElement : e.getStackTrace())
                        {
                            DynamicShop.console.sendMessage(stackTraceElement.toString());
                        }
                    }
                }
            }
        }
    }

    private String CreatePageButtonName()
    {
        String pageString = t(player, "SHOP.PAGE_TITLE");
        pageString = pageString.replace("{curPage}", String.valueOf(page));
        pageString = pageString.replace("{maxPage}", String.valueOf(maxPage));
        return pageString;
    }

    private String CreatePageButtonLore()
    {
        String pageLore = t(player, "SHOP.PAGE_LORE_V2");
        if (player.hasPermission(P_ADMIN_SHOP_EDIT))
        {
            pageLore += "\n" + t(player, "SHOP.GO_TO_PAGE_EDITOR");
        }
        return pageLore;
    }

    private String CreateShopInfoText()
    {
        String shopLore = l("SHOP.INFO");

        StringBuilder finalLoreText = new StringBuilder();
        if (shopData.contains("Options.lore"))
        {
            String loreTxt = shopData.getString("Options.lore");
            if (loreTxt != null && loreTxt.length() > 0)
            {
                String[] loreArray = loreTxt.split(Pattern.quote("\\n"));
                for (String s : loreArray)
                {
                    finalLoreText.append("§f").append(s).append("\n");
                }
            }
        }

        // 권한
        String finalPermText = "";
        String perm = shopData.getString("Options.permission");
        if (perm != null && perm.length() > 0)
        {
            finalPermText += t(player, "SHOP.PERMISSION") + "\n";
            finalPermText += t(player, "SHOP.PERMISSION_ITEM").replace("{permission}", perm) + "\n";
        }

        // 세금
        String finalTaxText = "";
        finalTaxText += t(player, "TAX.SALES_TAX") + ":" + "\n";
        finalTaxText += t(player, "SHOP.SHOP_INFO_DASH") + Calc.getTaxRate(shopName) + "%" + "\n";

        // 상점 잔액
        String finalShopBalanceText = "";

        if(!shopData.contains("Options.flag.hideshopbalance"))
        {
            finalShopBalanceText += t(player, "SHOP.SHOP_BAL") + "\n";
            if (ShopUtil.getShopBalance(shopName) >= 0)
            {
                String temp;
                if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_JOBPOINT))
                    temp = n(ShopUtil.getShopBalance(shopName)) + t(player,"JOB_POINTS");
                else if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_PLAYERPOINT))
                    temp = n(ShopUtil.getShopBalance(shopName), true) + t(player,"PLAYER_POINTS");
                else if (ShopUtil.GetCurrency(shopData).equalsIgnoreCase(Constants.S_EXP))
                    temp = n(ShopUtil.getShopBalance(shopName), true) + t(player,"EXP_POINTS");
                else
                    temp = n(ShopUtil.getShopBalance(shopName));

                finalShopBalanceText += t(player, "SHOP.SHOP_INFO_DASH") + temp + "\n";
            } else
            {
                finalShopBalanceText += t(player, "SHOP.SHOP_INFO_DASH") + ChatColor.stripColor(t(player, "SHOP.SHOP_BAL_INF")) + "\n";
            }
        }

        // 영업시간
        String finalShopHourText = "";
        if (shopData.contains("Options.shophours"))
        {
            String[] temp = shopData.getString("Options.shophours").split("~");
            int open = Integer.parseInt(temp[0]);
            int close = Integer.parseInt(temp[1]);

            finalShopHourText += t(player, "TIME.SHOPHOURS") + "\n";
            finalShopHourText += t(player, "SHOP.SHOP_INFO_DASH") + t(player, "TIME.OPEN") + ": " + open + "\n";
            finalShopHourText += t(player, "SHOP.SHOP_INFO_DASH") + t(player, "TIME.CLOSE") + ": " + close + "\n";
        }

        // 상점 좌표
        String finalShopPosText = "";
        if (shopData.contains("Options.pos1") && shopData.contains("Options.pos2"))
        {
            finalShopPosText += t(player, "SHOP.SHOP_LOCATION_B") + "\n";
            finalShopPosText += t(player, "SHOP.SHOP_INFO_DASH") + shopData.getString("Options.world") + "\n";
            finalShopPosText += t(player, "SHOP.SHOP_INFO_DASH") + shopData.getString("Options.pos1") + "\n";
            finalShopPosText += t(player, "SHOP.SHOP_INFO_DASH") + shopData.getString("Options.pos2") + "\n";
        }

        shopLore = shopLore.replace("{\\nShopLore}", finalLoreText.toString().isEmpty() ? "" : "\n" + finalLoreText);
        shopLore = shopLore.replace("{\\nPermission}", finalPermText.isEmpty() ? "" : "\n" + finalPermText);
        shopLore = shopLore.replace("{\\nTax}", "\n" + finalTaxText);
        shopLore = shopLore.replace("{\\nShopBalance}", finalShopBalanceText.isEmpty() ? "" : "\n" + finalShopBalanceText);
        shopLore = shopLore.replace("{\\nShopHour}", finalShopHourText.isEmpty() ? "" : "\n" + finalShopHourText);
        shopLore = shopLore.replace("{\\nShopPosition}", finalShopPosText.isEmpty() ? "" : "\n" + finalShopPosText);

        shopLore = shopLore.replace("{ShopLore}", finalLoreText);
        shopLore = shopLore.replace("{Permission}", finalPermText);
        shopLore = shopLore.replace("{Tax}", finalTaxText);
        shopLore = shopLore.replace("{ShopBalance}", finalShopBalanceText);
        shopLore = shopLore.replace("{ShopHour}", finalShopHourText);
        shopLore = shopLore.replace("{ShopPosition}", finalShopPosText);

        String temp = shopLore.replace(" ","");
        if(ChatColor.stripColor(temp).startsWith("\n"))
            shopLore = shopLore.replaceFirst("\n","");

        // 어드민이면----------
        if (player.hasPermission(P_ADMIN_SHOP_EDIT))
            shopLore += "\n";

        // 플래그
        StringBuilder finalFlagText = new StringBuilder();
        if (player.hasPermission(P_ADMIN_SHOP_EDIT))
        {
            if (shopData.contains("Options.flag") && shopData.getConfigurationSection("Options.flag").getKeys(false).size() > 0)
            {
                finalFlagText = new StringBuilder(t(player, "SHOP.FLAGS") + "\n");
                for (String s : shopData.getConfigurationSection("Options.flag").getKeys(false))
                {
                    finalFlagText.append(t(player, "SHOP.FLAGS_ITEM").replace("{flag}", s)).append("\n");
                }
                finalFlagText.append("\n");
            }
        }
        shopLore += finalFlagText;

        if (player.hasPermission(P_ADMIN_SHOP_EDIT))
        {
            shopLore += t(player, "SHOP_SETTING.SHOP_SETTINGS_LORE");
        }

        return shopLore;
    }

    private void CloseUI()
    {
        // 표지판으로 접근한 경우에는 그냥 창을 닫음
        if (UserUtil.userTempData.get(player.getUniqueId()).equalsIgnoreCase("sign"))
        {
            UserUtil.userTempData.put(player.getUniqueId(), "");
            player.closeInventory();
        }
        else
        {
            if (ConfigUtil.GetOpenStartPageWhenClickCloseButton())
            {
                DynaShopAPI.openStartPage(player);
            } else
            {
                ShopUtil.closeInventoryWithDelay(player);
            }
        }
    }

    private void OnClickPageButton(boolean isLeftClick, boolean isRightClick, boolean isShiftClick)
    {
        int targetPage = page;
        if (isLeftClick)
        {
            targetPage -= 1;
            if (targetPage < 1)
                targetPage = GetShopMaxPage(shopName);
        } else if (isRightClick)
        {
            if (!isShiftClick)
            {
                targetPage += 1;
                if (targetPage > GetShopMaxPage(shopName))
                    targetPage = 1;
            } else
            {
                if (player.hasPermission(P_ADMIN_SHOP_EDIT))
                {
                    ShopUtil.SortShopData(shopName);
                    DynaShopAPI.openPageEditor(player, shopName, page);
                    return;
                }
            }
        }
        page = targetPage;
        RefreshUI();
    }

    private void OnClickShopSettingsButton()
    {
        if(player.hasPermission(P_ADMIN_SHOP_EDIT))
        {
            DynaShopAPI.openShopSettingGui(player, shopName);
        }
    }

    private void OnClickItemSlot(int idx, InventoryClickEvent e)
    {
        if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR)
        {
            if (e.getCurrentItem().getItemMeta() != null &&
                    e.getCurrentItem().getItemMeta().getDisplayName().equals(t(null, "SHOP.INCOMPLETE_DATA")))
            {
                return;
            }

            // 거래화면 열기
            if (e.isLeftClick() && shopData.contains(idx + ".value"))
            {
                SoundUtil.playerSoundEffect(player, "tradeview");
                DynaShopAPI.openItemTradeGui(player, shopName, String.valueOf(idx));
            }
            // 아이탬 이동, 수정, 또는 장식탬 삭제
            else if (e.isRightClick() && player.hasPermission(P_ADMIN_SHOP_EDIT))
            {
                if (e.isShiftClick())
                {
                    if (shopData.contains(idx + ".value"))
                    {
                        double buyValue = shopData.getDouble(idx + ".value");
                        double sellValue = buyValue;
                        if (shopData.contains(idx + ".value2"))
                        {
                            sellValue = shopData.getDouble(idx + ".value2");
                        }
                        double valueMin = shopData.getDouble(idx + ".valueMin");
                        if (valueMin <= 0.0001) valueMin = 0.0001;
                        double valueMax = shopData.getDouble(idx + ".valueMax");
                        if (valueMax <= 0) valueMax = -1;
                        int median = shopData.getInt(idx + ".median");
                        int stock = shopData.getInt(idx + ".stock");
                        int maxStock = shopData.getInt(idx + ".maxStock", -1);
                        int discount = shopData.getInt(idx + ".discount", 0);
                        int sellLimit = shopData.getInt(idx + ".tradeLimitPerPlayer.sell", 0);
                        int buyLimit = shopData.getInt(idx + ".tradeLimitPerPlayer.buy", 0);
                        long tradeLimitInterval = shopData.getLong(idx + ".tradeLimitPerPlayer.interval", MathUtil.dayInMilliSeconds);
                        long tradeLimitNextTimer = shopData.getLong(idx + ".tradeLimitPerPlayer.nextTimer");

                        ItemStack iStack = new ItemStack(e.getCurrentItem().getType());
                        iStack.setItemMeta((ItemMeta) shopData.get(idx + ".itemStack"));

                        DSItem dsItem = new DSItem(iStack, buyValue, sellValue, valueMin, valueMax, median, stock, maxStock, discount,
                                                   sellLimit, buyLimit, tradeLimitInterval, tradeLimitNextTimer);
                        DynaShopAPI.openItemSettingGui(player, shopName, idx, 0, dsItem);
                    } else
                    {
                        ShopUtil.removeItemFromShop(shopName, idx);
                        selectedSlot = -1;
                        RefreshUI();
                    }
                } else if (selectedSlot == -1)
                {
                    player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "SHOP.ITEM_MOVE_SELECTED"));
                    selectedSlot = idx;
                }
            }
        }
        else if (player.hasPermission(P_ADMIN_SHOP_EDIT))
        {
            // 아이탬 이동. 또는 장식 복사
            if (e.isRightClick() && selectedSlot != -1)
            {
                shopData.set(String.valueOf(idx), shopData.get(String.valueOf(selectedSlot)));

                if (shopData.contains(selectedSlot + ".value"))
                {
                    shopData.set(String.valueOf(selectedSlot), null);
                }

                ShopUtil.shopConfigFiles.get(shopName).save();
                selectedSlot = -1;
                RefreshUI();
            }
            // 팔렛트 열기
            else
            {
                DynaShopAPI.openItemPalette(player, 0, shopName, idx, 1, "");
            }
        }
    }

    @Override
    public void RefreshUI()
    {
        if(!CheckShopIsEnable())
            return;

        for (int i = 0; i < 45; i++)
            inventory.setItem(i, null);

        ItemStack pageButton = inventory.getItem(PAGE);
        ItemMeta pageButtonMeta = pageButton.getItemMeta();
        pageButtonMeta.setDisplayName(CreatePageButtonName());
        pageButton.setItemMeta(pageButtonMeta);
        pageButton.setAmount(page);

        ItemStack infoButton = inventory.getItem(SHOP_INFO);
        ItemMeta infoMeta = infoButton.getItemMeta();
        infoMeta.setLore(new ArrayList<>(Arrays.asList(CreateShopInfoText().split("\n"))));
        infoButton.setItemMeta(infoMeta);

        ShowItems();
    }

    public boolean CheckShopIsEnable()
    {
        if (!ShopUtil.shopConfigFiles.containsKey(shopName))
        {
            player.sendMessage(DynamicShop.dsPrefix(player) + t(player, "ERR.INVALID_TRANSACTION"));
            player.closeInventory();
            return false;
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
