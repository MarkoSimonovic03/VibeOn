package com.metropolitan.VibeOn.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageServiceTest {

    private final ImageStorageService imageStorageService = new ImageStorageService();

    @Test
    void rejectsEmptyUnsupportedAndFakeImageFiles() {
        assertThatThrownBy(() -> imageStorageService.save(
                new MockMultipartFile("image", "empty.png", "image/png", new byte[0])
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> imageStorageService.save(
                new MockMultipartFile("image", "notes.txt", "text/plain", "not an image".getBytes())
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> imageStorageService.save(
                new MockMultipartFile("image", "fake.png", "image/png", "not an image".getBytes())
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsARealSupportedImageAndUsesASafeGeneratedName() throws Exception {
        Path imageDirectory = Path.of("images");
        boolean directoryAlreadyExisted = Files.exists(imageDirectory);
        String storedName = null;

        try {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);

            storedName = imageStorageService.save(
                    new MockMultipartFile("image", "../../unsafe-name.png", "image/png", bytes.toByteArray())
            );

            assertThat(storedName).matches("[0-9a-f-]{36}\\.png");
            assertThat(Files.exists(imageDirectory.resolve(storedName))).isTrue();
        } finally {
            if (storedName != null) {
                Files.deleteIfExists(imageDirectory.resolve(storedName));
            }
            if (!directoryAlreadyExisted) {
                Files.deleteIfExists(imageDirectory);
            }
        }
    }
}
