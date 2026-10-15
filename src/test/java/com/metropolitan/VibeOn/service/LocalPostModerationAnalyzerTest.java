package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.entity.AiModerationCategory;
import com.metropolitan.VibeOn.service.impl.LocalPostModerationAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPostModerationAnalyzerTest {
    private final LocalPostModerationAnalyzer analyzer = new LocalPostModerationAnalyzer(0.80);

    @Test
    void detectsSuspiciousTextAndReturnsCategoryConfidence() throws Exception {
        PostModerationAnalysis result = analyzer.analyze(
                "Selling cocaine and heroin with a gun",
                List.of(image(Color.WHITE))
        );

        assertThat(result.suspicious()).isTrue();
        assertThat(result.textConfidence()).isGreaterThanOrEqualTo(0.90);
        assertThat(result.confidence(AiModerationCategory.DRUGS)).isGreaterThanOrEqualTo(0.90);
        assertThat(result.confidence(AiModerationCategory.WEAPONS)).isGreaterThanOrEqualTo(0.90);
    }

    @Test
    void analyzesImagePixelsForViolenceBloodSignals() throws Exception {
        PostModerationAnalysis result = analyzer.analyze("A photo", List.of(image(new Color(210, 10, 10))));

        assertThat(result.suspicious()).isTrue();
        assertThat(result.confidence(AiModerationCategory.VIOLENCE_BLOOD)).isGreaterThanOrEqualTo(0.90);
        assertThat(result.categoryConfidences()).containsKeys(
                AiModerationCategory.NSFW,
                AiModerationCategory.VIOLENCE_BLOOD,
                AiModerationCategory.WEAPONS,
                AiModerationCategory.DRUGS
        );
    }

    @Test
    void cleanTextAndNeutralImageAreNotFlagged() throws Exception {
        PostModerationAnalysis result = analyzer.analyze("A peaceful day at the park", List.of(image(Color.WHITE)));
        assertThat(result.suspicious()).isFalse();
    }

    @Test
    void detectsRepeatedMentionsAndTargetedHarassmentWithoutFlaggingNormalMentions() throws Exception {
        PostModerationAnalysis repeated = analyzer.analyze(
                "Hey @person @person @person", List.of()
        );
        assertThat(repeated.suspicious()).isTrue();
        assertThat(repeated.confidence(AiModerationCategory.MENTION_ABUSE)).isEqualTo(0.90);

        PostModerationAnalysis targetedHarassment = analyzer.analyze(
                "@person kill yourself", List.of()
        );
        assertThat(targetedHarassment.confidence(AiModerationCategory.MENTION_ABUSE)).isEqualTo(0.95);

        PostModerationAnalysis normal = analyzer.analyze(
                "Thanks for the help @friend", List.of()
        );
        assertThat(normal.suspicious()).isFalse();
        assertThat(normal.confidence(AiModerationCategory.MENTION_ABUSE)).isZero();
    }

    private MockMultipartFile image(Color color) throws Exception {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("images", "test.png", "image/png", output.toByteArray());
    }
}
