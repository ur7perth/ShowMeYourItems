package com.abo9kr.showmeyouritems;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class ShowMeYourItemsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(this::onAfterEntities);
    }

    private void onAfterEntities(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        for (PlayerEntity player : client.world.getPlayers()) {
            // تجاهل رسم المعلومات فوق رأسك أنت لو كنت بمنظور الشخص الأول
            if (player == client.player && client.options.getPerspective().isFirstPerson()) {
                continue;
            }
            renderInfoAbovePlayer(context, player);
        }
    }

    private void renderInfoAbovePlayer(WorldRenderContext context, PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        Vec3d cameraPos = context.camera().getPos();
        double x = player.getX() - cameraPos.x;
        double y = player.getY() + player.getHeight() + 0.7 - cameraPos.y;
        double z = player.getZ() - cameraPos.z;

        PlayerInventory inv = player.getInventory();
        ItemStack mainHand = player.getMainHandStack();

        List<String> lines = new ArrayList<>();

        // الخانة الأولى: الآيتم الممسوك حالياً
        if (!mainHand.isEmpty()) {
            lines.add(mainHand.getName().getString() + " x" + mainHand.getCount());
        } else {
            lines.add("يد فارغة");
        }

        // باقي الـ9 خانات (hotbar) اللي يقدر اللاعب يبدل بينها
        StringBuilder hotbar = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                if (hotbar.length() > 0) hotbar.append(" | ");
                hotbar.append(stack.getName().getString()).append(" x").append(stack.getCount());
            }
        }
        if (hotbar.length() > 0) {
            lines.add(hotbar.toString());
        }

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider.Immediate vertexConsumers =
                client.getBufferBuilders().getEntityVertexConsumers();

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(client.gameRenderer.getCamera().getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float lineHeight = 10f;
        float startY = -(lines.size() * lineHeight) / 2f;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float width = textRenderer.getWidth(line);
            float xOffset = -width / 2f;
            float yOffset = startY + i * lineHeight;
            textRenderer.draw(
                    line, xOffset, yOffset, 0xFFFFFF, false,
                    matrix, vertexConsumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0x40000000, light
            );
        }

        vertexConsumers.draw();
        matrices.pop();
    }
}
