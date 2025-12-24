#version 150

layout(std140) uniform TextureData {
    vec4 rect;      // x, y, width, height
    vec4 screen;    // screenWidth, screenHeight, smoothness, guiScale
    vec4 uvCoords;  // u0, v0, u1, v1
    vec4 radii;     // topLeft, topRight, bottomRight, bottomLeft
    vec4 colors[4]; // 4 угловых цвета для tint
};

out vec2 fragCoord;
out vec2 pixelCoord;
out vec2 texCoord;
out vec2 rectSize;
out vec4 cornerRadii;
out vec4 fragColors[4];
out float fragSmoothness;
out float guiScale;

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

    // UV координаты с интерполяцией
    texCoord = vec2(
    mix(uvCoords.x, uvCoords.z, pos.x),
    mix(uvCoords.y, uvCoords.w, pos.y)
    );

    for (int i = 0; i < 4; i++) {
        fragColors[i] = colors[i];
    }
}