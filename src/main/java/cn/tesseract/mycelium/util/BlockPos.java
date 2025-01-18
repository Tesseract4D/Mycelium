package cn.tesseract.mycelium.util;

import org.joml.Vector3i;

public class BlockPos extends Vector3i {
    public BlockPos(int x, int y, int z) {
        super(x, y, z);
    }

    public BlockPos() {
    }

    public void offset(Direction dir) {
         offset(dir, 1);
    }

    public void offset(Direction dir, int dist) {
        offset(dist * dir.offsetX, dist * dir.offsetY, dist * dir.offsetZ);
    }

    public void offset(int offsetX, int offsetY, int offsetZ) {
        this.x += offsetX;
        this.y += offsetY;
        this.z += offsetZ;
    }
}
