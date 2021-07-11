package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.format.MaterialIdHolder;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.coderbot.iris.Iris;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.system.MemoryUtil;


public class XHFPModelVertexBufferWriterUnsafe extends VertexBufferWriterUnsafe implements ModelVertexSink {
    private MaterialIdHolder idHolder;

    public XHFPModelVertexBufferWriterUnsafe(VertexBufferView backingBuffer, MaterialIdHolder idHolder) {
        super(backingBuffer, ChunkModelVertexFormats.EXTENDED);

        this.idHolder = idHolder;
    }

    private static final int STRIDE = XHFPModelVertexType.STRIDE;

    int vertexCount = 0;
    float uSum;
    float vSum;

    private QuadView currentQuad = new QuadView();
    private Vec3f normal = new Vec3f();

    @Override
    public void writeVertex(int offsetX, int offsetY, int offsetZ, float posX, float posY, float posZ, int color, float u, float v, int light) {
        uSum += u;
        vSum += v;

        short materialId = idHolder.id;
        short renderType = idHolder.renderType;

        this.writeQuadInternal(offsetX, offsetY, offsetZ, posX, posY, posZ, color, u, v, light, materialId, renderType);
    }

    /*@Override
    public void writeQuad(float x, float y, float z, int color, float u, float v, int light) {
        uSum += u;iris_
        vSum += v;

        short materialId = idHolder.id;
        short renderType = idHolder.renderType;

        this.writeQuadInternal(
                ModelVertexUtil.denormalizeVertexPositionFloatAsShort(x),
                ModelVertexUtil.denormalizeVertexPositionFloatAsShort(y),
                ModelVertexUtil.denormalizeVertexPositionFloatAsShort(z),
                color,
                ModelVertexUtil.denormalizeVertexTextureFloatAsShort(u),
                ModelVertexUtil.denormalizeVertexTextureFloatAsShort(v),
                ModelVertexUtil.encodeLightMapTexCoord(light),
                materialId,
                renderType
        );
    }*/

    private void writeQuadInternal(int offsetX, int offsetY, int offsetZ, float posX, float posY, float posZ, int color,
                                   float u, float v, int light, short materialId, short renderType) {
        long i = this.writePointer;

        vertexCount++;
        // NB: uSum and vSum must already be incremented outside of this function.

        MemoryUtil.memPutByte(i, (byte) offsetX);
        MemoryUtil.memPutByte(i + 1, (byte) offsetY);
        MemoryUtil.memPutByte(i + 2, (byte) offsetZ);

        MemoryUtil.memPutShort(i + 4, XHFPModelVertexType.encodePosition(posX));
        MemoryUtil.memPutShort(i + 6, XHFPModelVertexType.encodePosition(posY));
        MemoryUtil.memPutShort(i + 8, XHFPModelVertexType.encodePosition(posZ));

        MemoryUtil.memPutInt(i + 12, color);

        MemoryUtil.memPutShort(i + 16, XHFPModelVertexType.encodeBlockTexture(u));
        MemoryUtil.memPutShort(i + 18, XHFPModelVertexType.encodeBlockTexture(v));

        MemoryUtil.memPutInt(i + 20, XHFPModelVertexType.encodeLightMapTexCoord(light));

        // NB: We don't set midTexCoord, normal, and tangent here, they will be filled in later.
        // block ID
        MemoryUtil.memPutShort(i + 36, materialId);
        MemoryUtil.memPutShort(i + 38, renderType);
        MemoryUtil.memPutShort(i + 40, (short) 0);
        MemoryUtil.memPutShort(i + 42, (short) 0);

        if (vertexCount == 4) {
            // TODO: Consider applying similar vertex coordinate transformations as the normal HFP texture coordinates
            short midU = (short)(65536.0F * (uSum * 0.25f));
            short midV = (short)(65536.0F * (vSum * 0.25f));
            int midTexCoord = (midV << 16) | midU;

            MemoryUtil.memPutInt(i + 24, midTexCoord);
            MemoryUtil.memPutInt(i + 24 - STRIDE, midTexCoord);
            MemoryUtil.memPutInt(i + 24 - STRIDE * 2, midTexCoord);
            MemoryUtil.memPutInt(i + 24 - STRIDE * 3, midTexCoord);

            vertexCount = 0;
            uSum = 0;
            vSum = 0;

            // normal computation
            // Implementation based on the algorithm found here:
            // https://github.com/IrisShaders/ShaderDoc/blob/master/vertex-format-extensions.md#surface-normal-vector
            
            currentQuad.writePointer = this.writePointer;
            NormalHelper.computeFaceNormal(normal, currentQuad, true);
            int packedNormal = NormalHelper.packNormal(normal, 0.0f);

            MemoryUtil.memPutInt(i + 32, packedNormal);
            MemoryUtil.memPutInt(i + 32 - STRIDE, packedNormal);
            MemoryUtil.memPutInt(i + 32 - STRIDE * 2, packedNormal);
            MemoryUtil.memPutInt(i + 32 - STRIDE * 3, packedNormal);

            // Capture all of the relevant vertex positions
            float x0 = XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(i + 4 - STRIDE * 3));
            float y0 = XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(i + 6 - STRIDE * 3));
            float z0 = XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(i + 8 - STRIDE * 3));

            float x1 = XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(i + 4 - STRIDE * 2));
            float y1 = XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(i + 6 - STRIDE * 2));
            float z1 = XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(i + 8 - STRIDE * 2));

            float x2 = XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(i + 4 - STRIDE));
            float y2 = XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(i + 6 - STRIDE));
            float z2 = XHFPModelVertexType.decodePosition(MemoryUtil.memGetShort(i + 8 - STRIDE));

            float edge1x = x1 - x0;
            float edge1y = y1 - y0;
            float edge1z = z1 - z0;

            float edge2x = x2 - x0;
            float edge2y = y2 - y0;
            float edge2z = z2 - z0;

            float u0 = XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(i + 16 - STRIDE * 3));
            float v0 = XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(i + 18 - STRIDE * 3));

            float u1 = XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(i + 16 - STRIDE * 2));
            float v1 = XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(i + 18 - STRIDE * 2));

            float u2 = XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(i + 16 - STRIDE));
            float v2 = XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(i + 18 - STRIDE));

            float deltaU1 = u1 - u0;
            float deltaV1 = v1 - v0;
            float deltaU2 = u2 - u0;
            float deltaV2 = v2 - v0;

            float fdenom = deltaU1 * deltaV2 - deltaU2 * deltaV1;
            float f;

            if (fdenom == 0.0) {
                f = 1.0f;
            } else {
                f = 1.0f / fdenom;
            }

            float tangentx = f * (deltaV2 * edge1x - deltaV1 * edge2x);
            float tangenty = f * (deltaV2 * edge1y - deltaV1 * edge2y);
            float tangentz = f * (deltaV2 * edge1z - deltaV1 * edge2z);
            float tcoeff = rsqrt(tangentx * tangentx + tangenty * tangenty + tangentz * tangentz);
            tangentx *= tcoeff;
            tangenty *= tcoeff;
            tangentz *= tcoeff;

            float bitangentx = f * (-deltaU2 * edge1x + deltaU1 * edge2x);
            float bitangenty = f * (-deltaU2 * edge1y + deltaU1 * edge2y);
            float bitangentz = f * (-deltaU2 * edge1z + deltaU1 * edge2z);
            float bitcoeff = rsqrt(bitangentx * bitangentx + bitangenty * bitangenty + bitangentz * bitangentz);
            bitangentx *= bitcoeff;
            bitangenty *= bitcoeff;
            bitangentz *= bitcoeff;

            // predicted bitangent = tangent × normal
            // Compute the determinant of the following matrix to get the cross product
            //  i  j  k
            // tx ty tz
            // nx ny nz

            float pbitangentx =   tangenty * normal.getZ() - tangentz * normal.getY();
            float pbitangenty = -(tangentx * normal.getZ() - tangentz * normal.getX());
            float pbitangentz =   tangentx * normal.getX() - tangenty * normal.getY();

            float dot = bitangentx * pbitangentx + bitangenty + pbitangenty + bitangentz * pbitangentz;
            byte tangentW;

            if (dot < 0) {
                tangentW = -127;
            } else {
                tangentW = 127;
            }

            int tangent = Norm3b.pack(tangentx, tangenty, tangentz);
            tangent |= (tangentW << 24);

            MemoryUtil.memPutInt(i + 28, tangent);
            MemoryUtil.memPutInt(i + 28 - STRIDE, tangent);
            MemoryUtil.memPutInt(i + 28 - STRIDE * 2, tangent);
            MemoryUtil.memPutInt(i + 28 - STRIDE * 3, tangent);
        }

        this.advance();
    }

    private static float rsqrt(float value) {
        if (value == 0.0f) {
            // You heard it here first, folks: 1 divided by 0 equals 1
            // In actuality, this is a workaround for normalizing a zero length vector (leaving it as zero length)
            return 1.0f;
        } else {
            return (float) (1.0 / Math.sqrt(value));
        }
    }
}
