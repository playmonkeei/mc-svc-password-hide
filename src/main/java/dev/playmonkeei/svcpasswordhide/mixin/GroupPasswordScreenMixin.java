package dev.playmonkeei.svcpasswordhide.mixin;

import dev.playmonkeei.svcpasswordhide.PasswordField;
import dev.playmonkeei.svcpasswordhide.PasswordVisibility;
import dev.playmonkeei.svcpasswordhide.VisibilityButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "de.maxhenkel.voicechat.gui.CreateGroupScreen",
        "de.maxhenkel.voicechat.gui.EnterPasswordScreen"
}, remap = false)
public abstract class GroupPasswordScreenMixin extends Screen {
    @Shadow(remap = false) private EditBox password;

    protected GroupPasswordScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init()V", at = @At("TAIL"), remap = false)
    private void svcpasswordhide$addVisibilityButton(CallbackInfo ci) {
        // init also runs when resizing: always start hidden, before the first frame.
        PasswordVisibility visibility = new PasswordVisibility();
        int originalRight = password.getRight();
        password.setWidth(password.getWidth() - VisibilityButton.SIZE - 3);
        ((PasswordField) password).svcpasswordhide$protect(visibility);
        password.setMessage(Component.translatable("svcpasswordhide.password"));
        addRenderableWidget(new VisibilityButton(originalRight - VisibilityButton.SIZE,
                password.getY(), password, visibility));
    }
}
