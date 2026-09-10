#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 glow = texture(Sampler0, texCoord);
    if (glow.a < 0.003) discard;
    fragColor = glow;
    gl_FragDepth = texture(Sampler1, texCoord).r;
}
