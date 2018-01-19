package us.accretion.commandshop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;


public class PConfig{

	private CommentedConfigurationNode config;
	public CommentedConfigurationNode configs(){
		return config;
	}
	
	CommandShop plugin;

	public PConfig(CommandShop plugin, Path configDir, File defConfig) {
		this.plugin = plugin; 
		try {
			Files.createDirectories(configDir);
			if (!defConfig.exists()){
				Logger.info("Creating config file...");
				defConfig.createNewFile();
			}
			config = plugin.getCfManager().load();
			
			config.getNode("enable-short-aliases").setValue(config.getNode("enable-short-aliases").getBoolean(true))
			.setComment("Turn on command /shop, short for /commandshop");
		    
			config.getNode("debug-messages").setValue(config.getNode("debug-messages").getBoolean(false))
			.setComment("Enable debug messages?");
			
			config.getNode("use-uuids-instead-names").setValue(config.getNode("use-uuids-instead-names").getBoolean(true))
			.setComment("Use uuids to store players stats on playerstats.conf?");
			
			
			config.getNode("buy-sell-ratio").setValue(config.getNode("buy-sell-ratio").getDouble(0.2))
			.setComment("Set buying and selling ratio:");

			config.getNode("item-prices").setValue(config.getNode("item-prices").getChildrenMap())
			.setComment("Item pricing:\n item-prices { \n \"minecraft:cake\"=32 \n \"minecraft:carrot\"=5 \n \"minecraft:cookie\"=7 \n }");
			
			save();        			
			Logger.info("All configurations loaded!");
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
    public void save(){
    	try {
			plugin.getCfManager().save(config);
		} catch (IOException e) {
			Logger.severe("Problems during save file:");
			e.printStackTrace();
		}
    }

	public Boolean getBool(Object... key){		
		return config.getNode(key).getBoolean();
	}
	public Float getFloat(Object... key){		
		return config.getNode(key).getFloat();
	}
    public boolean useUUIDs() {
    	return getBool("use-uuids-instead-names");
    }
    public boolean enableAliases() {
    	return getBool("enable-short-aliases");
    }
	public Float getPrice(String itemKey){
		return Float.valueOf(config.getNode("item-prices").getChildrenMap().get(itemKey).getValue().toString());
	}
	public Float getBuyPrice(String itemKey) {
		return getPrice(itemKey);
	}
	public Float getSellPrice(String itemKey) {
		return Float.valueOf((getPrice(itemKey) * plugin.cfgs.getFloat("buy-sell-ratio")));
	}
	public Float displayPrice(Float price) {
		return Float.valueOf((float) (Math.round(price*100.0) / 100.0));
	}
	public String displayName(String name) {
		return name.split(":")[1];
	}
	public void setSellPrice() {
	  
	}
}