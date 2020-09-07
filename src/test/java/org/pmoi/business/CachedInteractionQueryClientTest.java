package org.pmoi.business;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CachedInteractionQueryClientTest {

    CachedInteractionQueryClient instance;

    @BeforeAll
    void init() {
        instance = CachedInteractionQueryClient.getInstance();
    }

    @Test
    void createCache() {
        instance.createCache();
    }
}