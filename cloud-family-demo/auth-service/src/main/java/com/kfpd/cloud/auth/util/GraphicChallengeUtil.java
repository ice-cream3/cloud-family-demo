package com.kfpd.cloud.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class GraphicChallengeUtil {

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
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.toString().getBytes(StandardCharsets.UTF_8));
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
}
