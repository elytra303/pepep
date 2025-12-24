#version 150

in vec2 fragCoord;
in vec2 pixelCoord;
in vec2 texCoord;
in vec2 rectSize;
in vec4 cornerRadii;
in vec4 fragColors[4];
in float fragSmoothness;
in float guiScale;

out vec4 fragColor;

uniform sampler2D Sampler0;

// SDF для скруглённого прямоугольника
float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;

    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

// Билинейная интерполяция 4 цветов
vec4 sampleGradient(vec2 uv) {
    // colors[0] = top-left, colors[1] = bottom-left
    // colors[2] = bottom-right, colors[3] = top-right
    vec4 top = mix(fragColors[0], fragColors[3], uv.x);
    vec4 bottom = mix(fragColors[1], fragColors[2], uv.x);
    return mix(top, bottom, uv.y);
}

void main() {
    vec2 halfSize = rectSize * 0.5;
    vec2 center = pixelCoord - halfSize;

    // Ограничиваем радиусы
    float maxRadius = min(halfSize.x, halfSize.y);
    vec4 radii = min(cornerRadii, vec4(maxRadius));

    float dist = roundedBoxSDF(center, halfSize, radii);

    // Адаптивное сглаживание
    float pixelWidth = fwidth(dist);
    float smoothing = max(pixelWidth * fragSmoothness, 0.5 / guiScale);

    float alpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    // Отбрасываем полностью прозрачные пиксели
    if (alpha < 0.001) {
        discard;
    }

    // Сэмплируем текстуру
    vec4 texColor = texture(Sampler0, texCoord);

    // Применяем tint
    vec4 tintColor = sampleGradient(fragCoord);

    // Финальный цвет
    vec3 finalColor = texColor.rgb * tintColor.rgb;
    float finalAlpha = texColor.a * tintColor.a * alpha;

    // Premultiplied alpha
    fragColor = vec4(finalColor * finalAlpha, finalAlpha);
}