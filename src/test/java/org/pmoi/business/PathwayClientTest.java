package org.pmoi.business;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class PathwayClientTest {

    @Test
    void getPathway() {
        PathwayClient pathwayClient = new PathwayClient();
        byte[] imageBytes = pathwayClient.getPathway("LEP", "IRS2");
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("pathway.svg"))) {
                outputStream.write(imageBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
    }
}