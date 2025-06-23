package com.example.entity.client;

import com.example.entity.ShadowEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ShadowEntityRenderer extends EntityRenderer<ShadowEntity> {
    
    public ShadowEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        // No shadow for shadow entities (ironic!)
        this.shadowRadius = 0.0F;
        this.shadowOpacity = 0.0F;
    }

    @Override
    public Identifier getTexture(ShadowEntity entity) {
        // This is not used since the entity is invisible, but it's required by the abstract class
        return new Identifier("minecraft", "textures/entity/armor_stand.png");
    }
}