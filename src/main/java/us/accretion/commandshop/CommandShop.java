package us.accretion.commandshop;

import java.io.File;
import java.nio.file.Path;


import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;

import com.google.inject.Inject;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id="commandshop", 
name="CommandShop", 
version="0.1.1",
authors="RioS2", 
description="Buy and Sell with commands")
public class CommandShop {
	
	public Game game;
	public EconomyService econ;
	public PConfig cfgs;

	private PluginContainer instance;
	public PluginContainer get(){
		return this.instance;
	}
	
	@Inject private Logger logger;
	public Logger getLogger(){	
		return logger;
	}
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDir;

	@Inject
	@DefaultConfig(sharedRoot = false)
	private File defConfig;
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;	
	public ConfigurationLoader<CommentedConfigurationNode> getCfManager(){
		return configManager;
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
    	game = Sponge.getGame();
    	instance = Sponge.getPluginManager().getPlugin("commandshop").get();
    	
    	Logger.init(this);
    	Logger.info("Logger initialized...");
    	
    	Logger.info("Init config module...");
    	configManager = HoconConfigurationLoader.builder().setFile(defConfig).build();	
        cfgs = new PConfig(this, configDir, defConfig);
        
        Logger.info("Init commands module...");
        PCommands.init(this);
        
        Logger.success("CommandShop enabled.");

    }
	
	@Listener
	public void onStopServer(GameStoppingServerEvent event) {
        Logger.info("Stopping Plugin...");
        //for (Task task:Sponge.getScheduler().getScheduledTasks(this)){
        //	task.cancel();
        //}
        //Logger.info("Saved config.");
        Logger.severe("CommandShop disabled.");
    }
	
	public void reload(){
		//for (Task task:Sponge.getScheduler().getScheduledTasks(this)){
		//	task.cancel();
		//}
		//cfgs.savePlayersStats();
		cfgs = new PConfig(this, configDir, defConfig);
		//AutoSaveHandler();
        //CheckPoolHandler();
	}
	
	@Listener
	public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
		if (event.getService().equals(EconomyService.class)) {
            econ = (EconomyService) event.getNewProviderRegistration().getProvider();
		}
	}
}

class Logger{	
	private static CommandShop plugin;

	public static void init(CommandShop pl){
		plugin = pl;
	}
	
	public static void success(String s) {
		Sponge.getServer().getConsole().sendMessage(Text.of("CommandShop: [§a§l"+s+"§r]"));
    }
	
    public static void info(String s) {
    	Sponge.getServer().getConsole().sendMessage(Text.of("CommandShop: ["+s+"]"));
    }
    
    public static void warning(String s) {
    	Sponge.getServer().getConsole().sendMessage(Text.of("CommandShop: [§6"+s+"§r]"));
    }
    
    public static void severe(String s) {
    	Sponge.getServer().getConsole().sendMessage(Text.of("CommandShop: [§c§l"+s+"§r]"));
    }
    
    public static void log(String s) {
    	Sponge.getServer().getConsole().sendMessage(Text.of("CommandShop: ["+s+"]"));
    }
    
    public static void debug(String s) {
        if (plugin.cfgs.getBool("debug-messages")) {
        	Sponge.getServer().getConsole().sendMessage(Text.of("CommandShop: [§b"+s+"§r]"));
        }  
    }
}
