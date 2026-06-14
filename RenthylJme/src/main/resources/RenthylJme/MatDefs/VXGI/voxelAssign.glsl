
layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y, local_size_z = LOCAL_SIZE_Z) in;

layout(RGBA8) uniform image3D VoxelMap;
uniform vec4 Value;

void main() {
    
    imageStore(VoxelMap, ivec3(gl_GlobalInvocationID), Value);
    
}

