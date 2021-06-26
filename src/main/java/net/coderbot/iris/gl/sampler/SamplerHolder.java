package net.coderbot.iris.gl.sampler;

import java.util.function.IntSupplier;

public interface SamplerHolder {
	void addExternalSampler(int textureUnit, String... names);
	boolean hasSampler(String name);
	boolean addDynamicSampler(IntSupplier sampler, Runnable postBind, String... names);

	default boolean addDynamicSampler(IntSupplier sampler, String... names) {
		return addDynamicSampler(sampler, () -> {}, names);
	}
}
