#version 330

in vec3 FragPos;
in vec2 TexCoord;
in vec4 FragColor;
in vec3 ViewDir;

out vec4 fragColor;

uniform sampler2D Sampler0;
uniform float Mode;
uniform float time;
uniform vec3 CameraPosition;
uniform float SkyPitch;
uniform float SkyYaw;
uniform float TanHalfFov;
uniform float Aspect;
uniform float Intensity;
uniform vec4 FirstColor;
uniform vec4 SecondColor;
uniform vec4 PurpleColor;

const float TAU = 6.28318530718;

float clamp01(float x) { return clamp(x, 0.0, 1.0); }

vec3 getSkyDirection(vec3 viewDirection) {
    float pitchCos = cos(SkyPitch);
    float pitchSin = sin(SkyPitch);
    vec3 pitchedDirection = normalize(vec3(
            viewDirection.x,
            viewDirection.y * pitchCos + viewDirection.z * pitchSin,
            viewDirection.z * pitchCos - viewDirection.y * pitchSin
    ));
    float yawCos = cos(SkyYaw);
    float yawSin = sin(SkyYaw);
    return normalize(vec3(
            pitchedDirection.x * yawCos + pitchedDirection.z * yawSin,
            pitchedDirection.y,
            pitchedDirection.z * yawCos - pitchedDirection.x * yawSin
    ));
}

float fullSkyAmount(float skyHeight, float amount) {
    float lowerSky = 1.0 - smoothstep(-0.18, 0.18, skyHeight);
    float enabled = smoothstep(0.02, 0.18, amount);
    return clamp(mix(amount, max(amount, 0.96), lowerSky * enabled), 0.0, 1.0);
}

float hashFloat2(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float hashFloat1(float p) {
    return fract(sin(p * 91.3458) * 47453.5453);
}

float noiseValue(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
            mix(hashFloat2(i), hashFloat2(i + vec2(1.0, 0.0)), u.x),
            mix(hashFloat2(i + vec2(0.0, 1.0)), hashFloat2(i + vec2(1.0, 1.0)), u.x),
            u.y
    );
}

float fbmValue(vec2 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 5; i++) {
        value += noiseValue(p) * amplitude;
        p = mat2(1.62, 1.10, -1.10, 1.62) * p + vec2(18.7, 7.3);
        amplitude *= 0.52;
    }
    return value;
}

vec3 gradientSky(vec3 worldDirection) {
    float skyHeight = worldDirection.y * cos(SkyPitch) + worldDirection.z * sin(SkyPitch);
    float gradient = smoothstep(-0.22, 0.86, skyHeight);
    return mix(FirstColor.rgb, SecondColor.rgb, gradient);
}

vec3 aquaSky(vec3 worldDirection) {
    float skyHeight = abs(worldDirection.y);
    vec2 uv = worldDirection.xz / max(0.36 + skyHeight, 0.32);
    vec2 p = mod((uv * 1.35 + vec2(time * 0.018, -time * 0.012)) * TAU, TAU) - 250.0;
    vec2 i = p;
    float c = 1.0;
    float inten = 0.005;
    float localTime = time * 0.45 + 23.0;

    for (int n = 0; n < 5; n++) {
        float t = localTime * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
        c += 1.0 / length(vec2(p.x / (sin(i.x + t) / inten), p.y / (cos(i.y + t) / inten)));
    }

    c /= 5.0;
    c = 1.17 - pow(c, 1.4);
    vec3 color = vec3(pow(abs(c), 8.0));
    color = clamp(color + vec3(0.0, 0.35, 0.50), 0.0, 1.0);

    float skyTop = 1.0 - smoothstep(0.86, 1.0, skyHeight);
    vec3 base = mix(vec3(0.02, 0.06, 0.10), vec3(0.04, 0.18, 0.26), smoothstep(0.0, 0.72, skyHeight));
    return mix(base, color, skyTop);
}

vec3 purpleSky(vec3 worldDirection) {
    float skyHeight = abs(worldDirection.y);
    vec2 uv = worldDirection.xz / max(0.36 + skyHeight, 0.32);
    vec2 p = mod(uv * TAU, TAU) - 250.0;
    vec2 i = p;
    float c = 1.0;
    float inten = 0.005;
    float localTime = time * 1.5 + 23.0;

    for (int n = 0; n < 5; n++) {
        float t = localTime * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
        c += 1.0 / length(vec2(p.x / (sin(i.x + t) / inten), p.y / (cos(i.y + t) / inten)));
    }

    c /= 5.0;
    c = 1.17 - pow(c, 1.4);

    vec3 shaderColor = vec3(pow(abs(c), 8.0));
    shaderColor = clamp(shaderColor + PurpleColor.rgb, 0.0, 1.0);

    float skyTop = 1.0 - smoothstep(0.86, 1.0, skyHeight);
    vec3 baseColor = mix(PurpleColor.rgb * 0.10, PurpleColor.rgb * 0.28, smoothstep(0.0, 0.72, skyHeight));
    return mix(baseColor, shaderColor, skyTop);
}

float fieldValue(vec3 p) {
    float strength = 7.0 + 0.03 * log(1.0e-6 + fract(sin(time) * 4373.11));
    float accum = 0.0;
    float previous = 0.0;
    float totalWeight = 0.0;

    for (int i = 0; i < 6; i++) {
        float mag = dot(p, p);
        p = abs(p) / max(mag, 0.001) + vec3(-0.5, -0.8 + 0.1 * sin(time * 0.7 + 2.0), -1.1 + 0.3 * cos(time * 0.3));
        float weight = exp(-float(i) / 7.0);
        accum += weight * exp(-strength * pow(abs(mag - previous), 2.3));
        totalWeight += weight;
        previous = mag;
    }

    return max(0.0, 5.0 * accum / max(totalWeight, 0.001) - 0.7);
}

vec3 galaxySky(vec3 worldDirection) {
    float skyHeight = abs(worldDirection.y);
    vec2 uv = worldDirection.xz / max(0.42 + skyHeight, 0.34);
    uv *= 0.82;

    float time2 = time * 0.16;
    vec3 dir = normalize(vec3(uv * 0.9, 1.0));
    vec3 from = vec3(0.0);
    vec3 forward = vec3(0.0, 0.0, 1.0);

    from.x += 1.2 * cos(0.3 * time2) + 0.018 * time2;
    from.y += 1.2 * sin(0.2 * time2) + 0.018 * time2;
    from.z += 0.04 * time2;

    float angleXZ = 0.9;
    float angleYZ = -0.6;
    float angleXY = 0.9 + time2 * 0.22;
    mat2 rotXZ = mat2(cos(angleXZ), sin(angleXZ), -sin(angleXZ), cos(angleXZ));
    mat2 rotYZ = mat2(cos(angleYZ), sin(angleYZ), -sin(angleYZ), cos(angleYZ));
    mat2 rotXY = mat2(cos(angleXY), sin(angleXY), -sin(angleXY), cos(angleXY));

    dir.xy = rotXY * dir.xy;
    forward.xy = rotXY * forward.xy;
    dir.xz = rotXZ * dir.xz;
    forward.xz = rotXZ * forward.xz;
    dir.yz = rotYZ * dir.yz;
    forward.yz = rotYZ * forward.yz;
    from.xy = -(rotXY * from.xy);
    from.xz = rotXZ * from.xz;
    from.yz = rotYZ * from.yz;

    float zoom = time2 * -0.012;
    from += forward * zoom;
    float sampleShift = mod(zoom, 0.29);
    float zOffset = -sampleShift;
    sampleShift /= 0.29;

    float s = 0.24;
    float s3 = s + 0.145;
    vec3 volume = vec3(0.0);
    vec3 cloudColor = vec3(0.0);

    for (int r = 0; r < 7; r++) {
        vec3 p = from + (s + zOffset) * dir;
        vec3 pCloud = from + (s3 + zOffset) * dir;

        p = abs(vec3(0.95) - mod(p, vec3(1.9)));
        pCloud = abs(vec3(0.95) - mod(pCloud, vec3(1.9)));

        float cloud = fieldValue(pCloud);
        float previousAmount = 0.0;
        float amount = 0.0;

        for (int i = 0; i < 7; i++) {
            p = abs(p) / max(dot(p, p), 0.001) - 0.50;
            float diff = abs(length(p) - previousAmount);
            amount += i > 5 ? min(12.0, diff) : diff;
            previousAmount = length(p);
        }

        amount *= amount * amount;
        float depth = s + zOffset;
        float fade = pow(0.66, max(0.0, float(r) - sampleShift));

        if (r == 0) {
            fade *= 1.0 - sampleShift;
        }
        if (r == 6) {
            fade *= sampleShift;
        }

        volume += vec3(depth, depth * depth, depth * depth * depth * depth) * amount * 0.0045 * fade;
        cloudColor += vec3(1.8 * cloud * cloud * cloud, 1.4 * cloud * cloud, cloud) * fade * 0.31;

        s += 0.29;
        s3 += 0.29;
    }

    volume = mix(vec3(length(volume)), volume, 0.82);
    cloudColor.b *= 1.8;
    cloudColor.r *= 0.06;
    cloudColor.g *= 0.12;

    vec3 color = volume * 0.36 + cloudColor;
    float skyTopFade = 1.0 - smoothstep(0.98, 1.0, skyHeight);
    vec3 base = mix(vec3(0.015, 0.018, 0.052), vec3(0.035, 0.055, 0.13), smoothstep(0.0, 0.72, skyHeight));
    return base + color * skyTopFade;
}

vec3 overcastSky(vec3 worldDirection) {
    float skyHeight = clamp(worldDirection.y * 0.5 + 0.5, 0.0, 1.0);
    vec2 cameraDrift = CameraPosition.xz * 0.0009;
    vec2 cloudUv = worldDirection.xz / max(0.28 + abs(worldDirection.y), 0.24);
    cloudUv += cameraDrift + vec2(time * 0.012, time * -0.009);

    float broadClouds = fbmValue(cloudUv * 1.55);
    float tornClouds = fbmValue(cloudUv * 4.15 + broadClouds * 1.70);
    float stormVeins = fbmValue(cloudUv * vec2(9.0, 2.1) + vec2(time * -0.04, time * 0.01));
    float cloudMass = smoothstep(0.24, 0.88, broadClouds * 0.78 + tornClouds * 0.50);
    float underside = 1.0 - smoothstep(0.48, 0.96, skyHeight);
    float horizonMist = 1.0 - smoothstep(0.02, 0.52, skyHeight);

    vec3 horizon = vec3(0.28, 0.31, 0.33);
    vec3 skyTopColor = vec3(0.055, 0.065, 0.085);
    vec3 base = mix(horizon, skyTopColor, smoothstep(0.0, 1.0, skyHeight));
    vec3 cloudLight = vec3(0.35, 0.37, 0.39);
    vec3 cloudDark = vec3(0.035, 0.040, 0.055);
    vec3 clouds = mix(cloudLight, cloudDark, cloudMass * (0.60 + underside * 0.48));

    float streakShadow = fbmValue(cloudUv * vec2(0.65, 2.4) + vec2(0.0, time * 0.035));
    vec3 sky = mix(base, clouds, 0.58 + cloudMass * 0.34);
    sky *= 0.58 + streakShadow * 0.13 - stormVeins * underside * 0.13;
    sky = mix(sky, vec3(0.30, 0.33, 0.35), horizonMist * 0.24);

    return sky;
}

float rainLayer(vec2 uv, float columns, float speed, float slant, float width, float length) {
    vec2 rainUv = uv;
    rainUv.x += rainUv.y * slant;
    rainUv.x += sin(time * 0.95 + rainUv.y * 6.0) * 0.008;
    rainUv.y -= time * speed;

    vec2 grid = vec2(columns, columns * 0.33);
    vec2 cell = floor(rainUv * grid);
    vec2 local = fract(rainUv * grid);
    float random = hashFloat2(cell);
    float x = abs(local.x - random);
    float streak = 1.0 - smoothstep(width, width * 2.6, x);
    float body = smoothstep(0.0, length, local.y) * (1.0 - smoothstep(length, 1.0, local.y));
    float head = exp(-local.y * 9.0) * 0.55;
    float drop = streak * (body + head) * step(0.28, random);

    return drop;
}

float rainMistLayer(vec2 uv) {
    float fine = fbmValue(vec2(uv.x * 24.0 + time * 0.4, uv.y * 5.0 - time * 1.2));
    return smoothstep(0.48, 1.0, fine) * (1.0 - smoothstep(0.70, 1.05, uv.y));
}

float lightningPulseValue() {
    float stormTime = time * 3.20;
    float cell = floor(stormTime);
    float local = fract(stormTime);
    float strike = step(0.58, hashFloat1(cell));
    float firstFlash = exp(-local * 28.0);
    float secondFlash = exp(-abs(local - 0.16) * 36.0) * 0.55;
    float afterGlow = exp(-abs(local - 0.32) * 18.0) * 0.22;
    return strike * clamp01(firstFlash + secondFlash + afterGlow);
}

float lightningBoltValue(vec3 worldDirection, float pulse) {
    if (pulse <= 0.001 || worldDirection.y < 0.05) {
        return 0.0;
    }
    float stormCell = floor(time * 3.20);
    float anchor = mix(-0.62, 0.62, hashFloat1(stormCell + 3.1));
    vec2 uv = vec2(worldDirection.x - anchor, worldDirection.y - 0.18);
    uv.x += sin(uv.y * 28.0 + hashFloat1(stormCell + 9.2) * 6.2831) * 0.022;
    uv.x += sin(uv.y * 57.0 + time * 0.85) * 0.010;

    float mainBolt = (1.0 - smoothstep(0.006, 0.030, abs(uv.x))) * smoothstep(0.02, 0.22, uv.y)
            * (1.0 - smoothstep(0.76, 1.02, uv.y));
    float branchA = (1.0 - smoothstep(0.004, 0.022, abs(uv.x - uv.y * 0.34 + 0.13)))
            * smoothstep(0.20, 0.36, uv.y) * (1.0 - smoothstep(0.36, 0.56, uv.y));
    float branchB = (1.0 - smoothstep(0.004, 0.020, abs(uv.x + uv.y * 0.27 - 0.16)))
            * smoothstep(0.34, 0.48, uv.y) * (1.0 - smoothstep(0.48, 0.68, uv.y));

    return clamp01((mainBolt + branchA * 0.55 + branchB * 0.42) * pulse);
}

vec3 nebulaSky(vec3 worldDirection) {
    float skyHeight = abs(worldDirection.y);
    vec2 uv = worldDirection.xz / max(0.36 + skyHeight, 0.32);
    vec2 p = mod((uv * 1.5 + vec2(time * 0.02, time * 0.015)) * TAU, TAU) - 250.0;
    vec2 i = p; float c = 1.0; float inten = 0.005; float localTime = time * 0.6 + 10.0;
    for (int n = 0; n < 5; n++) {
        float t = localTime * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
        c += 1.0 / length(vec2(p.x / (sin(i.x + t) / inten), p.y / (cos(i.y + t) / inten)));
    }
    c /= 5.0; c = 1.17 - pow(c, 1.4);
    vec3 color = vec3(pow(abs(c), 6.0));
    vec3 nebCol = mix(vec3(0.9, 0.1, 0.6), vec3(0.1, 0.7, 0.9), sin(time * 0.3 + worldDirection.x) * 0.5 + 0.5);
    color = clamp(color + nebCol, 0.0, 1.0);
    float zenith = 1.0 - smoothstep(0.86, 1.0, skyHeight);
    vec3 base = mix(vec3(0.08, 0.02, 0.12), vec3(0.18, 0.04, 0.22), smoothstep(0.0, 0.72, skyHeight));
    return mix(base, color, zenith);
}

vec3 solarSky(vec3 worldDirection) {
    float skyHeight = abs(worldDirection.y);
    vec2 uv = worldDirection.xz / max(0.36 + skyHeight, 0.32);
    vec2 p = mod((uv * 1.2 - vec2(time * 0.025, time * 0.01)) * TAU, TAU) - 250.0;
    vec2 i = p; float c = 1.0; float inten = 0.005; float localTime = time * 0.5 + 45.0;
    for (int n = 0; n < 5; n++) {
        float t = localTime * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
        c += 1.0 / length(vec2(p.x / (sin(i.x + t) / inten), p.y / (cos(i.y + t) / inten)));
    }
    c /= 5.0; c = 1.17 - pow(c, 1.4);
    vec3 color = vec3(pow(abs(c), 7.0));
    color = clamp(color + vec3(0.95, 0.45, 0.08), 0.0, 1.0);
    float zenith = 1.0 - smoothstep(0.86, 1.0, skyHeight);
    vec3 base = mix(vec3(0.12, 0.04, 0.01), vec3(0.28, 0.12, 0.02), smoothstep(0.0, 0.72, skyHeight));
    return mix(base, color, zenith);
}

vec3 vortexSky(vec3 worldDirection) {
    float skyHeight = abs(worldDirection.y);
    vec2 uv = worldDirection.xz / max(0.36 + skyHeight, 0.32);
    float len = length(uv);
    float angle = atan(uv.y, uv.x) + len * 3.0 - time * 0.8;
    vec2 rotUv = vec2(cos(angle), sin(angle)) * len;
    float n = fbmValue(rotUv * 3.0 + vec2(time * 0.1));
    vec3 col1 = vec3(0.1, 0.4, 0.9);
    vec3 col2 = vec3(0.8, 0.1, 0.8);
    vec3 color = mix(col1, col2, n * 1.2);
    float spiral = sin(len * 12.0 - angle * 2.0 + time * 2.0) * 0.5 + 0.5;
    color += vec3(spiral * 0.3);
    float zenith = 1.0 - smoothstep(0.86, 1.0, skyHeight);
    vec3 base = vec3(0.02, 0.03, 0.08);
    return mix(base, color, zenith);
}

vec3 cyberSky(vec3 worldDirection) {
    float skyHeight = abs(worldDirection.y);
    vec2 uv = worldDirection.xz / max(0.36 + skyHeight, 0.32);
    vec2 p = mod((uv * 1.4 + vec2(time * 0.03, -time * 0.02)) * TAU, TAU) - 250.0;
    vec2 i = p; float c = 1.0; float inten = 0.005; float localTime = time * 0.75 + 80.0;
    for (int n = 0; n < 5; n++) {
        float t = localTime * (1.0 - (3.5 / float(n + 1)));
        i = p + vec2(cos(t - i.x) + sin(t + i.y), sin(t - i.y) + cos(t + i.x));
        c += 1.0 / length(vec2(p.x / (sin(i.x + t) / inten), p.y / (cos(i.y + t) / inten)));
    }
    c /= 5.0; c = 1.17 - pow(c, 1.4);
    vec3 color = vec3(pow(abs(c), 8.0));
    color = clamp(color + vec3(0.0, 0.85, 0.45), 0.0, 1.0);
    float zenith = 1.0 - smoothstep(0.86, 1.0, skyHeight);
    vec3 base = mix(vec3(0.0, 0.08, 0.04), vec3(0.01, 0.22, 0.12), smoothstep(0.0, 0.72, skyHeight));
    return mix(base, color, zenith);
}

void main() {
    vec4 texColor = texture(Sampler0, TexCoord);
    vec4 baseColor = texColor * FragColor;
    
    vec3 skyDirection = getSkyDirection(ViewDir);
    vec3 shaderColor;
    float alpha;
    
    if (Mode < 1.5) { // Gradient
        float skyHeight = skyDirection.y;
        float gradient = smoothstep(-0.22, 0.86, skyHeight);
        shaderColor = mix(FirstColor.rgb, SecondColor.rgb, gradient);
        float colorAlpha = clamp(max(FirstColor.a, SecondColor.a), 0.0, 1.0);
        float amount = clamp01(max(Intensity, 0.0) * colorAlpha);
        alpha = fullSkyAmount(skyHeight, amount);
    } else if (Mode < 2.5) { // Aqua
        shaderColor = aquaSky(skyDirection);
        alpha = fullSkyAmount(skyDirection.y, clamp01(max(Intensity, 0.0) * 0.84));
    } else if (Mode < 3.5) { // Purple
        shaderColor = purpleSky(skyDirection);
        alpha = fullSkyAmount(skyDirection.y, clamp01(max(Intensity, 0.0) * PurpleColor.a * 0.84));
    } else if (Mode < 4.5) { // Galaxy
        shaderColor = galaxySky(skyDirection);
        alpha = fullSkyAmount(skyDirection.y, clamp01(max(Intensity, 0.0) * 0.92));
    } else if (Mode < 5.5) { // Overcast
        float amount = clamp01(max(Intensity, 0.0) * 0.92);
        shaderColor = overcastSky(skyDirection);
        alpha = fullSkyAmount(skyDirection.y, amount);
        
        // Rain effect
        vec2 rainUv = vec2(TexCoord.x * Aspect, 1.0 - TexCoord.y);
        rainUv += vec2(CameraPosition.x * 0.006 + CameraPosition.z * 0.002, CameraPosition.y * 0.012);
        float farRain = rainLayer(rainUv * vec2(1.0, 1.35), 42.0, 2.7, -0.34, 0.010, 0.72);
        float midRain = rainLayer(rainUv * vec2(1.0, 1.18) + 19.3, 62.0, 3.8, -0.42, 0.007, 0.64);
        float nearRain = rainLayer(rainUv * vec2(1.0, 1.04) + 41.8, 86.0, 5.1, -0.50, 0.006, 0.58);
        float rain = farRain * 0.42 + midRain * 0.58 + nearRain * 0.74;
        float rainFade = 0.56 + (1.0 - smoothstep(0.12, 0.92, TexCoord.y)) * 0.44;
        float rainMask = amount * rainFade;
        vec3 rainColor = vec3(0.50, 0.56, 0.61);
        shaderColor = mix(shaderColor, rainColor, clamp(rain * 0.62 * rainMask, 0.0, 0.36));
        shaderColor = mix(shaderColor, vec3(0.23, 0.26, 0.28), rainMistLayer(rainUv) * amount * 0.10);
        
        // Lightning
        float pulse = lightningPulseValue() * amount;
        float bolt = lightningBoltValue(skyDirection, pulse);
        float cloudFlash = pulse * smoothstep(0.02, 0.82, skyDirection.y) * (0.35 + fbmValue(skyDirection.xz * 9.0) * 0.45);
        vec3 lightningColor = vec3(0.76, 0.84, 0.92);
        shaderColor += lightningColor * cloudFlash * alpha * 0.42;
        shaderColor = mix(shaderColor, lightningColor, clamp(bolt * alpha, 0.0, 0.86));
    } else if (Mode < 6.5) { // Nebula
        shaderColor = nebulaSky(skyDirection);
        alpha = fullSkyAmount(skyDirection.y, clamp01(max(Intensity, 0.0) * 0.88));
    } else if (Mode < 7.5) { // Solar
        shaderColor = solarSky(skyDirection);
        alpha = fullSkyAmount(skyDirection.y, clamp01(max(Intensity, 0.0) * 0.88));
    } else if (Mode < 8.5) { // Vortex
        shaderColor = vortexSky(skyDirection);
        alpha = fullSkyAmount(skyDirection.y, clamp01(max(Intensity, 0.0) * 0.88));
    } else { // Cyber
        shaderColor = cyberSky(skyDirection);
        alpha = fullSkyAmount(skyDirection.y, clamp01(max(Intensity, 0.0) * 0.88));
    }
    
    // Mix sky color with entity texture based on alpha
    vec3 finalColor = mix(baseColor.rgb, shaderColor, alpha);
    fragColor = vec4(finalColor, baseColor.a);
}