#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/PBR.glsllib"
#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "RenthylJme/MatDefs/VXGI/gbufferCompression.glsllib"
#import "RenthylJme/ShaderLib/Shadows.glsllib"

layout(location = 0) out vec4 outScene;
layout(location = 1) out vec4 outDiffuse;
layout(location = 2) out vec4 outPosition;
layout(location = 3) out vec4 outNormal;
layout(location = 4) out vec4 outMaterial;

varying vec2 texCoord;
#ifdef SEPARATE_TEXCOORD
    varying vec2 texCoord2;
#endif

varying vec4 Color;

uniform float m_VXGI_LightData[VXGI_LIGHT_DATA_SIZE];
uniform vec3 g_CameraPosition;
uniform float m_Roughness;
uniform float m_Metallic;

#ifdef VXGI_SHADOWS
    uniform sampler2D m_VXGI_LightContributionMap;
    uniform vec2 m_VXGI_ScreenSize;
#endif

varying vec3 wPosition;

#ifdef BASECOLORMAP
  uniform sampler2D m_BaseColorMap;
#endif

#ifdef USE_PACKED_MR
  uniform sampler2D m_MetallicRoughnessMap;
#else
    #ifdef METALLICMAP
      uniform sampler2D m_MetallicMap;
    #endif
    #ifdef ROUGHNESSMAP
      uniform sampler2D m_RoughnessMap;
    #endif
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

#ifdef SPECGLOSSPIPELINE

  uniform vec4 m_Specular;
  uniform float m_Glossiness;
  #ifdef USE_PACKED_SG
    uniform sampler2D m_SpecularGlossinessMap;
  #else
    uniform sampler2D m_SpecularMap;
    uniform sampler2D m_GlossinessMap;
  #endif
#endif

#ifdef PARALLAXMAP
  uniform sampler2D m_ParallaxMap;  
#endif
#if (defined(PARALLAXMAP) || (defined(NORMALMAP_PARALLAX) && defined(NORMALMAP)))
    uniform float m_ParallaxHeight;
#endif

#ifdef LIGHTMAP
  uniform sampler2D m_LightMap;
#endif

#ifdef AO_STRENGTH
  uniform float m_AoStrength;
#endif
  
#if defined(NORMALMAP) || defined(PARALLAXMAP)
  uniform sampler2D m_NormalMap;   
  varying vec4 wTangent;
#endif
#ifdef NORMALSCALE
  uniform float m_NormalScale;
#endif
varying vec3 wNormal;

// Specular-AA
#ifdef SPECULAR_AA_SCREEN_SPACE_VARIANCE
  uniform float m_SpecularAASigma;
#endif
#ifdef SPECULAR_AA_THRESHOLD
  uniform float m_SpecularAAKappa;
#endif

#ifdef DISCARD_ALPHA
  uniform float m_AlphaDiscardThreshold;
#endif

vec4 readLightData(int i) {
    return vec4(m_VXGI_LightData[i], m_VXGI_LightData[i+1], m_VXGI_LightData[i+2], m_VXGI_LightData[i+3]);
}

void main(){
    vec2 newTexCoord;
    vec3 viewDir = normalize(g_CameraPosition - wPosition);
    
    vec3 norm = normalize(wNormal);
    #if defined(NORMALMAP) || defined(PARALLAXMAP)
        vec3 tan = normalize(wTangent.xyz);
        mat3 tbnMat = mat3(tan, wTangent.w * cross( (norm), (tan)), norm);
    #endif

    #if (defined(PARALLAXMAP) || (defined(NORMALMAP_PARALLAX) && defined(NORMALMAP)))
       vec3 vViewDir =  viewDir * tbnMat;  
       #ifdef STEEP_PARALLAX
           #ifdef NORMALMAP_PARALLAX
               //parallax map is stored in the alpha channel of the normal map         
               newTexCoord = steepParallaxOffset(m_NormalMap, vViewDir, texCoord, m_ParallaxHeight);
           #else
               //parallax map is a texture
               newTexCoord = steepParallaxOffset(m_ParallaxMap, vViewDir, texCoord, m_ParallaxHeight);         
           #endif
       #else
           #ifdef NORMALMAP_PARALLAX
               //parallax map is stored in the alpha channel of the normal map         
               newTexCoord = classicParallaxOffset(m_NormalMap, vViewDir, texCoord, m_ParallaxHeight);
           #else
               //parallax map is a texture
               newTexCoord = classicParallaxOffset(m_ParallaxMap, vViewDir, texCoord, m_ParallaxHeight);
           #endif
       #endif
    #else
       newTexCoord = texCoord;    
    #endif
    
    #ifdef BASECOLORMAP
        vec4 albedo = texture2D(m_BaseColorMap, newTexCoord) * Color;
    #else
        vec4 albedo = Color;
    #endif

    //ao in r channel, roughness in green channel, metallic in blue channel!
    vec3 aoRoughnessMetallicValue = vec3(1.0, 1.0, 0.0);
    #ifdef USE_PACKED_MR
        aoRoughnessMetallicValue = texture2D(m_MetallicRoughnessMap, newTexCoord).rgb;
        float Roughness = aoRoughnessMetallicValue.g * max(m_Roughness, 1e-4);
        float Metallic = aoRoughnessMetallicValue.b * max(m_Metallic, 0.0);
    #else
        #ifdef ROUGHNESSMAP
            float Roughness = texture2D(m_RoughnessMap, newTexCoord).r * max(m_Roughness, 1e-4);
        #else
            float Roughness =  max(m_Roughness, 1e-4);
        #endif
        #ifdef METALLICMAP
            float Metallic = texture2D(m_MetallicMap, newTexCoord).r * max(m_Metallic, 0.0);
        #else
            float Metallic =  max(m_Metallic, 0.0);
        #endif
    #endif
 
    float alpha = albedo.a;

    #ifdef DISCARD_ALPHA
        if(alpha < m_AlphaDiscardThreshold){
            discard;
        }
    #endif
 
    // ***********************
    // Read from textures
    // ***********************
    #if defined(NORMALMAP)
      vec4 normalHeight = texture2D(m_NormalMap, newTexCoord);
      // Note we invert directx style normal maps to opengl style

      #ifdef NORMALSCALE
        vec3 normal = normalize((normalHeight.xyz * vec3(2.0, NORMAL_TYPE * 2.0, 2.0) - vec3(1.0, NORMAL_TYPE * 1.0, 1.0)) * vec3(m_NormalScale, m_NormalScale, 1.0));
      #else
        vec3 normal = normalize((normalHeight.xyz * vec3(2.0, NORMAL_TYPE * 2.0, 2.0) - vec3(1.0, NORMAL_TYPE * 1.0, 1.0)));
      #endif
      normal = normalize(tbnMat * normal);
      //normal = normalize(normal * inverse(tbnMat));
    #else
      vec3 normal = norm;
    #endif

    #ifdef SPECGLOSSPIPELINE

        #ifdef USE_PACKED_SG
            vec4 specularColor = texture2D(m_SpecularGlossinessMap, newTexCoord);
            float glossiness = specularColor.a * m_Glossiness;
            specularColor *= m_Specular;
        #else
            #ifdef SPECULARMAP
                vec4 specularColor = texture2D(m_SpecularMap, newTexCoord);
            #else
                vec4 specularColor = vec4(1.0);
            #endif
            #ifdef GLOSSINESSMAP
                float glossiness = texture2D(m_GlossinessMap, newTexCoord).r * m_Glossiness;
            #else
                float glossiness = m_Glossiness;
            #endif
            specularColor *= m_Specular;
        #endif
        vec4 diffuseColor = albedo;// * (1.0 - max(max(specularColor.r, specularColor.g), specularColor.b));
        Roughness = 1.0 - glossiness;
        vec3 fZero = specularColor.xyz;
    #else
        float specular = 0.5;
        float nonMetalSpec = 0.08 * specular;
        vec4 specularColor = (nonMetalSpec - nonMetalSpec * Metallic) + albedo * Metallic;
        vec4 diffuseColor = albedo - albedo * Metallic;
        vec3 fZero = vec3(specular);
    #endif

    outScene.rgb = vec3(0.0);
    vec3 ao = vec3(1.0);

    #ifdef LIGHTMAP
       vec3 lightMapColor;
       #ifdef SEPARATE_TEXCOORD
          lightMapColor = texture2D(m_LightMap, texCoord2).rgb;
       #else
          lightMapColor = texture2D(m_LightMap, texCoord).rgb;
       #endif
       #ifdef AO_MAP
         lightMapColor.gb = lightMapColor.rr;
         ao = lightMapColor;
       #else
         outScene.rgb += diffuseColor.rgb * lightMapColor;
       #endif
       specularColor.rgb *= lightMapColor;
    #endif

    #if defined(AO_PACKED_IN_MR_MAP) && defined(USE_PACKED_MR)
       ao = aoRoughnessMetallicValue.rrr;
    #endif

    #ifdef AO_STRENGTH
       ao = 1.0 + m_AoStrength * (ao - 1.0);
       // sanity check
       ao = clamp(ao, 0.0, 1.0);
    #endif

    #ifdef SPECULAR_AA
        float sigma = 1.0;
        float kappa = 0.18;
        #ifdef SPECULAR_AA_SCREEN_SPACE_VARIANCE
            sigma = m_SpecularAASigma;
        #endif
        #ifdef SPECULAR_AA_THRESHOLD
            kappa = m_SpecularAAKappa;
        #endif
    #endif
    #ifdef VXGI_SHADOWS
        vec2 screenUv = vec2(gl_FragCoord.xy) / m_VXGI_ScreenSize;
    #endif
    float ndotv = max( dot( normal, viewDir ),0.0);
    for (int i = 0; i < VXGI_LIGHT_DATA_SIZE; i += 12) {
        vec4 lightColor = readLightData(i);
        int lightType = int(lightColor.w);
        #ifdef VXGI_SHADOWS
            int shadowIndex = int(extractShadowIndex(lightType));
            if (!isExposedToLight(shadowIndex, m_VXGI_LightContributionMap, screenUv)) {
                continue;
            }
        #endif
        lightType = normalizeLightType(lightType);
        vec4 lightData1 = readLightData(i + 4);                
        vec4 lightDir;
        vec3 lightVec;            
        lightComputeDir(wPosition, lightColor.w, lightData1, lightDir, lightVec);

        float fallOff = 1.0;
        #if __VERSION__ >= 110
            // allow use of control flow
        if(lightColor.w > 1.0){
        #endif
            fallOff =  computeSpotFalloff(readLightData(i + 8), lightVec);
        #if __VERSION__ >= 110
        }
        #endif
        //point light attenuation
        fallOff *= lightDir.w;

        lightDir.xyz = normalize(lightDir.xyz);            
        vec3 directDiffuse;
        vec3 directSpecular;

        #ifdef SPECULAR_AA
            float hdotv = PBR_ComputeDirectLightWithSpecularAA(
                                normal, lightDir.xyz, viewDir,
                                lightColor.rgb, fZero, Roughness, sigma, kappa, ndotv,
                                directDiffuse,  directSpecular);
        #else
            float hdotv = PBR_ComputeDirectLight(
                                normal, lightDir.xyz, viewDir,
                                lightColor.rgb, fZero, Roughness, ndotv,
                                directDiffuse,  directSpecular);
        #endif

        vec3 directLighting = diffuseColor.rgb *directDiffuse + directSpecular;
        
        outScene.rgb += directLighting * fallOff;
    }

    #if defined(EMISSIVE) || defined (EMISSIVEMAP)
        #ifdef EMISSIVEMAP
            vec4 emissive = texture2D(m_EmissiveMap, newTexCoord);
            #ifdef EMISSIVE
                emissive *= m_Emissive;
            #endif
        #else
            vec4 emissive = m_Emissive;
        #endif
        outScene += emissive * pow(emissive.a, m_EmissivePower) * m_EmissiveIntensity;
    #endif
    outScene.a = alpha;
    
    outDiffuse = diffuseColor;
    outPosition = vec4(wPosition, 0.0);
    //outDiffuse = compressGBuffer(diffuseColor.rgb, wPosition);
    outNormal = vec4(normalize(mix(wNormal, normal, 0.0)), 0.0);
    outMaterial = compressGBuffer(specularColor.rgb, Metallic, Roughness);
    outScene = vec4(0.0, VXGI_LIGHT_DATA_SIZE, 0.0, 1.0);
   
}
