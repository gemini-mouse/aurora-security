import argparse
import math
import subprocess
from pathlib import Path

import cv2
import numpy as np


def decode_audio(path: Path, sample_rate: int) -> np.ndarray:
    process = subprocess.run(
        [
            "ffmpeg",
            "-v",
            "error",
            "-i",
            str(path),
            "-ac",
            "1",
            "-ar",
            str(sample_rate),
            "-f",
            "f32le",
            "-",
        ],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=True,
    )
    audio = np.frombuffer(process.stdout, dtype=np.float32).copy()
    peak = float(np.max(np.abs(audio))) if audio.size else 1.0
    if peak > 0:
        audio /= peak
    return audio


def log_bins(sample_rate: int, fft_size: int, count: int) -> list[np.ndarray]:
    freqs = np.fft.rfftfreq(fft_size, 1.0 / sample_rate)
    edges = np.geomspace(80, 7600, count + 1)
    bins: list[np.ndarray] = []
    for start, end in zip(edges[:-1], edges[1:]):
        idx = np.where((freqs >= start) & (freqs < end))[0]
        if idx.size == 0:
            idx = np.array([int(np.argmin(np.abs(freqs - (start + end) * 0.5)))])
        bins.append(idx)
    return bins


def compute_features(audio: np.ndarray, sample_rate: int, fps: int, bar_count: int) -> tuple[np.ndarray, np.ndarray]:
    frame_count = int(math.ceil(audio.size / sample_rate * fps))
    fft_size = 2048
    half = fft_size // 2
    window = np.hanning(fft_size).astype(np.float32)
    bins = log_bins(sample_rate, fft_size, bar_count)

    rms = np.zeros(frame_count, dtype=np.float32)
    spectrum = np.zeros((frame_count, bar_count), dtype=np.float32)

    for frame_idx in range(frame_count):
        center = int(frame_idx / fps * sample_rate)
        start = center - half
        end = start + fft_size
        segment = np.zeros(fft_size, dtype=np.float32)
        src_start = max(0, start)
        src_end = min(audio.size, end)
        if src_end > src_start:
            dst_start = src_start - start
            segment[dst_start : dst_start + (src_end - src_start)] = audio[src_start:src_end]

        rms[frame_idx] = float(np.sqrt(np.mean(segment * segment)))
        mag = np.abs(np.fft.rfft(segment * window))
        for bar_idx, idx in enumerate(bins):
            spectrum[frame_idx, bar_idx] = float(np.mean(mag[idx]))

    rms_scale = float(np.percentile(rms, 96.5)) or 1.0
    rms = np.clip(rms / rms_scale, 0.0, 1.0)

    spectrum = np.log1p(spectrum * 16.0)
    spec_scale = float(np.percentile(spectrum, 99.0)) or 1.0
    spectrum = np.clip(spectrum / spec_scale, 0.0, 1.0)

    kernel = np.array([0.08, 0.18, 0.28, 0.18, 0.08], dtype=np.float32)
    kernel /= kernel.sum()
    for frame_idx in range(frame_count):
        spectrum[frame_idx] = np.convolve(spectrum[frame_idx], kernel, mode="same")

    for frame_idx in range(1, frame_count):
        attack = 0.45 if rms[frame_idx] > rms[frame_idx - 1] else 0.18
        rms[frame_idx] = rms[frame_idx - 1] * (1.0 - attack) + rms[frame_idx] * attack
        spectrum[frame_idx] = spectrum[frame_idx - 1] * 0.58 + spectrum[frame_idx] * 0.42

    return rms, spectrum


def make_background(width: int, height: int) -> np.ndarray:
    y, x = np.mgrid[0:height, 0:width]
    cx = width * 0.5
    cy = height * 0.5
    radial = np.exp(-(((x - cx) / (width * 0.42)) ** 2 + ((y - cy) / (height * 0.2)) ** 2))
    bg = np.zeros((height, width, 3), dtype=np.float32)
    bg[..., 1] = radial * 5.0
    bg += np.random.default_rng(514).normal(0, 0.45, bg.shape)
    return np.clip(bg, 0, 255).astype(np.uint8)


def draw_frame(
    background: np.ndarray,
    amp: float,
    spec: np.ndarray,
    frame_idx: int,
    spike_profile: np.ndarray,
) -> np.ndarray:
    height, width = background.shape[:2]
    cy = height // 2
    left = int(width * 0.055)
    right = int(width * 0.945)
    xs = np.linspace(left, right, spec.size)

    frame = background.copy()
    glow = np.zeros_like(frame)
    core = np.zeros_like(frame)

    amp = float(np.clip(amp, 0.0, 1.0))
    base_pulse = 0.72 + 0.28 * math.sin(frame_idx * 0.33)
    center_color = (12, 92, 12)
    cv2.line(core, (left, cy), (right, cy), center_color, 1, cv2.LINE_AA)

    for i, (x_float, s) in enumerate(zip(xs, spec)):
        x = int(round(x_float))
        phase = math.sin(frame_idx * 0.19 + i * 0.73)
        local_texture = 0.74 + 0.26 * phase
        local_energy = 0.36 + 0.64 * float(s)

        # Static layout: every column stays at the same x-position, only height changes with current sound.
        h = 1.8 + (amp ** 0.82) * height * 0.088 * local_energy * local_texture * base_pulse
        h += (amp ** 1.15) * height * 0.078 * spike_profile[i]

        if amp < 0.035:
            h = 1.2 + 2.0 * local_texture

        h = min(h, height * 0.245)
        y1 = int(cy - h)
        y2 = int(cy + h)

        brightness = int(88 + 155 * min(1.0, amp * 1.25 + s * 0.25))
        color = (14, brightness, 20)
        glow_color = (4, int(brightness * 0.45), 5)

        thickness = 1
        if h > height * 0.09 and spike_profile[i] > 0.55:
            thickness = 2

        cv2.line(core, (x, y1), (x, y2), color, thickness, cv2.LINE_AA)
        cv2.line(glow, (x, y1), (x, y2), glow_color, thickness + 1, cv2.LINE_AA)

    glow = cv2.GaussianBlur(glow, (0, 0), 2.4)
    frame = cv2.addWeighted(frame, 1.0, glow, 0.28, 0)
    frame = cv2.addWeighted(frame, 1.0, core, 0.98, 0)

    vignette_x = np.linspace(-1, 1, width, dtype=np.float32)[None, :] ** 2
    vignette_y = np.linspace(-1, 1, height, dtype=np.float32)[:, None] ** 2
    vignette = 1.0 - 0.36 * vignette_x - 0.2 * vignette_y
    return np.clip(frame.astype(np.float32) * vignette[..., None], 0, 255).astype(np.uint8)


def render(input_audio: Path, output_video: Path, width: int, height: int, fps: int) -> None:
    sample_rate = 44100
    bar_count = 300
    audio = decode_audio(input_audio, sample_rate)
    rms, spectrum = compute_features(audio, sample_rate, fps, bar_count)
    output_video.parent.mkdir(parents=True, exist_ok=True)

    rng = np.random.default_rng(20260515)
    spike_profile = rng.beta(0.7, 5.5, bar_count).astype(np.float32)
    for idx in [8, 41, 72, 138, 198, 236, 288]:
        if idx < bar_count:
            spike_profile[max(0, idx - 1) : min(bar_count, idx + 2)] += np.array([0.7, 1.15, 0.55], dtype=np.float32)[
                : min(bar_count, idx + 2) - max(0, idx - 1)
            ]
    spike_profile = np.clip(spike_profile, 0.0, 1.0)
    background = make_background(width, height)

    process = subprocess.Popen(
        [
            "ffmpeg",
            "-y",
            "-v",
            "error",
            "-f",
            "rawvideo",
            "-pix_fmt",
            "bgr24",
            "-s",
            f"{width}x{height}",
            "-r",
            str(fps),
            "-i",
            "-",
            "-i",
            str(input_audio),
            "-map",
            "0:v:0",
            "-map",
            "1:a:0",
            "-c:v",
            "libx264",
            "-preset",
            "veryfast",
            "-crf",
            "18",
            "-pix_fmt",
            "yuv420p",
            "-c:a",
            "aac",
            "-b:a",
            "192k",
            "-shortest",
            str(output_video),
        ],
        stdin=subprocess.PIPE,
    )
    if process.stdin is None:
        raise RuntimeError("Failed to open FFmpeg stdin.")

    try:
        for frame_idx in range(rms.size):
            frame = draw_frame(background, float(rms[frame_idx]), spectrum[frame_idx], frame_idx, spike_profile)
            process.stdin.write(frame.tobytes())
    finally:
        process.stdin.close()

    return_code = process.wait()
    if return_code != 0:
        raise RuntimeError(f"FFmpeg failed with exit code {return_code}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--width", type=int, default=1920)
    parser.add_argument("--height", type=int, default=1080)
    parser.add_argument("--fps", type=int, default=30)
    args = parser.parse_args()
    render(args.input, args.output, args.width, args.height, args.fps)


if __name__ == "__main__":
    main()
