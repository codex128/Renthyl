
layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y, local_size_z = LOCAL_SIZE_Z) in;

//#import "Common/ShaderLib/GLSLCompat.glsllib"
//#import "RenthylJme/ShaderLib/Shadows.glsllib"

uniform sampler2D ShadowMap;
uniform mat4 LightMatrix;
uniform int LightIndex;

layout(R32F) uniform image3D VoxelLightMap;
uniform vec3 GridMin;
uniform vec3 GridMax;

void main() {
    
    ivec3 texel = ivec3(gl_GlobalInvocationID);
    int table = int(imageLoad(VoxelLightMap, texel).r);
    uvec3 globalSize = gl_NumWorkGroups * gl_WorkGroupSize;
    vec3 texCoord = vec3(gl_GlobalInvocationID) / vec3(globalSize);
    vec3 voxSize = (GridMax - GridMin) / vec3(imageSize(VoxelLightMap));
    vec3 wPos = ((GridMax - GridMin) / vec3(globalSize)) * vec3(gl_GlobalInvocationID) + GridMin + voxSize*0.5;
    vec4 lightViewPos = LightMatrix * vec4(wPos, 1.0);
    vec3 lightUv = (lightViewPos.xyz / lightViewPos.w) * 0.5 + 0.5;
    float depth = lightUv.z;
    float shadow = textureLod(ShadowMap, lightUv.xy, 0.0).r;
    if (depth >= 0.0 && lightUv.x >= 0.0 && lightUv.x <= 1.0 && lightUv.y >= 0.0 && lightUv.y <= 1.0 && depth - 0.005 <= shadow) {
        table |= (1 << LightIndex);
    } else {
        table &= ~(1 << LightIndex);
    }
    imageStore(VoxelLightMap, texel, vec4(table, 0.0, 0.0, 0.0));
    
}

