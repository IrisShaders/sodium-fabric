package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;

public class QuadView {
	ByteBuffer buffer;
	int writeOffset;
	long writePointer;
	private static final int STRIDE = XHFPModelVertexType.STRIDE;

    float x(int index, boolean unsafe) {
        if(unsafe) {
            return XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(writePointer + 4 - STRIDE * (3 - index)));
        }
        return XHFPModelVertexType.decodePosition(buffer.getShort(writeOffset + 4 - STRIDE * (3 - index)));
    }

    float y(int index, boolean unsafe) {
        if(unsafe) {
            return XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(writePointer + 6 - STRIDE * (3 - index)));
        }
        return XHFPModelVertexType.decodePosition(buffer.getShort(writeOffset + 6 - STRIDE * (3 - index)));
    }

    float z(int index, boolean unsafe) {
        if(unsafe) {
            return XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(writePointer + 8 - STRIDE * (3 - index)));
        }
        return XHFPModelVertexType.decodePosition(buffer.getShort(writeOffset + 8 - STRIDE * (3 - index)));
    }
}
