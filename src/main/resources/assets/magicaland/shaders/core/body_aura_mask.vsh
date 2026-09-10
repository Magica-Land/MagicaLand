#version 150
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;
out vec4 vertexColor;
out vec2 texCoord;
out float vertexDistance;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord = UV0;
    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
}
