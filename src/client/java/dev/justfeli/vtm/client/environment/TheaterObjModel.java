package dev.justfeli.vtm.client.environment;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

final class TheaterObjModel {
    private static final int FULL_BRIGHT = 15728880;

    private final List<Triangle> triangles;

    private TheaterObjModel(List<Triangle> triangles) {
        this.triangles = triangles;
    }

    public static TheaterObjModel parse(String source) {
        List<Vector3f> positions = new ArrayList<>();
        List<Vec2f> uvs = new ArrayList<>();

        List<Triangle> triangles = new ArrayList<>();

        String[] lines = source.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] tokens = line.split("\\s+");
            switch (tokens[0]) {
                case "v" -> positions.add(new Vector3f(
                    parseFloat(tokens, 1),
                    parseFloat(tokens, 2),
                    parseFloat(tokens, 3)
                ));
                case "vt" -> {
                    float u = parseFloat(tokens, 1);
                    float v = tokens.length > 2 ? parseFloat(tokens, 2) : 0.0F;
                    // OBJ uses a bottom-left UV origin, Minecraft samples top-left.
                    uvs.add(new Vec2f(u, 1.0F - v));
                }
                case "f" -> parseFace(tokens, positions, uvs, triangles);
                default -> {
                    // mtllib/usemtl/o/g/s and everything else are ignored here.
                }
            }
        }

        return new TheaterObjModel(triangles);
    }

    public void render(Matrix4f transform, VertexConsumerProvider.Immediate bufferSource, Identifier textureId, boolean translucent) {
        RenderLayer renderLayer = translucent
            ? RenderLayer.getEntityTranslucent(textureId)
            : RenderLayer.getEntityCutoutNoCull(textureId);
        VertexConsumer consumer = bufferSource.getBuffer(renderLayer);
        for (Triangle triangle : triangles) {
            triangle.emit(consumer, transform);
        }
    }

    private static void parseFace(
        String[] tokens,
        List<Vector3f> positions,
        List<Vec2f> uvs,
        List<Triangle> triangles)
    {
        if (tokens.length < 4) {
            return;
        }

        FaceVertex first = parseFaceVertex(tokens[1], positions, uvs);
        FaceVertex previous = parseFaceVertex(tokens[2], positions, uvs);
        for (int i = 3; i < tokens.length; i++) {
            FaceVertex current = parseFaceVertex(tokens[i], positions, uvs);
            triangles.add(new Triangle(first, previous, current));
            previous = current;
        }
    }

    private static FaceVertex parseFaceVertex(String token, List<Vector3f> positions, List<Vec2f> uvs) {
        String[] parts = token.split("/");
        int positionIndex = parseIndex(parts[0], positions.size());
        Vector3f position = new Vector3f(positions.get(positionIndex));

        Vec2f uv = new Vec2f(0.0F, 0.0F);
        if (parts.length > 1 && !parts[1].isEmpty()) {
            int uvIndex = parseIndex(parts[1], uvs.size());
            uv = uvs.get(uvIndex);
        }

        return new FaceVertex(position, uv);
    }

    private static int parseIndex(String token, int size) {
        int index = Integer.parseInt(token);
        if (index < 0) {
            index = size + index;
        } else {
            index -= 1;
        }
        return index;
    }

    private static float parseFloat(String[] tokens, int index) {
        return Float.parseFloat(tokens[index]);
    }

    private record FaceVertex(Vector3f position, Vec2f uv) {
    }

    private static final class Triangle {
        private final FaceVertex a;
        private final FaceVertex b;
        private final FaceVertex c;
        private final Vector3f normal;

        private Triangle(FaceVertex a, FaceVertex b, FaceVertex c) {
            this.a = a;
            this.b = b;
            this.c = c;
            Vector3f ab = new Vector3f(b.position).sub(a.position);
            Vector3f ac = new Vector3f(c.position).sub(a.position);
            this.normal = ab.cross(ac, new Vector3f());
            if (this.normal.lengthSquared() > 0.0F) {
                this.normal.normalize();
            } else {
                this.normal.set(0.0F, 1.0F, 0.0F);
            }
        }

        private void emit(VertexConsumer consumer, Matrix4f transform) {
            // The entity render layers draw quads, so emit each triangle as a degenerate quad.
            emitVertex(consumer, transform, a);
            emitVertex(consumer, transform, b);
            emitVertex(consumer, transform, c);
            emitVertex(consumer, transform, c);
        }

        private void emitVertex(VertexConsumer consumer, Matrix4f transform, FaceVertex vertex) {
            consumer.vertex(transform, vertex.position.x, vertex.position.y, vertex.position.z)
                .color(-1)
                .texture(vertex.uv.x, vertex.uv.y)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT)
                .normal(normal.x, normal.y, normal.z);
        }
    }

}
