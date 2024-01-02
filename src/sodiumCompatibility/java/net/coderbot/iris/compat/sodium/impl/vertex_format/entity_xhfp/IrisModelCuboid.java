package net.coderbot.iris.compat.sodium.impl.vertex_format.entity_xhfp;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.coderbot.iris.vertices.NormI8;
import net.coderbot.iris.vertices.NormalHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.joml.*;

import java.util.Set;

public class IrisModelCuboid {
	public final float x1, y1, z1;
	public final float x2, y2, z2;

	public final float u0, u1, u2, u3, u4, u5;
	public final float v0, v1, v2;

	private final int faces;

	public final boolean mirror;

	public final Direction[] tangents = new Direction[6];
	public final byte[] tangentW = new byte[6];


	private static final int
		FACE_NEG_Y = 0, // DOWN
		FACE_POS_Y = 1, // UP
		FACE_NEG_Z = 2, // NORTH
		FACE_POS_Z = 3, // SOUTH
		FACE_NEG_X = 4, // WEST
		FACE_POS_X = 5; // EAST

	public IrisModelCuboid(int u, int v,
					   float x1, float y1, float z1,
					   float sizeX, float sizeY, float sizeZ,
					   float extraX, float extraY, float extraZ,
					   boolean mirror,
					   float textureWidth, float textureHeight,
					   Set<Direction> renderDirections) {
		float x2 = x1 + sizeX;
		float y2 = y1 + sizeY;
		float z2 = z1 + sizeZ;

		x1 -= extraX;
		y1 -= extraY;
		z1 -= extraZ;

		x2 += extraX;
		y2 += extraY;
		z2 += extraZ;

		if (mirror) {
			float tmp = x2;
			x2 = x1;
			x1 = tmp;
		}

		this.x1 = x1 / 16.0f;
		this.y1 = y1 / 16.0f;
		this.z1 = z1 / 16.0f;

		this.x2 = x2 / 16.0f;
		this.y2 = y2 / 16.0f;
		this.z2 = z2 / 16.0f;

		var scaleU = 1.0f / textureWidth;
		var scaleV = 1.0f / textureHeight;

		this.u0 = scaleU * (u);
		this.u1 = scaleU * (u + sizeZ);
		this.u2 = scaleU * (u + sizeZ + sizeX);
		this.u3 = scaleU * (u + sizeZ + sizeX + sizeX);
		this.u4 = scaleU * (u + sizeZ + sizeX + sizeZ);
		this.u5 = scaleU * (u + sizeZ + sizeX + sizeZ + sizeX);

		this.v0 = scaleV * (v);
		this.v1 = scaleV * (v + sizeZ);
		this.v2 = scaleV * (v + sizeZ + sizeY);

		this.mirror = mirror;

		int faces = 0;

		for (var dir : renderDirections) {
			faces |= 1 << dir.ordinal();
		}

		for (int cornerIndex = 0; cornerIndex < NUM_CUBE_VERTICES; cornerIndex++) {
			CUBE_CORNERS[cornerIndex] = new Vector3f();
		}

		for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
			for (int vertexIndex = 0; vertexIndex < NUM_FACE_VERTICES; vertexIndex++) {
				VERTEX_TEXTURES[quadIndex][vertexIndex] = new Vector2f();
				VERTEX_POSITIONS[quadIndex][vertexIndex] = CUBE_CORNERS[CUBE_VERTICES[quadIndex][vertexIndex]];
			}

		}

		for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
			for (int vertexIndex = 0; vertexIndex < NUM_FACE_VERTICES; vertexIndex++) {
				VERTEX_TEXTURES_MIRRORED[quadIndex][vertexIndex] = VERTEX_TEXTURES[quadIndex][3 - vertexIndex];
				VERTEX_POSITIONS_MIRRORED[quadIndex][vertexIndex] = VERTEX_POSITIONS[quadIndex][3 - vertexIndex];
			}
		}

		prepareVertices();
		prepareNormals();

        for (int i = 0; i < tangents.length; i++) {
			Vector3f normal = Direction.values()[i].step();

			if (mirror && i == 4) {
				normal = Direction.EAST.step();
			} else if (mirror && i == 5) {
				normal = Direction.WEST.step();
			}

			final var positions = mirror ? VERTEX_POSITIONS_MIRRORED : VERTEX_POSITIONS;
			final var textures = mirror ? VERTEX_TEXTURES_MIRRORED : VERTEX_TEXTURES;
			final var normals = mirror ? CUBE_NORMALS_MIRRORED :  CUBE_NORMALS;

			int tangent = NormalHelper.computeTangent(normal.x, normal.y, normal.z, positions[i][0].x, positions[i][0].y, positions[i][0].z, textures[i][0].x, textures[i][0].y,
				positions[i][1].x, positions[i][1].y, positions[i][1].z, textures[i][1].x, textures[i][1].y,
				positions[i][2].x, positions[i][2].y, positions[i][2].z, textures[i][2].x, textures[i][2].y);

			tangents[i] = Direction.getNearest(NormI8.unpackX(tangent), NormI8.unpackY(tangent), NormI8.unpackZ(tangent));

			tangentW[i] = (byte) ((tangent >> 24) & 0xFF);
        }


		this.faces = faces;
	}

	private static final int NUM_CUBE_VERTICES = 8;
	private static final int NUM_CUBE_FACES = 6;
	private static final int NUM_FACE_VERTICES = 4;

	private static final int
		VERTEX_X1_Y1_Z1 = 0,
		VERTEX_X2_Y1_Z1 = 1,
		VERTEX_X2_Y2_Z1 = 2,
		VERTEX_X1_Y2_Z1 = 3,
		VERTEX_X1_Y1_Z2 = 4,
		VERTEX_X2_Y1_Z2 = 5,
		VERTEX_X2_Y2_Z2 = 6,
		VERTEX_X1_Y2_Z2 = 7;

	private static final Vector3f[] CUBE_CORNERS = new Vector3f[NUM_CUBE_VERTICES];
	private static final int[][] CUBE_VERTICES = new int[][] {
		{ VERTEX_X2_Y1_Z2, VERTEX_X1_Y1_Z2, VERTEX_X1_Y1_Z1, VERTEX_X2_Y1_Z1 },
		{ VERTEX_X2_Y2_Z1, VERTEX_X1_Y2_Z1, VERTEX_X1_Y2_Z2, VERTEX_X2_Y2_Z2 },
		{ VERTEX_X2_Y1_Z1, VERTEX_X1_Y1_Z1, VERTEX_X1_Y2_Z1, VERTEX_X2_Y2_Z1 },
		{ VERTEX_X1_Y1_Z2, VERTEX_X2_Y1_Z2, VERTEX_X2_Y2_Z2, VERTEX_X1_Y2_Z2 },
		{ VERTEX_X2_Y1_Z2, VERTEX_X2_Y1_Z1, VERTEX_X2_Y2_Z1, VERTEX_X2_Y2_Z2 },
		{ VERTEX_X1_Y1_Z1, VERTEX_X1_Y1_Z2, VERTEX_X1_Y2_Z2, VERTEX_X1_Y2_Z1 },
	};

	private final Vector3f[][] VERTEX_POSITIONS = new Vector3f[NUM_CUBE_FACES][NUM_FACE_VERTICES];
	private final Vector3f[][] VERTEX_POSITIONS_MIRRORED = new Vector3f[NUM_CUBE_FACES][NUM_FACE_VERTICES];

	private final Vector2f[][] VERTEX_TEXTURES = new Vector2f[NUM_CUBE_FACES][4];
	private final Vector2f[][] VERTEX_TEXTURES_MIRRORED = new Vector2f[NUM_CUBE_FACES][4];

	private final int[] CUBE_NORMALS = new int[NUM_CUBE_FACES];
	private final int[] CUBE_NORMALS_MIRRORED = new int[NUM_CUBE_FACES];

	private void prepareVertices() {
		buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y1_Z1], x1, y1, z1);
		buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y1_Z1], x2, y1, z1);
		buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y2_Z1], x2, y2, z1);
		buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y2_Z1], x1, y2, z1);
		buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y1_Z2], x1, y1, z2);
		buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y1_Z2], x2, y1, z2);
		buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y2_Z2], x2, y2, z2);
		buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y2_Z2], x1, y2, z2);

		buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_Y], u1, v0, u2, v1);
		buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_Y], u2, v1, u3, v0);
		buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_Z], u1, v1, u2, v2);
		buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_Z], u4, v1, u5, v2);
		buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_X], u2, v1, u4, v2);
		buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_X], u0, v1, u1, v2);
	}

	private void prepareNormals() {
		CUBE_NORMALS[FACE_NEG_Y] = NormI8.pack(Direction.DOWN.step());
		CUBE_NORMALS[FACE_POS_Y] = NormI8.pack(Direction.UP.step());
		CUBE_NORMALS[FACE_NEG_Z] = NormI8.pack(Direction.NORTH.step());
		CUBE_NORMALS[FACE_POS_Z] = NormI8.pack(Direction.SOUTH.step());
		CUBE_NORMALS[FACE_POS_X] = NormI8.pack(Direction.WEST.step());
		CUBE_NORMALS[FACE_NEG_X] = NormI8.pack(Direction.EAST.step());

		// When mirroring is used, the normals for EAST and WEST are swapped.
		CUBE_NORMALS_MIRRORED[FACE_NEG_Y] = CUBE_NORMALS[FACE_NEG_Y];
		CUBE_NORMALS_MIRRORED[FACE_POS_Y] = CUBE_NORMALS[FACE_POS_Y];
		CUBE_NORMALS_MIRRORED[FACE_NEG_Z] = CUBE_NORMALS[FACE_NEG_Z];
		CUBE_NORMALS_MIRRORED[FACE_POS_Z] = CUBE_NORMALS[FACE_POS_Z];
		CUBE_NORMALS_MIRRORED[FACE_POS_X] = CUBE_NORMALS[FACE_NEG_X]; // mirrored
		CUBE_NORMALS_MIRRORED[FACE_NEG_X] = CUBE_NORMALS[FACE_POS_X]; // mirrored
	}

	private void buildVertexPosition(Vector3f vector, float x, float y, float z) {
		vector.x = x;
		vector.y = y;
		vector.z = z;
	}

	private void buildVertexTexCoord(Vector2f[] uvs, float u1, float v1, float u2, float v2) {
		uvs[0].set(u2, v1);
		uvs[1].set(u1, v1);
		uvs[2].set(u1, v2);
		uvs[3].set(u2, v2);
	}

	public boolean shouldDrawFace(int quadIndex) {
		return (this.faces & (1 << quadIndex)) != 0;
	}
}
