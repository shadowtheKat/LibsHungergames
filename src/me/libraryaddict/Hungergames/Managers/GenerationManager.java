package me.libraryaddict.Hungergames.Managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import me.libraryaddict.Hungergames.Types.HungergamesApi;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;

public class GenerationManager {
    private class BlockInfo {
        byte data;

        Material id;

        public BlockInfo(int id, byte data) {
            this.id = Material.getMaterial(id);
            this.data = data;
        }

        public BlockInfo(Material id, byte data) {
            this.id = id;
            this.data = data;
        }
    }

    private List<BlockFace> faces = new ArrayList<BlockFace>();
    private List<BlockFace> jungleFaces = new ArrayList<BlockFace>();
    private LinkedList<Block> processedBlocks = new LinkedList<Block>();
    private HashMap<Block, BlockInfo> queued = new HashMap<Block, BlockInfo>();
    private BukkitRunnable runnable;

    public GenerationManager() {
        faces.add(BlockFace.UP);
        faces.add(BlockFace.DOWN);
        faces.add(BlockFace.SOUTH);
        faces.add(BlockFace.NORTH);
        faces.add(BlockFace.WEST);
        faces.add(BlockFace.EAST);
        faces.add(BlockFace.SELF);
        jungleFaces.add(BlockFace.UP);
        jungleFaces.add(BlockFace.SELF);
        jungleFaces.add(BlockFace.DOWN);
    }

    public void addToProcessedBlocks(Block block) {
        processedBlocks.add(block);
    }

    /**
     * Generate pillars..
     */
    public void generatePillars(Location loc, int radius, int pillarCornerId, int pillarCornerData, int pillarInsideId,
            int pillarInsideData) {
        int[] cords = new int[] { (int) (-radius / 2.5), (int) (radius / 2.5) };
        int pillarRadius = Math.round(radius / 8);
        for (int px = 0; px <= 1; px++)
            for (int pz = 0; pz <= 1; pz++)
                for (int x = -pillarRadius; x <= pillarRadius; x++) {
                    for (int z = -pillarRadius; z <= pillarRadius; z++) {
                        Block b = loc.getWorld().getBlockAt(x + loc.getBlockX() + cords[px], loc.getBlockY() - 2,
                                z + loc.getBlockZ() + cords[pz]);
                        while (!isSolid(b)) {
                            if (Math.abs(x) == pillarRadius && Math.abs(z) == pillarRadius)
                                setBlockFast(b, pillarCornerId, (short) pillarCornerData);
                            else
                                setBlockFast(b, pillarInsideId, (short) pillarInsideData);
                            b = b.getRelative(BlockFace.DOWN);
                        }
                    }
                }
    }

    /**
     * Generates a feast
     * 
     * @param loc
     * @param lowestLevel
     * @param radius
     */
    public void generatePlatform(Location loc, int lowestLevel, int radius, int yHeight, int platformGround,
            short platformDurability) {
        loc.setY(lowestLevel + 1);
        double radiusSquared = radius * radius;
        // Sets to air and generates to stand on
        for (int radiusX = -radius; radiusX <= radius; radiusX++) {
            for (int radiusZ = -radius; radiusZ <= radius; radiusZ++) {
                if ((radiusX * radiusX) + (radiusZ * radiusZ) <= radiusSquared) {
                    for (int y = loc.getBlockY() - 1; y < loc.getBlockY() + yHeight; y++) {
                        if (y > loc.getWorld().getMaxHeight())
                            break;
                        Block b = loc.getWorld().getBlockAt(radiusX + loc.getBlockX(), y, radiusZ + loc.getBlockZ());
                        removeLeaves(b);
                        if (y >= loc.getBlockY()) {// If its less then 0
                            setBlockFast(b, 0, (byte) 0);
                        } else {
                            // Generates to stand on
                            setBlockFast(b, platformGround, platformDurability);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates a feast
     * 
     * @param loc
     * @param lowestLevel
     * @param radius
     */
    public void generatePlatform(Location loc, int lowestLevel, int radius, Material groundType, int groundDurability) {
        int yHeight = radius;
        if (yHeight < 4)
            yHeight = 4;
        generatePlatform(loc, lowestLevel, radius, yHeight, groundType.getId(), (short) groundDurability);
    }

    public int getHeight(ArrayList<Integer> heights, int radius) {
        List<List<Integer>> commons = new ArrayList<List<Integer>>();
        for (int i = 0; i < heights.size(); i++) {
            List<Integer> numbers = new ArrayList<Integer>();
            numbers.add(heights.get(i));
            for (int a = i; a < heights.size(); a++) {
                if (heights.get(i) - heights.get(a) >= -radius)
                    numbers.add(heights.get(a));
                else
                    break;
            }
            commons.add(numbers);
        }
        int highest = 0;
        List<Integer> found = new ArrayList<Integer>();
        for (List l : commons) {
            if (l.size() > highest) {
                highest = l.size();
                found = l;
            }
        }
        if (found.size() == 0)
            return 0;
        return found.get((int) Math.round(found.size() / 3));
    }

    private Block getHighest(Block b) {
        while (b.getY() > 0 && !isSolid(b))
            b = b.getRelative(BlockFace.DOWN);
        return b;
    }

    /**
     * Gets the best Y level to spawn the feast at This also modifies the Location fed to it for use by feast generation
     * 
     * @param loc
     * @param radius
     * @return Best Y Level
     */
    public int getSpawnHeight(Location loc, int radius) {
        ArrayList<Integer> heightLevels = new ArrayList<Integer>();
        for (double degree = 0; degree <= 360; degree += 1) {
            double angle = degree * Math.PI / 180;
            int x = (int) (loc.getX() + .5 + radius * Math.cos(angle));
            int z = (int) (loc.getZ() + radius * Math.sin(angle));
            Block b = getHighest(loc.getWorld().getHighestBlockAt(x, z));
            if (isBlockValid(b))
                heightLevels.add(b.getY());
            /*
             * // Do it again but at 2/3 the radius angle = degree * Math.PI /
             * 180; x = (int) (loc.getX() + .5 + ((radius / 3) * 2) *
             * Math.cos(angle)); z = (int) (loc.getZ() + ((radius / 3) * 2) *
             * Math.sin(angle)); b =
             * getHighest(loc.getWorld().getHighestBlockAt(x, z)); if
             * (!b.getChunk().isLoaded()) b.getChunk().load(true); if
             * (isBlockValid(b)) heightLevels.add(b.getY());
             */
        }
        Block b = getHighest(loc.getBlock());
        if (isBlockValid(b))
            heightLevels.add(b.getY());
        Collections.sort(heightLevels);
        int y = getHeight(heightLevels, 5);
        if (y == -1)
            y = b.getY();
        loc = new Location(loc.getWorld(), loc.getBlockX(), y + 1, loc.getBlockZ());
        return y;
    }

    private boolean isBlockValid(Block b) {
        if (b.isLiquid() || b.getRelative(BlockFace.UP).isLiquid())
            return false;
        return true;
    }

    private boolean isSolid(Block b) {
        return (!(b.getType() == Material.AIR || b.isLiquid() || b.getType() == Material.VINE || b.getType() == Material.LOG
                || b.getType() == Material.LEAVES || b.getType() == Material.SNOW || b.getType() == Material.LONG_GRASS
                || b.getType() == Material.WOOD || b.getType() == Material.COBBLESTONE || b.getType().name().contains("FLOWER") || b
                .getType().name().contains("MUSHROOM")));
    }

    private void removeLeaves(Block b) {
        for (BlockFace face : ((b.getBiome() == Biome.JUNGLE || b.getBiome() == Biome.JUNGLE_HILLS) ? jungleFaces : faces)) {
            Block newB = b.getRelative(face);
            if ((newB.getType() == Material.LEAVES || newB.getType() == Material.LOG || newB.getType() == Material.VINE)) {
                if (!queued.containsKey(newB) && !processedBlocks.contains(newB)) {
                    setBlockFast(newB, 0, (byte) 0);
                    if (newB.getRelative(BlockFace.DOWN).getType() == Material.DIRT) {
                        newB = newB.getRelative(BlockFace.DOWN);
                        if (!queued.containsKey(newB) && !processedBlocks.contains(newB)) {
                            setBlockFast(newB, Material.GRASS.getId(), (byte) 0);
                        }
                    }
                }
            } else if (newB.getType() == Material.SNOW && face == BlockFace.UP) {
                if (!queued.containsKey(newB) && !processedBlocks.contains(newB)) {
                    setBlockFast(newB, 0, (byte) 0);
                }
            }
        }
    }

    public void setBlockFast(Block b, int typeId, short s) {
        setBlockFast(b, Material.getMaterial(typeId), s);
    }

    public void setBlockFast(Block b, Material type, short s) {
        try {
            if (b.getType() != type || b.getData() != s) {
                queued.put(b, new BlockInfo(type, (byte) s));
                if (runnable == null) {
                    runnable = new BukkitRunnable() {
                        public void run() {
                            if (queued.size() == 0) {
                                runnable = null;
                                processedBlocks.clear();
                                cancel();
                            }
                            int i = 0;
                            HashMap<Block, BlockInfo> toDo = new HashMap<Block, BlockInfo>();
                            for (Block b : queued.keySet()) {
                                if (i++ >= 200)
                                    break;
                                if (b.getType() == queued.get(b).id && b.getData() == queued.get(b).data)
                                    i--;
                                toDo.put(b, queued.get(b));
                                b = b.getRelative(BlockFace.UP);
                                while (b != null
                                        && queued.containsKey(b)
                                        && (b.isLiquid() || b.getType() == Material.SAND || b.getType() == Material.ANVIL || b
                                                .getType() == Material.GRAVEL)) {
                                    toDo.put(b, queued.get(b));
                                    b = b.getRelative(BlockFace.UP);
                                }
                            }
                            for (Block b : toDo.keySet()) {
                                if (!processedBlocks.contains(b))
                                    processedBlocks.add(b);
                                queued.remove(b);
                                removeLeaves(b);
                                if (!b.getChunk().isLoaded())
                                    b.getChunk().load();
                                b.setTypeIdAndData(toDo.get(b).id.getId(), toDo.get(b).data, true);
                            }
                        }
                    };
                    runnable.runTaskTimer(HungergamesApi.getHungergames(), 2, 1);
                }
                // return ((CraftChunk) b.getChunk()).getHandle().a(b.getX() & 15,
                // b.getY(), b.getZ() & 15, typeId, data);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}