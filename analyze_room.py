base = r"src\client\resources\assets\vivecraft_theater_mode\theater"

def load(name):
    vs = []
    with open(base + "\\" + name) as f:
        for line in f:
            if line.startswith("v "):
                t = line.split()
                vs.append((float(t[1]), float(t[2]), float(t[3])))
    return vs

room = load("hologram_vr_livingroom.obj")

def ascii_map(vs, x0, x1, z0, z1, w=78, h=40, ymax=None):
    grid = [[0] * w for _ in range(h)]
    for x, y, z in vs:
        if ymax is not None and y > ymax:
            continue
        if x0 <= x < x1 and z0 <= z < z1:
            cx = int((x - x0) / (x1 - x0) * w)
            cz = int((z - z0) / (z1 - z0) * h)
            grid[cz][cx] += 1
    chars = " .:+#@"
    for r, row in enumerate(grid):
        line = "".join(chars[min(len(chars) - 1, c if c < 3 else 2 + min(3, c // 5))] for c in row)
        print(f"{int(z0 + (z1 - z0) * r / h):6} {line}")
    print("       x from", x0, "to", x1, " (top=z" + str(z0) + ")")

print("=== full model, all heights ===")
ascii_map(room, -2400, 700, -400, 1350)
print()
print("=== zoom: x -700..700, z -300..600, y<300 (furniture height) ===")
ascii_map(room, -700, 700, -300, 600, ymax=300)
