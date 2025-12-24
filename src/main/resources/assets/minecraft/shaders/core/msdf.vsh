#version 150

layout(std140) uniform FontData {
    vec4 screenData;
    vec4 outlineColor;
    vec4 reserved;
    ivec4 charCount;
    vec4 chars[256 * 3];
};

out vec2 texCoord;
out vec4 charColor;
out float outlineWidth;
out vec4 outColor;
out float pxRange;

void main() {
    int charIndex = gl_VertexID / 6;
    int vertexIndex = gl_VertexID % 6;

    vec4 posSize = chars[charIndex * 3];
    vec4 uvCoords = chars[charIndex * 3 + 1];
    vec4 color = chars[charIndex * 3 + 2];

    vec2 positions[6] = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 1.0)
    );

    vec2 pos = positions[vertexIndex];

    vec2 screenPos = posSize.xy + pos * posSize.zw;
    vec2 ndcPos = (screenPos / screenData.xy) * 2.0 - 1.0;
    ndcPos.y = -ndcPos.y;

    gl_Position = vec4(ndcPos, 0.0, 1.0);

    texCoord = vec2(
    mix(uvCoords.x, uvCoords.z, pos.x),
    mix(uvCoords.y, uvCoords.w, pos.y)
    );

    charColor = color;
    outlineWidth = screenData.w;
    outColor = outlineColor;
    pxRange = 15.0;
}