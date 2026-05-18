import argparse
import math
import subprocess
from pathlib import Path

import cv2
import numpy as np


def run(command: list[str], *, input_bytes: bytes | None = None) -> bytes:
    process = subprocess.run(
        command,
        input=input_bytes,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=True,
    )
    return process.stdout


def decode_audio(path: Path, sample_rate: int) -> np.ndarray:
    raw = run(
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
        ]
    )
    audio = np.frombuffer(raw, dtype=np.float32).copy()
    peak = float(np.max(np.abs(audio))) if audio.size else 1.0
    if peak > 0:
        audio /= peak
    return audio


def make_log_bins(sample_rate: int, fft_size: int, bar_count: int) -> list[np.ndarray]:
    freqs = np.fft.rfftfreq(fft_size, 1.0 / sample_rate)
    edges = np.geomspace(70, 9000, bar_count + 1)
    bins: list[np.ndarray] = []
    for start, end in zip(edges[:-1], edges[1:]):
        idx = np.where((freqs >= start) & (freqs < end))[0]
        if idx.size == 0:
            idx = np.array([int(np.argmin(np.abs(freqs - (start + end) * 0.5)))])
        bins.append(idx)
    return bins


def compute_features(audio: np.ndarray, sample_rate: int, fps: int, bar_count: int) -> tuple[np.ndarray, np.ndarray]:
    frame_count = int(math.ceil(audio.size / sample_rate * fps))
    fft_size = 4096
    half = fft_size // 2
    window = np.hanning(fft_size).astype(np.float32)
    bins = make_log_bins(sample_rate, fft_size, bar_count)
    spectra = np.zeros((frame_count, bar_count), dtype=np.float32)
    envelopes = np.zeros(frame_count, dtype=np.float32)

    for frame in range(frame_count):
        center = int(frame / fps * sample_rate)
        start = center - half
        end = start + fft_size
        segment = np.zeros(fft_size, dtype=np.float32)
        src_start = max(0, start)
        src_end = min(audio.size, end)
        if src_end > src_start:
            dst_start = src_start - start
            segment[dst_start : dst_start + (src_end - src_start)] = audio[src_start:src_end]

        rms = float(np.sqrt(np.mean(segment * segment)))
        envelopes[frame] = rms

        mag = np.abs(np.fft.rfft(segment * window))
        for bar_idx, bin_idx in enumerate(bins):
            spectra[frame, bar_idx] = float(np.mean(mag[bin_idx]))

    spectra = np.log1p(spectra * 18.0)
    global_scale = float(np.percentile(spectra, 99.2)) or 1.0
    spectra = np.clip(spectra / global_scale, 0.0, 1.0)

    env_scale = float(np.percentile(envelopes, 96.0)) or 1.0
    envelopes = np.clip(envelopes / env_scale, 0.0, 1.0)

    kernel = np.array([0.06, 0.14, 0.24, 0.32, 0.24, 0.14, 0.06], dtype=np.float32)
    kernel /= kernel.sum()
    for frame in range(frame_count):
        spectra[frame] = np.convolve(spectra[frame], kernel, mode="same")

    for frame in range(1, frame_count):
        spectra[frame] = spectra[frame - 1] * 0.72 + spectra[frame] * 0.28
        envelopes[frame] = envelopes[frame - 1] * 0.68 + envelopes[frame] * 0.32

    return spectra, envelopes


def make_background(width: int, height: int, rng: np.random.Generator) -> np.ndarray:
    y, x = np.mgrid[0:height, 0:width]
    cx = width * 0.5
    cy = height * 0.5
    radial = np.exp(-(((x - cx) / (width * 0.52)) ** 2 + ((y - cy) / (height * 0.23)) ** 2))
    bg = np.zeros((height, width, 3), dtype=np.float32)
    bg[..., 0] = 2 + radial * 10
    bg[..., 1] = 3 + radial * 18
    bg[..., 2] = 7 + radial * 36

    # Subtle vertical texture keeps the frame from looking like a flat generated waveform.
    for _ in range(260):
        x_pos = int(rng.integers(0, width))
        alpha = float(rng.uniform(4, 22))
        length = int(rng.integers(height * 0.16, height * 0.9))
        top = int(rng.integers(0, height - length))
        color = np.array([alpha * 1.6, alpha * 1.0, alpha * 0.25], dtype=np.float32)
        bg[top : top + length, max(0, x_pos - 1) : min(width, x_pos + 1)] += color

    noise = rng.normal(0, 1.15, bg.shape).astype(np.float32)
    bg += noise
    return np.clip(bg, 0, 255).astype(np.uint8)


def draw_frame(
    background: np.ndarray,
    bars: np.ndarray,
    envelope: float,
    frame_idx: int,
    rng: np.random.Generator,
) -> np.ndarray:
    height, width = background.shape[:2]
    cy = height // 2
    left = int(width * 0.075)
    right = int(width * 0.925)
    xs = np.linspace(left, right, bars.size)

    frame = background.copy()
    glow = np.zeros_like(frame)
    sharp = np.zeros_like(frame)
    dust = np.zeros_like(frame)

    pulse = 0.86 + 0.14 * math.sin(frame_idx * 0.055)
    main_limit = height * 0.115
    tail_limit = height * 0.235

    for i, (x_float, value) in enumerate(zip(xs, bars)):
        x = int(round(x_float + math.sin(frame_idx * 0.017 + i * 1.71) * 1.6))
        local = float(np.clip(value, 0.0, 1.0))
        phase = 0.5 + 0.5 * math.sin(frame_idx * 0.021 + i * 0.37)
        irregular = 0.72 + 0.34 * math.sin(frame_idx * 0.043 + i * 2.13)
        height_main = 4 + (local ** 0.72) * main_limit * (0.58 + envelope * 0.32) * pulse * irregular
        height_main += phase * 4.0

        # Faint tails create the stock-footage light-beam feel without making the main waveform jump.
        tail_boost = 1.0 if i % 8 else 1.45 + envelope * 0.45
        height_tail = min(tail_limit, height_main * (1.35 + 0.55 * phase) * tail_boost)

        blue = int(155 + 95 * local)
        green = int(42 + 132 * local)
        color_core = (blue, green, 12)
        color_glow = (105, 42 + int(72 * local), 4)

        thickness = 1
        if local > 0.76 and i % 9 == 0:
            thickness = 2

        y1 = int(cy - height_main)
        y2 = int(cy + height_main)
        yt1 = int(cy - height_tail)
        yt2 = int(cy + height_tail)

        cv2.line(glow, (x, yt1), (x, yt2), color_glow, thickness + 2, cv2.LINE_AA)
        cv2.line(sharp, (x, y1), (x, y2), color_core, thickness, cv2.LINE_AA)

        if local > 0.42 and i % 7 == 0:
            for _ in range(2):
                dot_y = int(cy + rng.choice([-1, 1]) * rng.uniform(height_main, height_tail))
                cv2.circle(dust, (x + int(rng.integers(-2, 3)), dot_y), 1, (190, 155, 40), -1, cv2.LINE_AA)

    cv2.line(glow, (left, cy), (right, cy), (42, 24, 4), 1, cv2.LINE_AA)

    glow = cv2.GaussianBlur(glow, (0, 0), 5)
    dust = cv2.GaussianBlur(dust, (0, 0), 1.2)
    frame = cv2.addWeighted(frame, 1.0, glow, 0.58, 0)
    frame = cv2.addWeighted(frame, 1.0, dust, 0.65, 0)
    frame = cv2.addWeighted(frame, 1.0, sharp, 0.86, 0)

    vignette = np.linspace(-1, 1, width, dtype=np.float32)[None, :] ** 2
    vignette = 1.0 - 0.52 * vignette
    frame = (frame.astype(np.float32) * vignette[..., None]).clip(0, 255).astype(np.uint8)
    return frame


def render_video(input_audio: Path, output_video: Path, width: int, height: int, fps: int) -> None:
    sample_rate = 44100
    bar_count = 270
    output_video.parent.mkdir(parents=True, exist_ok=True)

    audio = decode_audio(input_audio, sample_rate)
    spectra, envelopes = compute_features(audio, sample_rate, fps, bar_count)
    frame_count = spectra.shape[0]

    rng = np.random.default_rng(20260514)
    background = make_background(width, height, rng)

    ffmpeg = subprocess.Popen(
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

    if ffmpeg.stdin is None:
        raise RuntimeError("Unable to open FFmpeg stdin.")

    try:
        for frame_idx in range(frame_count):
            frame_rng = np.random.default_rng(9000 + frame_idx)
            frame = draw_frame(background, spectra[frame_idx], float(envelopes[frame_idx]), frame_idx, frame_rng)
            ffmpeg.stdin.write(frame.tobytes())
    finally:
        ffmpeg.stdin.close()

    return_code = ffmpeg.wait()
    if return_code != 0:
        raise RuntimeError(f"FFmpeg failed with exit code {return_code}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Render an Aurora-style voice-driven spectrum animation.")
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--width", type=int, default=1920)
    parser.add_argument("--height", type=int, default=1080)
    parser.add_argument("--fps", type=int, default=30)
    args = parser.parse_args()

    render_video(args.input, args.output, args.width, args.height, args.fps)


if __name__ == "__main__":
    main()
