package dev.playmonkeei.svcpasswordhide;

/** One screen's display state. Never stores the password itself. */
public final class PasswordVisibility {
    private boolean visible;

    public boolean isVisible() {
        return visible;
    }

    public void toggle() {
        visible = !visible;
    }

    public String display(String value) {
        // Preserve UTF-16 offsets used by EditBox for selection and the cursor.
        return visible ? value : "*".repeat(value.length());
    }
}
