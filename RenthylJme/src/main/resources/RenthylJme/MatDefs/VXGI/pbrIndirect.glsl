
layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y, local_size_z = LOCAL_SIZE_Z) in;

#import "RenthylJme/ShaderLib/Projection.glsllib"
#import "RenthylJme/MatDefs/VXGI/gbufferCompression.glsllib"

uniform sampler3D VoxelMap;
uniform sampler2D ColorMap;
uniform sampler2D DepthMap;
uniform sampler2D DiffuseMap;
uniform sampler2D PositionMap;
uniform sampler2D NormalMap;
uniform sampler2D MaterialMap;
uniform mat4 CameraMatrixInverse;
uniform vec3 CameraPosition;
uniform vec3 GridMin;
uniform vec3 GridMax;
uniform int GridSize;
uniform float TraceTangent;
uniform vec2 SpecularAngleRange;
uniform float TraceQuality;
uniform vec3 TraceDirections[NUM_TRACES];
uniform float IndirectFactor;
uniform float Time;

layout(RGBA8) uniform image2D Target;

#define TRACING_VOXEL_MAP VoxelMap
#define GRID_MIN GridMin
#define GRID_MAX GridMax
#define GRID_SIZE GridSize
#define TRACE_DISPLACEMENT 0.1
#import "RenthylJme/MatDefs/VXGI/voxelConeTracing.glsllib"

shared int atomicR = 0;
shared int atomicG = 0;
shared int atomicB = 0;
shared int atomicA = 0;
const vec2 expansion = vec2(100000.0, 0.00001);
const float pi = 3.14159265359;

void main() {
    
    ivec2 texel = ivec2(gl_WorkGroupID.xy);
    
    vec3 diffuse = texelFetch(DiffuseMap, texel, 0).rgb;
    vec3 wPosition = texelFetch(PositionMap, texel, 0).xyz;
    vec3 normal = texelFetch(NormalMap, texel, 0).xyz;
    
    // unpack material
    vec3 specular;
    vec2 metalRough;
    vec4 gbuffer = texelFetch(MaterialMap, texel, 0);
    expandGBuffer(gbuffer, specular, metalRough);
    wPosition += normal * maxVoxelSize();
    
    // get trace direction and angle
    vec3 direction;
    float apertureTan;
    float factor;
    if (gl_LocalInvocationIndex == 0) {
        // first invocation does specular
        direction = reflect(normalize(wPosition - CameraPosition), normal);
        apertureTan = tan(mix(SpecularAngleRange.x, SpecularAngleRange.y, clamp(1.0, 0.0, 1.0)));
        //apertureTan = TraceTangent;
        factor = 1.0;
    } else {
        // all other invocations do diffuse
        //float angle = noise1(float(gl_GlobalInvocationID.x * gl_GlobalInvocationID.y) * Time) * pi * 2.0;
        //float angle = (gl_WorkGroupID.x + gl_WorkGroupID.y + gl_WorkGroupID.z) * 0.005;
        vec3 seed = vec3(gl_WorkGroupID);
        vec2 noisePos = vec2(gl_WorkGroupID.xy);
        vec3 xtan = orthogonal(normal, random3(seed));
        vec3 ytan = cross(normal, xtan);
        mat3 tanSpace = mat3(xtan, ytan, normal);
        direction = normalize(tanSpace * TraceDirections[max(gl_LocalInvocationIndex - 1, 0)]);
        apertureTan = TraceTangent;
        factor = 1.0;
    }
    
    // approximate indirect lighting
    vec4 color = traceVoxelCone(wPosition, direction, apertureTan * 2.0, TraceQuality);
    //vec4 color = vec4(0.0);
    color.rgb *= factor;
    
    // combine trace results with results from other invocations
    ivec4 iClr = ivec4(color * expansion.x);
    atomicAdd(atomicR, iClr.r);
    atomicAdd(atomicG, iClr.g);
    atomicAdd(atomicB, iClr.b);
    atomicAdd(atomicA, iClr.a);
    
    // add indirect color to result
    if (gl_LocalInvocationIndex == 0) {
        memoryBarrier();
        vec4 baseColor = texelFetch(ColorMap, texel, 0);
        vec4 indirect = vec4(atomicR, atomicG, atomicB, atomicA) * vec4(diffuse, 1.0) * expansion.y;
        indirect.a /= (gl_WorkGroupSize.x * gl_WorkGroupSize.y * gl_WorkGroupSize.z);
        imageStore(Target, texel, indirect * IndirectFactor + baseColor);
        //imageStore(Target, texel, vec4(indirect.a));
        //imageStore(Target, texel, indirect);
        //imageStore(Target, texel, color.rgbb);
        //imageStore(Target, texel, vec4(wPosition * 0.01, 1.0));
        //imageStore(Target, texel, vec4(diffuse, 1.0));
        //imageStore(Target, texel, indirect);
        //imageStore(Target, texel, vec4(1.0 - depth, 0.0, 0.0, 1.0));
        //imageStore(Target, texel, vec4(direction, 1.0));
        //imageStore(Target, texel, vec4(1.0, 0.0, 0.0, 1.0));
    }
    
}

