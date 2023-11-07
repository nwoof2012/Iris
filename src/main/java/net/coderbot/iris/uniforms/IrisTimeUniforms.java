package net.coderbot.iris.uniforms;

import net.coderbot.iris.gl.uniform.UniformHolder;
import net.coderbot.iris.gl.uniform.UniformUpdateFrequency;
import net.coderbot.iris.vendored.joml.Vector3i;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;

public class IrisTimeUniforms {
	private static LocalDateTime dateTime;

	public static void updateTime() {
		dateTime = LocalDateTime.now();
	}

	public static void addTimeUniforms(UniformHolder uniforms) {
		Vector3i date = new Vector3i();
		Vector3i time = new Vector3i();
		uniforms.uniform3i(UniformUpdateFrequency.PER_TICK, "currentDate", () -> date.set(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth()));
		uniforms.uniform3i(UniformUpdateFrequency.PER_TICK, "currentTime", () -> time.set(dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond()));
	}
}
