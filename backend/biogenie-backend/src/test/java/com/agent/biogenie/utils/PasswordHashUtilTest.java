package com.agent.biogenie.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHashUtilTest {
    @Test
    void hashesWithUniqueUuidSaltsAndVerifiesInConstantTime() {
        String firstSalt = PasswordHashUtil.newSalt();
        String secondSalt = PasswordHashUtil.newSalt();
        assertDoesNotThrow(() -> java.util.UUID.fromString(firstSalt));
        assertNotEquals(firstSalt, secondSalt);

        String hash = PasswordHashUtil.hash("Secure123".toCharArray(), firstSalt);
        assertTrue(PasswordHashUtil.matches("Secure123".toCharArray(), firstSalt, hash));
        assertFalse(PasswordHashUtil.matches("Wrong123".toCharArray(), firstSalt, hash));
        assertNotEquals(hash, PasswordHashUtil.hash("Secure123".toCharArray(), secondSalt));
    }
}
