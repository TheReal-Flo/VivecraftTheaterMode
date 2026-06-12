import math
from PIL import Image

base = r"src\client\resources\assets\vivecraft_theater_mode\theater"

def load_obj(name):
    vs, vts, tris = [], [], []
    with open(base + "\\" + name) as f:
        for line in f:
            t = line.split()
            if not t:
                continue
            if t[0] == "v":
                vs.append((float(t[1]), float(t[2]), float(t[3])))
            elif t[0] == "vt":
                vts.append((float(t[1]), float(t[2])))
            elif t[0] == "f":
                idx = []
                for tok in t[1:]:
                    p = tok.split("/")
                    vi = int(p[0]) - 1
                    ti = int(p[1]) - 1 if len(p) > 1 and p[1] else 0
                    idx.append((vi, ti))
                for i in range(2, len(idx)):
                    tris.append((idx[0], idx[i - 1], idx[i]))
    return vs, vts, tris

tex = None
TW = TH = 0
tp = None

def set_tex(name):
    global tex, TW, TH, tp
    tex = Image.open(base + "\\" + name).convert("RGB")
    TW, TH = tex.size
    tp = tex.load()

def render(model, eye, look_scale, out, W=420, H=420):
    vs, vts, tris = model
    F = (W / 2) / math.tan(math.radians(45))
    img = Image.new("RGB", (W, H), (30, 30, 60))
    px = img.load()
    zbuf = [[1e30] * W for _ in range(H)]
    for (a, b, c) in tris:
        pts = []
        ok = True
        for vi, ti in (a, b, c):
            x, y, z = vs[vi]
            x -= eye[0]; y -= eye[1]; z -= eye[2]
            if z > -1:
                ok = False
                break
            sxp = W / 2 + F * x / -z
            syp = H / 2 - F * y / -z
            u, v = vts[ti] if ti < len(vts) else (0, 0)
            pts.append((sxp, syp, -z, u, v))
        if not ok:
            continue
        (x0, y0, z0, u0, v0), (x1, y1, z1, u1, v1), (x2, y2, z2, u2, v2) = pts
        minx = max(0, int(min(x0, x1, x2))); maxx = min(W - 1, int(max(x0, x1, x2)) + 1)
        miny = max(0, int(min(y0, y1, y2))); maxy = min(H - 1, int(max(y0, y1, y2)) + 1)
        den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
        if abs(den) < 1e-9:
            continue
        for pyy in range(miny, maxy + 1):
            for pxx in range(minx, maxx + 1):
                w0 = ((y1 - y2) * (pxx - x2) + (x2 - x1) * (pyy - y2)) / den
                w1 = ((y2 - y0) * (pxx - x2) + (x0 - x2) * (pyy - y2)) / den
                w2 = 1 - w0 - w1
                if w0 < 0 or w1 < 0 or w2 < 0:
                    continue
                z = w0 * z0 + w1 * z1 + w2 * z2
                if z >= zbuf[pyy][pxx]:
                    continue
                zbuf[pyy][pxx] = z
                u = (w0 * u0 + w1 * u1 + w2 * u2) % 1.0
                v = (w0 * v0 + w1 * v1 + w2 * v2) % 1.0
                px[pxx, pyy] = tp[int(u * (TW - 1)), int((1 - v) * (TH - 1))]
    img.save(out)
    print("saved", out)

frame = load_obj("hologram_livingroom_frame.obj")
table = load_obj("hologram_livingroom_table.obj")
set_tex("hologram_wall_rgba.png")
render(frame, (0, 0, 90), 1, "frame_view.png")
render(table, (0, 60, 900), 1, "table_view.png")
