package org.inventivetalent.blockshrink;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BlockShrink extends JavaPlugin implements Listener {

	public static BlockShrink     instance;
	public static String          prefix = "§e[BlockShrink]§r ";
	public static WorldEditPlugin worldEdit;

	@Override
	public void onEnable() {
		instance = this;

		if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit") || (worldEdit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit")) == null) {
			this.getLogger().severe("****************************************");
			this.getLogger().severe("");
			this.getLogger().severe("    This plugin depends on WorldEdit    ");
			this.getLogger().severe("         Please download it here        ");
			this.getLogger().severe("http://dev.bukkit.org/bukkit-plugins/worldedit/");
			this.getLogger().severe("");
			this.getLogger().severe("****************************************");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		Bukkit.getPluginManager().registerEvents(this, this);

		new Metrics(this);

	}

	@Override
	public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("blockshrink")) {
			if (args.length == 0) {
				sender.sendMessage(prefix + "§b/bs shrink <Size>");
				sender.sendMessage(prefix + "§b/bs clear");
				sender.sendMessage(prefix + "§b/bs move <x> <y> <z>");

				return true;
			}
			if (!(sender instanceof Player)) {
				sender.sendMessage(prefix + "§cYou must be a player");
				return false;
			}
			final Player p = (Player) sender;
			if ("shrink".equalsIgnoreCase(args[0])) {
				if (!sender.hasPermission("blockshrink.command.shrink")) {
					sender.sendMessage(prefix + "§cNo permission");
					return false;
				}
				if (args.length == 1) {
					sender.sendMessage(prefix + "§c/bs shrink <Size>");
					String sizes = prefix + "§bSizes: §a";
					for (BlockSize size : BlockSize.values()) {
						sizes += size.name() + ", ";
					}
					sizes = sizes.substring(0, sizes.length() - 2);
					sender.sendMessage(sizes);
					return false;
				}
				if (args.length == 2) {
					LocalSession session = worldEdit.getSession(p);
					try {
						Region sel = session.getSelection(session.getSelectionWorld());
						if (sel == null) {
							sender.sendMessage(prefix + "§cPlease make a WorldEdit selection first.");
							return false;
						}
						BlockSize size = null;
						try {
							size = BlockSize.valueOf(args[1].toUpperCase());
						} catch (Exception e) {
						}
						if (size == null) {
							sender.sendMessage(prefix + "§cInvalid size");
							return false;
						}
						sender.sendMessage(prefix + "§aShrinking region...");
						boolean b = this.shrinkRegion(p, sel.getMinimumPoint(), sel.getMaximumPoint(), size);
						if (b) {
							sender.sendMessage(prefix + "§aDone!");
						} else {
							sender.sendMessage(prefix + "§cFailed.");
						}
						return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
					return false;
				}
			}
			if ("clear".equalsIgnoreCase(args[0])) {
				if (!sender.hasPermission("blockshrink.command.clear")) {
					sender.sendMessage(prefix + "§cNo permission");
					return false;
				}
				LocalSession session = worldEdit.getSession(p);
				try {
					Region sel = session.getSelection(session.getSelectionWorld());
					if (sel == null) {
						sender.sendMessage(prefix + "§cPlease make a WorldEdit selection first.");
						return false;
					}
					if (sel == null) {
						sender.sendMessage(prefix + "§cPlease make a WorldEdit selection first.");
						return false;
					}
					new BukkitRunnable() {

						@Override
						public void run() {
							sender.sendMessage(prefix + "§aRemoving entities....");
							BlockShrink.this.removeBlocksInSelection(sel, p.getWorld());
							sender.sendMessage(prefix + "§aDone!");
						}
					}.runTask(instance);
					return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
			if ("move".equalsIgnoreCase(args[0])) {
				if (!sender.hasPermission("blockshrink.command.move")) {
					sender.sendMessage(prefix + "§cNo permission");
					return false;
				}
				if (args.length != 4) {
					sender.sendMessage(prefix + "§c/bs move <x> <y> <z>");
					return false;
				}
				LocalSession session = worldEdit.getSession(p);
				try {
					Region sel = session.getSelection(session.getSelectionWorld());
					if (sel == null) {
						sender.sendMessage(prefix + "§cPlease make a WorldEdit selection first.");
						return false;
					}
					try {
						final double x = Double.parseDouble(args[1]);
						final double y = Double.parseDouble(args[2]);
						final double z = Double.parseDouble(args[3]);

						new BukkitRunnable() {

							@Override
							public void run() {
								sender.sendMessage(prefix + "§aShifting blocks by " + x + "," + y + "," + z + "...");
								for (Entity ent : BlockShrink.this.getBlocksInSelection(sel, p.getWorld())) {
									ent.teleport(ent.getLocation().add(x, y, z));
								}
								sender.sendMessage(prefix + "§aDone!");
							}
						}.runTask(instance);
					} catch (NumberFormatException e) {
						sender.sendMessage(prefix + "§cInvalid coordinate");
						return false;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;

			}
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> list = new ArrayList<>();
		if (command.getName().equalsIgnoreCase("blockshrink")) {
			if (args.length == 1) {
				if (sender.hasPermission("blockshrink.command.shrink")) {
					list.add("shrink");
				}
				if (sender.hasPermission("blockshrink.command.clear")) {
					list.add("clear");
				}
				if (sender.hasPermission("blockshrink.command.move")) {
					list.add("move");
				}
			}
			if (args.length >= 2) {
				if (args[0].equalsIgnoreCase("shrink")) {
					for (BlockSize size : BlockSize.values()) {
						list.add(size.name().toUpperCase());
					}
				}
			}
		}
		return TabCompletionHelper.getPossibleCompletionsForGivenArgs(args, list.toArray(new String[list.size()]));
	}

	@EventHandler
	public void onInteractEntity(PlayerInteractEntityEvent e) {
		if (e.getRightClicked() instanceof ArmorStand) {
			if ("ShrunkBlock".equals(((ArmorStand) e.getRightClicked()).getCustomName())) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onArmorStandModify(PlayerArmorStandManipulateEvent e) {
		if ("ShrunkBlock".equals(e.getRightClicked().getCustomName())) {
			e.setCancelled(true);
		}
	}

	boolean removeBlocksInSelection(Region sel, World world) {
		for (Entity ent : this.getBlocksInSelection(sel, world)) {
			ent.remove();
		}
		return true;
	}

	Collection<ArmorStand> getBlocksInSelection(Region sel, World world) {
		final Collection<ArmorStand> entities = world.getEntitiesByClass(ArmorStand.class);
		final Collection<ArmorStand> list = new ArrayList<>();
		for (Entity ent : entities) {
			if (sel.contains(BlockVector3.at(ent.getLocation().getX(), ent.getLocation().getY(), ent.getLocation().getZ()))) {
				if (ent instanceof ArmorStand) {
					ArmorStand stand = (ArmorStand) ent;
					if ("ShrunkBlock".equals(stand.getCustomName())) {
						list.add(stand);
					}
				}
			}
		}
		return list;
	}

	public boolean shrinkRegion(Player p, BlockVector3 min, BlockVector3 max, BlockSize size) {
		if (min == null || max == null || size == null) { return false; }

		EditSession session = worldEdit.createEditSession(p);

		World world = p.getWorld();
		int minX = min.getBlockX();
		int minY = min.getBlockY();
		int minZ = min.getBlockZ();
		int maxX = max.getBlockX();
		int maxY = max.getBlockY();
		int maxZ = max.getBlockZ();

		// Stats
		int replacedBlocks = 0;

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					Block block = world.getBlockAt(x, y, z);
					if (block.getType() == Material.AIR) {
						continue;
					}

					double j = 0.0D;
					double j2 = 0.0D;
					double j1 = 0.0D;
					double i = 0.0D;

					if (size == BlockSize.HEAD) {
						j = j1 = j2 = .625;
						i = 1.75;
					}
					if (size == BlockSize.HEAD_SMALL) {
						j = j1 = .465;
						j2 = .43;
						i = .4675;
						if (x != minX) {
							j = .4375;
						}
						if (z != minX) {
							j1 = .4375;
						}
					}
					if (size == BlockSize.HAND) {
						j = j1 = j2 = .37;
						i = .4675;
					}
					if (size == BlockSize.HAND_SMALL) {
						j = j1 = .188;
						j2 = .185;
						i = .4675;
						if (x != minX) {
							j = .185;
						}
						if (z != minX) {
							j1 = .185;
						}
					}

					double spaceX = .25;
					double spaceY = .25;
					double spaceZ = .25;

					if (size == BlockSize.HAND) {
						spaceX = .25;
						spaceZ = -.125;
					}
					if (size == BlockSize.HAND_SMALL) {
						spaceX = .375;
						spaceZ = .125;
					}

					double xPos = (x - minX) * j + minX + spaceX;
					double yPos = (y - i - minY) * j2 + minY + spaceY;
					double zPos = (z - minZ) * j1 + minZ + spaceZ;

					this.spawnArmorStand(new Location(world, x, y, z), new Location(world, xPos, yPos, zPos), block.getType(), size);

					try {
						session.setBlock(BlockVector3.at(x, y, z), BukkitAdapter.asBlockState(new ItemStack(Material.AIR)));
					} catch (Exception e) {
						e.printStackTrace();
					}

					block.setType(Material.AIR);
					replacedBlocks++;
				}
			}
		}

		worldEdit.getSession(p).remember(session);

		return true;
	}

	public void spawnArmorStand(Location loc, Location origLoc, Material mat, BlockSize size) {
		loc = new Location(loc.getWorld(), origLoc.getX(), origLoc.getY(), origLoc.getZ(), size == BlockSize.HAND_SMALL ? 44.9f : 0f, 0f);
		ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);

		stand.setCustomName("ShrunkBlock");
		stand.setCustomNameVisible(false);

		stand.setArms(true);
		stand.setBasePlate(false);
		stand.setGravity(false);
		stand.setVisible(false);
		stand.setSmall(size == BlockSize.HEAD_SMALL || size == BlockSize.HAND_SMALL);

		if (size == BlockSize.HAND || size == BlockSize.HAND_SMALL) {
			stand.setItemInHand(new ItemStack(mat, 1));
		}
		if (size == BlockSize.HEAD || size == BlockSize.HEAD_SMALL) {
			stand.setHelmet(new ItemStack(mat, 1));
		}

		if (size == BlockSize.HAND) {
			stand.setRightArmPose(new EulerAngle(Math.toRadians(-14.05), Math.toRadians(-46.0), 0.0));
		}
		if (size == BlockSize.HAND_SMALL) {
			stand.setRightArmPose(new EulerAngle(Math.toRadians(-34.0), 0.0, 0.0));
		}
	}

}
