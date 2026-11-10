"""Render the BugList jewelry sprite from Blender. Run in background with --python.

Pass the output directory after --. The numbered transparent renders are packed
left to right into an atlas by pack_gold_charm.py.
"""

import math
from pathlib import Path
import sys

import bpy
from mathutils import Vector


out = Path(sys.argv[sys.argv.index("--") + 1]).resolve()
out.mkdir(parents=True, exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
scene = bpy.context.scene

charm = bpy.data.objects.new("Mr. L collectible charm", None)
scene.collection.objects.link(charm)


def material(name, color, metal, roughness):
    result = bpy.data.materials.new(name)
    result.diffuse_color = (*color, 1)
    result.use_nodes = True
    surface = result.node_tree.nodes.get("Principled BSDF")
    surface.inputs["Base Color"].default_value = (*color, 1)
    surface.inputs["Metallic"].default_value = metal
    surface.inputs["Roughness"].default_value = roughness
    return result


gold = material("Polished gold", (0.90, 0.51, 0.12), 0.84, 0.24)
satin = material("Satin gold", (0.73, 0.41, 0.12), 0.72, 0.32)
edge = material("Bright gold", (0.97, 0.73, 0.34), 0.7, 0.24)
shadow_gold = material("Recessed antique gold", (0.25, 0.10, 0.027), 0.63, 0.39)
enamel = material("Black enamel", (0.012, 0.009, 0.006), 0.08, 0.38)
inlay = material("Gold lettering", (0.91, 0.62, 0.24), 0.45, 0.28)


def xy(x, y):
    return (x - 90) / 18, (50 - y) / 18


def plate(name, points, front, depth, radius, finish):
    outline = [xy(x, y) for x, y in points]
    count = len(outline)
    vertices = [(x, y, z) for z in (front - depth, front) for x, y in outline]
    faces = [tuple(range(count)), tuple(reversed(range(count, 2 * count)))]
    for index in range(count):
        following = (index + 1) % count
        faces.append((index, following, following + count, index + count))
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(vertices, [], faces)
    obj = bpy.data.objects.new(name, mesh)
    scene.collection.objects.link(obj)
    obj.parent = charm
    obj.data.materials.append(finish)
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.mesh.normals_make_consistent(inside=False)
    bpy.ops.object.mode_set(mode="OBJECT")
    obj.select_set(False)
    bevel = obj.modifiers.new("Jewelry bevel", "BEVEL")
    bevel.width = radius
    bevel.segments = 3
    bevel.harden_normals = True
    obj.modifiers.new("Polished face normals", "WEIGHTED_NORMAL")
    return obj


plate("Cast silhouette", [
    (7, 40), (22, 40), (22, 42), (34, 42), (35, 37), (40, 37), (41, 41),
    (48, 41), (50, 35), (61, 35), (71, 29), (121, 29), (128, 36),
    (138, 40), (165, 24), (171, 26), (169, 49), (151, 52), (135, 49),
    (124, 51), (116, 58), (96, 58), (93, 55), (75, 56), (61, 53),
    (48, 48), (21, 48), (21, 50), (7, 50)
], 0.25, 0.63, 0.12, gold)
plate("Curved ornament", [
    (76, 52), (96, 53), (94, 63), (91, 71), (85, 80), (74, 90),
    (59, 82), (66, 76), (72, 67)
], 0.23, 0.49, 0.10, gold)
plate("Lower gold accent", [(102, 51), (113, 51), (122, 72), (111, 77), (102, 60)],
      0.19, 0.43, 0.09, satin)
plate("Upper engraving lip", [(71, 31), (121, 31), (124, 34), (73, 34)],
      0.29, 0.09, 0.03, edge)
plate("Tail inlay", [(139, 42), (164, 28), (166, 29), (164, 46), (150, 49), (140, 46)],
      0.30, 0.10, 0.05, satin)
plate("Front inlay", [(45, 42), (53, 38), (67, 36), (67, 50), (61, 50), (49, 46), (45, 46)],
      0.30, 0.10, 0.045, satin)
plate("Long polished edge", [(8, 41), (21, 41), (21, 43), (47, 43), (47, 45), (8, 44)],
      0.29, 0.06, 0.026, edge)
plate("Fine tail engraving", [(143, 44), (162, 33), (162, 35), (143, 46)],
      0.32, 0.035, 0.016, edge)
plate("Fine front engraving", [(50, 43), (63, 39), (63, 41), (50, 45)],
      0.32, 0.035, 0.016, edge)
plate("Upper shadow line", [(74, 35), (122, 35), (124, 37), (72, 37)],
      0.31, 0.035, 0.014, shadow_gold)
plate("Tail shadow line", [(142, 46), (161, 37), (161, 39), (145, 48)],
      0.325, 0.034, 0.014, shadow_gold)
plate("Lower panel lip", [(75, 53), (120, 53), (117, 55), (78, 55)],
      0.32, 0.035, 0.018, edge)
plate("Grip accent", [(105, 57), (111, 56), (117, 70), (112, 72)],
      0.24, 0.09, 0.048, gold)
plate("Black nameplate", [(71, 37), (120, 37), (126, 41), (122, 53), (73, 53), (69, 49)],
      0.31, 0.12, 0.05, enamel)

for x, y in ((74, 62), (80, 63), (86, 63)):
    plate("Decorative fluting", [(x, y), (x+2, y), (x-2, y+12), (x-8, y+21),
                                  (x-10, y+20), (x-4, y+11)], 0.26, 0.06, 0.025, shadow_gold)

for x, y in ((75, 40.5), (120, 41), (55, 46), (154, 44)):
    radius = 1.15
    outline = [
        (x + radius * math.cos(index * math.tau / 12), y + radius * math.sin(index * math.tau / 12))
        for index in range(12)
    ]
    plate("Gold pin detail", outline, 0.355, 0.07, 0.018, edge)

plate("Eyelet mount", [(86, 23), (94, 23), (97, 31), (83, 31)],
      0.22, 0.48, 0.07, satin)
bpy.ops.mesh.primitive_torus_add(
    major_radius=0.39, minor_radius=0.115, major_segments=36, minor_segments=12,
    location=(*xy(90, 16), 0.04)
)
ring = bpy.context.object
ring.name = "Jewelry eyelet"
ring.parent = charm
ring.data.materials.append(gold)
for face in ring.data.polygons:
    face.use_smooth = True
ring.select_set(False)

lettering = bpy.data.curves.new("Mr. L", "FONT")
lettering.body = "Mr. L"
lettering.align_x = "CENTER"
lettering.align_y = "CENTER"
lettering.size = 0.88
lettering.extrude = 0.011
lettering.bevel_depth = 0.006
font_file = Path("C:/Windows/Fonts/segoeuib.ttf")
if font_file.exists():
    lettering.font = bpy.data.fonts.load(str(font_file))
nameplate = bpy.data.objects.new("Mr. L inlay", lettering)
scene.collection.objects.link(nameplate)
nameplate.location = (*xy(97, 45), 0.324)
nameplate.parent = charm
nameplate.data.materials.append(inlay)
bpy.context.view_layer.objects.active = nameplate
nameplate.select_set(True)
bpy.ops.object.convert(target="MESH")
bpy.context.object.select_set(False)


def aim(obj, point):
    obj.rotation_euler = (Vector(point) - obj.location).to_track_quat("-Z", "Y").to_euler()


def softbox(name, position, power, size, color, target=(0, 0, 0)):
    light = bpy.data.lights.new(name, "AREA")
    light.energy = power
    light.shape = "DISK"
    light.size = size
    light.color = color
    obj = bpy.data.objects.new(name, light)
    scene.collection.objects.link(obj)
    obj.location = position
    aim(obj, target)


softbox("Long warm key", (-2, 5, 6), 1150, 6.2, (1, 0.91, 0.74))
softbox("Cream front fill", (2, 0.8, 7), 420, 7, (1, 0.98, 0.93))
softbox("Cool edge separation", (5, 1, -2), 1300, 4, (0.70, 0.81, 1))
softbox("Lower bounce", (-3, -4, 4), 460, 4, (1, 0.88, 0.65))

world = bpy.data.worlds.new("Studio")
world.use_nodes = True
world.node_tree.nodes.get("Background").inputs["Color"].default_value = (0.22, 0.22, 0.22, 1)
world.node_tree.nodes.get("Background").inputs["Strength"].default_value = 0.48
scene.world = world

camera = bpy.data.objects.new("Orthographic artwork camera", bpy.data.cameras.new("Artwork camera"))
scene.collection.objects.link(camera)
camera.location = (0, 0, 20)
aim(camera, (0, 0, 0))
camera.data.type = "ORTHO"
camera.data.ortho_scale = 10.25
scene.camera = camera

scene.render.engine = "CYCLES"
scene.cycles.samples = 32
scene.cycles.use_denoising = True
scene.cycles.max_bounces = 4
scene.render.resolution_x = 400
scene.render.resolution_y = 224
scene.render.resolution_percentage = 100
scene.render.film_transparent = True
scene.render.image_settings.file_format = "PNG"
scene.render.image_settings.color_mode = "RGBA"
scene.view_settings.view_transform = "AgX"
scene.view_settings.look = "AgX - Medium High Contrast"

bpy.context.preferences.filepaths.save_version = 0
charm.rotation_euler = (math.radians(-4), math.radians(-7), 0)
bpy.ops.wm.save_as_mainfile(filepath=str(out / "BugList-Mr-L.blend"))
for frame in [17, *range(17), *range(18, 35)]:
    sweep = (frame - 17) / 17
    charm.rotation_euler = (math.radians(-4 + 12 * sweep), math.radians(-7 + 43 * sweep), 0)
    scene.render.filepath = str(out / f"charm-{frame:02d}.png")
    bpy.ops.render.render(write_still=True)
