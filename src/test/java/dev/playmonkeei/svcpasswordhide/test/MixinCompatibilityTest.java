package dev.playmonkeei.svcpasswordhide.test;

import dev.playmonkeei.svcpasswordhide.PasswordField;
import dev.playmonkeei.svcpasswordhide.PasswordVisibility;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.MixinService;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MixinCompatibilityTest {
    @Test
    void appliesToRealMinecraftAndVoiceChatClasses() throws Exception {
        // Fabric supplies Mixin's global properties service on the test classpath.
        var properties = net.fabricmc.loader.impl.launch.FabricLauncherBase.class
                .getDeclaredMethod("setProperties", Map.class);
        properties.setAccessible(true);
        properties.invoke(null, new HashMap<String, Object>());
        MixinBootstrap.init();
        var advancePhase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
        advancePhase.setAccessible(true);
        advancePhase.invoke(null, MixinEnvironment.Phase.DEFAULT);
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("svc-password-hide.mixins.json");
        IMixinTransformer transformer = ((HeadlessMixinService) MixinService.getService()).transformer();

        ClassNode editBox = transform(transformer, "net.minecraft.client.gui.components.EditBox");
        assertTrue(editBox.interfaces.contains("dev/playmonkeei/svcpasswordhide/PasswordField"));
        // Ensure the actual render path and narration are patched, while the
        // network-facing value and ordinary edit operations remain untouched.
        for (String name : new String[]{"extractWidgetRenderState", "updateTextPosition", "scrollTo",
                "getScreenX", "findClickedPositionInText", "createNarrationMessage", "applyFormat"}) {
            MethodNode method = editBox.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            assertTrue(callsAddon(method), "Missing injection into " + name);
        }
        for (String name : new String[]{"getValue", "setValue", "insertText", "keyPressed", "getHighlighted"}) {
            MethodNode method = editBox.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            assertFalse(callsAddon(method), "Password editing must remain unchanged: " + name);
        }
        exerciseTransformedField(editBox);

        for (String screen : new String[]{"CreateGroupScreen", "EnterPasswordScreen"}) {
            ClassNode node = transform(transformer, "de.maxhenkel.voicechat.gui." + screen);
            MethodNode init = node.methods.stream().filter(m -> m.name.equals("init") && m.desc.equals("()V"))
                    .findFirst().orElseThrow();
            assertTrue(callsAddon(init), "Missing password setup in " + screen + ": "
                    + Arrays.stream(init.instructions.toArray())
                    .filter(i -> i instanceof MethodInsnNode)
                    .map(i -> ((MethodInsnNode) i).name).toList());
        }
    }

    private static void exerciseTransformedField(ClassNode node) throws Exception {
        var writer = new org.objectweb.asm.ClassWriter(0);
        node.accept(writer);
        Class<?> type = new ClassLoader(MixinCompatibilityTest.class.getClassLoader()) {
            Class<?> define() {
                byte[] bytes = writer.toByteArray();
                return defineClass(node.name.replace('/', '.'), bytes, 0, bytes.length);
            }
        }.define();
        // Use a headless font with deliberately different glyph widths. This
        // checks that cursor/selection layout measures stars, not the secret.
        Font font = new Font(null) {
            @Override public int width(String text) {
                return text.chars().map(c -> c == 'i' ? 2 : c == '*' ? 6 : 8).sum();
            }
            @Override public String plainSubstrByWidth(String text, int width) {
                int end = 0;
                while (end < text.length() && width(text.substring(0, end + 1)) <= width) end++;
                return text.substring(0, end);
            }
            @Override public String plainSubstrByWidth(String text, int width, boolean reverse) {
                if (!reverse) return plainSubstrByWidth(text, width);
                int start = text.length();
                while (start > 0 && width(text.substring(start - 1)) <= width) start--;
                return text.substring(start);
            }
        };
        Object field = type.getConstructor(Font.class, int.class, int.class, int.class, int.class, Component.class)
                .newInstance(font, 0, 0, 50, 14, Component.empty());
        type.getMethod("setValue", String.class).invoke(field, "iiWW42");
        PasswordVisibility visibility = new PasswordVisibility();
        ((PasswordField) field).svcpasswordhide$protect(visibility);
        assertEquals("iiWW42", type.getMethod("getValue").invoke(field));
        assertEquals(12, type.getMethod("getScreenX", int.class).invoke(field, 2));

        var format = type.getDeclaredMethod("applyFormat", String.class, int.class);
        format.setAccessible(true);
        FormattedCharSequence masked = (FormattedCharSequence) format.invoke(field, "iiWW42", 0);
        StringBuilder rendered = new StringBuilder();
        masked.accept((index, style, codepoint) -> { rendered.appendCodePoint(codepoint); return true; });
        assertEquals("******", rendered.toString());
        var narration = type.getDeclaredMethod("createNarrationMessage");
        narration.setAccessible(true);
        assertFalse(((Component) narration.invoke(field)).getString().contains("iiWW42"));

        visibility.toggle();
        ((PasswordField) field).svcpasswordhide$refreshLayout();
        assertEquals(4, type.getMethod("getScreenX", int.class).invoke(field, 2));
        assertEquals("iiWW42", type.getMethod("getValue").invoke(field));
        type.getMethod("setHighlightPos", int.class).invoke(field, 0);
        assertEquals("iiWW42", type.getMethod("getHighlighted").invoke(field));
        visibility.toggle();
        ((PasswordField) field).svcpasswordhide$refreshLayout();
        assertEquals("iiWW42", type.getMethod("getHighlighted").invoke(field));
    }

    private static boolean callsAddon(MethodNode method) {
        return Arrays.stream(method.instructions.toArray()).anyMatch(i ->
                i instanceof MethodInsnNode call && call.name.contains("svcpasswordhide$"));
    }

    private static ClassNode transform(IMixinTransformer transformer, String name) throws Exception {
        try (InputStream input = MixinCompatibilityTest.class.getClassLoader()
                .getResourceAsStream(name.replace('.', '/') + ".class")) {
            assertNotNull(input, "Missing real dependency " + name);
            byte[] original = input.readAllBytes();
            byte[] transformed = transformer.transformClassBytes(name, name, original);
            assertFalse(Arrays.equals(original, transformed), "Mixin was not applied to " + name);
            ClassNode node = new ClassNode();
            new ClassReader(transformed).accept(node, 0);
            return node;
        }
    }
}
