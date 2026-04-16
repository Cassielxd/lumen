package com.lumencloud.lumen.auth.endpoint;

import cn.hutool.core.lang.Validator;
import com.lumencloud.lumen.common.core.constant.CacheConstants;
import com.lumencloud.lumen.common.core.constant.SecurityConstants;
import com.lumencloud.lumen.common.core.util.RedisUtils;
import com.lumencloud.lumen.common.security.annotation.Inner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Verification code endpoints.
 */
@RestController
@Inner(false)
@RequestMapping("/code")
@Tag(name = "code", description = "Verification code endpoints")
public class ImageCodeEndpoint {

    private static final int DEFAULT_IMAGE_WIDTH = 100;

    private static final int DEFAULT_IMAGE_HEIGHT = 40;

    private static final Font CAPTCHA_FONT = new Font("SansSerif", Font.BOLD, 24);

    @SneakyThrows
    @GetMapping("/image")
    @Operation(summary = "Generate an arithmetic captcha image")
    public void image(String randomStr, HttpServletResponse response) {
        if (Validator.isMobile(randomStr)) {
            return;
        }

        ArithmeticChallenge challenge = ArithmeticChallenge.create();
        RedisUtils.set(CacheConstants.DEFAULT_CODE_KEY + randomStr, challenge.answer(), SecurityConstants.CODE_TIME,
                TimeUnit.SECONDS);

        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setHeader("Pragma", "no-cache");
        ImageIO.write(renderCaptcha(challenge.expression()), "png", response.getOutputStream());
    }

    private BufferedImage renderCaptcha(String expression) {
        BufferedImage image = new BufferedImage(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(245, 247, 250));
            graphics.fillRect(0, 0, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);

            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < 6; i++) {
                graphics.setColor(new Color(random.nextInt(180, 230), random.nextInt(180, 230),
                        random.nextInt(180, 230)));
                int x1 = random.nextInt(DEFAULT_IMAGE_WIDTH);
                int y1 = random.nextInt(DEFAULT_IMAGE_HEIGHT);
                int x2 = random.nextInt(DEFAULT_IMAGE_WIDTH);
                int y2 = random.nextInt(DEFAULT_IMAGE_HEIGHT);
                graphics.drawLine(x1, y1, x2, y2);
            }

            graphics.setFont(CAPTCHA_FONT);
            graphics.setColor(new Color(31, 41, 55));
            FontMetrics metrics = graphics.getFontMetrics();
            int textWidth = metrics.stringWidth(expression);
            int x = (DEFAULT_IMAGE_WIDTH - textWidth) / 2;
            int y = (DEFAULT_IMAGE_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
            graphics.drawString(expression, x, y);
        }
        finally {
            graphics.dispose();
        }
        return image;
    }

    private record ArithmeticChallenge(String expression, String answer) {

        private static ArithmeticChallenge create() {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int left = random.nextInt(1, 10);
            int right = random.nextInt(1, 10);
            boolean addition = random.nextBoolean();
            if (!addition && left < right) {
                int temp = left;
                left = right;
                right = temp;
            }
            int result = addition ? left + right : left - right;
            String operator = addition ? "+" : "-";
            return new ArithmeticChallenge(left + " " + operator + " " + right + " = ?", String.valueOf(result));
        }

    }

}
