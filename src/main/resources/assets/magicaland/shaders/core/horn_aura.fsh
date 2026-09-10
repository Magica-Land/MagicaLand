#version 150
#moj_import <fog.glsl>

uniform vec4 ColorModulator;
uniform sampler2D Sampler0;
uniform float FogStart;
uniform float FogEnd;
in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewPosition;
in vec3 viewNormal;
in float flowTime;
flat in int effect;
out vec4 fragColor;

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
        float wave = 0.5 + 0.5 * sin(-flowTime * 3.141593);
        opacity = mask * (0.3 + 0.7 * edge) * (0.45 + 0.55 * wave * wave);
    } else if (effectType == 3) {
        if (texCoord0.x < 0.0) {
            vec2 p = vec2((-texCoord0.x - 1.0) * 2.0 - 1.0, texCoord0.y * 2.0 - 1.0);
            float radius = length(p);
            float wave = 0.5 + 0.5 * sin(radius * 6.283185 - flowTime * 3.141593);
            opacity = (1.0 - smoothstep(0.12, 1.0, radius)) * (0.82 + 0.18 * wave);
        } else {
            float edge = 1.0 - smoothstep(0.5, 1.0, abs(texCoord0.y * 2.0 - 1.0));
            float wave = 0.5 + 0.5 * sin(texCoord0.x * 12.56637 - flowTime * 3.141593 + texCoord0.y * 1.2);
            opacity = edge * (0.76 + 0.24 * wave);
        }
    } else {
        vec3 facing = -viewPosition / max(length(viewPosition), 0.0001);
        float edge = pow(abs(dot(normalize(viewNormal), facing)), 0.8);
        float height = smoothstep(0.0, 0.17, texCoord0.y) * (1.0 - smoothstep(0.84, 1.0, texCoord0.y));
        float wave = 0.5 + 0.5 * sin(texCoord0.y * 18.84956 - flowTime * 3.141593
                + sin(texCoord0.x * 6.283185) * 1.4);
        opacity = edge * height * (0.45 + 0.55 * wave * wave);
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
