package rich.util.string;

import lombok.experimental.UtilityClass;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.*;
import rich.IMinecraft;

import java.util.*;

/**
 *  © 2025 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

@UtilityClass
public class PlayerInteractionHelper implements IMinecraft {

    public List<BlockPos> getCube(BlockPos center, float radius) {
        return getCube(center, radius,radius,true);
    }

    public List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
        return getCube(center,radiusXZ,radiusY,true);
    }

    public boolean nullCheck() {return mc.player == null || mc.world == null;}

    public List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY, boolean down) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        int posY = down ? centerY - (int) radiusY : centerY;

        for (int x = centerX - (int) radiusXZ; x <= centerX + radiusXZ; x++) {
            for (int z = centerZ - (int) radiusXZ; z <= centerZ + radiusXZ; z++) {
                for (int y = posY; y <= centerY + radiusY; y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }

    public List<BlockPos> getCube(BlockPos start, BlockPos end) {
        List<BlockPos> positions = new ArrayList<>();

        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int z = start.getZ(); z <= end.getZ(); z++) {
                for (int y = start.getY(); y <= end.getY(); y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }

    public InputUtil.Type getKeyType(int key) {
        return key < 8 ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM;
    }




}