package net.kunmc.lab.invertchunk;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandHandler implements TabExecutor {
    private final Map<Long, Boolean> chunkKeyBooleanMap = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player p = ((Player) sender);

        int distance = 3;
        try {
            distance = Integer.parseInt(args[0]);
        } catch (Exception ignored) {
        }

        List<Chunk> chunkList = getNearbyChunks(p.getLocation(), distance);
        for (int i = 0; i < chunkList.size(); i++) {
            Chunk chunk = chunkList.get(i);
            int finalI = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    long chunkKey = chunk.getChunkKey();
                    if (chunkKeyBooleanMap.getOrDefault(chunkKey, false)) {
                        p.sendMessage(String.format(ChatColor.GREEN + "%d/%d chunks completed", finalI, chunkList.size()));
                        return;
                    }
                    chunkKeyBooleanMap.put(chunkKey, true);

                    for (int y = 0; y < 128; y++) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                Block b = chunk.getBlock(x, y, z);
                                if (!(b.getType().equals(Material.AIR) || b.getType().equals(Material.CAVE_AIR))) {
                                    int finalX = x;
                                    int finalY = y;
                                    int finalZ = z;
                                    InvertChunk.taskScheduler.offer(new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            if (b.getType().equals(Material.BEDROCK)) {
                                                chunk.getBlock(finalX, 255 - finalY, finalZ).setType(Material.STONE);
                                            } else {
                                                chunk.getBlock(finalX, 255 - finalY, finalZ).setBlockData(b.getBlockData(), false);
                                            }
                                            b.setType(Material.AIR, false);
                                        }
                                    });
                                }
                            }
                        }
                    }
                    p.sendMessage(String.format(ChatColor.GREEN + "%d/%d chunks completed", finalI, chunkList.size()));
                }
            }.runTaskLaterAsynchronously(InvertChunk.instance, i * 30);
        }

        if (p.getWorld().getEnvironment().equals(World.Environment.THE_END)) {
            Bukkit.selectEntities(p, "@e[type=minecraft:end_crystal]").forEach(crystal -> {
                Location to = crystal.getLocation().clone();
                if (to.getY() > 127) {
                    return;
                }

                to.setY(255 - to.getY());

                crystal.remove();
                to.getWorld().spawnEntity(to, EntityType.ENDER_CRYSTAL);
            });
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    private List<Chunk> getNearbyChunks(Location location, int chunkDistance) {
        List<Chunk> chunkList = new ArrayList<>();

        Chunk origin = location.getChunk();
        for (int x = -chunkDistance; x < chunkDistance; x++) {
            for (int z = -chunkDistance; z < chunkDistance; z++) {
                chunkList.add(location.getWorld().getChunkAt(origin.getX() + x, origin.getZ() + z));
            }
        }

        return chunkList;
    }
}
