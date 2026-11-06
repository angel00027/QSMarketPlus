"""Genera los fondos y botones de la tienda (QSMarketPlus) para Nexo.

Uso:  python generar_menus.py <carpeta Nexo del servidor>
Escribe texturas en pack/assets/qsmarket/textures/{gui,icons},
glifos en glyphs/qsmarket/menus.yml e items en items/qsmarket/iconos.yml.

Coordenadas medidas en el cliente (iguales a los menus de QsProteccion):
marcos de slot del cofre en x=7+18c, y=17+18r; inventario del jugador en
y=30+18*filas y barra rapida 58 px mas abajo. El texto "Inventario" del
juego cae en x=8, y=20+18*filas: por eso esa zona es clara (pergamino).
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

OUT = sys.argv[1] if len(sys.argv) > 1 else 'out'
NS = 'qsmarket'
random.seed(7)

# ------------------------------------------------------------------ paleta
WOOD_D = (52, 32, 22)
WOOD = (96, 60, 36)
WOOD_L = (132, 86, 52)
WOOD_H = (160, 110, 68)
GOLD_D = (140, 92, 30)
GOLD = (226, 172, 64)
GOLD_L = (255, 226, 140)
FELT_D = (14, 44, 42)
FELT = (26, 70, 64)
FELT_L = (44, 98, 88)
PARCH = (226, 204, 160)
PARCH_D = (190, 160, 112)
INK = (70, 46, 30)
EMERALD = (60, 200, 110)
RUBY = (220, 64, 64)
VELVET_D = (22, 18, 48)
VELVET = (40, 30, 84)
VELVET_L = (70, 56, 130)
CREAM = (250, 238, 210)
BURG_D = (44, 10, 18)
BURG = (92, 22, 36)
BURG_L = (138, 42, 58)
AWNING_R = (180, 46, 46)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def jitter(c, amount=6):
    d = random.randint(-amount, amount)
    return tuple(max(0, min(255, v + d)) for v in c)


class Canvas:
    def __init__(self, w, h):
        self.im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        self.px = self.im.load()
        self.w, self.h = w, h

    def put(self, x, y, c, a=255):
        if 0 <= x < self.w and 0 <= y < self.h:
            if a >= 255:
                self.px[x, y] = (*c[:3], 255)
            else:
                o = self.px[x, y]
                t = a / 255
                self.px[x, y] = (*mix(o[:3], c, t), max(o[3], a))

    def rect(self, x, y, w, h, c, a=255):
        for j in range(y, y + h):
            for i in range(x, x + w):
                self.put(i, j, c, a)

    def hline(self, x, y, w, c, a=255):
        self.rect(x, y, w, 1, c, a)

    def vline(self, x, y, h, c, a=255):
        self.rect(x, y, 1, h, c, a)

    # ------------------------------------------------------------- texturas
    def planks(self, x, y, w, h, base, plank_h=6, vertical=False):
        """Tablas de madera con veta y juntas."""
        for j in range(h):
            for i in range(w):
                along = j if not vertical else i
                across = i if not vertical else j
                band = along // plank_h
                c = mix(base, WOOD_D, 0.10 * (band % 2))
                grain = math.sin((across + band * 13) * 0.35 + math.sin(across * 0.07 + band) * 2.2)
                c = mix(c, WOOD_D if grain > 0.75 else c, 0.25)
                if along % plank_h == 0:
                    c = mix(c, WOOD_D, 0.55)
                elif along % plank_h == 1:
                    c = mix(c, WOOD_H, 0.18)
                self.put(x + i, y + j, jitter(c, 4))

    def felt(self, x, y, w, h):
        for j in range(h):
            for i in range(w):
                t = ((i - w / 2) ** 2 / (w * w) + (j - h / 2) ** 2 / (h * h)) * 2.2
                c = mix(FELT_L, FELT_D, min(1, t))
                if (i + j) % 2 == 0:
                    c = mix(c, FELT_D, 0.08)
                self.put(x + i, y + j, jitter(c, 3))

    def parchment(self, x, y, w, h):
        for j in range(h):
            for i in range(w):
                edge = min(i, j, w - 1 - i, h - 1 - j)
                c = mix(PARCH_D, PARCH, min(1, edge / 7))
                n = math.sin(i * 0.9 + j * 0.3) * math.sin(j * 0.7 - i * 0.2)
                c = mix(c, PARCH_D, 0.12 if n > 0.6 else 0)
                self.put(x + i, y + j, jitter(c, 4))

    def velvet(self, x, y, w, h):
        for j in range(h):
            for i in range(w):
                fold = (math.sin(i * 0.22 + j * 0.05) + 1) / 2
                c = mix(VELVET_D, VELVET_L, fold * 0.55)
                self.put(x + i, y + j, jitter(c, 3))

    def cloth(self, x, y, w, h, dark, light):
        """Tela con pliegues verticales (cortinas, terciopelo)."""
        for j in range(h):
            for i in range(w):
                fold = (math.sin(i * 0.5) + 1) / 2
                c = mix(dark, light, fold * 0.6)
                self.put(x + i, y + j, jitter(c, 3))

    def fringe(self, x, y, w):
        """Fleco dorado bajo una tela."""
        self.hline(x, y, w, GOLD_D)
        for i in range(x, x + w, 3):
            self.vline(i, y + 1, 2, GOLD)
            self.put(i, y + 3, GOLD_L)

    def awning(self, x, y, w, h):
        """Toldo a rayas con borde ondulado."""
        for j in range(h):
            for i in range(w):
                stripe = ((i // 11) % 2 == 0)
                c = AWNING_R if stripe else CREAM
                c = mix(c, (0, 0, 0), 0.25 * (j / h))
                self.put(x + i, y + j, jitter(c, 3))
        for i in range(w):
            k = (i % 11)
            depth = int(3 - abs(5 - k) * 0.6)
            stripe = ((i // 11) % 2 == 0)
            for d in range(max(0, depth)):
                self.put(x + i, y + h + d, mix(AWNING_R if stripe else CREAM, (0, 0, 0), 0.3))
            self.put(x + i, y + h + max(0, depth), (0, 0, 0), 70)

    # --------------------------------------------------------------- marcos
    def frame(self, x, y, w, h):
        """Marco de madera oscura con filo dorado y remaches en las esquinas."""
        for k, c in enumerate([WOOD_D, WOOD, WOOD_L, WOOD]):
            self.rect(x + k, y + k, w - 2 * k, 1, c)
            self.rect(x + k, y + h - 1 - k, w - 2 * k, 1, c)
            self.rect(x + k, y + k, 1, h - 2 * k, c)
            self.rect(x + w - 1 - k, y + k, 1, h - 2 * k, c)
        self.rect(x + 4, y + 4, w - 8, 1, GOLD_D)
        self.rect(x + 4, y + h - 5, w - 8, 1, GOLD_D)
        self.rect(x + 4, y + 4, 1, h - 8, GOLD_D)
        self.rect(x + w - 5, y + 4, 1, h - 8, GOLD_D)
        for cx, cy in ((x + 1, y + 1), (x + w - 5, y + 1), (x + 1, y + h - 5), (x + w - 5, y + h - 5)):
            self.corner(cx, cy)

    def corner(self, x, y):
        shape = [".##.", "#OO#", "#OD#", ".##."]
        for j, row in enumerate(shape):
            for i, ch in enumerate(row):
                if ch == '#':
                    self.put(x + i, y + j, GOLD_D)
                elif ch == 'O':
                    self.put(x + i, y + j, GOLD_L)
                elif ch == 'D':
                    self.put(x + i, y + j, GOLD)

    def gold_line(self, x, y, w):
        self.hline(x, y, w, GOLD_D)
        self.hline(x, y + 1, w, GOLD)
        for i in range(x, x + w, 9):
            self.put(i, y + 1, GOLD_L)

    def slot(self, x, y, style='felt'):
        """Marco de slot 18x18 hundido; el interior es donde va el objeto."""
        palette = {
            'felt': (FELT_D, mix(FELT_L, CREAM, 0.15), mix(FELT_D, FELT, 0.4)),
            'wood': (WOOD_D, WOOD_H, mix(WOOD_D, WOOD, 0.45)),
            'parch': (mix(INK, PARCH_D, 0.3), mix(PARCH, CREAM, 0.6), mix(PARCH_D, INK, 0.25)),
            'hotbar': (FELT_D, GOLD, mix(FELT_D, FELT, 0.4)),
            'red': ((70, 20, 20), (230, 120, 110), (96, 34, 32)),
            'green': ((18, 60, 30), (130, 220, 150), (30, 84, 46)),
            'velvet': (VELVET_D, VELVET_L, mix(VELVET_D, VELVET, 0.5)),
            'glass': ((24, 34, 44), (170, 220, 240), (38, 54, 66)),
            'burg': (BURG_D, BURG_L, mix(BURG_D, BURG, 0.5)),
        }[style]
        dark, light, inner = palette
        self.rect(x, y, 18, 1, dark)
        self.rect(x, y, 1, 18, dark)
        self.rect(x + 1, y + 17, 17, 1, light)
        self.rect(x + 17, y + 1, 1, 17, light)
        for j in range(16):
            for i in range(16):
                self.put(x + 1 + i, y + 1 + j, mix(inner, dark, 0.25 if (i < 2 or j < 2) else 0))

    def ornate(self, x, y, color, glow=True):
        """Slot destacado: marco dorado doble y halo del color dado."""
        if glow:
            self.halo(x + 9, y + 9, 14, color, 110)
        self.rect(x - 1, y - 1, 20, 20, GOLD_D)
        self.rect(x, y, 18, 18, GOLD)
        self.rect(x + 1, y + 1, 16, 16, mix(color, (0, 0, 0), 0.65))
        for i in range(16):
            for j in range(16):
                t = ((i - 7.5) ** 2 + (j - 7.5) ** 2) / 113
                self.put(x + 1 + i, y + 1 + j, mix(mix(color, (0, 0, 0), 0.35), mix(color, (0, 0, 0), 0.75), min(1, t)))
        for cx, cy in ((x - 1, y - 1), (x + 18, y - 1), (x - 1, y + 18), (x + 18, y + 18)):
            self.put(cx, cy, GOLD_L)

    def halo(self, cx, cy, r, color, alpha):
        for j in range(cy - r, cy + r + 1):
            for i in range(cx - r, cx + r + 1):
                d = math.hypot(i - cx, j - cy) / r
                if d < 1:
                    self.put(i, j, color, int(alpha * (1 - d) ** 2))

    def sparkle(self, x, y, c=GOLD_L, size=2):
        self.put(x, y, (255, 255, 255))
        for d in range(1, size + 1):
            a = int(230 / d)
            for dx, dy in ((d, 0), (-d, 0), (0, d), (0, -d)):
                self.put(x + dx, y + dy, c, a)

    def coins(self, x, y):
        """Pila de monedas para las esquinas libres (16x16)."""
        for k in range(4):
            cy = y + 12 - k * 3
            for i in range(10):
                for j in range(4):
                    if (i in (0, 9)) and j in (0, 3):
                        continue
                    c = GOLD if j == 0 else (GOLD_D if j == 3 else mix(GOLD, GOLD_D, 0.4))
                    self.put(x + 3 + i, cy + j, c)
            self.hline(x + 4, cy, 8, GOLD_L)
        self.sparkle(x + 12, y + 2)

    def gem(self, x, y, color):
        shape = ["..#..", ".#O#.", "#OOO#", ".#D#.", "..#.."]
        for j, row in enumerate(shape):
            for i, ch in enumerate(row):
                if ch == '#':
                    self.put(x + i, y + j, mix(color, (0, 0, 0), 0.5))
                elif ch == 'O':
                    self.put(x + i, y + j, mix(color, (255, 255, 255), 0.35))
                elif ch == 'D':
                    self.put(x + i, y + j, color)

    def vignette(self, strength=70):
        for j in range(self.h):
            for i in range(self.w):
                d = math.hypot((i - self.w / 2) / (self.w / 2), (j - self.h / 2) / (self.h / 2))
                if d > 0.7:
                    self.put(i, j, (0, 0, 0), int(strength * (d - 0.7) / 0.7))


def slot_xy(index):
    return 7 + 18 * (index % 9), 17 + 18 * (index // 9)


def player_inventory(cv, rows):
    """Zona del inventario del jugador en pergamino, con la barra rapida separada."""
    top = 17 + 18 * rows
    cv.parchment(5, top, 166, cv.h - top - 5)
    cv.gold_line(5, top, 166)
    py = 30 + 18 * rows
    for r in range(3):
        for c in range(9):
            cv.slot(7 + 18 * c, py + 18 * r, 'parch')
    hb = py + 58
    cv.hline(10, hb - 3, 156, PARCH_D)
    cv.hline(10, hb - 2, 156, mix(PARCH, CREAM, 0.5))
    for c in range(9):
        cv.slot(7 + 18 * c, hb, 'parch')


# ================================================================ fondos
def menu_cantidad():
    cv = Canvas(176, 222)
    cv.planks(0, 0, 176, 222, WOOD)
    cv.frame(0, 0, 176, 222)

    # cabecera
    cv.planks(5, 5, 166, 11, WOOD_L, plank_h=11)
    cv.gold_line(5, 15, 166)

    # vista del inventario: tapete verde; la ultima fila es la barra rapida
    cv.felt(5, 17, 166, 72)
    for r in range(4):
        for c in range(9):
            cv.slot(7 + 18 * c, 17 + 18 * r, 'hotbar' if r == 3 else 'felt')

    # mostrador de madera con los controles
    cv.planks(5, 89, 166, 36, WOOD_L)
    cv.gold_line(5, 88, 166)
    for s in (37, 38, 39):
        cv.slot(*slot_xy(s), 'red')
    for s in (41, 42, 43):
        cv.slot(*slot_xy(s), 'green')
    for s in (46, 47, 51, 52):
        cv.slot(*slot_xy(s), 'wood')
    cv.ornate(*slot_xy(40), GOLD)
    cv.ornate(*slot_xy(49), EMERALD)

    # esquinas libres y huecos con adorno
    for s in (36, 44, 45, 53):
        cv.coins(*slot_xy(s))
    x, y = slot_xy(48)
    cv.gem(x + 6, y + 6, EMERALD)
    cv.slot(*slot_xy(50), 'wood')

    cv.sparkle(slot_xy(40)[0] + 19, slot_xy(40)[1] - 1)
    cv.sparkle(slot_xy(49)[0] - 2, slot_xy(49)[1] + 17, (180, 255, 200))

    player_inventory(cv, 6)
    cv.vignette(50)
    return cv.im


def menu_accion():
    cv = Canvas(176, 168)
    cv.planks(0, 0, 176, 168, WOOD)
    cv.frame(0, 0, 176, 168)
    cv.awning(5, 5, 166, 11)

    # puesto: tablas verticales
    cv.planks(5, 19, 166, 52, WOOD_L, plank_h=8, vertical=True)

    # pedestal del objeto (13), comprar (11), vender (15), volver (22)
    x, y = slot_xy(13)
    cv.halo(x + 9, y + 9, 16, GOLD_L, 120)
    cv.ornate(x, y, GOLD, glow=False)
    cv.ornate(*slot_xy(11), EMERALD)
    cv.ornate(*slot_xy(15), RUBY)
    cv.slot(*slot_xy(22), 'wood')
    cv.sparkle(x + 20, y - 1)
    cv.sparkle(x - 3, y + 16, CREAM, 1)
    for s in (9, 17):
        sx, sy = slot_xy(s)
        cv.coins(sx, sy)

    player_inventory(cv, 3)
    cv.vignette(45)
    return cv.im


def menu_confirmar():
    cv = Canvas(176, 168)
    cv.planks(0, 0, 176, 168, WOOD_D)
    cv.frame(0, 0, 176, 168)
    cv.velvet(5, 5, 166, 66)
    cv.gold_line(5, 15, 166)

    # estrellas sueltas en el terciopelo
    for _ in range(22):
        sx, sy = random.randint(8, 166), random.randint(18, 68)
        cv.put(sx, sy, CREAM, random.randint(90, 200))

    # contrato: rayos dorados detras del objeto
    x, y = slot_xy(13)
    for k in range(12):
        ang = k * math.pi / 6
        for d in range(11, 26):
            px = int(x + 9 + math.cos(ang) * d)
            py = int(y + 9 + math.sin(ang) * d * 0.7)
            cv.put(px, py, GOLD, int(150 * (1 - (d - 11) / 15)))
    cv.ornate(x, y, GOLD, glow=False)

    cv.ornate(*slot_xy(11), EMERALD)
    cv.ornate(*slot_xy(15), RUBY)
    cv.slot(*slot_xy(18), 'velvet')
    cv.sparkle(x + 19, y - 2)

    player_inventory(cv, 3)
    cv.vignette(55)
    return cv.im


def menu_categorias():
    """Plaza del mercado: puestos en rombo 4-3-4 sobre empedrado."""
    cv = Canvas(176, 222)
    cv.planks(0, 0, 176, 222, WOOD)
    cv.frame(0, 0, 176, 222)
    cv.awning(5, 5, 166, 11)

    for j in range(19, 106):
        for i in range(5, 171):
            row = (j - 19) // 6
            off = 5 if row % 2 else 0
            edge = (j - 19) % 6 == 0 or (i + off) % 10 == 0
            c = (92, 88, 82) if not edge else (58, 54, 50)
            if (i * 7 + j * 3) % 5 == 0:
                c = mix(c, (120, 112, 100), 0.25)
            cv.put(i, j, jitter(c, 5))

    stalls = [10, 12, 14, 16, 20, 22, 24, 28, 30, 32, 34]
    colors = [EMERALD, GOLD, (90, 160, 240), RUBY, (190, 120, 230), GOLD, (240, 150, 70),
              EMERALD, (90, 160, 240), RUBY, (190, 120, 230)]
    for s, col in zip(stalls, colors):
        x, y = slot_xy(s)
        cv.halo(x + 9, y + 9, 13, col, 70)
        cv.ornate(x, y, col, glow=False)

    for s in (0, 8):
        x, y = slot_xy(s)
        cv.halo(x + 9, y + 12, 10, GOLD_L, 90)
        cv.rect(x + 7, y + 6, 4, 9, GOLD_D)
        cv.rect(x + 8, y + 7, 2, 7, GOLD_L)

    cv.planks(5, 107, 166, 18, WOOD_L, plank_h=18)
    cv.gold_line(5, 106, 166)
    cv.ornate(*slot_xy(49), GOLD)
    cv.slot(*slot_xy(53), 'wood')
    cv.slot(*slot_xy(47), 'wood')
    for s in (45, 51):
        x, y = slot_xy(s)
        cv.gem(x + 6, y + 6, (90, 160, 240))
    cv.sparkle(slot_xy(49)[0] + 18, slot_xy(49)[1])

    player_inventory(cv, 6)
    cv.vignette(50)
    return cv.im


def menu_items():
    """Estanteria de 4 baldas con 7 huecos y mostrador de botones."""
    cv = Canvas(176, 222)
    cv.planks(0, 0, 176, 222, WOOD_D)
    cv.frame(0, 0, 176, 222)
    cv.planks(5, 5, 166, 11, WOOD_L, plank_h=11)
    cv.gold_line(5, 15, 166)

    cv.planks(5, 17, 166, 89, mix(WOOD_D, WOOD, 0.5), plank_h=10, vertical=True)

    # cuerda con banderines en la fila de arriba
    flags = [AWNING_R, GOLD, EMERALD]
    for i in range(8, 168):
        y = 21 + int(3 * math.sin((i - 8) / 160 * math.pi))
        cv.put(i, y, (200, 180, 140))
        k = (i - 8) % 12
        if 0 < k < 6:
            col = flags[((i - 8) // 12) % 3]
            for d in range(1, 7 - abs(3 - k)):
                cv.put(i, y + d, col)

    for r in range(1, 5):
        y = 17 + 18 * r
        for c in range(1, 8):
            cv.slot(7 + 18 * c, y, 'wood')
        cv.rect(25, y + 17, 126, 1, WOOD_H)
        for c in (0, 8):
            x = 7 + 18 * c
            cv.rect(x + 3, y, 12, 18, mix(WOOD_D, WOOD, 0.3))
            cv.vline(x + 3, y, 18, WOOD_H)

    cv.planks(5, 107, 166, 18, WOOD_L, plank_h=18)
    cv.gold_line(5, 106, 166)
    for s in (45, 53, 48):
        cv.slot(*slot_xy(s), 'wood')
    for s in (46, 47):
        cv.slot(*slot_xy(s), 'green')
    cv.ornate(*slot_xy(49), GOLD, glow=False)
    cv.coins(*slot_xy(51))
    for s in (50, 52):
        x, y = slot_xy(s)
        cv.gem(x + 6, y + 6, (90, 160, 240))

    player_inventory(cv, 6)
    cv.vignette(50)
    return cv.im


def vitrina(cv, x, y):
    """Slot de cristal con brillo diagonal, como una vitrina de joyeria."""
    cv.slot(x, y, 'glass')
    for k in range(4):
        cv.put(x + 3 + k, y + 2 + k, (210, 240, 255), 90)
        cv.put(x + 8 + k, y + 2 + k, (210, 240, 255), 50)


def ah_base(cv, rows):
    """Marco comun de la casa de subastas: madera oscura y cabecera de terciopelo."""
    cv.planks(0, 0, 176, cv.h, WOOD_D)
    cv.frame(0, 0, 176, cv.h)
    cv.cloth(5, 5, 166, 10, BURG_D, BURG_L)
    cv.fringe(5, 15, 166)


def ah_principal():
    cv = Canvas(176, 222)
    ah_base(cv, 6)
    cv.planks(5, 19, 166, 87, mix(WOOD_D, WOOD, 0.35), plank_h=9)

    # pestanas de filtros arriba (2..6) y adornos en los extremos
    for s in range(2, 7):
        x, y = slot_xy(s)
        cv.slot(x, y, 'burg')
        cv.hline(x + 2, y + 17, 14, GOLD)
    for s in (0, 8):
        x, y = slot_xy(s)
        cv.gem(x + 6, y + 6, RUBY)

    # 4 filas de vitrinas entre columnas talladas
    for r in range(1, 5):
        for c in range(1, 8):
            vitrina(cv, 7 + 18 * c, 17 + 18 * r)
        for c in (0, 8):
            x, y = 7 + 18 * c, 17 + 18 * r
            cv.rect(x + 4, y, 10, 18, mix(WOOD, WOOD_L, 0.4))
            cv.vline(x + 4, y, 18, WOOD_H)
            cv.vline(x + 13, y, 18, WOOD_D)
            if r in (1, 4):
                cv.rect(x + 3, y + (0 if r == 1 else 15), 12, 3, GOLD_D)

    # barra inferior
    cv.cloth(5, 107, 166, 18, BURG_D, BURG)
    cv.gold_line(5, 106, 166)
    for s in (45, 53, 47, 51):
        cv.slot(*slot_xy(s), 'burg')
    cv.ornate(*slot_xy(49), GOLD, glow=False)
    for s in (46, 48, 50, 52):
        x, y = slot_xy(s)
        cv.gem(x + 6, y + 6, GOLD)
    cv.sparkle(slot_xy(49)[0] + 18, slot_xy(49)[1])

    player_inventory(cv, 6)
    cv.vignette(55)
    return cv.im


def ah_lista():
    """Libro de registros: pagina de pergamino con renglones."""
    cv = Canvas(176, 222)
    ah_base(cv, 6)
    cv.cloth(5, 19, 166, 87, BURG_D, BURG)
    cv.parchment(22, 33, 132, 74)
    for j in range(36, 105, 6):
        cv.hline(26, j, 124, PARCH_D, 120)
    cv.vline(88, 33, 74, mix(PARCH_D, INK, 0.3))
    for r in range(1, 5):
        for c in range(1, 8):
            cv.slot(7 + 18 * c, 17 + 18 * r, 'parch')
    for s in (0, 8):
        x, y = slot_xy(s)
        cv.coins(x, y)

    cv.cloth(5, 107, 166, 18, BURG_D, BURG)
    cv.gold_line(5, 106, 166)
    for s in (45, 47, 53):
        cv.slot(*slot_xy(s), 'burg')
    cv.ornate(*slot_xy(49), GOLD, glow=False)
    x, y = slot_xy(51)
    cv.gem(x + 6, y + 6, RUBY)

    player_inventory(cv, 6)
    cv.vignette(55)
    return cv.im


def ah_pack():
    """Cofre abierto forrado de terciopelo: 5 filas completas para el contenido."""
    cv = Canvas(176, 222)
    ah_base(cv, 6)
    cv.cloth(5, 17, 166, 90, BURG_D, BURG_L)
    for r in range(5):
        for c in range(9):
            cv.slot(7 + 18 * c, 17 + 18 * r, 'burg')

    cv.planks(5, 107, 166, 18, WOOD_L, plank_h=18)
    cv.gold_line(5, 106, 166)
    cv.ornate(*slot_xy(48), RUBY, glow=False)
    cv.ornate(*slot_xy(49), EMERALD)
    cv.ornate(*slot_xy(50), GOLD, glow=False)
    for s in (45, 53):
        cv.coins(*slot_xy(s))

    player_inventory(cv, 6)
    cv.vignette(55)
    return cv.im


def ah_opciones():
    """Atril con cortinas: el lote arriba y las acciones debajo."""
    cv = Canvas(176, 168)
    ah_base(cv, 3)
    cv.planks(5, 19, 166, 52, mix(WOOD_D, WOOD, 0.4), plank_h=8, vertical=True)
    cv.cloth(5, 19, 22, 52, BURG_D, BURG_L)
    cv.cloth(149, 19, 22, 52, BURG_D, BURG_L)
    cv.vline(27, 19, 52, GOLD_D)
    cv.vline(148, 19, 52, GOLD_D)

    x, y = slot_xy(4)
    cv.halo(x + 9, y + 9, 15, GOLD_L, 100)
    cv.ornate(x, y, GOLD, glow=False)
    cv.sparkle(x + 19, y)
    for s in (11, 12, 15):
        cv.slot(*slot_xy(s), 'wood')
    cv.ornate(*slot_xy(13), EMERALD)

    player_inventory(cv, 3)
    cv.vignette(50)
    return cv.im


def ah_confirmar():
    """Mesa de firma: lote, etiqueta de precio, aceptar y cancelar."""
    cv = Canvas(176, 168)
    ah_base(cv, 3)
    cv.cloth(5, 19, 166, 52, BURG_D, BURG)
    cv.parchment(30, 22, 116, 46)
    cv.ornate(*slot_xy(11), GOLD, glow=False)
    x, y = slot_xy(13)
    cv.slot(x, y, 'parch')
    cv.ornate(*slot_xy(15), EMERALD)
    cv.ornate(*slot_xy(22), RUBY, glow=False)
    # sello de cera junto al precio
    cv.halo(x + 9, y + 26, 5, RUBY, 160)

    player_inventory(cv, 3)
    cv.vignette(50)
    return cv.im


def tiendas_jugadores():
    """Calle de noche con puestos: cada fila de tiendas bajo su toldo."""
    cv = Canvas(176, 222)
    cv.planks(0, 0, 176, 222, WOOD_D)
    cv.frame(0, 0, 176, 222)

    # cielo nocturno con estrellas en la cabecera y la primera fila
    for j in range(5, 35):
        for i in range(5, 171):
            cv.put(i, j, mix((18, 22, 52), (40, 34, 80), (j - 5) / 30))
    for _ in range(30):
        cv.put(random.randint(8, 168), random.randint(6, 32), CREAM, random.randint(80, 220))
    cv.gold_line(5, 15, 166)

    # farolas y cartel central (slot 4)
    for s in (0, 8):
        x, y = slot_xy(s)
        cv.halo(x + 9, y + 8, 11, GOLD_L, 110)
        cv.rect(x + 8, y + 10, 2, 8, (60, 60, 70))
        cv.rect(x + 6, y + 4, 6, 6, GOLD_D)
        cv.rect(x + 7, y + 5, 4, 4, (255, 220, 140))
    cv.ornate(*slot_xy(4), GOLD, glow=False)

    # calle empedrada
    for j in range(35, 106):
        for i in range(5, 171):
            row = (j - 35) // 5
            edge = (j - 35) % 5 == 0 or (i + (4 if row % 2 else 0)) % 9 == 0
            c = (70, 66, 64) if not edge else (44, 40, 40)
            cv.put(i, j, jitter(c, 5))

    # una fila de puestos por fila de tiendas, cada una con su toldo
    awnings = [AWNING_R, (60, 120, 200), EMERALD, (220, 150, 50)]
    for r, col in zip(range(1, 5), awnings):
        y = 17 + 18 * r
        for i in range(25, 151):
            stripe = ((i - 25) // 6) % 2 == 0
            cv.put(i, y - 1, col if stripe else CREAM)
        for c in range(1, 8):
            cv.slot(7 + 18 * c, y, 'wood')

    cv.planks(5, 107, 166, 18, WOOD_L, plank_h=18)
    cv.gold_line(5, 106, 166)
    for s in (45, 53):
        cv.slot(*slot_xy(s), 'wood')
    cv.ornate(*slot_xy(49), GOLD, glow=False)
    for s in (46, 47, 48, 50, 51, 52):
        x, y = slot_xy(s)
        if s in (47, 51):
            cv.coins(x, y)
        else:
            cv.gem(x + 6, y + 6, (90, 160, 240))

    player_inventory(cv, 6)
    cv.vignette(55)
    return cv.im


def cupones():
    """Talonario de cupones sobre tapete verde."""
    cv = Canvas(176, 222)
    cv.planks(0, 0, 176, 222, WOOD)
    cv.frame(0, 0, 176, 222)
    cv.planks(5, 5, 166, 11, WOOD_L, plank_h=11)
    cv.gold_line(5, 15, 166)
    cv.felt(5, 17, 166, 89)

    # cupones de papel con borde troquelado detras de cada fila
    for r in range(1, 5):
        y = 17 + 18 * r
        cv.parchment(23, y - 1, 130, 20)
        for i in range(24, 152, 3):
            cv.put(i, y - 1, FELT_D)
            cv.put(i, y + 18, FELT_D)
        for c in range(1, 8):
            cv.slot(7 + 18 * c, y, 'parch')
    for s in (0, 8):
        x, y = slot_xy(s)
        cv.coins(x, y)
    x, y = slot_xy(4)
    cv.gem(x + 6, y + 6, GOLD)

    cv.planks(5, 107, 166, 18, WOOD_L, plank_h=18)
    cv.gold_line(5, 106, 166)
    for s in (45, 47, 51, 53):
        cv.slot(*slot_xy(s), 'wood')
    cv.ornate(*slot_xy(49), GOLD, glow=False)
    for s in (46, 48, 50, 52):
        x, y = slot_xy(s)
        cv.gem(x + 6, y + 6, EMERALD)

    player_inventory(cv, 6)
    cv.vignette(50)
    return cv.im


# ================================================================ botones 32x32
DIGITS = {
    '0': ["###", "#.#", "#.#", "#.#", "###"], '1': [".#.", "##.", ".#.", ".#.", "###"],
    '6': ["###", "#..", "###", "#.#", "###"], '4': ["#.#", "#.#", "###", "..#", "..#"],
    '+': ["...", ".#.", "###", ".#.", "..."], '-': ["...", "...", "###", "...", "..."],
    'A': [".#.", "#.#", "###", "#.#", "#.#"], 'Z': ["###", "..#", ".#.", "#..", "###"],
}


def outline(im, color=(30, 20, 14, 255)):
    """Contorno de 1 px alrededor de lo dibujado, como los iconos ya existentes."""
    src = im.load()
    out = im.copy()
    o = out.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            if src[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 0:
                        o[x, y] = color
                        break
    return out


def text(cv, s, x, y, c, scale=2, shadow=True):
    cx = x
    for ch in s:
        g = DIGITS[ch]
        for j, row in enumerate(g):
            for i, v in enumerate(row):
                if v == '#':
                    if shadow:
                        cv.rect(cx + i * scale + 1, y + j * scale + 1, scale, scale, (0, 0, 0), 120)
                    cv.rect(cx + i * scale, y + j * scale, scale, scale, c)
        cx += 3 * scale + 1


def text_width(s, scale=2):
    return len(s) * (3 * scale + 1) - 1


def pill(cv, color):
    """Ficha redondeada con relieve."""
    for j in range(8, 24):
        for i in range(3, 29):
            r = 4
            dx = max(3 + r - i, 0, i - (28 - r))
            dy = max(8 + r - j, 0, j - (23 - r))
            if dx * dx + dy * dy > r * r:
                continue
            t = (j - 8) / 16
            c = mix(mix(color, (255, 255, 255), 0.35), mix(color, (0, 0, 0), 0.35), t)
            cv.put(i, j, c)
    cv.hline(6, 9, 20, mix(color, (255, 255, 255), 0.6))


def icon_amount(sign, number):
    cv = Canvas(32, 32)
    color = (205, 64, 58) if sign == '-' else (70, 180, 90)
    pill(cv, color)
    label = sign + number
    text(cv, label, 16 - text_width(label) // 2, 11, CREAM)
    return outline(cv.im)


def icon_coin(cv, x, y, r=6):
    for j in range(-r, r + 1):
        for i in range(-r, r + 1):
            d = math.hypot(i, j)
            if d <= r:
                c = GOLD_L if d < r - 2 and (i + j) < -1 else (GOLD if d < r - 1 else GOLD_D)
                cv.put(x + i, y + j, c)
    cv.vline(x, y - 2, 5, GOLD_D)


def icon_minimo():
    cv = Canvas(32, 32)
    icon_coin(cv, 16, 22, 7)
    # flecha hacia abajo, hacia una sola moneda
    cv.rect(14, 1, 5, 6, (120, 170, 230))
    for k in range(6):
        cv.rect(11 + k, 7 + k, 11 - 2 * k, 1, (120, 170, 230))
    return outline(cv.im)


def icon_maximo(frame=0, frames=1):
    cv = Canvas(32, 32)
    for k in range(4):
        icon_coin(cv, 10 + k * 2, 24 - k * 4, 6)
    icon_coin(cv, 23, 22, 6)
    for k in range(5):
        cv.rect(24 - k, 3 + k, 2 * k + 1, 1, EMERALD)
    cv.rect(22, 8, 5, 4, EMERALD)
    im = outline(cv.im)
    if frames > 1:
        sx = [(6, 6), (26, 14), (14, 3), (4, 18)][frame % 4]
        c2 = Canvas(32, 32)
        c2.im = im
        c2.px = im.load()
        c2.sparkle(*sx, size=2)
    return im


def icon_confirmar(frame=0, frames=1):
    cv = Canvas(32, 32)
    for j in range(32):
        for i in range(32):
            d = math.hypot(i - 15.5, j - 15.5)
            if d <= 13:
                t = d / 13
                cv.put(i, j, mix((90, 230, 140), (20, 110, 60), t))
    check = [(8, 16), (13, 21), (24, 10)]
    for (x1, y1), (x2, y2) in zip(check, check[1:]):
        steps = max(abs(x2 - x1), abs(y2 - y1))
        for s in range(steps + 1):
            px = round(x1 + (x2 - x1) * s / steps)
            py = round(y1 + (y2 - y1) * s / steps)
            cv.rect(px - 1, py - 1, 3, 3, CREAM)
    im = outline(cv.im)
    if frames > 1:
        # brillo que cruza en diagonal
        p = im.load()
        pos = -10 + frame * (52 / (frames - 1))
        for y in range(32):
            for x in range(32):
                if p[x, y][3] and abs((x + y) / 2 - pos) < 2:
                    p[x, y] = (*mix(p[x, y][:3], (255, 255, 255), 0.55), 255)
    return im


def icon_libre():
    cv = Canvas(32, 32)
    for i in range(4, 28):
        for j in (4, 27):
            if i % 4 < 2:
                cv.put(i, j, (110, 230, 140), 200)
                cv.put(j, i, (110, 230, 140), 200)
    cv.rect(14, 9, 4, 14, (110, 230, 140))
    cv.rect(9, 14, 14, 4, (110, 230, 140))
    return outline(cv.im, (20, 70, 36, 255))


def icon_sin_dinero():
    cv = Canvas(32, 32)
    # bolsa vacia
    for j in range(10, 28):
        for i in range(6, 26):
            if math.hypot((i - 16) / 10, (j - 20) / 8) <= 1:
                cv.put(i, j, mix((176, 132, 84), (110, 76, 44), (j - 10) / 18))
    cv.rect(12, 6, 8, 5, (150, 108, 64))
    cv.hline(11, 10, 10, (90, 60, 34))
    # cruz roja
    for k in range(-7, 8):
        cv.rect(16 + k - 1, 18 + k - 1, 3, 3, RUBY)
        cv.rect(16 + k - 1, 18 - k - 1, 3, 3, RUBY)
    return outline(cv.im)


def icon_shape(draw):
    cv = Canvas(32, 32)
    draw(cv)
    return outline(cv.im)


def _palette(cv):
    for j in range(32):
        for i in range(32):
            if math.hypot((i - 16) / 13, (j - 17) / 10) <= 1 and math.hypot(i - 22, j - 22) > 4:
                cv.put(i, j, mix((214, 170, 110), (150, 104, 60), (j - 7) / 20))
    for (x, y), col in zip(((10, 14), (16, 11), (22, 14), (11, 21)), (RUBY, GOLD, (90, 160, 240), EMERALD)):
        cv.rect(x - 2, y - 2, 5, 4, col)
        cv.put(x - 1, y - 2, mix(col, (255, 255, 255), 0.5))


def _apple(cv):
    for j in range(32):
        for i in range(32):
            if math.hypot((i - 16) / 11, (j - 19) / 10) <= 1:
                cv.put(i, j, mix((240, 80, 70), (150, 30, 30), max(0, (i - 8) / 18)))
    cv.rect(10, 13, 3, 4, (255, 170, 160))
    cv.rect(15, 4, 2, 6, (100, 66, 40))
    cv.rect(17, 5, 6, 3, EMERALD)


def _bone(cv):
    for k in range(6, 26):
        cv.rect(k - 1, k - 1, 4, 4, (232, 226, 206))
    for cx, cy in ((6, 9), (9, 6), (26, 23), (23, 26)):
        cv.rect(cx - 3, cy - 3, 6, 6, (232, 226, 206))
    for k in range(7, 25):
        cv.put(k + 2, k, (180, 170, 150))


def _redstone(cv):
    cv.rect(14, 12, 4, 16, (120, 80, 48))
    cv.vline(14, 12, 16, (150, 104, 64))
    cv.halo(16, 9, 8, (255, 60, 40), 160)
    cv.rect(13, 5, 6, 7, (210, 30, 30))
    cv.rect(14, 6, 2, 2, (255, 160, 140))


def _lantern(cv):
    cv.halo(16, 18, 12, GOLD_L, 120)
    cv.rect(10, 9, 12, 18, GOLD_D)
    cv.rect(12, 11, 8, 14, (255, 200, 90))
    cv.rect(14, 13, 3, 6, (255, 250, 210))
    cv.rect(12, 5, 8, 4, (70, 70, 80))
    cv.rect(15, 2, 2, 3, (70, 70, 80))


def _sort(arrow_color, up):
    def draw(cv):
        icon_coin(cv, 11, 20, 7)
        if up:
            for k in range(6):
                cv.rect(24 - k, 3 + k, 2 * k + 1, 1, arrow_color)
            cv.rect(22, 9, 5, 7, arrow_color)
        else:
            cv.rect(22, 3, 5, 7, arrow_color)
            for k in range(6):
                cv.rect(19 + k, 10 + k, 11 - 2 * k, 1, arrow_color)
    return draw


def _sort_name(cv):
    cv.rect(4, 7, 24, 18, PARCH)
    cv.rect(4, 7, 24, 1, PARCH_D)
    text(cv, 'AZ', 9, 11, INK, scale=2, shadow=False)


def _hourglass(cv):
    cv.rect(8, 3, 16, 3, WOOD_L)
    cv.rect(8, 26, 16, 3, WOOD_L)
    for k in range(10):
        w = 14 - k * 1.2
        cv.rect(int(16 - w / 2), 6 + k, int(w), 1, (200, 230, 245))
        cv.rect(int(16 - w / 2), 25 - k, int(w), 1, (200, 230, 245))
    for k in range(4):
        cv.rect(14 - k, 7 + k, 4 + 2 * k, 1, GOLD)
    for k in range(5):
        cv.rect(12 - k, 20 + k, 8 + 2 * k, 1, GOLD)
    cv.vline(15, 11, 9, GOLD_L)


def _ledger(cv):
    cv.rect(6, 5, 20, 23, BURG)
    cv.rect(8, 6, 17, 21, PARCH)
    for j in range(9, 25, 3):
        cv.hline(10, j, 13, PARCH_D)
    cv.vline(7, 5, 23, GOLD_D)
    icon_coin(cv, 22, 23, 5)


def _gavel(cv):
    for k in range(14):
        cv.rect(8 + k, 20 - k, 3, 3, (130, 84, 50))
    for j in range(8):
        for i in range(12):
            cv.put(13 + i - j // 2, 4 + j + i // 3, (170, 112, 66) if i % 11 else GOLD)
    cv.rect(4, 25, 14, 3, WOOD_D)
    cv.hline(4, 25, 14, WOOD_L)


def _no_filter(cv):
    for k in range(10):
        cv.rect(5 + k, 5 + k, 22 - 2 * k, 1, (120, 170, 230))
    cv.rect(14, 15, 4, 10, (120, 170, 230))
    for k in range(-5, 6):
        cv.rect(22 + k - 1, 22 + k - 1, 3, 3, RUBY)
        cv.rect(22 + k - 1, 22 - k - 1, 3, 3, RUBY)


def _price_tag(cv):
    for j in range(8, 24):
        for i in range(6, 28):
            if i - 6 > abs(j - 16) - 2 or i > 12:
                cv.put(i, j, mix(PARCH, PARCH_D, (j - 8) / 16))
    cv.halo(10, 16, 2, WOOD_D, 255)
    text(cv, '+', 15, 11, GOLD_D, scale=2, shadow=False)


def _gem(cv):
    shape = ["....####....", "..##OOOO##..", ".#OOOOOOOO#.", "#DDDDDDDDDD#",
             ".#DDDDDDDD#.", "..#DDDDDD#..", "...#DDDD#...", "....#DD#....", ".....##....."]
    for j, row in enumerate(shape):
        for i, ch in enumerate(row):
            px, py = 10 + i, 9 + j * 2
            col = {'#': (40, 90, 140), 'O': (170, 230, 255), 'D': (80, 160, 230)}.get(ch)
            if col:
                cv.rect(px, py, 1, 2, col)
    cv.sparkle(24, 8)


def _stall(cv):
    for i in range(3, 29):
        stripe = ((i - 3) // 4) % 2 == 0
        for j in range(6, 12):
            cv.put(i, j, AWNING_R if stripe else CREAM)
        if (i - 3) % 4 == 1:
            cv.put(i, 12, AWNING_R if stripe else CREAM)
    cv.rect(5, 12, 2, 14, WOOD_D)
    cv.rect(25, 12, 2, 14, WOOD_D)
    cv.rect(3, 20, 26, 7, WOOD_L)
    cv.hline(3, 20, 26, WOOD_H)
    icon_coin(cv, 11, 17, 3)
    cv.rect(17, 15, 6, 5, EMERALD)


def _ticket(color):
    def draw(cv):
        for j in range(9, 24):
            for i in range(3, 29):
                notch = (i in (3, 4) or i in (27, 28)) and 14 <= j <= 18
                if not notch:
                    cv.put(i, j, mix(color, (255, 255, 255), 0.15) if j < 12 else color)
        for j in range(10, 23, 2):
            cv.put(21, j, mix(color, (0, 0, 0), 0.45))
        text(cv, '-', 7, 13, CREAM, scale=2, shadow=True)
        cv.rect(14, 12, 2, 2, CREAM)
        cv.rect(17, 18, 2, 2, CREAM)
        for k in range(6):
            cv.put(18 - k, 13 + k, CREAM)
    return draw


def animated(fn, frames):
    strip = Image.new('RGBA', (32, 32 * frames))
    for f in range(frames):
        strip.alpha_composite(fn(f, frames), (0, 32 * f))
    return strip


# ================================================================ salida
def save(img, rel):
    path = os.path.join(OUT, 'pack', 'assets', NS, 'textures', rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    return path


def main():
    backgrounds = {
        'cupones': (cupones(), '\\uE60B', 222),
        'tiendas_jugadores': (tiendas_jugadores(), '\\uE60A', 222),
        'ah_principal': (ah_principal(), '\\uE605', 222),
        'ah_lista': (ah_lista(), '\\uE606', 222),
        'ah_pack': (ah_pack(), '\\uE607', 222),
        'ah_opciones': (ah_opciones(), '\\uE608', 168),
        'ah_confirmar': (ah_confirmar(), '\\uE609', 168),
        'menu_categorias': (menu_categorias(), '\\uE603', 222),
        'menu_items': (menu_items(), '\\uE604', 222),
        'menu_cantidad': (menu_cantidad(), '\\uE600', 222),
        'menu_accion': (menu_accion(), '\\uE601', 168),
        'menu_confirmar': (menu_confirmar(), '\\uE602', 168),
    }

    glyphs = ['# Fondos de la tienda (QSMarketPlus). Generados por generar_menus.py.',
              '# Fuente minecraft:default para usarlos en titulos de texto normal.',
              '# Titulo: "<white>%shift:-8%<char>%shift:-169%<color>Texto"', '']
    for name, (img, char, height) in backgrounds.items():
        save(img, f'gui/{name}.png')
        glyphs += [f'qsm_{name}:', f'  texture: {NS}:gui/{name}', '  font: minecraft:default',
                   f'  height: {height}', '  ascent: 13', f'  char: "{char}"',
                   '  tabcomplete: false', '  is_emoji: false', '']

    icons = {
        'menos_1': icon_amount('-', '1'), 'menos_16': icon_amount('-', '16'), 'menos_64': icon_amount('-', '64'),
        'mas_1': icon_amount('+', '1'), 'mas_16': icon_amount('+', '16'), 'mas_64': icon_amount('+', '64'),
        'minimo': icon_minimo(), 'libre': icon_libre(), 'sin_dinero': icon_sin_dinero(),
        'cat_colores': icon_shape(_palette), 'cat_comida': icon_shape(_apple),
        'cat_mob_drops': icon_shape(_bone), 'cat_redstone': icon_shape(_redstone),
        'cat_decoracion': icon_shape(_lantern),
        'orden_sube': icon_shape(_sort(EMERALD, True)), 'orden_baja': icon_shape(_sort(RUBY, False)),
        'orden_nombre': icon_shape(_sort_name),
        'ah_reloj': icon_shape(_hourglass), 'ah_historial': icon_shape(_ledger),
        'ah_mazo': icon_shape(_gavel), 'ah_quitar_filtro': icon_shape(_no_filter),
        'ah_etiqueta': icon_shape(_price_tag), 'ah_solo_items': icon_shape(_gem),
        'tiendas': icon_shape(_stall),
        'cupon': icon_shape(_ticket((200, 70, 60))),
        'cupon_activo': icon_shape(_ticket((60, 170, 90))),
    }
    animations = {'maximo': (icon_maximo, 4, 4), 'confirmar': (icon_confirmar, 8, 2)}

    items = ['# Botones de la tienda (QSMarketPlus). Generados por generar_menus.py.',
             '# En los menus de QSMarketPlus:  nexo: "qsm_<nombre>"  (material queda de respaldo)', '']

    def item(name):
        return [f'qsm_{name}:', f'  itemname: "{name}"', '  material: PAPER', '  excludeFromInventory: true',
                '  excludeFromCommands: true', '  Components:', f'    item_model: nexo:qsm_{name}', '  Pack:',
                '    parent_model: minecraft:item/generated', f'    texture: {NS}:icons/{name}', '']

    for name, img in icons.items():
        save(img, f'icons/{name}.png')
        items += item(name)

    for name, (fn, frames, ticks) in animations.items():
        path = save(animated(fn, frames), f'icons/{name}.png')
        with open(path + '.mcmeta', 'w') as f:
            f.write('{"animation": {"frametime": %d, "interpolate": false}}' % ticks)
        items += item(name)

    for rel, lines in (('glyphs/qsmarket/menus.yml', glyphs), ('items/qsmarket/iconos.yml', items)):
        path = os.path.join(OUT, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write('\n'.join(lines))

    print('listo en', OUT)


if __name__ == '__main__':
    main()
