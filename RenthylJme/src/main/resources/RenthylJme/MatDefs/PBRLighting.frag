#import "Common/ShaderLib/GLSLCompat.glsllib"

// enable apis and import PBRLightingUtils
#define ENABLE_PBRLightingUtils_getWorldPosition 1
#define ENABLE_PBRLightingUtils_getWorldNormal 1
#define ENABLE_PBRLightingUtils_getWorldTangent 1
#define ENABLE_PBRLightingUtils_getTexCoord 1
#define ENABLE_PBRLightingUtils_readPBRSurface 1
#define ENABLE_PBRLightingUtils_computeDirectLightContribution 1
#define ENABLE_PBRLightingUtils_computeProbesContribution 1

#import "Common/ShaderLib/module/pbrlighting/PBRLightingUtils.glsllib"
#import "RenthylJme/ShaderLib/Shadows.glsllib"

#ifdef DEBUG_VALUES_MODE
    uniform int m_DebugValuesMode;
#endif

#ifdef NUM_LIGHTS
    uniform int m_NumLights;
    uniform float m_LightData[NUM_LIGHTS];
#endif
uniform vec3 g_CameraPosition;

#ifdef USE_FOG
#import "Common/ShaderLib/MaterialFog.glsllib"
#endif

#ifdef SHADOW_MASK
    uniform sampler2D m_ShadowMask;
#endif

const int DIRECTIONAL_LIGHT_STRIDE = 7;
const int POINT_LIGHT_STRIDE = 8;
const int SPOT_LIGHT_STRIDE = 12;

void main() {
    vec3 wpos = PBRLightingUtils_getWorldPosition();
    vec3 worldViewDir = normalize(g_CameraPosition - wpos);

    // Create a blank PBRSurface.
    PBRSurface surface = PBRLightingUtils_createPBRSurface(worldViewDir);

    // Read surface data from standard PBR matParams. (note: matParams are declared in 'PBRLighting.j3md' and initialized as uniforms in 'PBRLightingUtils.glsllib')
    PBRLightingUtils_readPBRSurface(surface);

    //Calculate necessary variables from pbr surface prior to applying lighting. Ensure all texture/param reading and blending occurrs prior to this being called!
    PBRLightingUtils_calculatePreLightingValues(surface);

    #ifdef SHADOW_MASK
        vec2 screenspaceCoord = vec2(gl_FragCoord.xy) / textureSize(m_ShadowMask, 0);
    #endif

    // Calculate direct lights
    #ifdef NUM_LIGHTS
    for (int i = 0; i < m_NumLights;) {
        int type = int(m_LightData[i + 3]);
        #ifdef SHADOW_MASK
            int shadow = extractShadowIndex(type);
            if (shadow >= 0) {
                uint mask = uint(texture(m_ShadowMask, screenspaceCoord).r);
                if ((mask & (1u << shadow)) == 0u) {
                    continue;
                }
            }
        #endif
        vec4 lightData0 = vec4(m_LightData[i], m_LightData[i + 1], m_LightData[i + 2], normalizeLightType(type));
        vec4 lightData1 = vec4(m_LightData[i + 4], m_LightData[i + 5], m_LightData[i + 6], 0.0);
        vec4 lightData2;
        if (lightData0.w < 0.5) { // directional
            lightData2 = vec4(0.0);
            i += DIRECTIONAL_LIGHT_STRIDE;
        } else if (lightData0.w < 1.5) { // point
            lightData1.w = m_LightData[i + 7];
            lightData2 = vec4(0.0);
            i += POINT_LIGHT_STRIDE;
        } else { // spot
            lightData1.w = m_LightData[i + 7];
            lightData2 = vec4(m_LightData[i + 8], m_LightData[i + 9], m_LightData[i + 10], m_LightData[i + 11]);
            i += SPOT_LIGHT_STRIDE;
        }
        PBRLightingUtils_computeDirectLightContribution(
            lightData0, lightData1, lightData2,
            surface
        );
    }
    #endif

    // Calculate env probes
    PBRLightingUtils_computeProbesContribution(surface);

    // Put it all together
    gl_FragColor.rgb = vec3(0.0);
    gl_FragColor.rgb += surface.bakedLightContribution;
    gl_FragColor.rgb += surface.directLightContribution;
    gl_FragColor.rgb += surface.envLightContribution;
    gl_FragColor.rgb += surface.emission;
    gl_FragColor.a = surface.alpha;

    #ifdef SHADOW_MASK
        //gl_FragColor.rgb = vec3(normalizeLightType(int(m_LightData[0])));
    #endif

    #ifdef USE_FOG
    gl_FragColor = MaterialFog_calculateFogColor(vec4(gl_FragColor));
    #endif

    //outputs the final value of the selected layer as a color for debug purposes.
    #ifdef DEBUG_VALUES_MODE
    gl_FragColor = PBRLightingUtils_getColorOutputForDebugMode(m_DebugValuesMode, vec4(gl_FragColor.rgba), surface);
    #endif
}