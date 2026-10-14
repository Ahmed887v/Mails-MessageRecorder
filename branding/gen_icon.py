#!/usr/bin/env python3
"""مولّد أيقونة NewRecorder - يرسم PNG بدون مكتبات خارجية (zlib + struct فقط)."""
import zlib, struct, math, os

BG = (0x15, 0x65, 0xC0)
WHITE = (0xFF, 0xFF, 0xFF)
AMBER = (0xF9, 0xA8, 0x25)

def write_png(path, w, h, rgba):
    def chunk(typ, data):
        return (struct.pack('>I', len(data)) + typ + data +
                struct.pack('>I', zlib.crc32(typ + data) & 0xffffffff))
    raw = b''.join(b'\x00' + bytes(rgba[y*w*4:(y+1)*w*4]) for y in range(h))
    data = (b'\x89PNG\r\n\x1a\n' +
            chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) +
            chunk(b'IDAT', zlib.compress(raw, 9)) +
            chunk(b'IEND', b''))
    with open(path, 'wb') as f:
        f.write(data)

def rr_inside(x, y, x0, y0, x1, y1, r):
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    cx = min(max(x, x0 + r), x1 - r)
    cy = min(max(y, y0 + r), y1 - r)
    dx, dy = x - cx, y - cy
    return dx * dx + dy * dy <= r * r

def circle_inside(x, y, cx, cy, r):
    dx, dy = x - cx, y - cy
    return dx * dx + dy * dy <= r * r

def seg_dist(x, y, ax, ay, bx, by):
    vx, vy = bx - ax, by - ay
    wx, wy = x - ax, y - ay
    L2 = vx * vx + vy * vy
    t = 0.0 if L2 == 0 else max(0.0, min(1.0, (wx * vx + wy * vy) / L2))
    px, py = ax + t * vx, ay + t * vy
    return math.hypot(x - px, y - py)

# هندسة معيارية (نسبة إلى 108)
BODY = (27/108, 40/108, 81/108, 78/108, 4/108)
FLAP = [((29/108, 43/108), (54/108, 62/108)), ((54/108, 62/108), (79/108, 43/108))]
TRAY = [((27/108, 78/108), (44/108, 78/108)), ((64/108, 78/108), (81/108, 78/108))]
DOT = (74/108, 33/108, 7/108)
LINE_HW = 1.6/108
MARGIN = 0.06
CORNER = 0.2037

def sample(x, y, rounded):
    if rounded:
        if not circle_inside(x, y, 0.5, 0.5, 0.5):
            return (0, 0, 0, 0)
        col = BG
    else:
        inset = MARGIN
        if not rr_inside(x, y, inset, inset, 1 - inset, 1 - inset, CORNER):
            return (0, 0, 0, 0)
        col = BG
    if circle_inside(x, y, DOT[0], DOT[1], DOT[2]):
        col = AMBER
    if rr_inside(x, y, BODY[0], BODY[1], BODY[2], BODY[3], BODY[4]):
        col = WHITE
    for (a, b) in FLAP:
        if seg_dist(x, y, a[0], a[1], b[0], b[1]) <= LINE_HW:
            col = BG
    for (a, b) in TRAY:
        if seg_dist(x, y, a[0], a[1], b[0], b[1]) <= LINE_HW:
            col = BG
    return col + (255,)

def render(size, rounded=False, ss=3):
    out = bytearray(size * size * 4)
    inv = 1.0 / (size * ss)
    for py in range(size):
        for px in range(size):
            r = g = b = a = 0
            for sy in range(ss):
                for sx in range(ss):
                    x = (px * ss + sx + 0.5) * inv
                    y = (py * ss + sy + 0.5) * inv
                    cr, cg, cb, ca = sample(x, y, rounded)
                    r += cr; g += cg; b += cb; a += ca
            n = ss * ss
            i = (py * size + px) * 4
            out[i] = r // n; out[i+1] = g // n; out[i+2] = b // n; out[i+3] = a // n
    return out

if __name__ == '__main__':
    base = os.path.dirname(os.path.abspath(__file__))
    res = os.path.abspath(os.path.join(base, '..', 'app', 'src', 'main', 'res'))
    densities = {
        'mipmap-mdpi': 48, 'mipmap-hdpi': 72, 'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144, 'mipmap-xxxhdpi': 192
    }
    for folder, size in densities.items():
        d = os.path.join(res, folder)
        os.makedirs(d, exist_ok=True)
        write_png(os.path.join(d, 'ic_launcher.png'), size, size, render(size, rounded=False))
        write_png(os.path.join(d, 'ic_launcher_round.png'), size, size, render(size, rounded=True))
        print('wrote', folder, size)

    # نسخة العلامة التجارية عالية الدقة للتحميل/العرض
    branding = os.path.join(res, '..', '..', '..', '..', 'branding')
    branding = os.path.abspath(branding)
    os.makedirs(branding, exist_ok=True)
    for s in (512, 192):
        write_png(os.path.join(branding, f'icon-{s}.png'), s, s, render(s, rounded=False, ss=2))
        print('wrote branding icon-%d.png' % s)
