package me.sat7.dynamicshop.files;

import me.sat7.dynamicshop.DynamicShop;
import me.sat7.dynamicshop.constants.Constants;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CustomConfig
{
    private File file;
    protected FileConfiguration customFile; // 버킷의 데이터 타입

    //Finds or generates the custom config file
    public void setup(String name, String folder)
    {
        String path = name + ".yml";
        if (folder != null)
        {
            path = folder + "/" + path;

            File directory = new File(DynamicShop.plugin.getDataFolder() + "/" + folder);
            if (!directory.exists())
            {
                directory.mkdir();
            }
        }

        file = new File(DynamicShop.plugin.getDataFolder(), path);

        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            } catch (IOException e)
            {
                DynamicShop.console.sendMessage("Fatal error! Config Setup Fail. File name: " + name);
                //DynamicShop.console.sendMessage(e.toString());
            }
        }

        customFile = YamlConfiguration.loadConfiguration(file);
    }

    public boolean open(String name, String folder)
    {
        file = new File(DynamicShop.plugin.getDataFolder(), folder + "/" + name + ".yml");

        if (!file.exists())
        {
            DynamicShop.console.sendMessage(Constants.DYNAMIC_SHOP_PREFIX + name + " not found");
            return false;
        }

        customFile = YamlConfiguration.loadConfiguration(file);
        return true;
    }

    public FileConfiguration get()
    {
        return customFile;
    }

    public void save()
    {
        try
        {
            customFile.save(file);
        } catch (IOException e)
        {
            DynamicShop.console.sendMessage("Couldn't save file :" + e);
        }
    }

    public void rename(String newName)
    {
        if (!file.exists())
            return;

        try
        {
            File newFile = new File(file.toPath().resolveSibling(newName) + ".yml");
            Files.copy(file.toPath(), newFile.toPath());
            file.delete();
            file = newFile;
        }
        catch (Exception e)
        {
            System.out.println("rename fail :" + e);
        }
    }

    public void copy(String newName)
    {
        if (!file.exists())
            return;

        try
        {
            File newFile = new File(file.toPath().resolveSibling(newName) + ".yml");
            Files.copy(file.toPath(), newFile.toPath());
        }
        catch (Exception e)
        {
            System.out.println("copy fail :" + e);
        }
    }

    public void delete()
    {
        if (file.exists())
        {
            if (!file.delete())
                System.out.println("file delete fail: " + file.getName());
        } else
        {
            System.out.println("file delete fail: not exist");
        }
    }

    public void reload()
    {
        customFile = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration GetFileFromPath(String name, String folder)
    {
        File tempFile = new File(Bukkit.getServer().getPluginManager().getPlugin("DynamicShop").getDataFolder(), folder + "/" + name + ".yml");

        if (!tempFile.exists())
        {
            DynamicShop.console.sendMessage(Constants.DYNAMIC_SHOP_PREFIX + name + " not found");
            return null;
        }

        return YamlConfiguration.loadConfiguration(tempFile);
    }

    public String GetFileName()
    {
        return file.getName();
    }

    public String GetFileNameWithoutExtension()
    {
        return FilenameUtils.getBaseName(file.getName());
    }
}
