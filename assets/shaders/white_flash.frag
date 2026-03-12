#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords);
    if (texColor.a > 0.0) {
        gl_FragColor = vec4(1.0, 1.0, 1.0, texColor.a * v_color.a);
    } else {
        gl_FragColor = vec4(0.0);
    }
}
