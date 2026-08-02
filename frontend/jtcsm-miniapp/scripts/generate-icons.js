const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const SIZE = 81;
const ICONS_DIR = path.join(__dirname, '..', 'src', 'static');

const INACTIVE = '#999999';
const ACTIVE = '#e74c3c';

function hexToRgb(hex) {
    return [
        parseInt(hex.slice(1, 3), 16),
        parseInt(hex.slice(3, 5), 16),
        parseInt(hex.slice(5, 7), 16),
    ];
}

function createCanvas(w, h) {
    const canvas = [];
    for (let y = 0; y < h; y++) {
        canvas.push(new Array(w).fill(null));
    }
    return canvas;
}

function setPixel(canvas, x, y, w, h, color) {
    const ix = x | 0;
    const iy = y | 0;
    if (ix >= 0 && ix < w && iy >= 0 && iy < h) {
        canvas[iy][ix] = color;
    }
}

function drawCircle(canvas, cx, cy, radius, w, h, color) {
    for (let y = Math.floor(cy - radius); y <= Math.ceil(cy + radius); y++) {
        for (let x = Math.floor(cx - radius); x <= Math.ceil(cx + radius); x++) {
            const dx = x - cx;
            const dy = y - cy;
            if (dx * dx + dy * dy <= radius * radius) {
                setPixel(canvas, x, y, w, h, color);
            }
        }
    }
}

function drawLine(canvas, x1, y1, x2, y2, w, h, color) {
    const dx = Math.abs(x2 - x1);
    const dy = Math.abs(y2 - y1);
    const sx = x1 < x2 ? 1 : -1;
    const sy = y1 < y2 ? 1 : -1;
    let err = dx - dy;
    let x = x1, y = y1;
    while (true) {
        setPixel(canvas, x, y, w, h, color);
        if (x === x2 && y === y2) break;
        const e2 = 2 * err;
        if (e2 > -dy) { err -= dy; x += sx; }
        if (e2 < dx) { err += dx; y += sy; }
    }
}

function drawRect(canvas, x, y, rw, rh, w, h, color) {
    const sx = x | 0;
    const sy = y | 0;
    const ex = (x + rw) | 0;
    const ey = (y + rh) | 0;
    for (let py = sy; py < ey; py++) {
        for (let px = sx; px < ex; px++) {
            setPixel(canvas, px, py, w, h, color);
        }
    }
}

function drawTriangle(canvas, points, w, h, color) {
    const minY = Math.floor(Math.min(...points.map(p => p[1])));
    const maxY = Math.ceil(Math.max(...points.map(p => p[1])));
    for (let y = minY; y <= maxY; y++) {
        const xs = [];
        for (let i = 0; i < points.length; i++) {
            const p1 = points[i];
            const p2 = points[(i + 1) % points.length];
            if ((p1[1] <= y && p2[1] > y) || (p2[1] <= y && p1[1] > y)) {
                const dy = p2[1] - p1[1];
                if (dy !== 0) {
                    const x = p1[0] + (y - p1[1]) / dy * (p2[0] - p1[0]);
                    xs.push(x);
                }
            }
        }
        xs.sort((a, b) => a - b);
        for (let i = 0; i < xs.length; i += 2) {
            for (let x = Math.floor(xs[i]); x <= Math.ceil(xs[i + 1]); x++) {
                setPixel(canvas, x, y, w, h, color);
            }
        }
    }
}

function drawDiamond(canvas, cx, cy, r, w, h, color) {
    const points = [
        [cx, cy - r],
        [cx + r, cy],
        [cx, cy + r],
        [cx - r, cy],
    ];
    const minY = cy - r;
    const maxY = cy + r;
    for (let y = minY; y <= maxY; y++) {
        const xs = [];
        for (let i = 0; i < points.length; i++) {
            const p1 = points[i];
            const p2 = points[(i + 1) % points.length];
            if ((p1[1] <= y && p2[1] > y) || (p2[1] <= y && p1[1] > y)) {
                const x = p1[0] + (y - p1[1]) / (p2[1] - p1[1]) * (p2[0] - p1[0]);
                xs.push(x);
            }
        }
        xs.sort((a, b) => a - b);
        for (let i = 0; i < xs.length; i += 2) {
            for (let x = Math.floor(xs[i]); x <= Math.ceil(xs[i + 1]); x++) {
                setPixel(canvas, x, y, w, h, color);
            }
        }
    }
}

function drawHeart(canvas, cx, cy, size, w, h, color) {
    for (let y = cy - size; y <= cy + size; y++) {
        for (let x = cx - size; x <= cx + size; x++) {
            const nx = (x - cx) / size;
            const ny = -(y - cy) / size;
            const val = Math.pow(nx * nx + ny * ny - 1, 3) - nx * nx * ny * ny * ny;
            if (val <= 0) {
                setPixel(canvas, x, y, w, h, color);
            }
        }
    }
}

function drawStar(canvas, cx, cy, outerR, innerR, points, w, h, color) {
    const step = Math.PI / points;
    const coords = [];
    let angle = -Math.PI / 2;
    for (let i = 0; i < points * 2; i++) {
        const r = i % 2 === 0 ? outerR : innerR;
        coords.push([cx + r * Math.cos(angle), cy + r * Math.sin(angle)]);
        angle += step;
    }
    const minY = Math.floor(cy - outerR);
    const maxY = Math.ceil(cy + outerR);
    for (let y = minY; y <= maxY; y++) {
        const xs = [];
        for (let i = 0; i < coords.length; i++) {
            const p1 = coords[i];
            const p2 = coords[(i + 1) % coords.length];
            if ((p1[1] <= y && p2[1] > y) || (p2[1] <= y && p1[1] > y)) {
                const x = p1[0] + (y - p1[1]) / (p2[1] - p1[1]) * (p2[0] - p1[0]);
                xs.push(x);
            }
        }
        xs.sort((a, b) => a - b);
        for (let i = 0; i < xs.length; i += 2) {
            for (let x = Math.floor(xs[i]); x <= Math.ceil(xs[i + 1]); x++) {
                setPixel(canvas, x, y, w, h, color);
            }
        }
    }
}

function drawHomeIcon(canvas, color) {
    const [r, g, b] = hexToRgb(color);
    const cx = SIZE / 2;
    const cy = SIZE / 2;
    const s = 16;

    const roofPoints = [
        [cx, cy - s - 4],
        [cx + s + 2, cy - 2],
        [cx - s - 2, cy - 2],
    ];
    drawTriangle(canvas, roofPoints, SIZE, SIZE, [r, g, b]);

    drawRect(canvas, cx - s + 2, cy - 2, 2 * s - 4, s + 8, SIZE, SIZE, [r, g, b]);

    drawRect(canvas, cx - 4, cy + 6, 8, 12, SIZE, SIZE, [255, 255, 255]);
}

function drawAIIcon(canvas, color) {
    const [r, g, b] = hexToRgb(color);
    const cx = SIZE / 2;
    const cy = SIZE / 2;

    drawCircle(canvas, cx, cy - 4, 14, SIZE, SIZE, [r, g, b]);

    drawCircle(canvas, cx - 5, cy - 6, 3, SIZE, SIZE, [255, 255, 255]);
    drawCircle(canvas, cx + 5, cy - 6, 3, SIZE, SIZE, [255, 255, 255]);

    drawCircle(canvas, cx - 5, cy - 6, 1.5, SIZE, SIZE, [r, g, b]);
    drawCircle(canvas, cx + 5, cy - 6, 1.5, SIZE, SIZE, [r, g, b]);

    drawRect(canvas, cx - 4, cy + 1, 8, 2, SIZE, SIZE, [255, 255, 255]);

    drawRect(canvas, cx - 10, cy + 10, 20, 8, SIZE, SIZE, [r, g, b]);

    drawCircle(canvas, cx - 8, cy - 16, 2, SIZE, SIZE, [r, g, b]);
    drawCircle(canvas, cx + 8, cy - 16, 2, SIZE, SIZE, [r, g, b]);
    drawLine(canvas, cx - 8, cy - 14, cx - 8, cy - 10, SIZE, SIZE, [r, g, b]);
    drawLine(canvas, cx + 8, cy - 14, cx + 8, cy - 10, SIZE, SIZE, [r, g, b]);
}

function drawFavIcon(canvas, color) {
    const [r, g, b] = hexToRgb(color);
    const cx = SIZE / 2;
    const cy = SIZE / 2;
    drawHeart(canvas, cx, cy + 2, 18, SIZE, SIZE, [r, g, b]);
}

function drawProfileIcon(canvas, color) {
    const [r, g, b] = hexToRgb(color);
    const cx = SIZE / 2;
    const cy = SIZE / 2;

    drawCircle(canvas, cx, cy - 8, 10, SIZE, SIZE, [r, g, b]);

    drawCircle(canvas, cx, cy + 16, 14, SIZE, SIZE, [r, g, b]);
}

function canvasToPNG(canvas) {
    const w = SIZE;
    const h = SIZE;
    const rawData = Buffer.alloc(h * (1 + w * 4));

    for (let y = 0; y < h; y++) {
        rawData[y * (1 + w * 4)] = 0;
        for (let x = 0; x < w; x++) {
            const offset = y * (1 + w * 4) + 1 + x * 4;
            const pixel = canvas[y][x];
            if (pixel) {
                rawData[offset] = pixel[0];
                rawData[offset + 1] = pixel[1];
                rawData[offset + 2] = pixel[2];
                rawData[offset + 3] = 255;
            } else {
                rawData[offset] = 0;
                rawData[offset + 1] = 0;
                rawData[offset + 2] = 0;
                rawData[offset + 3] = 0;
            }
        }
    }

    const compressed = zlib.deflateSync(rawData);

    function crc32(buf) {
        const table = [];
        for (let n = 0; n < 256; n++) {
            let c = n;
            for (let k = 0; k < 8; k++) {
                c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
            }
            table[n] = c;
        }
        let crc = 0xffffffff;
        for (let i = 0; i < buf.length; i++) {
            crc = table[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
        }
        return (crc ^ 0xffffffff) >>> 0;
    }

    function createChunk(type, data) {
        const typeBuffer = Buffer.from(type);
        const length = Buffer.alloc(4);
        length.writeUInt32BE(data.length);
        const crcData = Buffer.concat([typeBuffer, data]);
        const crc = Buffer.alloc(4);
        crc.writeUInt32BE(crc32(crcData));
        return Buffer.concat([length, typeBuffer, data, crc]);
    }

    const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(w, 0);
    ihdr.writeUInt32BE(h, 4);
    ihdr[8] = 8;
    ihdr[9] = 6;
    ihdr[10] = 0;
    ihdr[11] = 0;
    ihdr[12] = 0;

    const ihdrChunk = createChunk('IHDR', ihdr);
    const idatChunk = createChunk('IDAT', compressed);
    const iendChunk = createChunk('IEND', Buffer.alloc(0));

    return Buffer.concat([signature, ihdrChunk, idatChunk, iendChunk]);
}

const iconDefs = [
    { name: 'tab-home', color: INACTIVE, draw: drawHomeIcon },
    { name: 'tab-home-active', color: ACTIVE, draw: drawHomeIcon },
    { name: 'tab-ai', color: INACTIVE, draw: drawAIIcon },
    { name: 'tab-ai-active', color: ACTIVE, draw: drawAIIcon },
    { name: 'tab-fav', color: INACTIVE, draw: drawFavIcon },
    { name: 'tab-fav-active', color: ACTIVE, draw: drawFavIcon },
    { name: 'tab-profile', color: INACTIVE, draw: drawProfileIcon },
    { name: 'tab-profile-active', color: ACTIVE, draw: drawProfileIcon },
];

if (!fs.existsSync(ICONS_DIR)) {
    fs.mkdirSync(ICONS_DIR, { recursive: true });
}

iconDefs.forEach(({ name, color, draw }) => {
    const canvas = createCanvas(SIZE, SIZE);
    draw(canvas, color);
    const png = canvasToPNG(canvas);
    const filePath = path.join(ICONS_DIR, `${name}.png`);
    fs.writeFileSync(filePath, png);
    console.log(`Created: ${filePath} (${png.length} bytes)`);
});

console.log('All icons generated successfully!');
