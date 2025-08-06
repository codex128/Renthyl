
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/PBR.glsllib"
#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"

#ifdef TEMPORAL
    uniform float g_Time;
    uniform sampler3D m_TemporalVoxelMap;
    uniform vec3 m_GridMin;
    uniform vec3 m_GridMax;
    uniform int m_GridSize;
    uniform float m_Attenuation;
    #define TRACING_VOXEL_MAP m_TemporalVoxelMap
    #define GRID_MIN m_GridMin
    #define GRID_MAX m_GridMax
    #define GRID_SIZE m_GridSize
    #define TRACE_DISPLACEMENT 2.0
    #import "RenthylJme/MatDefs/VXGI/voxelConeTracing.glsllib"
#endif
#ifdef SHADOWS
    #import "RenthylJme/ShaderLib/Shadows.glsllib"
    uniform sampler3D m_LightContributionMap;
#endif
#define COMPONENTS_PER_LIGHT 12

uniform vec3 g_CameraPosition;
uniform float m_LightData[LIGHT_DATA_SIZE];
uniform vec4 m_AmbientLight;

layout(RGBA32F) uniform image3D m_VoxelMap;

varying vec3 wPosition;
varying vec3 vPosition;
varying vec3 wNormal;
varying vec2 texCoord;
varying vec4 Color;

#ifdef BASECOLORMAP
    uniform sampler2D m_BaseColorMap;
#endif
#ifdef EMISSIVE
    uniform vec4 m_Emissive;
#endif
#ifdef EMISSIVEMAP
    uniform sampler2D m_EmissiveMap;
#endif
#if defined(EMISSIVE) || defined(EMISSIVEMAP)
    uniform float m_EmissivePower;
    uniform float m_EmissiveIntensity;
#endif
#ifdef DISCARD_ALPHA
    uniform float m_AlphaDiscardThreshold;
#endif

vec4 readLightData(int i) {
    return vec4(m_LightData[i], m_LightData[i + 1], m_LightData[i + 2], m_LightData[i + 3]);
}

void main() {
    
    if (vPosition.x < 0.0 || vPosition.x >= 1.0 || vPosition.y < 0.0 || vPosition.y >= 1.0 || vPosition.z < 0.0 || vPosition.z >= 1.0) {
        discard;
    }
    
    vec4 diffuseColor = Color;
    #ifdef BASECOLORMAP
        diffuseColor *= texture2D(m_BaseColorMap, texCoord);
    #endif
    
    float alpha = diffuseColor.a;
    #ifdef DISCARD_ALPHA
        if (alpha < m_AlphaDiscardThreshold) {
            discard;
        }
    #endif
    
    vec3 viewDir = normalize(g_CameraPosition - wPosition);
    vec3 normal = normalize(wNormal);
    vec3 fZero = vec3(0.5);
    vec4 result = vec4(diffuseColor.rgb * m_AmbientLight.rgb, diffuseColor.a);
    float ndotv = max(dot(normal, viewDir), 0.0);
    
    for (int i = 0; i < LIGHT_DATA_SIZE; i += COMPONENTS_PER_LIGHT) {
        #ifdef USE_LIGHT_TEXTURES
            vec2 pixel = vec2(m_LightTexInv * i, 0);
            vec4 lightColor = texture2D(m_LightTex1, pixel);
            vec4 lightData1 = texture2D(m_LightTex2, pixel);
        #else
            vec4 lightColor = readLightData(i);
            vec4 lightData1 = readLightData(i + 4);
        #endif
        int lightType = int(lightColor.w);
        #ifdef SHADOWS
            int shadowIndex = extractShadowIndex(lightType);
            lightType = normalizeLightType(lightType);
            if (!isExposedToLight(shadowIndex, m_LightContributionMap, vPosition)) {
                continue;
            }
        #endif
        vec4 lightDir;
        vec3 lightVec;
        lightComputeDir(wPosition, lightType, lightData1, lightDir, lightVec);

        float spotFallOff = 1.0;
        #if __VERSION__ >= 110
        if (lightType == 2) {
        #endif
            #if USE_LIGHT_TEXTURES
                spotFallOff = computeSpotFalloff(texture2D(m_LightTex3, pixel), lightVec);
            #else
                spotFallOff = computeSpotFalloff(readLightData(i + 8), lightVec);
            #endif
        #if __VERSION__ >= 110
        }
        #endif
        //point light attenuation
        spotFallOff *= lightDir.w;

        lightDir.xyz = normalize(lightDir.xyz);            
        vec3 directDiffuse;
        vec3 directSpecular;
        float roughness = 1.0;
        
        PBR_ComputeDirectLight(normal, lightDir.xyz, viewDir, lightColor.rgb,
                fZero, roughness, ndotv, directDiffuse, directSpecular);

        vec3 directLighting = diffuseColor.rgb * directDiffuse /*+ directSpecular*/;
        result.rgb += directLighting * spotFallOff;
        
    }

    #if defined(EMISSIVE) || defined(EMISSIVEMAP)
        #ifdef EMISSIVEMAP
            vec4 emissive = texture2D(m_EmissiveMap, texCoord).rgb;
            #ifdef EMISSIVE
                emissive *= m_Emissive;
            #endif
        #else
            vec4 emissive = m_Emissive;
        #endif
        result.rgb += emissive.rgb * pow(emissive.a, m_EmissivePower) * m_EmissiveIntensity;
    #endif
    
    // temporal multibounce
    #ifdef TEMPORAL
        vec3 sampleDirBias = normalize(g_CameraPosition - wPosition);
        vec4 indirect = approximatePositionIndirectVXGI(wPosition, sampleDirBias, 1.0, g_Time);
        result.rgb += indirect.rgb * diffuseColor.rgb * m_Attenuation;
    #endif
    
    // this will likely produce flickering
    ivec3 voxIndex = ivec3(imageSize(m_VoxelMap) * vPosition);
    //imageStore(m_VoxelMap, voxIndex, result);
    //imageAtomicMax(m_VoxelMap, ivec3(m_GridSize * vPosition), result);
    
    /*vec4 prev = imageLoad(m_VoxelMap, voxIndex);
    prev.rgb += result.rgb;
    prev.a += 1.0;*/
    
    //result.a *= 1.f / 255.f;
    //vec4 prev = imageLoad(m_VoxelMap, voxIndex);
    //vec3 avg = (prev.rgb * prev.a + result.rgb * result.a) / (prev.a + result.a);
    //result = vec4(avg, result.a + result.a);
    //result.a = 1000.0;
    result.a = 1.0;
    imageStore(m_VoxelMap, voxIndex, result);
   
}






