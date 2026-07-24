package com.techwave.svcvisualizer.speaker;

/** Turns a raw PCM audio frame into a normalised 0..1 loudness value. */
public final class AudioLevel {

	private AudioLevel() {
	}

	/**
	 * Root-mean-square loudness of a 16-bit PCM frame, normalised and perceptually boosted so that
	 * normal speech lands in the upper half of the 0..1 range.
	 */
	public static float rms(short[] audio) {
		if (audio == null || audio.length == 0) {
			return 0f;
		}
		double sum = 0.0;
		for (short sample : audio) {
			double v = sample / 32768.0;
			sum += v * v;
		}
		double rms = Math.sqrt(sum / audio.length);
		// Speech RMS is typically low; boost and clamp for a lively indicator.
		float level = (float) Math.min(1.0, rms * 3.2);
		return level;
	}
}
