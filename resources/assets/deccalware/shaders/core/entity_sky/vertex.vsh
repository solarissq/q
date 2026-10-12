#version 330

#moj_import <deccalware:common.glsl>

in vec3 Position; // POSITION_TEXTURE_COLOR vertex attributes
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 CameraPos;

out vec3 FragPos;
out vec2 TexCoord;
out vec4 FragColor;
out vec3 ViewDir;

void main() {
    FragPos = (ModelViewMat * vec4(Position, 1.0)).xyz;
    TexCoord = UV0;
    FragColor = Color;
    
    // Calculate view direction from camera to fragment in world space
    ViewDir = normalize(FragPos - CameraPos);
    
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}