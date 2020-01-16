package org.pmoi.business;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

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
}