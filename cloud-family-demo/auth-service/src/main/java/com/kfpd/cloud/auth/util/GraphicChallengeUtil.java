package com.kfpd.cloud.auth.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * @author july
 */
public final class GraphicChallengeUtil {

    private static final String PUZZLE_IMAGE_PATTERN = "classpath*:static/challenge-images/*";
    private static final int SLIDER_WIDTH = 320;
    private static final int SLIDER_HEIGHT = 160;
    private static final int SLIDER_PIECE_SIZE = 48;

    private static final List<String> READABLE_SHAPE_CLIP_PATHS = List.of(
            "50% 0%,75% 7%,93% 25%,100% 50%,93% 75%,75% 93%,50% 100%,25% 93%,7% 75%,0% 50%,7% 25%,25% 7%",
            "0% 0%,100% 0%,100% 100%,0% 100%",
            "50% 4%,96% 94%,4% 94%",
            "50% 0%,100% 50%,50% 100%,0% 50%",
            "50% 0%,98% 36%,80% 100%,20% 100%,2% 36%",
            "25% 6%,75% 6%,100% 50%,75% 94%,25% 94%,0% 50%",
            "30% 0%,70% 0%,100% 30%,100% 70%,70% 100%,30% 100%,0% 70%,0% 30%",
            "50% 0%,61% 35%,98% 35%,68% 57%,79% 91%,50% 70%,21% 91%,32% 57%,2% 35%,39% 35%",
            "36% 0%,64% 0%,64% 36%,100% 36%,100% 64%,64% 64%,64% 100%,36% 100%,36% 64%,0% 64%,0% 36%,36% 36%",
            "50% 0%,92% 18%,86% 66%,50% 100%,14% 66%,8% 18%",
            "18% 8%,82% 8%,100% 92%,0% 92%",
            "25% 0%,100% 0%,75% 100%,0% 100%",
            "50% 0%,84% 12%,100% 48%,78% 82%,42% 100%,10% 78%,0% 34%,24% 8%",
            "50% 3%,88% 18%,78% 55%,95% 86%,55% 78%,23% 96%,6% 58%,20% 18%",
            "44% 0%,74% 12%,95% 44%,76% 72%,40% 96%,8% 78%,16% 42%,4% 12%",
            "26% 10%,62% 0%,92% 24%,82% 62%,98% 92%,54% 86%,20% 100%,0% 54%",
            "52% 6%,94% 10%,82% 42%,100% 72%,65% 92%,30% 84%,0% 96%,12% 48%,2% 18%",
            "38% 2%,70% 16%,96% 8%,88% 48%,100% 86%,58% 96%,24% 78%,2% 88%,14% 42%,0% 12%",
            "48% 4%,90% 0%,78% 32%,96% 58%,68% 94%,34% 86%,8% 100%,0% 54%,18% 20%",
            "20% 4%,58% 12%,92% 2%,100% 40%,76% 64%,84% 100%,44% 88%,8% 96%,0% 42%",
            "42% 0%,84% 16%,100% 52%,70% 68%,76% 98%,36% 86%,2% 100%,12% 56%,0% 22%",
            "56% 0%,98% 20%,84% 46%,100% 82%,62% 88%,28% 100%,0% 70%,18% 38%,6% 8%",
            "32% 0%,66% 8%,88% 0%,100% 44%,82% 78%,46% 100%,14% 86%,0% 48%,12% 18%",
            "52% 0%,82% 24%,100% 18%,90% 64%,56% 78%,42% 100%,8% 82%,0% 38%,24% 28%",
            "40% 4%,80% 0%,96% 34%,82% 58%,100% 94%,54% 86%,22% 100%,0% 62%,8% 18%",
            "58% 2%,92% 12%,86% 46%,100% 74%,72% 100%,38% 82%,0% 94%,12% 48%,4% 18%",
            "44% 8%,76% 0%,100% 32%,88% 70%,60% 66%,44% 100%,16% 80%,0% 46%,18% 20%",
            "30% 2%,70% 8%,96% 24%,78% 50%,100% 78%,62% 94%,20% 100%,0% 66%,12% 30%",
            "50% 0%,86% 8%,78% 36%,100% 58%,84% 94%,48% 82%,12% 100%,0% 54%,20% 24%",
            "24% 0%,62% 10%,96% 0%,84% 36%,100% 70%,64% 76%,36% 100%,6% 86%,0% 38%"
    );

    private GraphicChallengeUtil() {
    }

    public static List<String> readableShapeTemplates() {
        return READABLE_SHAPE_CLIP_PATHS;
    }

    public static String jitterShapeClipPath(String template) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder points = new StringBuilder();
        String[] templatePoints = template.split(",");
        for (int i = 0; i < templatePoints.length; i++) {
            String[] xy = templatePoints[i].trim().split("% ");
            double x = Double.parseDouble(xy[0]) + random.nextDouble(-2.5d, 2.5d);
            double y = Double.parseDouble(xy[1].replace("%", "")) + random.nextDouble(-2.5d, 2.5d);
            if (i > 0) {
                points.append(",");
            }
            points.append(Math.round(clampPercent(x))).append("% ")
                    .append(Math.round(clampPercent(y))).append("%");
        }
        return points.toString();
    }

    public static String buildQuestionImage(List<? extends SvgShape> targets, String prompt, boolean ordered) {
        StringBuilder svg = new StringBuilder();
        String antiForgeryCode = randomAntiForgeryCode();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"240\" height=\"42\" viewBox=\"0 0 240 42\">");
        svg.append("<rect width=\"240\" height=\"42\" rx=\"8\" fill=\"#f7f9ff\"/>");
        appendQuestionImageNoise(svg, antiForgeryCode);
        svg.append("<text x=\"10\" y=\"25\" font-size=\"14\" font-weight=\"700\" fill=\"#245bff\">")
                .append(prompt)
                .append("</text>");
        int x = ordered ? 116 : 78;
        int index = 1;
        for (SvgShape target : targets) {
            if (ordered) {
                svg.append("<text x=\"").append(x - 15).append("\" y=\"25\" font-size=\"10\" font-weight=\"800\" fill=\"#245bff\">")
                        .append(index)
                        .append("</text>");
            }
            appendSvgShape(svg, target.shapeClipPath(), target.color(), target.fillMode(), x, 21, 9);
            x += 28;
            index++;
        }
        svg.append("<text x=\"194\" y=\"26\" font-size=\"11\" font-weight=\"700\" fill=\"#7f8da8\" opacity=\"0.62\" transform=\"rotate(-8 194 26)\">")
                .append(antiForgeryCode)
                .append("</text>");
        svg.append("</svg>");
        return svgDataUrl(svg.toString());
    }

    public static PuzzleChallenge buildPuzzleChallenge(List<String> orderedValues, String prompt) {
        String content = buildPuzzleContent();
        String questionImage = buildPuzzleQuestionImage(content, prompt);
        List<PuzzleTile> tiles = new ArrayList<>();
        tiles.add(new PuzzleTile(orderedValues.get(0), buildPuzzleTileImage(content, 0, 0)));
        tiles.add(new PuzzleTile(orderedValues.get(1), buildPuzzleTileImage(content, 60, 0)));
        tiles.add(new PuzzleTile(orderedValues.get(2), buildPuzzleTileImage(content, 0, 60)));
        tiles.add(new PuzzleTile(orderedValues.get(3), buildPuzzleTileImage(content, 60, 60)));
        Collections.shuffle(tiles);
        return new PuzzleChallenge(questionImage, tiles);
    }

    public static SliderChallenge buildSliderChallenge() {
        int width = SLIDER_WIDTH;
        int height = SLIDER_HEIGHT;
        int pieceSize = SLIDER_PIECE_SIZE;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int targetX = random.nextInt(100, width - pieceSize - 12);
        int pieceY = random.nextInt(16, height - pieceSize - 12);
        BufferedImage artwork = readSliderArtwork(width, height);
        Area shape = new Area(new Rectangle2D.Double(0, 8, 40, 40));
        shape.add(new Area(new Ellipse2D.Double(14, 0, 16, 16)));
        shape.add(new Area(new Ellipse2D.Double(32, 22, 16, 16)));
        shape.subtract(new Area(new Ellipse2D.Double(-8, 22, 16, 16)));
        shape.subtract(new Area(new Ellipse2D.Double(14, 40, 16, 16)));

        BufferedImage piece = new BufferedImage(pieceSize, pieceSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = piece.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setClip(shape);
            graphics.drawImage(artwork, -targetX, -pieceY, null);
            graphics.setClip(null);
            graphics.setColor(new Color(255, 255, 255, 220));
            graphics.setStroke(new BasicStroke(1.2f));
            graphics.draw(shape);
        } finally {
            graphics.dispose();
        }
        graphics = artwork.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.translate(targetX, pieceY);
            graphics.setColor(new Color(35, 43, 56));
            graphics.fill(shape);
            graphics.setColor(new Color(255, 255, 255, 210));
            graphics.setStroke(new BasicStroke(1.2f));
            graphics.draw(shape);
        } finally {
            graphics.dispose();
        }
        return new SliderChallenge(pngDataUrl(artwork), pngDataUrl(piece), width, height,
                pieceSize, pieceY, targetX);
    }

    public static boolean matchesSliderAnswer(int targetX, String answer) {
        if (answer == null || !answer.matches("\\d{1,3}")) {
            return false;
        }
        int offset = Integer.parseInt(answer);
        return offset >= 0 && offset <= SLIDER_WIDTH - SLIDER_PIECE_SIZE && Math.abs(offset - targetX) <= 5;
    }

    private static BufferedImage readSliderArtwork(int width, int height) {
        try {
            List<Resource> resources = new ArrayList<>(Arrays.asList(
                    new PathMatchingResourcePatternResolver().getResources(PUZZLE_IMAGE_PATTERN)));
            Collections.shuffle(resources);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || !filename.toLowerCase(Locale.ROOT).matches(".*\\.(png|jpe?g)")) {
                    continue;
                }
                try (InputStream input = resource.getInputStream()) {
                    BufferedImage source = ImageIO.read(input);
                    if (source == null) {
                        continue;
                    }
                    BufferedImage artwork = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = artwork.createGraphics();
                    try {
                        graphics.setColor(Color.WHITE);
                        graphics.fillRect(0, 0, width, height);
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        double scale = Math.max((double) width / source.getWidth(), (double) height / source.getHeight());
                        int scaledWidth = (int) Math.ceil(source.getWidth() * scale);
                        int scaledHeight = (int) Math.ceil(source.getHeight() * scale);
                        graphics.drawImage(source, (width - scaledWidth) / 2, (height - scaledHeight) / 2,
                                scaledWidth, scaledHeight, null);
                    } finally {
                        graphics.dispose();
                    }
                    return artwork;
                } catch (IOException ignored) {
                    // Try another image if this resource cannot be decoded.
                }
            }
        } catch (IOException ignored) {
            // Generate artwork when no raster resources are available.
        }
        BufferedImage artwork = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = artwork.createGraphics();
        try {
            graphics.setPaint(new GradientPaint(0, 0, new Color(191, 219, 254), width, height, new Color(240, 253, 244)));
            graphics.fillRect(0, 0, width, height);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < 24; i++) {
                graphics.setColor(Color.decode(randomColor(random)));
                graphics.fillOval(random.nextInt(width), random.nextInt(height), 20 + random.nextInt(60), 20 + random.nextInt(60));
            }
        } finally {
            graphics.dispose();
        }
        return artwork;
    }

    private static String pngDataUrl(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot encode slider challenge image", ex);
        }
    }

    private static String buildPuzzleQuestionImage(String content, String prompt) {
        StringBuilder svg = new StringBuilder();
        String antiForgeryCode = randomAntiForgeryCode();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"240\" height=\"98\" viewBox=\"0 0 240 98\">");
        svg.append("<rect width=\"240\" height=\"98\" rx=\"10\" fill=\"#f7f9ff\"/>");
        appendQuestionImageNoise(svg, antiForgeryCode);
        svg.append("<text x=\"10\" y=\"23\" font-size=\"14\" font-weight=\"700\" fill=\"#245bff\">")
                .append(prompt)
                .append("</text>");
        svg.append("<g transform=\"translate(82 8) scale(0.62)\">").append(content).append("</g>");
        svg.append("<text x=\"190\" y=\"24\" font-size=\"11\" font-weight=\"700\" fill=\"#7f8da8\" opacity=\"0.62\" transform=\"rotate(-8 190 24)\">")
                .append(antiForgeryCode)
                .append("</text>");
        svg.append("</svg>");
        return svgDataUrl(svg.toString());
    }

    private static String buildPuzzleTileImage(String content, int x, int y) {
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"72\" height=\"72\" viewBox=\"")
                .append(x).append(" ").append(y).append(" 60 60\">");
        svg.append(content);
        svg.append("</svg>");
        return svgDataUrl(svg.toString());
    }

    private static String buildPuzzleContent() {
        return readRandomPuzzleImage().orElseGet(GraphicChallengeUtil::buildGeneratedPuzzleContent);
    }

    private static java.util.Optional<String> readRandomPuzzleImage() {
        List<Resource> resources;
        try {
            resources = new ArrayList<>(Arrays.asList(
                    new PathMatchingResourcePatternResolver().getResources(PUZZLE_IMAGE_PATTERN)));
        } catch (IOException ignored) {
            return java.util.Optional.empty();
        }
        Collections.shuffle(resources);
        for (Resource image : resources) {
            String filename = image.getFilename();
            if (filename == null) {
                continue;
            }
            filename = filename.toLowerCase(Locale.ROOT);
            if (!filename.endsWith(".svg") && !filename.endsWith(".jpg")
                    && !filename.endsWith(".jpeg") && !filename.endsWith(".png")) {
                continue;
            }
            try (InputStream input = image.getInputStream()) {
                byte[] bytes = input.readAllBytes();
                if (filename.endsWith(".svg")) {
                    return java.util.Optional.of(new String(bytes, StandardCharsets.UTF_8));
                }
                String mimeType;
                if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                    mimeType = "image/jpeg";
                } else if (filename.endsWith(".png")) {
                    mimeType = "image/png";
                } else {
                    continue;
                }
                // Normalize raster artwork to the same canvas used by the four tile viewBoxes.
                return java.util.Optional.of("<image x=\"0\" y=\"0\" width=\"120\" height=\"120\" "
                        + "preserveAspectRatio=\"xMidYMid slice\" href=\"data:" + mimeType + ";base64,"
                        + Base64.getEncoder().encodeToString(bytes) + "\"/>");
            } catch (IOException ignored) {
                // Fall back to generated artwork when packaged resources are unavailable.
            }
        }
        return java.util.Optional.empty();
    }

    private static String buildGeneratedPuzzleContent() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String c1 = randomColor(random);
        String c2 = randomColor(random);
        String c3 = randomColor(random);
        String c4 = randomColor(random);
        StringBuilder svg = new StringBuilder();
        svg.append("<rect width=\"120\" height=\"120\" fill=\"#ffffff\"/>");
        svg.append("<rect x=\"0\" y=\"0\" width=\"60\" height=\"60\" fill=\"#eef4ff\"/>");
        svg.append("<rect x=\"60\" y=\"0\" width=\"60\" height=\"60\" fill=\"#fff7ed\"/>");
        svg.append("<rect x=\"0\" y=\"60\" width=\"60\" height=\"60\" fill=\"#f0fdf4\"/>");
        svg.append("<rect x=\"60\" y=\"60\" width=\"60\" height=\"60\" fill=\"#fdf2f8\"/>");
        svg.append("<path d=\"M8 72 C28 24,84 18,112 44\" fill=\"none\" stroke=\"").append(c1).append("\" stroke-width=\"12\" stroke-linecap=\"round\" opacity=\"0.9\"/>");
        svg.append("<path d=\"M22 12 L98 92\" fill=\"none\" stroke=\"").append(c2).append("\" stroke-width=\"10\" stroke-linecap=\"round\" opacity=\"0.82\"/>");
        svg.append("<circle cx=\"78\" cy=\"46\" r=\"22\" fill=\"").append(c3).append("\" opacity=\"0.88\"/>");
        svg.append("<polygon points=\"28,36 48,18 70,34 64,62 34,66\" fill=\"").append(c4).append("\" opacity=\"0.86\"/>");
        svg.append("<line x1=\"60\" y1=\"0\" x2=\"60\" y2=\"120\" stroke=\"#ffffff\" stroke-width=\"3\"/>");
        svg.append("<line x1=\"0\" y1=\"60\" x2=\"120\" y2=\"60\" stroke=\"#ffffff\" stroke-width=\"3\"/>");
        return svg.toString();
    }

    private static String randomColor(ThreadLocalRandom random) {
        List<String> colors = List.of("#2563eb", "#059669", "#ea580c", "#7c3aed", "#dc2626", "#0891b2");
        return colors.get(random.nextInt(colors.size()));
    }

    private static String svgDataUrl(String svg) {
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private static String randomAntiForgeryCode() {
        String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder code = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            code.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return code.toString();
    }

    private static void appendQuestionImageNoise(StringBuilder svg, String antiForgeryCode) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            svg.append("<line x1=\"").append(random.nextInt(4, 210))
                    .append("\" y1=\"").append(random.nextInt(5, 38))
                    .append("\" x2=\"").append(random.nextInt(4, 210))
                    .append("\" y2=\"").append(random.nextInt(5, 38))
                    .append("\" stroke=\"#9fb0cc\" stroke-width=\"")
                    .append(random.nextBoolean() ? "0.7" : "0.45")
                    .append("\" opacity=\"0.26\"/>");
        }
        for (int i = 0; i < 18; i++) {
            svg.append("<circle cx=\"").append(random.nextInt(4, 210))
                    .append("\" cy=\"").append(random.nextInt(4, 38))
                    .append("\" r=\"").append(random.nextBoolean() ? "0.9" : "0.55")
                    .append("\" fill=\"#8da0bf\" opacity=\"0.26\"/>");
        }
        svg.append("<text x=\"20\" y=\"37\" font-size=\"8\" font-weight=\"700\" fill=\"#9fb0cc\" opacity=\"0.18\" transform=\"rotate(-6 20 37)\">")
                .append(antiForgeryCode)
                .append("</text>");
    }

    private static void appendSvgShape(StringBuilder svg, String clipPath, String color, String fillMode, int cx, int cy, int size) {
        String safeColor = color == null ? "#245bff" : color;
        svg.append("<polygon points=\"")
                .append(scalePolygonPoints(clipPath, cx, cy, size))
                .append("\" fill=\"").append("outline".equals(fillMode) ? "none" : safeColor)
                .append("\" stroke=\"").append(safeColor)
                .append("\" stroke-width=\"").append("outline".equals(fillMode) ? "2.4" : "0")
                .append("\" stroke-linejoin=\"round\"/>");
    }

    private static String scalePolygonPoints(String points, int cx, int cy, int size) {
        return Arrays.stream(points.split(","))
                .map(point -> {
                    String[] xy = point.trim().split("% ");
                    double px = Double.parseDouble(xy[0]) / 100d;
                    double py = Double.parseDouble(xy[1].replace("%", "")) / 100d;
                    int x = (int) Math.round(cx + ((px - 0.5d) * size * 2));
                    int y = (int) Math.round(cy + ((py - 0.5d) * size * 2));
                    return x + "," + y;
                })
                .reduce((first, second) -> first + " " + second)
                .orElse("");
    }

    private static double clampPercent(double value) {
        return Math.max(2d, Math.min(98d, value));
    }

    public interface SvgShape {
        String shapeClipPath();

        String color();

        String fillMode();
    }

    public record PuzzleChallenge(String questionImage, List<PuzzleTile> tiles) {
    }

    public record PuzzleTile(String value, String image) {
    }

    public record SliderChallenge(String backgroundImage, String pieceImage, int width, int height,
                                  int pieceSize, int pieceY, int targetX) {
    }
}
