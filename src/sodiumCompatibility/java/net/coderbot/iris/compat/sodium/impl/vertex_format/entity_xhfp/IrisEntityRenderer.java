package net.coderbot.iris.compat.sodium.impl.vertex_format.entity_xhfp;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import net.coderbot.iris.Iris;
import net.coderbot.iris.vertices.NormI8;
import net.coderbot.iris.vertices.NormalHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class IrisEntityRenderer {

	private static final int NUM_CUBE_VERTICES = 8;
	private static final int NUM_CUBE_FACES = 6;
	private static final int NUM_FACE_VERTICES = 4;

	private static final int
		FACE_NEG_Y = 0, // DOWN
		FACE_POS_Y = 1, // UP
		FACE_NEG_Z = 2, // NORTH
		FACE_POS_Z = 3, // SOUTH
		FACE_NEG_X = 4, // WEST
		FACE_POS_X = 5; // EAST

	private static final int
		VERTEX_X1_Y1_Z1 = 0,
		VERTEX_X2_Y1_Z1 = 1,
		VERTEX_X2_Y2_Z1 = 2,
		VERTEX_X1_Y2_Z1 = 3,
		VERTEX_X1_Y1_Z2 = 4,
		VERTEX_X2_Y1_Z2 = 5,
		VERTEX_X2_Y2_Z2 = 6,
		VERTEX_X1_Y2_Z2 = 7;


	private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64, (long) NUM_CUBE_FACES * NUM_FACE_VERTICES * EntityVertex.STRIDE);

	private static final Vector3f[] CUBE_CORNERS = new Vector3f[NUM_CUBE_VERTICES];
	private static final int[][] CUBE_VERTICES = new int[][] {
		{ VERTEX_X2_Y1_Z2, VERTEX_X1_Y1_Z2, VERTEX_X1_Y1_Z1, VERTEX_X2_Y1_Z1 },
		{ VERTEX_X2_Y2_Z1, VERTEX_X1_Y2_Z1, VERTEX_X1_Y2_Z2, VERTEX_X2_Y2_Z2 },
		{ VERTEX_X2_Y1_Z1, VERTEX_X1_Y1_Z1, VERTEX_X1_Y2_Z1, VERTEX_X2_Y2_Z1 },
		{ VERTEX_X1_Y1_Z2, VERTEX_X2_Y1_Z2, VERTEX_X2_Y2_Z2, VERTEX_X1_Y2_Z2 },
		{ VERTEX_X2_Y1_Z2, VERTEX_X2_Y1_Z1, VERTEX_X2_Y2_Z1, VERTEX_X2_Y2_Z2 },
		{ VERTEX_X1_Y1_Z1, VERTEX_X1_Y1_Z2, VERTEX_X1_Y2_Z2, VERTEX_X1_Y2_Z1 },
	};

	private static final Vector3f[][] VERTEX_POSITIONS = new Vector3f[NUM_CUBE_FACES][NUM_FACE_VERTICES];
	private static final Vector3f[][] VERTEX_POSITIONS_MIRRORED = new Vector3f[NUM_CUBE_FACES][NUM_FACE_VERTICES];

	private static final Vector2f[][] VERTEX_TEXTURES = new Vector2f[NUM_CUBE_FACES][NUM_FACE_VERTICES];
	private static final Vector2f[][] VERTEX_TEXTURES_MIRRORED = new Vector2f[NUM_CUBE_FACES][NUM_FACE_VERTICES];

	private static final Vector2f[] VERTEX_MIDCOORD = new Vector2f[NUM_CUBE_FACES];

	private static final int[] CUBE_NORMALS = new int[NUM_CUBE_FACES];
	private static final int[] CUBE_NORMALS_MIRRORED = new int[NUM_CUBE_FACES];

	static {
		for (int cornerIndex = 0; cornerIndex < NUM_CUBE_VERTICES; cornerIndex++) {
			CUBE_CORNERS[cornerIndex] = new Vector3f();
		}

		for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
			for (int vertexIndex = 0; vertexIndex < NUM_FACE_VERTICES; vertexIndex++) {
				VERTEX_TEXTURES[quadIndex][vertexIndex] = new Vector2f();
				VERTEX_POSITIONS[quadIndex][vertexIndex] = CUBE_CORNERS[CUBE_VERTICES[quadIndex][vertexIndex]];
			}

			VERTEX_MIDCOORD[quadIndex] = new Vector2f();
		}

		for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
			for (int vertexIndex = 0; vertexIndex < NUM_FACE_VERTICES; vertexIndex++) {
				VERTEX_TEXTURES_MIRRORED[quadIndex][vertexIndex] = VERTEX_TEXTURES[quadIndex][3 - vertexIndex];
				VERTEX_POSITIONS_MIRRORED[quadIndex][vertexIndex] = VERTEX_POSITIONS[quadIndex][3 - vertexIndex];
			}

		}
	}

	public static void render(PoseStack matrixStack, VertexBufferWriter writer, ModelPart part, int light, int overlay, int color) {
		IrisModelPartData accessor = IrisModelPartData.from(part);

		if (!accessor.isVisible()) {
			return;
		}

		var cuboids = accessor.getCuboids();
		var children = accessor.getChildren();

		if (ArrayUtils.isEmpty(cuboids) && ArrayUtils.isEmpty(children)) {
			return;
		}

		matrixStack.pushPose();

		part.translateAndRotate(matrixStack);

		if (!accessor.isHidden()) {
			renderCuboids(matrixStack.last(), writer, cuboids, light, overlay, color);
		}

		renderChildren(matrixStack, writer, light, overlay, color, children);

		matrixStack.popPose();
	}

	private static void renderChildren(PoseStack matrices, VertexBufferWriter writer, int light, int overlay, int color, ModelPart[] children) {
		for (ModelPart part : children) {
			render(matrices, writer, part, light, overlay, color);
		}
	}

	private static void renderCuboids(PoseStack.Pose matrices, VertexBufferWriter writer, IrisModelCuboid[] cuboids, int light, int overlay, int color) {
		prepareNormals(matrices);

		for (IrisModelCuboid cuboid : cuboids) {
			prepareVertices(matrices, cuboid);

			var vertexCount = emitQuads(cuboid, matrices.normal(), color, overlay, light);

			try (MemoryStack stack = MemoryStack.stackPush()) {
				writer.push(stack, SCRATCH_BUFFER, vertexCount, EntityVertex.FORMAT);
			}
		}
	}

	private static float NORM = 1.0f / 127.0f;

	public static int transformNormal(Matrix3f mat, int norm) {
		float x = (norm & 0xFF) * NORM;
		float y = (norm >> 8 & 0xFF) * NORM;
		float z = (norm >> 16 & 0xFF) * NORM;
		int w = norm >> 24;

		Iris.logger.warn(x + " " + y + " " + z + " " + w);
		return 0;
	}

	private static int emitQuads(IrisModelCuboid cuboid, Matrix3f normal, int color, int overlay, int light) {
		final var positions = cuboid.mirror ? VERTEX_POSITIONS_MIRRORED : VERTEX_POSITIONS;
		final var textures = cuboid.mirror ? VERTEX_TEXTURES_MIRRORED : VERTEX_TEXTURES;
		final var midTex = VERTEX_MIDCOORD;
		final var normals = cuboid.mirror ? CUBE_NORMALS_MIRRORED :  CUBE_NORMALS;
		final var tangents = cuboid.tangents;
		final var tangentW = cuboid.tangentW;

		var vertexCount = 0;

		long ptr = SCRATCH_BUFFER;

		for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
			if (!cuboid.shouldDrawFace(quadIndex)) {
				continue;
			}

			int tangent = MatrixHelper.transformNormal(normal, tangents[quadIndex]) | tangentW[quadIndex] << 24;

			emitVertex(ptr, positions[quadIndex][0], color, textures[quadIndex][0], midTex[quadIndex], overlay, light, normals[quadIndex], tangent);
			ptr += EntityVertex.STRIDE;

			emitVertex(ptr, positions[quadIndex][1], color, textures[quadIndex][1], midTex[quadIndex], overlay, light, normals[quadIndex], tangent);
			ptr += EntityVertex.STRIDE;

			emitVertex(ptr, positions[quadIndex][2], color, textures[quadIndex][2], midTex[quadIndex], overlay, light, normals[quadIndex], tangent);
			ptr += EntityVertex.STRIDE;

			emitVertex(ptr, positions[quadIndex][3], color, textures[quadIndex][3], midTex[quadIndex], overlay, light, normals[quadIndex], tangent);
			ptr += EntityVertex.STRIDE;

			vertexCount += 4;
		}

		return vertexCount;
	}

	private static void emitVertex(long ptr, Vector3f pos, int color, Vector2f tex, Vector2f midTex, int overlay, int light, int normal, int tangent) {
		EntityVertex.write(ptr, pos.x, pos.y, pos.z, color, tex.x, tex.y, midTex.x, midTex.y, light, overlay, normal, tangent);
	}

	private static void prepareVertices(PoseStack.Pose matrices, IrisModelCuboid cuboid) {
		buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y1_Z1], cuboid.x1, cuboid.y1, cuboid.z1, matrices.pose());
		buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y1_Z1], cuboid.x2, cuboid.y1, cuboid.z1, matrices.pose());
		buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y2_Z1], cuboid.x2, cuboid.y2, cuboid.z1, matrices.pose());
		buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y2_Z1], cuboid.x1, cuboid.y2, cuboid.z1, matrices.pose());
		buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y1_Z2], cuboid.x1, cuboid.y1, cuboid.z2, matrices.pose());
		buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y1_Z2], cuboid.x2, cuboid.y1, cuboid.z2, matrices.pose());
		buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y2_Z2], cuboid.x2, cuboid.y2, cuboid.z2, matrices.pose());
		buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y2_Z2], cuboid.x1, cuboid.y2, cuboid.z2, matrices.pose());

		buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_Y], VERTEX_MIDCOORD[FACE_NEG_Y], cuboid.u1, cuboid.v0, cuboid.u2, cuboid.v1);
		buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_Y], VERTEX_MIDCOORD[FACE_POS_Y], cuboid.u2, cuboid.v1, cuboid.u3, cuboid.v0);
		buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_Z], VERTEX_MIDCOORD[FACE_NEG_Z], cuboid.u1, cuboid.v1, cuboid.u2, cuboid.v2);
		buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_Z], VERTEX_MIDCOORD[FACE_POS_Z], cuboid.u4, cuboid.v1, cuboid.u5, cuboid.v2);
		buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_X], VERTEX_MIDCOORD[FACE_NEG_X], cuboid.u2, cuboid.v1, cuboid.u4, cuboid.v2);
		buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_X], VERTEX_MIDCOORD[FACE_POS_X], cuboid.u0, cuboid.v1, cuboid.u1, cuboid.v2);
	}

	private static void prepareNormals(PoseStack.Pose matrices) {
		CUBE_NORMALS[FACE_NEG_Y] = MatrixHelper.transformNormal(matrices.normal(), Direction.DOWN);
		CUBE_NORMALS[FACE_POS_Y] = MatrixHelper.transformNormal(matrices.normal(), Direction.UP);
		CUBE_NORMALS[FACE_NEG_Z] = MatrixHelper.transformNormal(matrices.normal(), Direction.NORTH);
		CUBE_NORMALS[FACE_POS_Z] = MatrixHelper.transformNormal(matrices.normal(), Direction.SOUTH);
		CUBE_NORMALS[FACE_POS_X] = MatrixHelper.transformNormal(matrices.normal(), Direction.WEST);
		CUBE_NORMALS[FACE_NEG_X] = MatrixHelper.transformNormal(matrices.normal(), Direction.EAST);

		// When mirroring is used, the normals for EAST and WEST are swapped.
		CUBE_NORMALS_MIRRORED[FACE_NEG_Y] = CUBE_NORMALS[FACE_NEG_Y];
		CUBE_NORMALS_MIRRORED[FACE_POS_Y] = CUBE_NORMALS[FACE_POS_Y];
		CUBE_NORMALS_MIRRORED[FACE_NEG_Z] = CUBE_NORMALS[FACE_NEG_Z];
		CUBE_NORMALS_MIRRORED[FACE_POS_Z] = CUBE_NORMALS[FACE_POS_Z];
		CUBE_NORMALS_MIRRORED[FACE_POS_X] = CUBE_NORMALS[FACE_NEG_X]; // mirrored
		CUBE_NORMALS_MIRRORED[FACE_NEG_X] = CUBE_NORMALS[FACE_POS_X]; // mirrored
	}

	private static void buildVertexPosition(Vector3f vector, float x, float y, float z, Matrix4f matrix) {
		vector.x = MatrixHelper.transformPositionX(matrix, x, y, z);
		vector.y = MatrixHelper.transformPositionY(matrix, x, y, z);
		vector.z = MatrixHelper.transformPositionZ(matrix, x, y, z);
	}

	private static void buildVertexTexCoord(Vector2f[] uvs, Vector2f midCoord, float u1, float v1, float u2, float v2) {
		uvs[0].set(u2, v1);
		uvs[1].set(u1, v1);
		uvs[2].set(u1, v2);
		uvs[3].set(u2, v2);
		midCoord.set((u1 + u2) * 0.5f, (v1 + v2) * 0.5f);
	}
}
