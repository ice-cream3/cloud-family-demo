package com.kfpd.cloud.common.showcase;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.common.exception.ErrorCode;

/** Shared validation; owner and moderation fields never come from a seller payload. */
public final class CommerceRules {
    private CommerceRules() { }
    public static Map<String, Object> shop(Map<String, Object> input) {
        require(input != null, "请填写店铺资料");
        Map<String, Object> result = new HashMap<>();
        result.put("name", text(input, "name", 100, true));
        result.put("description", text(input, "description", 2000, true));
        result.put("contact", text(input, "contact", 100, false));
        result.put("coverUrl", image(input, "coverUrl"));
        return result;
    }
    public static Map<String, Object> product(Map<String, Object> input) {
        require(input != null, "请填写商品资料");
        Map<String, Object> result = new HashMap<>();
        result.put("title", text(input, "title", 100, true));
        result.put("category", text(input, "category", 32, true));
        result.put("summary", text(input, "summary", 300, true));
        result.put("content", text(input, "content", 100000, true));
        result.put("coverUrl", image(input, "coverUrl"));
        result.put("status", state(input.get("status"), List.of("DRAFT", "PUBLISHED", "OFFLINE"), "DRAFT"));
        result.put("stock", integer(input.get("stock"), 0, Integer.MAX_VALUE));
        result.put("sortOrder", integer(input.get("sortOrder"), 0, Integer.MAX_VALUE));
        try {
            BigDecimal price = new BigDecimal(String.valueOf(input.get("price")));
            require(price.signum() >= 0 && price.compareTo(new BigDecimal("9999999999.99")) <= 0 && price.stripTrailingZeros().scale() <= 2,
                    "价格需为非负金额且最多两位小数");
            result.put("price", price.setScale(2));
        } catch (NumberFormatException ex) { throw bad("请输入有效价格"); }
        return result;
    }
    public static String typeCode(Object raw) {
        String code=raw instanceof String value?value.trim().toUpperCase(java.util.Locale.ROOT):"";
        require(code.matches("[A-Z][A-Z0-9_]{0,63}"),"类型编码必填，需以字母开头，仅包含字母、数字和下划线，最多64位");
        return code;
    }
    public static String state(Object raw, List<String> allowed, String defaultValue) {
        String value = raw == null ? defaultValue : String.valueOf(raw);
        require(allowed.contains(value), "状态无效"); return value;
    }
    public static int integer(Object raw, int min, int max) {
        try {
            int value = raw == null ? 0 : new BigDecimal(String.valueOf(raw)).intValueExact();
            require(value >= min && value <= max, "数量和排序需为非负整数"); return value;
        } catch (NumberFormatException | ArithmeticException ex) { throw bad("请输入有效整数"); }
    }
    public static long id(Object raw) {
        try { long id = new BigDecimal(String.valueOf(raw)).longValueExact(); require(id > 0, "商家ID无效"); return id; }
        catch (NumberFormatException | ArithmeticException ex) { throw bad("商家ID无效"); }
    }
    public static String text(Map<String, Object> input, String key, int limit, boolean required) {
        Object raw = input.get(key);
        String label = Map.of("name","店铺名称","description","店铺介绍","contact","公开联系方式","coverUrl","封面地址",
                "title","商品名称","category","商品分类","summary","商品简介","content","商品详情","ownerUsername","所属用户账号","moderationReason","系统停用原因").getOrDefault(key,key);
        require(raw == null || raw instanceof String, label + "格式不正确");
        String value = raw == null ? "" : ((String) raw).trim();
        require((!required || !value.isEmpty()) && value.length() <= limit, "请填写" + label + "，最多" + limit + "字");
        return value;
    }
    private static String image(Map<String, Object> input, String key) {
        String value = text(input, key, 1024, false);
        if (value.isEmpty()) return null;
        try {
            URI uri = URI.create(value);
            require((value.startsWith("/") && !value.startsWith("//") && uri.getHost() == null)
                || (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null),
                "封面需填写 http(s) 图片地址或站内路径");
            return value;
        } catch (IllegalArgumentException ex) { throw bad("封面地址无效"); }
    }
    public static void require(boolean condition, String message) { if (!condition) throw bad(message); }
    public static BusinessException bad(String message) { return new BusinessException(ErrorCode.COMMON_BAD_REQUEST, message); }
}
