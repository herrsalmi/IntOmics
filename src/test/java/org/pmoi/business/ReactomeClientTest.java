package org.pmoi.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactomeClientTest {

    private ReactomeClient reactomeClient;

    @BeforeEach
    void setUp() {
        reactomeClient = new ReactomeClient();
    }

    @Test
    void search() {
        var result = reactomeClient.search("PPARG");
        System.out.println(result);
        assertEquals(5, result.size());
        result = reactomeClient.search("PPARGX");
        assertTrue(result.isEmpty());
        result = reactomeClient.search("LEP");
        System.out.println(result);
    }

    @Test
    void getEntityName() {
        var result = reactomeClient.getEntityName("R-HSA-381340");
        assertEquals("Transcriptional regulation of white adipocyte differentiation", result.get());
        result = reactomeClient.getEntityName("R-HSA-3813400");
        assertTrue(result.isEmpty());
        result = reactomeClient.getEntityName("R-HSA-2586552");
        result.ifPresent(System.out::println);
    }

    @Test
    void getInteractors() {
        // [R-HSA-2151209, R-HSA-381340, R-HSA-9022707, R-HSA-8943724, R-HSA-6807070]
        var result = reactomeClient.getInteractors("R-HSA-381340");
        System.out.println(result);
        System.out.println("------------------");
        result = reactomeClient.getInteractors("R-HSA-2151209");
        System.out.println(result);
        System.out.println("------------------");
        result = reactomeClient.getInteractors("R-HSA-8943724");
        System.out.println(result);
        System.out.println("------------------");
        result = reactomeClient.getInteractors("R-HSA-9022707");
        System.out.println(result);
        System.out.println("------------------");
        result = reactomeClient.getInteractors("R-HSA-6807070");
        System.out.println(result);
        System.out.println("------------------");
        result = reactomeClient.getInteractors("R-HSA-2586552");
        System.out.println(result);
    }
}