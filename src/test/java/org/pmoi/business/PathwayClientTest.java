package org.pmoi.business;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathwayClientTest {

    @Test
    void getPathway() {
        try {
            BufferedImage image = ImageIO.read(new URL("http://rest.kegg.jp/get/hsa04010/image"));
            ImageIO.write(image, "png", new File("kegg.png"));
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    void KEGGSearch() {
        PathwayClient pathwayClient = new PathwayClient();
        assertEquals(2, pathwayClient.KEGGSearch("351").size());
        assertEquals("hsa04726  Serotonergic synapse", pathwayClient.KEGGSearch("351").get(0));
        assertEquals("hsa05010  Alzheimer disease", pathwayClient.KEGGSearch("351").get(1));

        assertEquals(0, pathwayClient.KEGGSearch("2022").size());

        pathwayClient.KEGGSearch("351").forEach(System.out::println);
    }

    @Test
    void getPathwayGenes() {
        PathwayClient pathwayClient = new PathwayClient();
        assertEquals(295, pathwayClient.getPathwayGenes("hsa04010").size());
    }

    @Test
    void getIntercatorsFromPathway() {
        PathwayClient pathwayClient = new PathwayClient();
        var res = pathwayClient.getIntercatorsFromPathway("TGFBR3");
        res.forEach(System.out::println);
    }
}