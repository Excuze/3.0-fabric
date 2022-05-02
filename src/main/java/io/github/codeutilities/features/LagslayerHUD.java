package io.github.codeutilities.features;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class LagslayerHUD {

    private static final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
    private static final Window mainWindow = MinecraftClient.getInstance().getWindow();
    public static boolean hasLagSlayer;
    public static boolean lagSlayerEnabled;
    private static Text barsText;
    private static Text numberText;
    private static long lastUpdate;

    private LagslayerHUD() {
        throw new RuntimeException("CPU_UsageText is a static class !");
    }
    
    public static void updateCPU(OverlayMessageS2CPacket packet) {
        JsonArray msgArray = Text.Serializer.toJsonTree(packet.getMessage()).getAsJsonObject().getAsJsonArray("extra");
        JsonObject msgPart = msgArray.get(2).getAsJsonObject();

        barsText = packet.getMessage();

        int sibs = barsText.getSiblings().size();

        Text pText = barsText.getSiblings().get(sibs - 2);
        pText.getSiblings().add(barsText.getSiblings().get(sibs - 1));

        barsText.getSiblings().remove(sibs - 1);
        barsText.getSiblings().remove(sibs - 2);
        barsText.getSiblings().remove(0);

        String numberStr = pText.asString().replaceAll("\\(", "").replaceAll("\\)", "");
        String numberColor = msgPart.get("color").getAsString();

        if (numberColor.equals("dark_gray")) numberColor = "white";

        numberText = Text.Serializer.fromJson("{\"extra\":[{\"italic\":false,\"color\":\"white\",\"text\":\"(\"}," +
                "{\"italic\":false,\"color\":\"" + numberColor + "\",\"text\":\"" + numberStr + "%\"}," +
                "{\"italic\":false,\"color\":\"white\",\"text\":\")\"}],\"text\":\"\"}");

        lastUpdate = System.currentTimeMillis();
    }

    public static void onRender(MatrixStack stack) {

        if (barsText == null || numberText == null) return;
        if ((System.currentTimeMillis() - lastUpdate) > 1200) {
            barsText = null;
            numberText = null;
            hasLagSlayer = false;
            return;
        }

        hasLagSlayer = true;

        try {
            renderText(stack, Formatting.GOLD.getColorValue());
            renderText(stack, barsText, 2);
            renderText(stack, numberText, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void renderText(MatrixStack stack, Text text, int line) {
        textRenderer.draw(stack, text, 5, mainWindow.getScaledHeight() - (textRenderer.fontHeight * line), 0xffffff);
    }

    private static void renderText(MatrixStack stack, int color) {
        textRenderer.draw(stack, "CPU Usage:", 5, mainWindow.getScaledHeight() - (textRenderer.fontHeight * 3), color);
    }
}