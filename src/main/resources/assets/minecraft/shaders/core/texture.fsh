#version 150

in vec2 fragCoord;
in vec2 pixelCoord;
in vec2 texCoord;
in vec2 rectSize;
in vec4 cornerRadii;
in vec4 fragColors[4];
in float fragSmoothness;
in float guiScale;
in float isPixelPerfect;

out vec4 fragColor;

uniform sampler2D Sampler0;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;

    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

vec4 sampleGradient(vec2 uv) {
    vec4 top = mix(fragColors[0], fragColors[3], uv.x);
    vec4 bottom = mix(fragColors[1], fragColors[2], uv.x);
    return mix(top, bottom, uv.y);
}

vec4 sampleTexturePixelPerfect(sampler2D tex, vec2 uv) {
    ivec2 texSize = textureSize(tex, 0);
    vec2 texelCoord = uv * vec2(texSize);
    vec2 snappedCoord = floor(texelCoord) + 0.5;
    vec2 snappedUV = snappedCoord / vec2(texSize);
    return texture(tex, snappedUV);
}

void main() {
    vec2 halfSize = rectSize * 0.5;
    vec2 center = pixelCoord - halfSize;

    float maxRadius = min(halfSize.x, halfSize.y);
    vec4 radii = min(cornerRadii, vec4(maxRadius));

    float dist = roundedBoxSDF(center, halfSize, radii);

    float pixelWidth = fwidth(dist);
    float smoothing = max(pixelWidth * fragSmoothness, 0.5 / guiScale);

    float alpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (alpha < 0.001) {
        discard;
    }

    vec4 texColor;
    if (isPixelPerfect > 0.5) {
        texColor = sampleTexturePixelPerfect(Sampler0, texCoord);
    } else {
        texColor = texture(Sampler0, texCoord);
    }

    vec4 tintColor = sampleGradient(fragCoord);

    vec3 finalColor = texColor.rgb * tintColor.rgb;
    float finalAlpha = texColor.a * tintColor.a * alpha;

    fragColor = vec4(finalColor * finalAlpha, finalAlpha);
}