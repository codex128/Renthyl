
layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y, local_size_z = LOCAL_SIZE_Z) in;

#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler3D VoxelMap;
uniform int SourceLevel;
layout(RGBA32F) uniform image3D TargetLevel;

void main() {
    
    ivec3 lower = ivec3(gl_GlobalInvocationID);
    ivec3 upper = lower * 2;
    vec4 result = texelFetch(VoxelMap, upper, SourceLevel);
    result += texelFetch(VoxelMap, upper + ivec3(1, 0, 0), SourceLevel);
    result += texelFetch(VoxelMap, upper + ivec3(0, 1, 0), SourceLevel);
    result += texelFetch(VoxelMap, upper + ivec3(1, 1, 0), SourceLevel);
    result += texelFetch(VoxelMap, upper + ivec3(0, 0, 1), SourceLevel);
    result += texelFetch(VoxelMap, upper + ivec3(1, 0, 1), SourceLevel);
    result += texelFetch(VoxelMap, upper + ivec3(0, 1, 1), SourceLevel);
    result += texelFetch(VoxelMap, upper + ivec3(1, 1, 1), SourceLevel);
    result *= 0.125;
    imageStore(TargetLevel, lower, result);
    
}

