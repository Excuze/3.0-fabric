package io.github.codeutilities.commands.text;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.codeutilities.CodeUtilities;
import io.github.codeutilities.commands.Command;
import io.github.codeutilities.commands.arguments.FreeStringArgumentType;
import io.github.codeutilities.features.PlayerState;
import io.github.codeutilities.features.commands.gradient.HSLColor;
import io.github.codeutilities.util.ItemUtil;
import io.github.codeutilities.util.chat.ChatType;
import io.github.codeutilities.util.chat.ChatUtil;
import io.github.codeutilities.util.chat.text.MinecraftColors;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.*;

import java.awt.*;

public class GradientCommand implements Command {
    @Override
    public void register(CommandDispatcher<FabricClientCommandSource> cd) {
        MinecraftClient mc = CodeUtilities.MC;
        cd.register(literal("gradient")
                .then(argument("startColor", FreeStringArgumentType.word())
                        .then(argument("endColor", FreeStringArgumentType.word())
                                .then(argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String text = StringArgumentType.getString(ctx, "text");
                                            String strStartColor = StringArgumentType.getString(ctx, "startColor");
                                            String strEndColor = StringArgumentType.getString(ctx, "endColor");

                                            HSLColor startColor;
                                            HSLColor endColor;

                                            try {
                                                if (strStartColor.startsWith("#")) {
                                                    startColor = new HSLColor(Color.decode(strStartColor.toLowerCase()));
                                                } else if (strStartColor.matches("^[0-9a-fA-F]*")) {
                                                    startColor = new HSLColor(Color.decode("#" + strStartColor.toLowerCase()));
                                                } else if (strStartColor.startsWith("&x")) {
                                                    startColor = new HSLColor(Color.decode(strStartColor.toLowerCase().replaceAll("&", "").replaceAll("x", "#")));
                                                } else if (strStartColor.matches("^&[0-9a-fA-F]")) {
                                                    char[] chars = strStartColor.toLowerCase().replaceAll("&", "").toCharArray();
                                                    MinecraftColors mcColor = MinecraftColors.fromCode(chars[0]);
                                                    startColor = new HSLColor(mcColor.getColor());
                                                } else {
                                                    ChatUtil.sendMessage("Invalid color!", ChatType.FAIL);
                                                    return -1;
                                                }

                                                if (strEndColor.startsWith("#")) {
                                                    endColor = new HSLColor(Color.decode(strEndColor.toLowerCase()));
                                                } else if (strEndColor.matches("^[0-9a-fA-F]*")) {
                                                    endColor = new HSLColor(Color.decode("#" + strEndColor.toLowerCase()));
                                                } else if (strEndColor.startsWith("&x")) {
                                                    endColor = new HSLColor(Color.decode(strEndColor.toLowerCase().replaceAll("&", "").replaceAll("x", "#")));
                                                } else if (strEndColor.matches("^&[0-9a-fA-F]")) {
                                                    char[] chars = strEndColor.toLowerCase().replaceAll("&", "").toCharArray();
                                                    MinecraftColors mcColor = MinecraftColors.fromCode(chars[0]);
                                                    endColor = new HSLColor(mcColor.getColor());
                                                } else {
                                                    ChatUtil.sendMessage("Invalid color!", ChatType.FAIL);
                                                    return -1;
                                                }
                                            } catch (Exception e) {
                                                ChatUtil.sendMessage("Invalid color!", ChatType.FAIL);
                                                return -1;
                                            }

                                            StringBuilder sb = new StringBuilder();
                                            char[] chars = text.toCharArray();
                                            float i = 0;
                                            String lastHex = "";
                                            int spaces = 0;

                                            LiteralText base = new LiteralText("§a→ §r");

                                            for (char c : chars) {
                                                if (c == ' ') {
                                                    spaces++;
                                                    base.getSiblings().add(new LiteralText(" "));
                                                    sb.append(c);
                                                    continue;
                                                }
                                                HSLColor temp = new HSLColor((float) lerp(startColor.getHue(), endColor.getHue(), i / (float) (text.length() - 1 - spaces)),
                                                        (float) lerp(startColor.getSaturation(), endColor.getSaturation(), i / (float) (text.length() - 1 - spaces)),
                                                        (float) lerp(startColor.getLuminance(), endColor.getLuminance(), i / (float) (text.length() - 1 - spaces)));
                                                Color temp2 = HSLColor.toRGB(temp.getHue(), temp.getSaturation(), temp.getLuminance());
                                                String hex = String.format("%02x%02x%02x", temp2.getRed(), temp2.getGreen(), temp2.getBlue());

                                                if (!lastHex.equals(hex)) {
                                                    lastHex = hex;
                                                    String dfHex = "&x&" + String.join("&", hex.split(""));
                                                    sb.append(dfHex);
                                                }

                                                Style colorStyle = Style.EMPTY.withColor(TextColor.fromRgb(temp2.getRGB()));
                                                String colorName = "#" + hex;
                                                LiteralText extra = new LiteralText(String.valueOf(c));
                                                LiteralText hover = new LiteralText(colorName);
                                                hover.append("\n§7Click to copy!");
                                                extra.setStyle(colorStyle);
                                                hover.setStyle(colorStyle);
                                                extra.styled((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/color hex " + colorName)));
                                                extra.styled((style) -> style.withHoverEvent(HoverEvent.Action.SHOW_TEXT.buildHoverEvent(hover)));
                                                base.getSiblings().add(extra);

                                                sb.append(c);
                                                i++;
                                            }
                                            MinecraftClient.getInstance().keyboard.setClipboard(sb.toString());
                                            ChatUtil.sendMessage("Copied text!", ChatType.SUCCESS);
                                            mc.player.sendMessage(base, false);

                                            if (PlayerState.getMode() == PlayerState.Mode.DEV) {
                                                mc.player.sendChatMessage("/txt " + sb);
                                            }

                                            return 1;
                                        })
                                )
                        )
                )
        );
    }

    @Override
    public String getDescription() {
        return "[blue]/gradient <start> <end> <text>[reset]\n"
                + "\n"
                + "Generates a text with a color gradient starting at the hex color 'start' and ending at 'end'.\n"
                + "\nThis command supports hex (ex. #87ceeb), Minecraft (ex. &c), and Minecraft hex (ex. &x&0&0&0&0&0&0) color codes.\n"
                + "\n[yellow]Example[reset]: /gradient #ff0000 #00ff00 Something";
    }

    @Override
    public String getName() {
        return "/gradient";
    }

    private double lerp(float x, float y, float p) {
        return x + (y - x) * p;
    }
}
