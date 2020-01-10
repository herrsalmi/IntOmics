package org.pmoi.business;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretomeManagerTest {

    @Test
    void isSecreted() {
        SecretomeManager secretomeDB = SecretomeManager.getInstance();
        assertTrue(secretomeDB.isSecreted("TIMP1"));
        assertFalse(secretomeDB.isSecreted("ALBU"));
        assertFalse(secretomeDB.isSecreted("wwox"));
    }
}