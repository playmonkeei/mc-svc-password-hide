package dev.playmonkeei.svcpasswordhide.mixin;

import dev.playmonkeei.svcpasswordhide.PasswordField;
import dev.playmonkeei.svcpasswordhide.PasswordVisibility;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EditBox.class, remap = false)
public abstract class EditBoxMixin implements PasswordField {
    @Unique private PasswordVisibility svcpasswordhide$visibility;
    @Shadow private String value;
    @Shadow private String suggestion;
    @Shadow private IMEPreeditOverlay preeditOverlay;
    @Shadow private int cursorPos;
    @Shadow protected abstract void updateTextPosition();
    @Shadow protected abstract void scrollTo(int position);

    @Override
    public void svcpasswordhide$protect(PasswordVisibility visibility) {
        svcpasswordhide$visibility = visibility;
        svcpasswordhide$refreshLayout();
    }

    @Override
    public void svcpasswordhide$refreshLayout() {
        scrollTo(cursorPos);
        updateTextPosition();
    }

    @Unique
    private boolean svcpasswordhide$isHidden() {
        return svcpasswordhide$visibility != null && !svcpasswordhide$visibility.isVisible();
    }

    // Substitute only display/layout reads. getValue(), editing, clipboard and
    // network packets continue to use the original password without modification.
    @Redirect(method = {"extractWidgetRenderState", "updateTextPosition", "scrollTo",
            "getScreenX", "findClickedPositionInText"}, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/gui/components/EditBox;value:Ljava/lang/String;",
            opcode = Opcodes.GETFIELD))
    private String svcpasswordhide$displayValue(EditBox field) {
        return svcpasswordhide$visibility == null ? value : svcpasswordhide$visibility.display(value);
    }

    @Inject(method = "applyFormat", at = @At("HEAD"), cancellable = true)
    private void svcpasswordhide$maskFormattedText(String text, int offset,
            CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (svcpasswordhide$isHidden()) {
            cir.setReturnValue(FormattedCharSequence.forward(
                    svcpasswordhide$visibility.display(text), Style.EMPTY));
        }
    }

    @Inject(method = "createNarrationMessage", at = @At("HEAD"), cancellable = true)
    private void svcpasswordhide$maskNarration(CallbackInfoReturnable<MutableComponent> cir) {
        if (svcpasswordhide$isHidden()) {
            cir.setReturnValue(Component.translatable("svcpasswordhide.password_hidden"));
        }
    }

    @Redirect(method = "extractWidgetRenderState", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/gui/components/EditBox;suggestion:Ljava/lang/String;",
            opcode = Opcodes.GETFIELD))
    private String svcpasswordhide$hideSuggestion(EditBox field) {
        return svcpasswordhide$isHidden() ? null : suggestion;
    }

    @Redirect(method = "extractWidgetRenderState", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/gui/components/EditBox;preeditOverlay:Lnet/minecraft/client/gui/components/IMEPreeditOverlay;",
            opcode = Opcodes.GETFIELD))
    private IMEPreeditOverlay svcpasswordhide$hideComposition(EditBox field) {
        return svcpasswordhide$isHidden() ? null : preeditOverlay;
    }
}
