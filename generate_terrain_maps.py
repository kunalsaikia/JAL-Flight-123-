import math
import os
import subprocess

WIDTH = 1024
HEIGHT = 1024

LON_MIN = 138.35
LON_MAX = 139.85
LAT_MIN = 34.65
LAT_MAX = 36.15

# Coherent pseudo-random noise for mountain ridges
def hash2(x, y):
    n = math.sin(x * 127.1 + y * 311.7) * 43758.5453123
    return n - math.floor(n)

def smooth_noise(x, y):
    ix = math.floor(x)
    iy = math.floor(y)
    fx = x - ix
    fy = y - iy
    # Smoothstep interpolation
    ux = fx * fx * (3.0 - 2.0 * fx)
    uy = fy * fy * (3.0 - 2.0 * fy)
    
    a = hash2(ix, iy)
    b = hash2(ix + 1, iy)
    c = hash2(ix, iy + 1)
    d = hash2(ix + 1, iy + 1)
    
    return a + (b - a) * ux + (c - a) * uy + (a - b - c + d) * ux * uy

def fbm(x, y, octaves=5):
    val = 0.0
    amp = 0.5
    freq = 1.0
    for _ in range(octaves):
        val += amp * smooth_noise(x * freq, y * freq)
        freq *= 2.02
        amp *= 0.48
    return val

print("Computing elevation grid...")
elevations = [[0.0 for _ in range(WIDTH)] for _ in range(HEIGHT)]

for py in range(HEIGHT):
    lat = LAT_MAX - (py / (HEIGHT - 1)) * (LAT_MAX - LAT_MIN)
    for px in range(WIDTH):
        lon = LON_MIN + (px / (WIDTH - 1)) * (LON_MAX - LON_MIN)
        
        # 1. Base land / water separation
        # Suruga Bay: lon 138.45 to 138.80, lat 34.65 to 35.15
        in_suruga = False
        if lat < 35.15 and 138.45 <= lon <= 138.85:
            # parabolic bay shape
            bay_lat_limit = 35.15 - ((lon - 138.65) / 0.22)**2 * 0.45
            if lat < bay_lat_limit:
                in_suruga = True
                
        # Sagami Bay: lon 139.12 to 139.60, lat 34.65 to 35.32
        in_sagami = False
        if lat < 35.32 and 139.12 <= lon <= 139.60:
            bay_lat_limit = 35.32 - ((lon - 139.36) / 0.26)**2 * 0.55
            if lat < bay_lat_limit:
                in_sagami = True
                
        # Tokyo Bay: lon 139.68 to 140.0, lat 35.25 to 35.70
        in_tokyo_bay = False
        if 35.25 <= lat <= 35.68 and 139.70 <= lon:
            # narrow uraga channel at south
            if lat < 35.35:
                if 139.70 <= lon <= 139.80:
                    in_tokyo_bay = True
            else:
                if 139.72 <= lon <= 140.0:
                    in_tokyo_bay = True

        # Open Pacific Ocean south of Izu
        in_ocean = (lat < 34.68 and (lon > 138.9 or lon < 138.8)) or (lat < 34.90 and 139.3 <= lon <= 139.6)

        is_water = in_suruga or in_sagami or in_tokyo_bay or in_ocean

        # Island: Izu Oshima (volcanic island in Sagami Bay at 139.38, 34.73)
        dist_oshima = math.hypot((lon - 139.40) * 1.2, lat - 34.73)
        if dist_oshima < 0.055:
            is_water = False

        if is_water:
            # Water depth
            elevations[py][px] = 0.0
            continue

        # 2. Base Landforms
        elev = 40.0 # base coastal elevation in meters

        # Kanto Plain (East & Northeast): low flat plains
        if lon > 139.28 and lat > 35.38:
            # Gentle plain with minor hills
            elev = 25.0 + fbm(px * 0.015, py * 0.015, octaves=3) * 60.0
            elevations[py][px] = elev
            continue

        # Mount Fuji (138.727, 35.360) - 3,776m
        d_fuji = math.hypot(lon - 138.727, (lat - 35.360) * 1.1)
        if d_fuji < 0.22:
            fuji_h = 3776.0 * math.exp(-(d_fuji / 0.075)**1.6)
            elev = max(elev, fuji_h)

        # Mount Takamagahara / Osutaka Ridge (138.693, 35.998) - 1,979m
        d_takamaga = math.hypot(lon - 138.693, lat - 35.998)
        if d_takamaga < 0.20:
            taka_h = 1979.0 * math.exp(-(d_takamaga / 0.08)**1.4)
            elev = max(elev, taka_h)

        # Regional Mountain Provinces:
        # Gunma / Chichibu / Okutama mountains (North & Northwest)
        if lat > 35.55 and lon < 139.25:
            dist_north = (lat - 35.55) / 0.6
            mountain_macro = fbm(px * 0.012, py * 0.012, octaves=5)
            ridge_noise = abs(fbm(px * 0.025, py * 0.025, octaves=4) - 0.5) * 2.0
            elev = max(elev, 400.0 + dist_north * 1200.0 * mountain_macro + 500.0 * (1.0 - ridge_noise))

        # Southern Alps / Akaishi (West, lon < 138.5)
        if lon < 138.55:
            dist_west = (138.55 - lon) / 0.20
            alps_macro = fbm(px * 0.015, py * 0.015, octaves=5)
            elev = max(elev, 500.0 + dist_west * 2200.0 * alps_macro)

        # Tanzawa Mountains (between Sagami Bay & Mt Fuji, 139.1, 35.45)
        d_tanzawa = math.hypot(lon - 139.15, lat - 35.46)
        if d_tanzawa < 0.16:
            tanzawa_h = 1673.0 * math.exp(-(d_tanzawa / 0.07)**1.5)
            elev = max(elev, tanzawa_h)

        # Izu Peninsula spine (138.95, 34.90) - Amagi mountains ~1,400m
        if 34.70 <= lat <= 35.05 and 138.82 <= lon <= 139.08:
            d_amagi = math.hypot(lon - 138.98, lat - 34.88)
            elev = max(elev, 1406.0 * math.exp(-(d_amagi / 0.09)**1.5))

        # Add fine-grained crags and valleys
        micro_detail = (fbm(px * 0.04, py * 0.04, octaves=4) - 0.5) * 200.0
        elev = max(10.0, elev + micro_detail)

        elevations[py][px] = elev

print("Rendering shaded relief and satellite coloring...")
# Prepare byte arrays for PPM (satellite) and PGM (heightmap)
ppm_bytes = bytearray(WIDTH * HEIGHT * 3)
pgm_bytes = bytearray(WIDTH * HEIGHT)

# Sun vector for hillshading (From NW: azimuth 315 deg, altitude 45 deg)
sun_az = math.radians(315.0)
sun_alt = math.radians(45.0)
sun_x = math.cos(sun_alt) * math.sin(sun_az)
sun_y = -math.cos(sun_alt) * math.cos(sun_az)
sun_z = math.sin(sun_alt)

idx_ppm = 0
idx_pgm = 0

for py in range(HEIGHT):
    for px in range(WIDTH):
        e = elevations[py][px]
        
        # Grayscale heightmap: 0m to 3800m -> 0 to 255
        h_val = int(min(255, max(0, (e / 3800.0) * 255.0)))
        pgm_bytes[idx_pgm] = h_val
        idx_pgm += 1

        if e <= 0.0:
            # Water (Sagami Bay, Suruga Bay, Tokyo Bay)
            # Subtle coastal bathymetry gradient
            lat = LAT_MAX - (py / (HEIGHT - 1)) * (LAT_MAX - LAT_MIN)
            lon = LON_MIN + (px / (WIDTH - 1)) * (LON_MAX - LON_MIN)
            
            # Deep navy to azure coastal water
            r = int(10 + math.sin(px * 0.02) * 4)
            g = int(32 + math.cos(py * 0.02) * 6)
            b = int(72 + (py / HEIGHT) * 25)
            
            # Shallow bay tint
            if lat > 35.2: # inner Tokyo Bay or Sagami shore
                g += 15
                b += 10
            
            ppm_bytes[idx_ppm] = min(255, r)
            ppm_bytes[idx_ppm + 1] = min(255, g)
            ppm_bytes[idx_ppm + 2] = min(255, b)
            idx_ppm += 3
            continue

        # Calculate slope / hillshade normal from neighbors
        e_left = elevations[py][max(0, px - 1)]
        e_right = elevations[py][min(WIDTH - 1, px + 1)]
        e_up = elevations[max(0, py - 1)][px]
        e_down = elevations[min(HEIGHT - 1, py + 1)][px]

        # Horizontal distance per pixel is ~160 meters
        dx = (e_right - e_left) / 320.0
        dy = (e_down - e_up) / 320.0

        # Normal vector (nx, ny, nz)
        nx = -dx * 1.8 # vertical exaggeration for shaded relief
        ny = -dy * 1.8
        nz = 1.0
        len_n = math.hypot(nx, ny, nz)
        nx /= len_n
        ny /= len_n
        nz /= len_n

        # Lambertian hillshade
        shade = max(0.15, nx * sun_x + ny * sun_y + nz * sun_z)
        shade = shade ** 0.85 # enhance contrast

        # Hypsometric satellite color palette based on elevation
        if e > 2700.0:
            # Snow cap (Mount Fuji summit)
            base_r, base_g, base_b = 240, 248, 255
        elif e > 2000.0:
            # Alpine rocky crags & tundra
            base_r, base_g, base_b = 160, 168, 175
        elif e > 1200.0:
            # High mountain evergreen forest (Mt Takamagahara ridge)
            base_r, base_g, base_b = 45, 78, 52
        elif e > 500.0:
            # Dense mixed deciduous mountain canopy
            base_r, base_g, base_b = 58, 98, 62
        elif e > 120.0:
            # Foothills & valleys
            base_r, base_g, base_b = 85, 125, 72
        else:
            # Lowland coastal plains (Kanto, Sagami coastal)
            # Mixed green fields & subtle urban gray
            is_urban = (LON_MIN + (px / WIDTH) * (LON_MAX - LON_MIN) > 139.5) and (LAT_MAX - (py / HEIGHT) * (LAT_MAX - LAT_MIN) > 35.45)
            if is_urban:
                base_r, base_g, base_b = 115, 125, 110 # Tokyo metropolitan region
            else:
                base_r, base_g, base_b = 75, 115, 65 # coastal agriculture / greenery

        # Apply hillshade lighting
        final_r = int(min(255, base_r * shade * 1.15))
        final_g = int(min(255, base_g * shade * 1.15))
        final_b = int(min(255, base_b * shade * 1.15))

        ppm_bytes[idx_ppm] = final_r
        ppm_bytes[idx_ppm + 1] = final_g
        ppm_bytes[idx_ppm + 2] = final_b
        idx_ppm += 3

print("Writing PPM and PGM files...")
with open("/tmp/terrain_satellite.ppm", "wb") as f:
    f.write(f"P6\n{WIDTH} {HEIGHT}\n255\n".encode("ascii"))
    f.write(ppm_bytes)

with open("/tmp/terrain_heightmap.pgm", "wb") as f:
    f.write(f"P5\n{WIDTH} {HEIGHT}\n255\n".encode("ascii"))
    f.write(pgm_bytes)

print("Converting to PNG using ImageMagick...")
subprocess.run(["convert", "/tmp/terrain_satellite.ppm", "PNG32:app/src/main/res/drawable/terrain_satellite_map.png"], check=True)
subprocess.run(["convert", "/tmp/terrain_heightmap.pgm", "PNG32:app/src/main/res/drawable/terrain_heightmap.png"], check=True)

print("Generated terrain maps successfully!")
