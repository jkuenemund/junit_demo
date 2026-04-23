package com.example.junitdemo.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Best Practice: AssertJ-Assertions
 *
 * Demonstriert:
 * - assertThat() statt JUnit assertEquals() — flüssige, lesbare API
 * - Assertion-Chaining für mehrere Prüfungen auf einem Objekt
 * - assertSoftly() — alle Assertions laufen durch, auch wenn eine fehlschlägt
 * - assertThatThrownBy() — Exceptions typsicher und beschreibend prüfen
 * - Beschreibende Fehlermeldungen mit as()
 */
@Tag("unit")
@DisplayName("Money Value Object")
class MoneyTest {

    // -------------------------------------------------------------------------
    // Best Practice: Aussagekräftige Testnamen — beschreiben Verhalten, nicht Implementierung
    // Schema: methodName_stateUnderTest_expectedBehavior
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("of() erzeugt Money mit korrektem Betrag und Währung")
    void of_validInput_createsMoney() {
        Money money = Money.of("42.50", "EUR");

        // Best Practice: Assertion-Chaining — mehrere Prüfungen in einem Block
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("42.50"));
        assertThat(money.getCurrency()).isEqualTo(Currency.getInstance("EUR"));
    }

    @Test
    @DisplayName("add() summiert zwei Money-Objekte gleicher Währung korrekt")
    void add_sameCurrency_returnsCorrectSum() {
        Money ten = Money.of("10.00", "EUR");
        Money twenty = Money.of("20.00", "EUR");

        Money result = ten.add(twenty);

        // Best Practice: as() liefert beschreibende Fehlermeldung bei Testfehler
        assertThat(result.getAmount())
            .as("Summe von 10 EUR + 20 EUR")
            .isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("multiply() berechnet das korrekte Vielfache")
    void multiply_positiveQuantity_returnsScaledAmount() {
        Money price = Money.of("15.00", "EUR");

        Money result = price.multiply(3);

        assertThat(result).isEqualTo(Money.of("45.00", "EUR"));
    }

    @Test
    @DisplayName("applyDiscount() reduziert Betrag um den angegebenen Prozentsatz")
    void applyDiscount_tenPercent_returnsReducedAmount() {
        Money price = Money.of("100.00", "EUR");

        Money discounted = price.applyDiscount(10.0);

        // Best Practice: within() für Fließkomma-Vergleiche
        assertThat(discounted.getAmount())
            .usingComparator(BigDecimal::compareTo)
            .isEqualByComparingTo(new BigDecimal("90.00"));
    }

    @Test
    @DisplayName("assertSoftly() prüft alle Felder eines Objekts in einem Schritt")
    void softAssertions_verifyAllMoneyProperties() {
        Money money = Money.of("19.99", "EUR");

        // Best Practice: assertSoftly() — alle Fehler werden gesammelt und gemeinsam gemeldet
        // Vermeidet, dass ein erster Fehler alle nachfolgenden Assertions blockiert
        assertSoftly(softly -> {
            softly.assertThat(money.getAmount())
                .as("Betrag")
                .isEqualByComparingTo(new BigDecimal("19.99"));
            softly.assertThat(money.getCurrency())
                .as("Währung")
                .isEqualTo(Currency.getInstance("EUR"));
            softly.assertThat(money.toString())
                .as("toString-Darstellung")
                .contains("EUR");
        });
    }

    @Test
    @DisplayName("Konstruktor wirft Exception bei negativem Betrag")
    void constructor_negativeAmount_throwsIllegalArgumentException() {
        // Best Practice: assertThatThrownBy() statt @Test(expected=...) oder try/catch
        // Erlaubt zusätzliche Prüfungen auf Typ, Message und Cause
        assertThatThrownBy(() -> new Money(new BigDecimal("-1.00"), Currency.getInstance("EUR")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-negative");
    }

    @Test
    @DisplayName("Konstruktor wirft Exception bei null-Währung")
    void constructor_nullCurrency_throwsNullPointerException() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Currency");
    }

    @Test
    @DisplayName("add() wirft Exception bei unterschiedlichen Währungen")
    void add_differentCurrencies_throwsIllegalArgumentException() {
        Money euro = Money.of("10.00", "EUR");
        Money dollar = Money.of("10.00", "USD");

        assertThatThrownBy(() -> euro.add(dollar))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("different currencies");
    }

    @Test
    @DisplayName("equals() basiert auf Wert, nicht Referenz")
    void equals_sameValueDifferentInstance_isEqual() {
        Money first = Money.of("50.00", "EUR");
        Money second = Money.of("50.00", "EUR");

        // Best Practice: isEqualTo() prüft equals(), nicht ==
        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }
}
