package com.oddlabs.tt.render;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.DXTImage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages the collection of renderers and texture/sprite registries.
 * Acts as a central hub for accessing various specialized rendering systems.
 */
public final class RenderQueues implements AutoCloseable {
    private final List<@NonNull SpriteRenderer> sprite_renderers = new ArrayList<>();
    private final List<@NonNull SpriteRenderer> blend_sprite_renderers = new ArrayList<>();
    private final List<@NonNull SpriteRenderer> plant_renderers = new ArrayList<>();

    private final List<@NonNull SpriteRenderer> sprite_list_lookup = new ArrayList<>();
    private final List<@NonNull ShadowListRenderer> shadow_renderer_lookup = new ArrayList<>();
    private final Map<@NonNull Supplier<@NonNull Texture @NonNull []>, @NonNull ShadowListKey> desc_to_shadow_key
            = new HashMap<>();
    private final List<@NonNull Texture> texture_lookup = new ArrayList<>();
    // Shared 128x128x32 array for particle effects
    private final TextureArray effect_texture_array = new TextureArray(128, 128, 32,
            Globals.COMPRESSED_RGBA_FORMAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
    private final InstancedSpriteRenderer spriteRenderer = new InstancedSpriteRenderer();
    private final DecalRenderer decalRenderer = new DecalRenderer();
    private final EmitterRenderer emitterRenderer = new EmitterRenderer();

    public RenderQueues() {
    }

    public @NonNull TextureArray getEffectTextureArray() {
        return effect_texture_array;
    }

    public @NonNull TextureKey registerEffectTexture(@NonNull Supplier<Texture[]> desc, int index, int layer) {
        TextureKey key = registerTexture(desc, index);
        Texture texture = getTexture(key);
        texture.setLayer(layer);

        GLImage[] mipmaps = texture.getSourceMipmaps();
        DXTImage dxt = texture.getSourceDXT();
        if (mipmaps != null) {
            effect_texture_array.uploadLayer(layer, mipmaps, Globals.COMPRESSED_RGBA_FORMAT);
            texture.setSourceMipmaps(null);
        } else if (dxt != null) {
            effect_texture_array.uploadLayer(layer, dxt, Globals.COMPRESSED_RGBA_FORMAT);
            texture.setSourceDXT(null);
        }
        return key;
    }

    public @NonNull TextureKey registerEffectTexture(@NonNull Supplier<Texture> desc, int layer) {
        TextureKey key = registerTexture(desc);
        Texture texture = getTexture(key);
        texture.setLayer(layer);

        GLImage[] mipmaps = texture.getSourceMipmaps();
        DXTImage dxt = texture.getSourceDXT();
        if (mipmaps != null) {
            effect_texture_array.uploadLayer(layer, mipmaps, Globals.COMPRESSED_RGBA_FORMAT);
            texture.setSourceMipmaps(null);
        } else if (dxt != null) {
            effect_texture_array.uploadLayer(layer, dxt, Globals.COMPRESSED_RGBA_FORMAT);
            texture.setSourceDXT(null);
        }
        return key;
    }

    public @NonNull EmitterRenderer getEmitterRenderer() {
        return emitterRenderer;
    }

    public @NonNull DecalRenderer getDecalRenderer() {
        return decalRenderer;
    }

    public @NonNull TextureKey registerTexture(@NonNull Supplier<Texture[]> desc, int index) {
        TextureKey key = new TextureKey(texture_lookup.size());
        Texture[] textures = Resources.findResource(desc);
        texture_lookup.add(textures[index]);
        return key;
    }

    public @NonNull TextureKey registerTexture(@NonNull Supplier<Texture> desc) {
        TextureKey key = new TextureKey(texture_lookup.size());
        texture_lookup.add(Resources.findResource(desc));
        return key;
    }

    @NonNull
    Texture getTexture(@NonNull TextureKey key) {
        return texture_lookup.get(key.key());
    }

    public @NonNull ShadowListKey registerRespondRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ShadowListKey key = desc_to_shadow_key.get(desc);
        if (key != null)
            return key;
        ShadowListRenderer renderer = new TargetRespondRenderer(desc);
        return register(desc, renderer);
    }

    private @NonNull ShadowListKey register(@NonNull Supplier<@NonNull Texture @NonNull []> desc,
            @NonNull ShadowListRenderer renderer) {
        int index = shadow_renderer_lookup.size();
        shadow_renderer_lookup.add(renderer);
        ShadowListKey key = new ShadowListKey(index);
        desc_to_shadow_key.put(desc, key);
        return key;
    }

    public @NonNull ShadowListKey registerSelectableShadowList(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ShadowListKey key = desc_to_shadow_key.get(desc);
        return key != null ? key : register(desc, new SelectableShadowRenderer(desc));
    }

    public @NonNull ShadowListKey registerCrackDecalList(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ShadowListKey key = desc_to_shadow_key.get(desc);
        return key != null ? key : register(desc, new CrackDecalRenderer(desc));
    }

    public @NonNull ShadowRenderer getDefaultShadowRenderer() {
        return getShadowRenderer(registerSelectableShadowList(RacesResources.DEFAULT_SHADOW_DESC));
    }

    @NonNull
    ShadowListRenderer getShadowRenderer(@NonNull ShadowListKey key) {
        return shadow_renderer_lookup.get(key.key());
    }

    public @NonNull SpriteKey register(@NonNull SpriteFile sprite_file) {
        return register(sprite_file, 0);
    }

    public @NonNull SpriteKey register(@NonNull SpriteFile sprite_file, int tex_index) {
        int index = sprite_list_lookup.size();
        SpriteList sprite_list = Resources.findResource(sprite_file);

        Texture[] textures = new Texture[sprite_list.getNumSprites()];
        Texture[] team_textures = new Texture[sprite_list.getNumSprites()];
        Texture[] bump_textures = new Texture[sprite_list.getNumSprites()];

        for (int i = 0; i < sprite_list.getNumSprites(); i++) {
            Sprite sprite = sprite_list.getSprite(i);
            textures[i] = sprite.textures[tex_index][Sprite.TEXTURE_NORMAL];
            team_textures[i] = sprite.textures[tex_index][Sprite.TEXTURE_TEAM];
            if (sprite.hasBumpMap(tex_index)) {
                bump_textures[i] = sprite.textures[tex_index][Sprite.TEXTURE_BUMP];
            }
        }

        SpriteRenderer sprite_renderer = new SpriteRenderer(sprite_list, textures, team_textures, bump_textures,
                spriteRenderer);
        sprite_list_lookup.add(sprite_renderer);
        registerSpriteRenderer(sprite_renderer, sprite_file.getLocation());
        AnimationInfo.AnimationType[] animation_types = sprite_list.getAnimationTypes();
        return new SpriteKey(index, sprite_list.getBounds(), animation_types);
    }

    public @NonNull SpriteRenderer getRenderer(@NonNull SpriteKey key) {
        return sprite_list_lookup.get(key.key());
    }

    public @NonNull SpriteKey registerDynamicSprite(@NonNull SpriteList sprite_list, @NonNull Texture texture) {
        int index = sprite_list_lookup.size();

        Texture[] textures = new Texture[sprite_list.getNumSprites()];
        Arrays.fill(textures, texture);
        Texture[] team_textures = new Texture[sprite_list.getNumSprites()];
        Texture[] bump_textures = new Texture[sprite_list.getNumSprites()];

        SpriteRenderer sprite_renderer = new SpriteRenderer(sprite_list, textures, team_textures, bump_textures,
                spriteRenderer);
        sprite_list_lookup.add(sprite_renderer);
        registerSpriteRenderer(sprite_renderer, "dynamic_emoji");
        AnimationInfo.AnimationType[] animation_types = sprite_list.getAnimationTypes();
        return new SpriteKey(index, sprite_list.getBounds(), animation_types);
    }

    public @NonNull SpriteKey registerDynamicSprite(@NonNull SpriteList sprite_list, @NonNull TextureKey texture_key) {
        return registerDynamicSprite(sprite_list, getTexture(texture_key));
    }

    public @NonNull SpriteKey registerIconSprite(com.oddlabs.tt.gui.@NonNull IconQuad icon) {
        SpriteList sprite_list = SpriteList.createQuadInstance(icon.getU1(), icon.getV1(), icon.getU2(), icon.getV2());
        return registerDynamicSprite(sprite_list, icon.getTexture());
    }

    public @NonNull InstancedSpriteRenderer getInstancedRenderer() {
        return spriteRenderer;
    }

    private void registerSpriteRenderer(@NonNull SpriteRenderer sprite_renderer, @NonNull String location) {
        if (sprite_renderer.getSpriteList().getSprite(0).modulateColor()) {
            blend_sprite_renderers.add(sprite_renderer);
        } else if (location.contains("plant") || location.contains("leaf")) {
            plant_renderers.add(sprite_renderer);
        } else {
            sprite_renderers.add(sprite_renderer);
        }
    }

    void getAllPicks(@NonNull Consumer<@NonNull Target> pick_list) {
        for (SpriteRenderer spriteRenderer : sprite_renderers) {
            spriteRenderer.getAllPicks(pick_list);
        }
        for (SpriteRenderer spriteRenderer : plant_renderers) {
            spriteRenderer.getAllPicks(pick_list);
        }
    }

    public void renderAll(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull MatrixStack projectionStack) {
        sprite_renderers.forEach(SpriteRenderer::renderAll);
        spriteRenderer.renderAll(context, camera_state, projectionStack);
    }

    void renderPlants(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull MatrixStack projectionStack) {
        plant_renderers.forEach(SpriteRenderer::renderAll);
        spriteRenderer.renderAll(context, camera_state, projectionStack);
    }

    void renderBlends(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull MatrixStack projectionStack) {
        blend_sprite_renderers.forEach(SpriteRenderer::renderAll);
        spriteRenderer.renderAll(context, camera_state, projectionStack);
    }

    void renderNoDetail() {
        sprite_renderers.forEach(SpriteRenderer::renderNoDetail);
        plant_renderers.forEach(SpriteRenderer::renderNoDetail);
        blend_sprite_renderers.forEach(SpriteRenderer::renderNoDetail);
    }

    void renderShadows(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        decalRenderer.clear();
        try (var _ = decalRenderer.setup(context, renderer, modelViewStack, projectionStack)) {
            for (ShadowListRenderer shadowListRenderer : shadow_renderer_lookup) {
                shadowListRenderer.renderShadows(context, this, renderer, modelViewStack, projectionStack);
            }
        }
    }

    void renderParticles(@NonNull RenderContext context, @NonNull CameraState state,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack, @NonNull Texture depthTexture) {
        emitterRenderer.render(context, this, state, modelViewStack, projectionStack, depthTexture);
    }

    @Override
    public void close() {
        spriteRenderer.close();
        decalRenderer.close();
        emitterRenderer.close();
        for (SpriteList spriteList : sprite_list_lookup.stream().map(SpriteRenderer::getSpriteList).distinct()
                .toList()) {
            spriteList.close();
        }
        shadow_renderer_lookup.forEach(ShadowListRenderer::close);
    }
}
