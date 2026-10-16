package dev.playmonkeei.svcpasswordhide.test;

import dev.playmonkeei.svcpasswordhide.PasswordVisibility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordVisibilityTest {
    @Test
    void hidesImmediatelyAndRevealsOnlyAfterExplicitToggle() {
        PasswordVisibility state = new PasswordVisibility();
        assertFalse(state.isVisible());
        assertEquals("********", state.display("secret42"));
        state.toggle();
        assertTrue(state.isVisible());
        assertEquals("secret42", state.display("secret42"));
        state.toggle();
        assertEquals("********", state.display("secret42"));
    }

    @Test
    void freshScreenDoesNotInheritRevealedState() {
        PasswordVisibility previous = new PasswordVisibility();
        previous.toggle();
        assertEquals("******", new PasswordVisibility().display("secret"));
    }

    @Test
    void keepsEmptyFieldsAndCursorOffsetsValid() {
        PasswordVisibility state = new PasswordVisibility();
        assertEquals("", state.display(""));
        for (String password : new String[]{"iW iW", "üß😀", "a".repeat(32)}) {
            String masked = state.display(password);
            assertEquals(password.length(), masked.length());
            assertTrue(masked.chars().allMatch(c -> c == '*'));
        }
    }
}
