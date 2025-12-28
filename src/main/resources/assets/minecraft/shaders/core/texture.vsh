#version 150

layout(std140) uniform TextureData {
    vec4 rect;
    vec4 screen;
    vec4 uvCoords;
    vec4 radii;
    vec4 settings;
    vec4 colors[4];
};

out vec2 fragCoord;
out vec2 pixelCoord;
out vec2 texCoord;
out vec2 rectSize;
out vec4 cornerRadii;
out vec4 fragColors[4];
out float fragSmoothness;
out float guiScale;
out float isPixelPerfect;

void main() {
    vec2 positions[6] = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 1.0)
    );

    vec2 pos = positions[gl_VertexID];

    vec2 screenPos = rect.xy + pos * rect.zw;
    vec2 ndcPos = (screenPos / screen.xy) * 2.0 - 1.0;
    ndcPos.y = -ndcPos.y;

    gl_Position = vec4(ndcPos, 0.0, 1.0);

    fragCoord = pos;
    pixelCoord = pos * rect.zw;
    rectSize = rect.zw;
    cornerRadii = radii;
    fragSmoothness = screen.z;
    guiScale = screen.w;
    isPixelPerfect = settings.x;

    texCoord = vec2(
    mix(uvCoords.x, uvCoords.z, pos.x),
    mix(uvCoords.y, uvCoords.w, pos.y)
    );

    for (int i = 0; i < 4; i++) {
        fragColors[i] = colors[i];
    }
}