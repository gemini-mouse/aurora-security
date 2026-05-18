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


def build_envelope(audio: np.ndarray, sample_rate: int, envelope_rate: int = 240) -> np.ndarray:
    hop = max(1, sample_rate // envelope_rate)
    frame_count = int(math.ceil(audio.size / hop))
    env = np.zeros(frame_count, dtype=np.float32)
    for idx in range(frame_count):
        start = idx * hop
        end = min(audio.size, start + hop)
        if end > start:
            chunk = audio[start:end]
            env[idx] = float(np.sqrt(np.mean(chunk * chunk)))

    scale = float(np.percentile(env, 97.5)) or 1.0
    env = np.clip(env / scale, 0.0, 1.0)

    # Smooth enough for speech, but keep attacks so occasional spikes remain visible.
    smoothed = env.copy()
    for idx in range(1, smoothed.size):
        attack = 0.42 if env[idx] > smoothed[idx - 1] else 0.16
        smoothed[idx] = smoothed[idx - 1] * (1.0 - attack) + env[idx] * attack
    return smoothed


def sample_envelope(envelope: np.ndarray, time_seconds: float, envelope_rate: int) -> float:
    pos = time_seconds * envelope_rate
    if pos <= 0:
        return 0.0
    if pos >= envelope.size - 1:
        return 0.0
    left = int(pos)
    frac = pos - left
    return float(envelope[left] * (1.0 - frac) + envelope[left + 1] * frac)


def make_background(width: int, height: int) -> np.ndarray:
    y, x = np.mgrid[0:height, 0:width]
    cx = width * 0.5
    cy = height * 0.5
    radial = np.exp(-(((x - cx) / (width * 0.48)) ** 2 + ((y - cy) / (height * 0.22)) ** 2))
    bg = np.zeros((height, width, 3), dtype=np.float32)
    bg[..., 1] = radial * 7.0
    bg[..., 0] = radial * 2.0
    bg += np.random.default_rng(1405).normal(0, 0.55, bg.shape)
    return np.clip(bg, 0, 255).astype(np.uint8)


def draw_frame(
    background: np.ndarray,
    envelope: np.ndarray,
    frame_idx: int,
    fps: int,
    envelope_rate: int,
) -> np.ndarray:
    height, width = background.shape[:2]
    cy = height // 2
    t = frame_idx / fps
    frame = background.copy()
    glow = np.zeros_like(frame)
    core = np.zeros_like(frame)

    left = int(width * 0.055)
    right = int(width * 0.945)
    bar_count = 255
    xs = np.linspace(left, right, bar_count)
    window_seconds = 5.6

    rng = np.random.default_rng(7000 + frame_idx)
    current_amp = sample_envelope(envelope, t, envelope_rate)

    center_color = (14, 116, 18)
    cv2.line(core, (left, cy), (right, cy), center_color, 1, cv2.LINE_AA)
    cv2.line(glow, (left, cy), (right, cy), (4, 70, 8), 1, cv2.LINE_AA)

    for i, x_float in enumerate(xs):
        x = int(round(x_float))
        local_time = t - window_seconds + (i / (bar_count - 1)) * window_seconds
        amp = sample_envelope(envelope, local_time, envelope_rate)
        if amp <= 0.002:
            amp = current_amp * 0.025

        micro = 0.72 + 0.28 * math.sin(frame_idx * 0.13 + i * 0.91)
        noise = 0.88 + 0.24 * rng.random()
        base_height = 2.0 + (amp ** 0.78) * height * 0.055 * micro * noise

        # Sparse taller spikes, triggered only by stronger speech moments.
        spike = 0.0
        if amp > 0.42 and i % 37 in (0, 1, 2):
            spike = (amp - 0.42) * height * 0.19 * (0.7 + 0.6 * rng.random())
        elif amp > 0.64 and i % 19 == 0:
            spike = (amp - 0.55) * height * 0.11

        h = min(height * 0.19, base_height + spike)
        y1 = int(cy - h)
        y2 = int(cy + h)

        brightness = int(118 + min(1.0, amp * 1.3) * 120)
        color = (18, brightness, 24)
        dim_color = (8, int(brightness * 0.48), 10)

        thickness = 1
        if spike > 12:
            thickness = 2

        cv2.line(core, (x, y1), (x, y2), color, thickness, cv2.LINE_AA)
        cv2.line(glow, (x, y1), (x, y2), dim_color, thickness + 1, cv2.LINE_AA)

        if i % 5 == 0:
            dot_h = max(1, int(base_height * 0.13))
            cv2.line(core, (x, cy - dot_h), (x, cy + dot_h), (10, 160, 16), 1, cv2.LINE_AA)

    glow = cv2.GaussianBlur(glow, (0, 0), 3.2)
    frame = cv2.addWeighted(frame, 1.0, glow, 0.36, 0)
    frame = cv2.addWeighted(frame, 1.0, core, 0.95, 0)

    vignette_x = np.linspace(-1, 1, width, dtype=np.float32)[None, :] ** 2
    vignette_y = np.linspace(-1, 1, height, dtype=np.float32)[:, None] ** 2
    vignette = 1.0 - 0.38 * vignette_x - 0.22 * vignette_y
    return np.clip(frame.astype(np.float32) * vignette[..., None], 0, 255).astype(np.uint8)


def render(input_audio: Path, output_video: Path, width: int, height: int, fps: int) -> None:
    sample_rate = 44100
    envelope_rate = 240
    audio = decode_audio(input_audio, sample_rate)
    envelope = build_envelope(audio, sample_rate, envelope_rate)
    duration = audio.size / sample_rate
    frame_count = int(math.ceil(duration * fps))
    output_video.parent.mkdir(parents=True, exist_ok=True)

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
        for frame_idx in range(frame_count):
            frame = draw_frame(background, envelope, frame_idx, fps, envelope_rate)
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
