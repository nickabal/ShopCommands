package us.accretion.commandshop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;

public class PCommands {

	private static CommandShop plugin;
    private static Float transactionTotal;
    
	public static void init(CommandShop pl){
		plugin = pl;
		List<String> aliases = new ArrayList<String>();
		aliases.add("commandshop");
		if (plugin.cfgs.enableAliases()) {
		  aliases.add("shop");
		}
		Sponge.getCommandManager().register(plugin, CommandShop(), aliases);
	}
	
	private static CommandCallable CommandShop() {
	CommandSpec help = CommandSpec.builder()
			.description(Text.of("Help command for CommandShop."))
		    .executor((src, args) -> { {	
		    	sendHelp(src);
		    	return CommandResult.success();	
		    }})
		    .build();
	
	CommandSpec reload = CommandSpec.builder()
			.description(Text.of("Reload CommandShop."))
			.permission("commandshop.reload")
		    .executor((src, args) -> { {	
		    	plugin.reload();
		    	src.sendMessage(Text.of("§aCommandShop reloaded!"));
		    	return CommandResult.success();	
		    }})
		    .build();
	
	CommandSpec buy = CommandSpec.builder()
			.description(Text.of("Buy items from server"))
			.permission("commandshop.buy")
			.arguments(
					GenericArguments.string(Text.of("item")),
					GenericArguments.optional(GenericArguments.string(Text.of("quantity"))))
		    .executor((src, args) -> { {
		    	if (src instanceof Player){
			      String itemName = args.getOne("item").get().toString();
			      if(!itemName.matches(".*:.*")) {
			    	  itemName = "minecraft:"+itemName;
			      }
			      Integer qty = 1; 
			      if(args.getOne("quantity").isPresent()){
			        qty = Integer.parseInt(args.getOne("quantity").get().toString());
			      }
			      
			      Float price = Float.NaN;
			      try { 
			    	price = plugin.cfgs.getBuyPrice(itemName)*qty;
			      } catch (NullPointerException e) {
			        src.sendMessage(Text.of("Item "+plugin.cfgs.displayName(itemName)+" not found in shop."));
			        return CommandResult.empty();
			      }
			      if (price<0) {
			        src.sendMessage(Text.of("This is the buy command, try /sell"));
			        return CommandResult.empty();
			      }
			      if (qty>64) {
			        src.sendMessage(Text.of("Try 64 items or less at a time."));
			        return CommandResult.empty();
			      }
		    	  src.sendMessage(Text.of("Buying "+qty+" "+plugin.cfgs.displayName(itemName)+" for $"+plugin.cfgs.displayPrice(price)));

		    	  Player player = (Player) src; 
		    	  ItemType itemType = null;
		    	  UniqueAccount acc = plugin.econ.getOrCreateAccount(player.getUniqueId()).get();
      			  TransactionResult result = acc.withdraw(plugin.econ.getDefaultCurrency(), BigDecimal.valueOf(price), Cause.of(EventContext.builder().build(), price));
				    if (result.getResult() == ResultType.SUCCESS) {
				    	  itemType = null;
				    	  if(Sponge.getRegistry().getType(ItemType.class, itemName).isPresent()) {
				    		  itemType = Sponge.getRegistry().getType(ItemType.class, itemName).get();
					    	  ItemStack toGive = ItemStack.builder().itemType(itemType).quantity(qty).build();
					    	  Collection<ItemStackSnapshot> rejectedItems = player.getInventory().offer(toGive).getRejectedItems();
					    	  for(Iterator<ItemStackSnapshot> rej_i = rejectedItems.iterator(); rej_i.hasNext();) {
					    		  ItemStackSnapshot iss = rej_i.next();
					    		  Integer rejCount = iss.getQuantity();
					    		  String rejName = iss.getType().getName();					    		  
					    		  Integer i = 0; 
						      	  src.sendMessage(Text.of("Inventory full!"));
					    		  while(i<rejCount) {
				                 	Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "give "+player.getName()+" "+rejName);
				                 	i++;
					    		  }
					    	  }
					    	  
				    	  }
	                 	  //Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "give "+player.getName()+" "+itemName+" "+qty);
				    } else if (result.getResult() == ResultType.ACCOUNT_NO_FUNDS) {
				      	  src.sendMessage(Text.of("Not enough funds?"));
				    } else {
				      	  src.sendMessage(Text.of("Transaction failed."));
				    }
		    	}

		    	return CommandResult.success();	
		    }})
		    .build();
	CommandSpec sell = CommandSpec.builder()
			.description(Text.of("Sell items to server"))
			.permission("commandshop.sell")
			.arguments(
					GenericArguments.optional(GenericArguments.string(Text.of("<stack|hand|all>"))))
		    .executor((src, args) -> { {	
		    	if (src instanceof Player){
			    	  Player player = (Player) src; 
			    	  HandType mainhand = HandTypes.MAIN_HAND;

			    	  String subcmd = null;
			    	  if(args.getOne("<stack|hand|all>").isPresent()){
			    	  subcmd = args.getOne("<stack|hand|all>").get().toString();
			    	  }
			    	  if(subcmd == null || subcmd.equalsIgnoreCase("stack")) {
				    	if(!player.getItemInHand(mainhand).isPresent()) {
				    	  src.sendMessage(Text.of("Put an item in your hand first or use /commandshop sell all"));
				          return CommandResult.empty();
				        }
			    	    ItemStack inHand = player.getItemInHand(mainhand).get();
				    	boolean res = sellItemStack(player, inHand);
				    	if(res) {
				    		player.setItemInHand(HandTypes.MAIN_HAND,null);
				    	}
			    	  } else if (subcmd.equalsIgnoreCase("all") || subcmd.equalsIgnoreCase("hand")) {
			    		    transactionTotal = Float.valueOf(0);
					    	String inHandName = null;
					    	if(!player.getItemInHand(mainhand).isPresent() && subcmd.equalsIgnoreCase("hand")) {
						    	  src.sendMessage(Text.of("Put an item in your hand first or use /commandshop sell all"));
						          return CommandResult.empty();
						        } else if (subcmd.equalsIgnoreCase("hand")) {
					    	      inHandName = player.getItemInHand(mainhand).get().getType().getName();
					    	    }
			    		    for(Iterator<Inventory> slots = player.getInventory().slots().iterator(); slots.hasNext(); ) {
			    		    	Inventory slot = slots.next();
			    		    	Optional<ItemStack> slotStack = slot.peek();
			    		    	if(slotStack.isPresent()) {
			    		    	  ItemStack slotStackPresent = slot.peek().get();
			    		          if(subcmd.equalsIgnoreCase("hand") && !slotStackPresent.getType().getName().equalsIgnoreCase(inHandName)) {
			    		        	  continue;
			    		          } else {
   					    	        boolean res = sellItemStack(player, slotStackPresent);
   					    	        if(res){ 
   					    	    	  slot.poll();
   					    	        }
			    		          }
			    		    	}
			    		    }
				    	    src.sendMessage(Text.of("Sold inventory for: $"+plugin.cfgs.displayPrice(transactionTotal)));
			    	  } else if (subcmd.equalsIgnoreCase("one")) {
				    	    src.sendMessage(Text.of("one"));
			    	  } else {
				    	src.sendMessage(Text.of("stack: sell just your hand (optional, the default)"));
			    	    src.sendMessage(Text.of("hand: sell all items matching your hand"));
			    	    src.sendMessage(Text.of("all: try to sell all."));
			    	  }
		    	}
		    	return CommandResult.success();	
		    }})
		    .build();
	//TODO implement offer/auction command if you like	
	
	CommandSpec search = CommandSpec.builder()
			.description(Text.of("Search for item names."))
			.permission("commandshop.search")
			.arguments(
					GenericArguments.string(Text.of("search")))
		    .executor((src, args) -> { {	
		    	String srch = args.getOne("search").get().toString();
			    src.sendMessage(Text.of("-------- Search results: --------"));
				for ( Iterator<?> i = plugin.cfgs.configs().getNode("item-prices").getChildrenMap().keySet().iterator(); i.hasNext(); ) {
					Object item = i.next();
					     String itemName = plugin.cfgs.displayName(item.toString());
					     if (itemName.matches(".*"+srch+".*")) {
					       src.sendMessage(Text.of(itemName)); //TODO: include item price
					     }
				}
				src.sendMessage(Text.of("------------------------------"));
	    	
		    	return CommandResult.success();	
		    }})
		    .build();
	
	CommandSpec commandshop = CommandSpec.builder()
		    .description(Text.of("Main command for CommandShop."))
		    .executor((src, args) -> { {	    	
		    	src.sendMessage(Text.of("------------------ "+plugin.get().getName()+" "+plugin.get().getVersion().get()+" -----------------"));
		    	src.sendMessage(Text.of("Developed by " + plugin.get().getAuthors()));
		    	src.sendMessage(Text.of("For more information about the commands, try /commandshop help"));
		    	src.sendMessage(Text.of("---------------------------------------------------"));
		    	return CommandResult.success();	
		    }})
		    .child(help, "?", "help")
		    .child(buy, "buy")
		    .child(search, "search")
		    .child(sell, "sell")
		    .child(reload, "reload")
		    .build();
	
	return commandshop;
    }
	
	private static boolean sellItemStack(Player player, ItemStack is) {
		String itemName = is.getType().getName();
	    Integer qty = is.getQuantity();
        Float price = Float.NaN;
        try {
      	  price = plugin.cfgs.getSellPrice(itemName)*qty;
        } catch (NullPointerException e) {
          player.sendMessage(Text.of("Item "+plugin.cfgs.displayName(itemName)+" not purchased by server shop."));
          return false;
        }
	    UniqueAccount acc = plugin.econ.getOrCreateAccount(player.getUniqueId()).get();
		TransactionResult result = acc.deposit(plugin.econ.getDefaultCurrency(), BigDecimal.valueOf(price), Cause.of(EventContext.builder().build(), price));
	    if (result.getResult() == ResultType.SUCCESS) {
	    	  player.sendMessage(Text.of("Selling: "+qty+" "+plugin.cfgs.displayName(itemName)+" for $"+plugin.cfgs.displayPrice(price)));
	    	  transactionTotal += price;
	    	  return true;
	    } else if (result.getResult() == ResultType.FAILED) {
	      	  player.sendMessage(Text.of("Transaction failed."));
	      	  return false;
	    } else {
	      	  player.sendMessage(Text.of("Transaction failed..."));
	      	  return false;
	    }
		
	}
	
	private static void sendHelp(CommandSource source){
		source.sendMessage(Text.of("CommandShop commands:"));
		Map<String, String> helpCommands = new HashMap<String, String>();
		helpCommands.put("help", "Displays this help");
		helpCommands.put("buy", "Buy items from server");
		helpCommands.put("search", "Search the store for item names.");
		helpCommands.put("sell", "Sell items to server");
		helpCommands.put("reload", "Reloads commandshop.conf");

	for (Iterator<Map.Entry<String, String>> i = helpCommands.entrySet().iterator(); i.hasNext();) {
		Map.Entry<String, String> cmd = i.next();
		
             String key = cmd.getKey();
             String value = cmd.getValue();
	         
			if (source.hasPermission("commandshop."+key)) {
				 //source.sendMessage(Text.of("Has self permission"));
		         source.sendMessage(Text.of("/commandshop "+key+ "  "+value));
			}/* else
			if (source.hasPermission("commandshop."+key+".others")){
				 //source.sendMessage(Text.of("Has others permission"));
		         source.sendMessage(Text.of("/commandshop "+key+ " <player>  "+value));
			} */

		}
	}	
}
