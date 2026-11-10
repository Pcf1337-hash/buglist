"""Pack 35 Blender PNG renders into the app's transparent WebP atlas."""

import argparse
from pathlib import Path

from PIL import Image


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("renders", type=Path)
parser.add_argument("output", type=Path)
args = parser.parse_args()
width, height = (400, 224)
columns, rows = (5, 7)
atlas = Image.new("RGBA", (width * columns, height * rows))
for index in range(columns * rows):
    with Image.open(args.renders / f"charm-{index:02d}.png") as frame:
        if frame.size != (width, height):
            raise ValueError(f"Unexpected dimensions in frame {index}")
        atlas.paste(frame, ((index % columns) * width, (index // columns) * height))
args.output.parent.mkdir(parents=True, exist_ok=True)
atlas.save(args.output, format="WEBP", lossless=True, method=6)
