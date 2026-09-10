#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform vec2 MaskSize;
uniform vec2 ProjectionDepth;
uniform float ProjectionScale;
uniform float FlowTime;
in vec2 texCoord;
out vec4 fragColor;

float bodyWisps(vec2 uv, float time) {
    float angle = uv.x * 6.283185;
    float phase = time * 0.5235988;
    float drift = sin(uv.y * 5.2 - phase * 2.0 + sin(angle + phase) * 1.15);
    float curl = sin(angle * 2.0 - uv.y * 3.1 + phase + sin(angle - phase * 3.0) * 0.7);
    return 0.80 + 0.20 * (0.5 + 0.5 * (drift * 0.6 + curl * 0.4));
}

void main() {
    vec4 center = texture(Sampler0, texCoord);
    float terrain = texture(Sampler2, texCoord).r;
    float nearest = 1.0;
    float halo = 0.0;
    vec3 haloColor = center.rgb;
    // 半径随距离缩小；所有骨骼先合成一个剪影，不累加关节内部亮度。
    for (int ring = 0; ring < 4; ring++) {
        float distancePixels = ring == 0 ? 1.0 : ring == 1 ? 2.5 : ring == 2 ? 4.8 : 7.5;
        for (int direction = 0; direction < 8; direction++) {
            float angle = float(direction) * 0.78539816;
            vec2 uv = texCoord + vec2(cos(angle), sin(angle)) * distancePixels / MaskSize;
            if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) continue;
            vec4 sampleColor = texture(Sampler0, uv);
            if (sampleColor.a < 0.001) continue;
            float depth = texture(Sampler1, uv).r;
            if (depth > terrain + 0.000001) continue;
            float viewDepth = abs(ProjectionDepth.y / (depth * 2.0 - 1.0 + ProjectionDepth.x));
            float radius = clamp(0.055 * ProjectionScale * MaskSize.y / max(viewDepth * 2.0, 0.001), 1.2, 8.5);
            float influence = sampleColor.a * exp(-2.5 * distancePixels * distancePixels / (radius * radius));
            if (influence > halo) { halo = influence; haloColor = sampleColor.rgb; }
            if (influence > 0.012) nearest = min(nearest, depth);
        }
    }
    float centerDepth = texture(Sampler1, texCoord).r;
    // 中心已在剪影阶段完成遮挡测试，避免再次用降采样深度误杀斜面。
    if (center.a > 0.001) nearest = min(nearest, centerDepth);
    float wave = bodyWisps(texCoord * vec2(2.0, 2.5), FlowTime);
    float coverage = centerDepth < 1.0 && center.a > 0.001 ? 1.0 : 0.0;
    float opacity = (0.045 * center.a + 0.40 * halo * (1.0 - coverage)) * wave;
    if (opacity < 0.003 || nearest >= 1.0) discard;
    vec3 tint = center.a > 0.5 ? center.rgb : haloColor;
    fragColor = vec4(tint, opacity);
    gl_FragDepth = nearest;
}
