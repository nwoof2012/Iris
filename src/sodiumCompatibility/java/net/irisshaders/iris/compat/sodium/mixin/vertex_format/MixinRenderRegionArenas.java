package net.irisshaders.iris.compat.sodium.mixin.vertex_format;

import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexType;
import net.irisshaders.iris.block_rendering.BlockRenderingSettings;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.IrisModelVertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderRegion.RenderRegionArenas.class)
public class MixinRenderRegionArenas {
	@Redirect(method = "<init>", remap = false,
		at = @At(value = "FIELD",
			target = "Lme/jellysquid/mods/sodium/client/render/vertex/type/ChunkModelVertexFormats;COMPACT:Lme/jellysquid/mods/sodium/client/render/vertex/type/ChunkVertexType;",
			remap = false))
	private ChunkVertexType iris$useExtendedStride() {
		return BlockRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat() ? IrisModelVertexFormats.MODEL_VERTEX_XHFP : ChunkModelVertexFormats.COMPACT;
	}
}