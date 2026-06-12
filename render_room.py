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
                    vi = int(p[0]); vi = vi - 1 if vi > 0 else len(vs) + vi
                    ti = 0
                    if len(p) > 1 and p[1]:
                        ti = int(p[1]); ti = ti - 1 if ti > 0 else len(vts) + ti
                    idx.append((vi, ti))
                for i in range(2, len(idx)):
                    tris.append((idx[0], idx[i - 1], idx[i]))
    return vs, vts, tris

tex = Image.open(base + "\\hologram_vr_living_room.png").convert("RGB")
TW, TH = tex.size
tp = tex.load()

models = [load_obj("hologram_vr_livingroom.obj"),
          load_obj("hologram_livingroom_table.obj"),
          load_obj("hologram_livingroom_frame.obj")]

W = H = 500
FOV = 90 * math.pi / 180
F = (W / 2) / math.tan(FOV / 2)

def render(eye, yaw_deg, out):
    yaw = yaw_deg * math.pi / 180
    cy, sy = math.cos(yaw), math.sin(yaw)
    img = Image.new("RGB", (W, H), (20, 20, 40))
    px = img.load()
    zbuf = [[1e30] * W for _ in range(H)]
    for vs, vts, tris in models:
        for (a, b, c) in tris:
            pts = []
            ok = True
            for vi, ti in (a, b, c):
                x, y, z = vs[vi]
                x -= eye[0]; y -= eye[1]; z -= eye[2]
                # rotate around Y by -yaw (camera looks toward -Z after rotation)
                xr = x * cy - z * sy
                zr = x * sy + z * cy
                if zr > -1:  # behind camera; cheap reject whole tri
                    ok = False
                    break
                sxp = W / 2 + F * xr / -zr
                syp = H / 2 - F * y / -zr
                u, v = vts[ti] if ti < len(vts) else (0, 0)
                pts.append((sxp, syp, -zr, u, v))
            if not ok:
                continue
            (x0, y0, z0, u0, v0), (x1, y1, z1, u1, v1), (x2, y2, z2, u2, v2) = pts
            minx = max(0, int(min(x0, x1, x2))); maxx = min(W - 1, int(max(x0, x1, x2)) + 1)
            miny = max(0, int(min(y0, y1, y2))); maxy = min(H - 1, int(max(y0, y1, y2)) + 1)
            if minx > maxx or miny > maxy:
                continue
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

# top-down: eye high above center, looking down -> fake with orthographic top view instead
def topdown(out, x0=-1200, x1=700, z0=-400, z1=1400):
    img = Image.new("RGB", (800, 800), (20, 20, 40))
    px = img.load()
    hbuf = [[-1e30] * 800 for _ in range(800)]
    vs, vts, tris = models[0]
    for (a, b, c) in tris:
        P = []
        for vi, ti in (a, b, c):
            x, y, z = vs[vi]
            sx = (x - x0) / (x1 - x0) * 799
            sz = (z - z0) / (z1 - z0) * 799
            u, v = vts[ti] if ti < len(vts) else (0, 0)
            P.append((sx, sz, y, u, v))
        (x0p, y0p, h0, u0, v0), (x1p, y1p, h1, u1, v1), (x2p, y2p, h2, u2, v2) = P
        minx = max(0, int(min(x0p, x1p, x2p))); maxx = min(799, int(max(x0p, x1p, x2p)) + 1)
        miny = max(0, int(min(y0p, y1p, y2p))); maxy = min(799, int(max(y0p, y1p, y2p)) + 1)
        den = (y1p - y2p) * (x0p - x2p) + (x2p - x1p) * (y0p - y2p)
        if abs(den) < 1e-9:
            continue
        for pyy in range(miny, maxy + 1):
            for pxx in range(minx, maxx + 1):
                w0 = ((y1p - y2p) * (pxx - x2p) + (x2p - x1p) * (pyy - y2p)) / den
                w1 = ((y2p - y0p) * (pxx - x2p) + (x0p - x2p) * (pyy - y2p)) / den
                w2 = 1 - w0 - w1
                if w0 < 0 or w1 < 0 or w2 < 0:
                    continue
                h = w0 * h0 + w1 * h1 + w2 * h2
                if h > 400:  # cut the ceiling so we can see in
                    continue
                if h <= hbuf[pyy][pxx]:
                    continue
                hbuf[pyy][pxx] = h
                u = (w0 * u0 + w1 * u1 + w2 * u2) % 1.0
                v = (w0 * v0 + w1 * v1 + w2 * v2) % 1.0
                px[pxx, pyy] = tp[int(u * (TW - 1)), int((1 - v) * (TH - 1))]
    img.save(out)
    print("saved", out)

eye = (-600, 150, 350)  # on the couch
render(eye, 0, "couch_north.png")    # looking -Z (toward rug/far wall)
render(eye, 90, "couch_rot90.png")
render(eye, 270, "couch_rot270.png")
