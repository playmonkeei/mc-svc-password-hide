package dev.playmonkeei.svcpasswordhide;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** A dark, framed button keeps the font-independent eye clear on light GUIs. */
public final class VisibilityButton extends Button {
    public static final int SIZE = 14;
    private final PasswordVisibility visibility;

    public VisibilityButton(int x, int y, EditBox field, PasswordVisibility visibility) {
        super(x, y, SIZE, SIZE, label(visibility), button -> {
            visibility.toggle();
            ((PasswordField) field).svcpasswordhide$refreshLayout();
            ((VisibilityButton) button).refreshLabel();
        }, DEFAULT_NARRATION);
        this.visibility = visibility;
        refreshLabel();
    }

    private static Component label(PasswordVisibility visibility) {
        return Component.translatable(visibility.isVisible()
                ? "svcpasswordhide.hide_password" : "svcpasswordhide.show_password");
    }

    private void refreshLabel() {
        setMessage(label(visibility));
        setTooltip(Tooltip.create(getMessage()));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean highlighted = active && isHoveredOrFocused();
        int border = highlighted ? 0xFFFFFFFF : 0xFF808080;
        int background = highlighted ? 0xFF383838 : 0xFF202020;
        graphics.fill(getX(), getY(), getRight(), getBottom(), border);
        graphics.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, background);

        int x = getX() + 2;
        int y = getY() + 4;
        int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
        graphics.fill(x + 3, y, x + 7, y + 1, color);
        graphics.fill(x + 1, y + 1, x + 3, y + 2, color);
        graphics.fill(x + 7, y + 1, x + 9, y + 2, color);
        graphics.fill(x, y + 2, x + 1, y + 4, color);
        graphics.fill(x + 9, y + 2, x + 10, y + 4, color);
        graphics.fill(x + 1, y + 4, x + 3, y + 5, color);
        graphics.fill(x + 7, y + 4, x + 9, y + 5, color);
        graphics.fill(x + 3, y + 5, x + 7, y + 6, color);
        graphics.fill(x + 4, y + 2, x + 6, y + 4, color);
        if (!visibility.isVisible()) {
            for (int i = 0; i < 8; i++) {
                graphics.fill(x + i + 1, y + 6 - i, x + i + 2, y + 7 - i, color);
            }
        }
    }
}
