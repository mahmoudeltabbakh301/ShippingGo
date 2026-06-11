package com.shipment.shippinggo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * اختبار شامل لأداة مطابقة النصوص العربية — يغطي:
 * - توحيد الحروف (Normalization)
 * - إزالة التشكيل وأل التعريف
 * - المطابقة الضبابية (Fuzzy Matching)
 * - حالات واقعية من عناوين الشحن المصرية
 */
class ArabicTextMatcherTest {

    private ArabicTextMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new ArabicTextMatcher();
    }

    // ==================== Normalization Tests ====================

    @Nested
    @DisplayName("Arabic Normalization")
    class NormalizationTests {

        @Test
        @DisplayName("تاء مربوطة ← هاء: قحافة → قحافه")
        void shouldNormalizeTaaMarbouta() {
            assertEquals("قحافه", matcher.normalize("قحافة"));
            assertEquals("قحافه", matcher.normalize("قحافه"));
        }

        @Test
        @DisplayName("ألف مقصورة ← ياء: مستشفى → مستشفي")
        void shouldNormalizeAlefMaksura() {
            assertEquals("مستشفي", matcher.normalize("مستشفى"));
            assertEquals("مستشفي", matcher.normalize("مستشفي"));
        }

        @Test
        @DisplayName("أشكال الألف: أحمد/إبراهيم/آخر → احمد/ابراهيم/اخر")
        void shouldNormalizeAlefVariants() {
            assertEquals("احمد", matcher.normalize("أحمد"));
            assertEquals("ابراهيم", matcher.normalize("إبراهيم"));
            assertEquals("اخر", matcher.normalize("آخر"));
        }

        @Test
        @DisplayName("إزالة التشكيل: شَارِع → شارع")
        void shouldRemoveDiacritics() {
            assertEquals("شارع", matcher.normalize("شَارِع"));
            assertEquals("المنصوره", matcher.normalize("المَنصُورَة"));
        }

        @Test
        @DisplayName("إزالة أل التعريف: العجيزي → عجيزي")
        void shouldRemoveDefiniteArticle() {
            assertEquals("عجيزي", matcher.normalizeWord("العجيزي"));
            assertEquals("منصوره", matcher.normalizeWord("المنصورة"));
        }

        @Test
        @DisplayName("null و empty")
        void shouldHandleNullAndEmpty() {
            assertEquals("", matcher.normalize(null));
            assertEquals("", matcher.normalize(""));
            assertEquals("", matcher.normalize("   "));
        }
    }

    // ==================== Exact Match After Normalization ====================

    @Nested
    @DisplayName("Exact Match بعد التوحيد")
    class ExactMatchAfterNormalizationTests {

        @Test
        @DisplayName("قحافة vs قحافه — تاء مربوطة/هاء")
        void shouldMatchTaaMarbouta() {
            var result = matcher.matchZoneInAddress("شارع قحافه طنطا", "قحافة");
            assertTrue(result.isMatched());
            assertEquals(1.0, result.getScore());
        }

        @Test
        @DisplayName("المنصورة vs المنصوره — تاء مربوطة + أل التعريف")
        void shouldMatchDefiniteArticleWithTaaMarbouta() {
            var result = matcher.matchZoneInAddress("المنصوره شارع الجيش", "المنصورة");
            assertTrue(result.isMatched());
            assertEquals(1.0, result.getScore());
        }

        @Test
        @DisplayName("مستشفى vs مستشفي — ألف مقصورة")
        void shouldMatchAlefMaksura() {
            var result = matcher.matchZoneInAddress("بجوار مستشفي الجامعة", "مستشفى");
            assertTrue(result.isMatched());
            assertEquals(1.0, result.getScore());
        }

        @Test
        @DisplayName("العجيزي vs عجيزي — أل التعريف")
        void shouldMatchWithoutDefiniteArticle() {
            var result = matcher.matchZoneInAddress("شارع عجيزي", "العجيزي");
            assertTrue(result.isMatched());
            assertEquals(1.0, result.getScore());
        }
    }

    // ==================== Fuzzy Match ====================

    @Nested
    @DisplayName("Fuzzy Match — أخطاء إملائية")
    class FuzzyMatchTests {

        @Test
        @DisplayName("عجيزي vs عجيري — حرف غلط (ز←ر)")
        void shouldFuzzyMatchTypo() {
            var result = matcher.matchZoneInAddress("شارع عجيري بجوار المسجد", "العجيزي");
            assertTrue(result.isMatched(), "يجب أن يتطابق عجيري مع العجيزي");
            assertTrue(result.getScore() >= 0.85, "Score should be >= 85%");
        }

        @Test
        @DisplayName("المنصورة vs المنصوره — مطابقة مع تاء مربوطة")
        void shouldFuzzyMatchMansoura() {
            var result = matcher.matchZoneInAddress("مدينة المنصوره", "المنصورة");
            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("كلمة مختلفة تماماً — يجب أن لا يتطابق")
        void shouldNotMatchCompletelyDifferent() {
            var result = matcher.matchZoneInAddress("شارع الجمهورية القاهرة", "المنصورة");
            assertFalse(result.isMatched());
        }

        @Test
        @DisplayName("كلمة قصيرة جداً — يجب أن لا يتطابق")
        void shouldNotMatchShortZoneName() {
            var result = matcher.matchZoneInAddress("شارع طه حسين", "طه");
            assertFalse(result.isMatched(), "Zone names < 3 chars should be rejected");
        }
    }

    // ==================== Real-World Scenarios ====================

    @Nested
    @DisplayName("سيناريوهات واقعية من الشحن")
    class RealWorldTests {

        @Test
        @DisplayName("أوردر: الغربية - طنطا - شارع الحلو ← المندوب مسجل له 'شارع الحلو'")
        void shouldMatchStreetInAddress() {
            var result = matcher.matchZoneInAddress(
                    "الغربية مركز طنطا شارع الحلو بجوار الصيدلية",
                    "شارع الحلو"
            );
            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("أوردر: شارع سعيد متفرع من البحر ← المندوب مسجل له 'شارع سعيد'")
        void shouldMatchPartialStreetName() {
            var result = matcher.matchZoneInAddress(
                    "شارع سعيد متفرع من شارع البحر",
                    "شارع سعيد"
            );
            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("عنوان يحتوي على اسم المنطقة بأل التعريف — المندوب بدون أل")
        void shouldMatchWithAndWithoutAl() {
            var result = matcher.matchZoneInAddress(
                    "قرية القحافه مركز طنطا",
                    "قحافة"
            );
            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("عنوان بتشكيل — يجب أن يتطابق")
        void shouldMatchWithDiacritics() {
            var result = matcher.matchZoneInAddress(
                    "شَارِع الحُلو - طنطا",
                    "شارع الحلو"
            );
            assertTrue(result.isMatched());
        }

        @Test
        @DisplayName("عنوان فارغ — لا يتطابق")
        void shouldNotMatchEmptyAddress() {
            var result = matcher.matchZoneInAddress("", "شارع الحلو");
            assertFalse(result.isMatched());
        }

        @Test
        @DisplayName("اسم منطقة فارغ — لا يتطابق")
        void shouldNotMatchEmptyZone() {
            var result = matcher.matchZoneInAddress("شارع الحلو", "");
            assertFalse(result.isMatched());
        }

        @Test
        @DisplayName("null values — لا يتطابق")
        void shouldNotMatchNulls() {
            assertFalse(matcher.matchZoneInAddress(null, "test").isMatched());
            assertFalse(matcher.matchZoneInAddress("test", null).isMatched());
        }
    }
}
