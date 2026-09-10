#version 150
#moj_import <fog.glsl>

uniform vec4 ColorModulator;
uniform sampler2D Sampler0;
uniform float FogStart;
uniform float FogEnd;
in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 auraCoord;
in vec3 viewPosition;
in vec3 viewNormal;
in float flowTime;
flat in int effect;
out vec4 fragColor;

float hornWisps(vec2 uv, float time) {
    float angle = uv.x * 6.283185;
    float phase = time * 0.5235988;
    // 宽而不等距的光纹缓慢游动，圆周接缝与 12 秒时钟都连续。
    float drift = sin(uv.y * 5.2 - phase * 2.0 + sin(angle + phase) * 1.15);
    float curl = sin(angle * 2.0 - uv.y * 3.1 + phase + sin(angle - phase * 3.0) * 0.7);
    float wisps = 0.5 + 0.5 * (drift * 0.6 + curl * 0.4);
    float breath = 0.5 + 0.5 * sin(phase + sin(phase * 2.0) * 0.45);
    return 0.49 + 0.36 * wisps + 0.045 * breath;
}

void main() {
    float opacity;
    int effectType = effect & 7;
    if (effectType == 1) {
        vec2 p = abs(texCoord0 * 2.0 - 1.0);
        float star = pow(p.x, 0.65) + pow(p.y, 0.65);
        opacity = 1.0 - smoothstep(0.52, 1.0, star);
    } else if (effectType == 2) {
        // 只复用物品的透明剪影，不能把花、剑等道具画成整张矩形。
        float mask = texture(Sampler0, texCoord0).a;
        vec3 facing = -viewPosition / max(length(viewPosition), 0.0001);
        float edge = pow(abs(dot(normalize(viewNormal), facing)), 0.8);
        opacity = mask * (0.3 + 0.7 * edge) * hornWisps(auraCoord, flowTime);
    } else if (effectType == 3) {
        if (texCoord0.x < 0.0) {
            vec2 p = vec2((-texCoord0.x - 1.0) * 2.0 - 1.0, texCoord0.y * 2.0 - 1.0);
            float radius = length(p);
            opacity = (1.0 - smoothstep(0.12, 1.0, radius)) * hornWisps(p * 0.5 + 0.5, flowTime);
        } else {
            float edge = 1.0 - smoothstep(0.5, 1.0, abs(texCoord0.y * 2.0 - 1.0));
            opacity = edge * hornWisps(vec2(texCoord0.y, texCoord0.x), flowTime);
        }
    } else if (effectType == 5) {
        if (texCoord0.x < 0.0) {
            vec2 p = vec2((-texCoord0.x - 1.0) * 2.0 - 1.0, texCoord0.y * 2.0 - 1.0);
            float phase = flowTime * 0.5235988;
            float warp = 1.0 + 0.035 * sin(p.x * 3.0 + phase * 2.0) * sin(p.y * 4.0 - phase);
            opacity = (1.0 - smoothstep(0.10, 1.0, length(p) * warp))
                    * (0.80 + 0.20 * hornWisps(p * 0.5 + 0.5, flowTime));
        } else {
            float curl = 0.13 * sin(texCoord0.y * 5.0 - flowTime * 1.0471976)
                    * sin(texCoord0.y * 3.141593);
            float edge = 1.0 - smoothstep(0.28, 1.0, abs(texCoord0.x * 2.0 - 1.0 + curl));
            edge *= 1.0 - smoothstep(0.85, 0.98, abs(texCoord0.x * 2.0 - 1.0));
            opacity = edge * smoothstep(0.0, 0.16, texCoord0.y)
                    * (1.0 - smoothstep(0.50, 1.0, texCoord0.y)) * hornWisps(texCoord0, flowTime);
        }
    } else {
        vec3 facing = -viewPosition / max(length(viewPosition), 0.0001);
        float edge = pow(abs(dot(normalize(viewNormal), facing)), 0.8);
        float height = smoothstep(0.0, 0.17, texCoord0.y) * (1.0 - smoothstep(0.84, 1.0, texCoord0.y));
        opacity = edge * height * hornWisps(texCoord0, flowTime);
        if (effectType == 4) {
            float progress = float(effect >> 3) / 32767.0;
            float front = progress * 1.16;
            opacity *= 1.0 - smoothstep(front - 0.16, front, texCoord0.y);
        }
    }
    vec4 color = vertexColor * ColorModulator;
    color.a *= opacity * linear_fog_fade(vertexDistance, FogStart, FogEnd);
    if (color.a * max(max(color.r, color.g), color.b) < 0.004) discard;
    fragColor = color;
}
