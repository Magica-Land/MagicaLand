#version 150
#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 MaskSize;
uniform float FogStart;
uniform float FogEnd;
in vec4 vertexColor;
in vec2 texCoord;
in float vertexDistance;
out vec4 fragColor;

void main() {
    if (texture(Sampler0, texCoord).a < 0.1 || vertexColor.a < 0.001) discard;
    float terrain = texture(Sampler1, gl_FragCoord.xy / MaskSize).r;
    // depth blit 与重新光栅化的采样中心不同，只容忍半个 mask 像素的自身斜率。
    float tolerance = max(0.000001, 0.51 * fwidth(gl_FragCoord.z));
    if (gl_FragCoord.z > terrain + tolerance) discard;
    fragColor = vec4(vertexColor.rgb, vertexColor.a * linear_fog_fade(vertexDistance, FogStart, FogEnd));
}
