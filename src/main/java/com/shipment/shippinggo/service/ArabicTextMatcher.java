package com.shipment.shippinggo.service;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * أداة مطابقة النصوص العربية الذكية — تعالج الاختلافات الإملائية الشائعة
 * في أسماء المناطق والعناوين المصرية.
 *
 * تدعم:
 * - توحيد الحروف العربية (ة↔ه، ى↔ي، أ/إ/آ→ا)
 * - إزالة التشكيل (حركات)
 * - إزالة أل التعريف
 * - تطابق ضبابي (Fuzzy) للأخطاء الإملائية باستخدام JaroWinkler
 *
 * مثال:
 *   "العجيزي" vs "عجيري" → normalized: "عجيزي" vs "عجيري" → JaroWinkler: 0.89 ✓
 *   "قحافة" vs "قحافه" → normalized: "قحافه" vs "قحافه" → 100% match ✓
 */
@Component
public class ArabicTextMatcher {

    private static final Logger log = LoggerFactory.getLogger(ArabicTextMatcher.class);

    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

    /**
     * حد أدنى للتشابه — 0.85 يعني 85% تشابه على الأقل.
     * قيمة محافظة تمنع التطابقات الخاطئة وتسمح بالأخطاء الإملائية البسيطة.
     */
    private static final double SIMILARITY_THRESHOLD = 0.85;

    /**
     * حد أدنى لطول الكلمة المبحوث عنها (بعد التنظيف) — لتجنب التطابقات العشوائية.
     */
    private static final int MIN_ZONE_NAME_LENGTH = 3;

    // ==================== Arabic Diacritics (تشكيل) ====================
    // Unicode range: 0x0610-0x061A, 0x064B-0x065F, 0x0670
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile(
            "[\\u0610-\\u061A\\u064B-\\u065F\\u0670]"
    );

    // ==================== أل التعريف ====================
    private static final Pattern DEFINITE_ARTICLE_PATTERN = Pattern.compile(
            "^(ال)"
    );

    // ==================== Normalization ====================

    /**
     * تنظيف وتوحيد النص العربي:
     * 1. إزالة التشكيل
     * 2. توحيد أشكال الألف (أ/إ/آ → ا)
     * 3. توحيد التاء المربوطة والهاء (ة → ه)
     * 4. توحيد الألف المقصورة والياء (ى → ي)
     * 5. إزالة أل التعريف
     * 6. إزالة المسافات الزائدة
     */
    public String normalize(String text) {
        if (text == null || text.trim().isEmpty()) return "";

        String result = text.trim();

        // 1. إزالة التشكيل
        result = DIACRITICS_PATTERN.matcher(result).replaceAll("");

        // 2. توحيد أشكال الألف
        result = result.replace('أ', 'ا')
                       .replace('إ', 'ا')
                       .replace('آ', 'ا');

        // 3. توحيد التاء المربوطة → هاء
        result = result.replace('ة', 'ه');

        // 4. توحيد الألف المقصورة → ياء
        result = result.replace('ى', 'ي');

        // 5. إزالة المسافات الزائدة
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    /**
     * تنظيف + إزالة أل التعريف من كلمة واحدة.
     * يُستخدم لمقارنة الكلمات المفردة (أسماء المناطق).
     */
    public String normalizeWord(String word) {
        String normalized = normalize(word);
        // إزالة أل التعريف من كل كلمة
        String[] parts = normalized.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(DEFINITE_ARTICLE_PATTERN.matcher(part).replaceFirst(""));
        }
        return sb.toString();
    }

    // ==================== Matching ====================

    /**
     * هل اسم المنطقة (zoneName) موجود في العنوان (address)؟
     *
     * الخطوات:
     * 1. تنظيف النصين
     * 2. بحث exact بعد التنظيف (يحل 70% من المشاكل)
     * 3. لو مفيش exact match → بحث fuzzy كلمة بكلمة (يحل 20% إضافية)
     *
     * @param address العنوان النصي الكامل
     * @param zoneName اسم المنطقة/المركز المربوط للمندوب
     * @return نتيجة المطابقة (score + matched)
     */
    public MatchResult matchZoneInAddress(String address, String zoneName) {
        if (address == null || zoneName == null) return MatchResult.NO_MATCH;

        String normalizedAddress = normalize(address);
        String normalizedZone = normalizeWord(zoneName);

        if (normalizedZone.length() < MIN_ZONE_NAME_LENGTH) {
            return MatchResult.NO_MATCH;
        }

        // خطوة 1: بحث exact بعد التنظيف
        // أيضاً نجرب بدون أل التعريف في العنوان
        String addressWithoutAl = normalizeWord(address);

        if (normalizedAddress.contains(normalizedZone) || addressWithoutAl.contains(normalizedZone)) {
            return new MatchResult(true, 1.0);
        }

        // خطوة 2: بحث fuzzy — نقسم العنوان لكلمات ونقارن كل كلمة مع اسم المنطقة
        // لو اسم المنطقة كلمة واحدة → نقارن كلمة بكلمة
        // لو اسم المنطقة أكتر من كلمة → نقارن عبارات (n-gram)
        String[] zoneWords = normalizedZone.split("\\s+");
        String[] addressWords = addressWithoutAl.split("\\s+");

        if (zoneWords.length == 1) {
            // مطابقة كلمة واحدة
            return fuzzyMatchSingleWord(normalizedZone, addressWords);
        } else {
            // مطابقة عبارة (كلمتين أو أكثر)
            return fuzzyMatchPhrase(zoneWords, addressWords);
        }
    }

    /**
     * مطابقة كلمة واحدة مع كلمات العنوان.
     */
    private MatchResult fuzzyMatchSingleWord(String zoneWord, String[] addressWords) {
        double bestScore = 0;

        for (String addrWord : addressWords) {
            if (addrWord.length() < 2) continue;

            double score = jaroWinkler.apply(zoneWord, addrWord);
            if (score > bestScore) {
                bestScore = score;
            }
        }

        if (bestScore >= SIMILARITY_THRESHOLD) {
            return new MatchResult(true, bestScore);
        }
        return MatchResult.NO_MATCH;
    }

    /**
     * مطابقة عبارة (كلمتين+) مع العنوان.
     * نبني n-grams من العنوان بنفس طول العبارة ونقارن كل n-gram.
     */
    private MatchResult fuzzyMatchPhrase(String[] zoneWords, String[] addressWords) {
        int phraseLen = zoneWords.length;
        if (addressWords.length < phraseLen) return MatchResult.NO_MATCH;

        String zonePhrase = String.join(" ", zoneWords);
        double bestScore = 0;

        for (int i = 0; i <= addressWords.length - phraseLen; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < phraseLen; j++) {
                if (j > 0) sb.append(" ");
                sb.append(addressWords[i + j]);
            }
            String addrPhrase = sb.toString();

            double score = jaroWinkler.apply(zonePhrase, addrPhrase);
            if (score > bestScore) {
                bestScore = score;
            }
        }

        if (bestScore >= SIMILARITY_THRESHOLD) {
            return new MatchResult(true, bestScore);
        }
        return MatchResult.NO_MATCH;
    }

    // ==================== MatchResult DTO ====================

    /**
     * نتيجة المطابقة — تحتوي على: هل تطابق؟ ونسبة التشابه.
     */
    public static class MatchResult {
        public static final MatchResult NO_MATCH = new MatchResult(false, 0.0);

        private final boolean matched;
        private final double score;

        public MatchResult(boolean matched, double score) {
            this.matched = matched;
            this.score = score;
        }

        public boolean isMatched() { return matched; }
        public double getScore() { return score; }

        @Override
        public String toString() {
            return matched ? String.format("MATCH(%.1f%%)", score * 100) : "NO_MATCH";
        }
    }
}
