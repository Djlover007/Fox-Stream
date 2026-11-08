# Zeichnet das Fox-Stream-Icon (Fuchs mit rotem Play-Knopf) in den Ordner "icons".
from PIL import Image, ImageDraw
import os

OR = (245, 124, 31, 255)
OR2 = (214, 92, 12, 255)
CR = (255, 241, 220, 255)
DK = (42, 22, 8, 255)
BG = (27, 34, 48, 255)
RED = (229, 57, 45, 255)
WH = (255, 255, 255, 255)

HEAD = [(12, 10), (38, 30), (62, 30), (88, 10), (93, 50), (50, 93), (7, 50)]
LC = [(7, 50), (38, 60), (50, 93)]
RC = [(93, 50), (62, 60), (50, 93)]
LE = [(17, 19), (31, 31), (18, 42)]
RE = [(83, 19), (69, 31), (82, 42)]


def fox(d, u, cx, cy):
    def P(pts):
        return [(cx + (x - 50) * u, cy + (y - 50) * u) for x, y in pts]
    d.polygon(P(HEAD), fill=OR)
    d.polygon(P(LC), fill=CR)
    d.polygon(P(RC), fill=CR)
    d.polygon(P(LE), fill=OR2)
    d.polygon(P(RE), fill=OR2)
    d.polygon(P([(30, 47), (42, 52), (31, 54)]), fill=DK)
    d.polygon(P([(70, 47), (58, 52), (69, 54)]), fill=DK)
    d.polygon(P([(43, 81), (57, 81), (50, 93)]), fill=DK)


def play(d, u, cx, cy):
    # roter Play-Knopf unten rechts
    def X(x):
        return cx + (x - 50) * u
    def Y(y):
        return cy + (y - 50) * u
    d.ellipse((X(68), Y(68), X(104), Y(104)), fill=RED, outline=BG, width=int(3 * u))
    d.polygon([(X(81), Y(77)), (X(81), Y(95)), (X(96), Y(86))], fill=WH)


def render(size, mode):
    S = 4
    N = size * S
    im = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if mode == 'fg':
        u = 0.52 * N / 80
    else:
        if mode == 'square':
            d.rounded_rectangle((0, 0, N - 1, N - 1), radius=int(N * 0.22), fill=BG)
        else:
            d.ellipse((0, 0, N - 1, N - 1), fill=BG)
        u = 0.62 * N / 80
    cx, cy = N / 2 - N * 0.05, N / 2 - N * 0.04
    fox(d, u, cx, cy)
    play(d, u, cx, cy)
    return im.resize((size, size), Image.LANCZOS)


DENS = {'mdpi': (48, 108), 'hdpi': (72, 162), 'xhdpi': (96, 216),
        'xxhdpi': (144, 324), 'xxxhdpi': (192, 432)}

for k, (legacy, fg) in DENS.items():
    p = 'icons/mipmap-' + k
    os.makedirs(p, exist_ok=True)
    render(legacy, 'square').save(p + '/ic_launcher.png')
    render(legacy, 'round').save(p + '/ic_launcher_round.png')
    render(fg, 'fg').save(p + '/ic_launcher_foreground.png')
render(512, 'square').save('icon-preview.png')
print('Fuchs-Icons erzeugt')
