package com.example.junitdemo.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Best Practice: @CsvSource in JUnit Jupiter 6
 *
 * Das Projekt nutzt JUnit Jupiter 6.0.3 (via Spring Boot 4.x).
 *
 * Demonstriert alle @CsvSource-Varianten:
 * 1. Einfaches @CsvSource mit String-Array
 * 2. @CsvSource mit textBlock (sauberere Formatierung seit Java 13)
 * 3. useHeadersInDisplayName — erste Zeile wird Spaltenname im Testnamen
 * 4. Benutzerdefinierter Separator mit delimiterString
 * 5. Nullwerte und leere Strings in CSV-Daten
 * 6. @CsvFileSource — Testdaten aus externer CSV-Datei
 */
@Tag("unit")
@DisplayName("PriceCalculationService — @CsvSource Varianten")
class PriceCalculationServiceTest {

    private final PriceCalculationService service = new PriceCalculationService();

    // =========================================================================
    // Variante 1: Klassisches @CsvSource mit String-Array
    // =========================================================================

    @Nested
    @DisplayName("Variante 1: @CsvSource — einfaches String-Array")
    class BasicCsvSource {

        /**
         * Standard-Syntax: Jeder String ist eine Zeile, Werte durch Komma getrennt.
         * Der Testname nutzt per Default den Index: [1], [2], ...
         */
        @ParameterizedTest
        @CsvSource({
            "1, 10.00, 10.00",
            "3,  5.00, 15.00",
            "2,  7.50, 15.00",
            "5, 20.00, 100.00"
        })
        @DisplayName("calculateTotal: Menge × Preis = Gesamtbetrag")
        void calculateTotal_variousInputs_returnsCorrectTotal(
            int quantity, String priceStr, String expectedStr) {

            Product product = new Product("p1", "Produkt", Money.of(priceStr, "EUR"));
            List<OrderItem> items = List.of(new OrderItem(product, quantity));

            Money result = service.calculateTotal(items);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal(expectedStr));
        }

        /**
         * name-Attribut mit Platzhaltern macht Testnamen aussagekräftig:
         * {0} = 1. Parameter, {1} = 2. Parameter usw.
         * {index} = laufende Nummer, {arguments} = alle Parameter als String
         */
        @ParameterizedTest(name = "[{index}] {0} Stück × {1} EUR → {2} EUR")
        @CsvSource({
            "1, 10.00, 10.00",
            "3,  5.00, 15.00",
            "5, 20.00, 100.00"
        })
        @DisplayName("calculateTotal: Testnamen mit Platzhaltern")
        void calculateTotal_withCustomDisplayName(
            int quantity, String priceStr, String expectedStr) {

            Product product = new Product("p1", "Produkt", Money.of(priceStr, "EUR"));
            List<OrderItem> items = List.of(new OrderItem(product, quantity));

            Money result = service.calculateTotal(items);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal(expectedStr));
        }
    }

    // =========================================================================
    // Variante 2: @CsvSource mit Java Text Block
    // =========================================================================

    @Nested
    @DisplayName("Variante 2: @CsvSource mit textBlock")
    class TextBlockCsvSource {

        /**
         * textBlock = Java Text Block (""" ... """) statt String-Array.
         * Vorteile:
         * - Tabellarisches Layout direkt im Code, leichter lesbar
         * - Kein Escaping von Anführungszeichen nötig
         * - Kommentarzeilen mit # möglich
         *
         * useHeadersInDisplayName = true:
         * Die erste Zeile (hier "menge, preis, erwartet") wird als Spaltenname
         * im Testnamen verwendet: "menge=1, preis=10.00, erwartet=10.00"
         */
        @ParameterizedTest(name = "{0} Stück × {1} EUR")
        @CsvSource(useHeadersInDisplayName = true, textBlock = """
            menge, preis,  erwartet
                1, 10.00,    10.00
                3,  5.00,    15.00
                2,  7.50,    15.00
                5, 20.00,   100.00
            """)
        @DisplayName("calculateTotal: textBlock mit useHeadersInDisplayName")
        void calculateTotal_textBlockWithHeaders(
            int quantity, String priceStr, String expectedStr) {

            Product product = new Product("p1", "Produkt", Money.of(priceStr.trim(), "EUR"));
            List<OrderItem> items = List.of(new OrderItem(product, quantity));

            Money result = service.calculateTotal(items);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal(expectedStr.trim()));
        }

        /**
         * textBlock eignet sich besonders gut wenn Werte Sonderzeichen enthalten.
         * Anführungszeichen in CSV-Werten müssen nur mit '' (doppeltes Einfachanführungszeichen)
         * escaped werden.
         */
        @ParameterizedTest(name = "Rabatt {1}% auf {0} EUR → {2} EUR")
        @CsvSource(useHeadersInDisplayName = true, textBlock = """
            basispreis, rabatt, erwartet
              100.00,     0.0,   100.00
              100.00,    10.0,    90.00
              100.00,    25.0,    75.00
               50.00,    10.0,    45.00
               80.00,    20.0,    64.00
            """)
        @DisplayName("calculateTotalWithDiscount: textBlock zeigt Rabattberechnung")
        void calculateTotalWithDiscount_textBlock(
            String basePrice, double discountPercent, String expectedPrice) {

            Product product = new Product("p1", "Produkt", Money.of(basePrice.trim(), "EUR"));
            List<OrderItem> items = List.of(new OrderItem(product, 1));

            Money result = service.calculateTotalWithDiscount(items, discountPercent);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal(expectedPrice.trim()));
        }
    }

    // =========================================================================
    // Variante 3: Benutzerdefinierter Separator mit delimiterString
    // =========================================================================

    @Nested
    @DisplayName("Variante 3: Benutzerdefinierter Separator")
    class CustomDelimiterCsvSource {

        /**
         * delimiter = einzelnes Zeichen als Trenner (statt Komma).
         * Nützlich wenn Testwerte selbst Kommas enthalten (z.B. formatierte Zahlen).
         */
        @ParameterizedTest(name = "{0} Stück à {1} EUR")
        @CsvSource(
            delimiter = '|',
            useHeadersInDisplayName = true,
            textBlock = """
                menge | preis | erwartet
                    1 | 10.00 | 10.00
                    3 |  5.00 | 15.00
                    2 |  7.50 | 15.00
                """
        )
        @DisplayName("calculateTotal: Pipe-Separator für bessere Lesbarkeit")
        void calculateTotal_pipeDelimiter(
            int quantity, String priceStr, String expectedStr) {

            Product product = new Product("p1", "Produkt", Money.of(priceStr.trim(), "EUR"));
            List<OrderItem> items = List.of(new OrderItem(product, quantity));

            Money result = service.calculateTotal(items);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal(expectedStr.trim()));
        }

        /**
         * delimiterString = mehrzeichen Trenner.
         * Nützlich für selbstdokumentierende Testdaten.
         */
        @ParameterizedTest(name = "Menge {0} → Rabatt {1}%")
        @CsvSource(
            delimiterString = "->",
            textBlock = """
                1  -> 0.0
                4  -> 0.0
                5  -> 10.0
                10 -> 10.0
                """
        )
        @DisplayName("resolveDiscount: Pfeil-Separator (-> ) zeigt Eingabe→Erwartung")
        void resolveDiscount_arrowDelimiter(int quantity, double expectedDiscount) {
            Order order = orderWithQuantity(quantity);

            double discount = service.resolveDiscount(order);

            assertThat(discount).isEqualTo(expectedDiscount);
        }
    }

    // =========================================================================
    // Variante 4: Null- und Leer-Werte in CSV
    // =========================================================================

    @Nested
    @DisplayName("Variante 4: Nullwerte und leere Strings")
    class NullAndEmptyValues {

        /**
         * In @CsvSource gilt:
         * - '' (zwei Einfachanführungszeichen)   → leerer String ""
         * - <leer>                               → null (wenn kein emptyValue gesetzt)
         * - nullValues = {"N/A"} erlaubt eigene Null-Repräsentation
         *
         * nullValues definiert Strings die als Java null interpretiert werden.
         */
        @ParameterizedTest(name = "preis=''{0}'' ergibt null={1}")
        @CsvSource(
            nullValues = {"NULL", "N/A"},
            textBlock = """
                10.00, false
                NULL,  true
                N/A,   true
                """
        )
        @DisplayName("nullValues — benutzerdefinierte Null-Repräsentation")
        void nullValues_customNullMarkers(String price, boolean expectNull) {
            // Demonstriert wie nullValues in Tests ankommt
            if (expectNull) {
                assertThat(price).isNull();
            } else {
                assertThat(price).isNotNull().isEqualTo("10.00");
            }
        }

        /**
         * emptyValue: Wenn ein Feld im CSV leer ist (zwei einfache Anführungszeichen: ''),
         * wird es mit dem Wert aus emptyValue ersetzt, statt als "" übergeben zu werden.
         *
         * '' ohne emptyValue → "" (leerer String)
         * '' mit emptyValue = "KEIN_WERT" → "KEIN_WERT"
         *
         * Typischer Anwendungsfall: Fachliche Standardwerte für fehlende CSV-Felder.
         */
        @ParameterizedTest(name = "Feld ''{0}'' kam als leer={1}")
        @CsvSource(
            emptyValue = "KEIN_WERT",
            textBlock = """
                '',    true
                hallo, false
                """
        )
        @DisplayName("emptyValue — leere CSV-Felder erhalten einen Ersatzwert")
        void emptyValue_emptyFieldReplacedWithDefault(String value, boolean wasEmpty) {
            if (wasEmpty) {
                // '' im CSV + emptyValue="KEIN_WERT" → Wert ist "KEIN_WERT", nicht ""
                assertThat(value).isEqualTo("KEIN_WERT");
            } else {
                assertThat(value).isEqualTo("hallo");
            }
        }
    }

    // =========================================================================
    // Variante 5: @CsvFileSource — Testdaten aus externer Datei
    // =========================================================================

    @Nested
    @DisplayName("Variante 5: @CsvFileSource — Daten aus Datei")
    class CsvFileSourceTests {

        /**
         * @CsvFileSource liest Testdaten aus einer CSV-Datei im Classpath.
         * Vorteile:
         * - Große Datensätze außerhalb des Codes pflegen
         * - Business-Anwender können CSV-Dateien befüllen
         * - Kein Recompile bei neuen Testfällen nötig
         *
         * numLinesToSkip = 1 überspringt die Header-Zeile.
         */
        @ParameterizedTest(name = "[{index}] {0} Stück × {1} EUR = {2} EUR")
        @CsvFileSource(
            resources = "/testdata/price-calculations.csv",
            numLinesToSkip = 1,
            delimiter = ';'
        )
        @DisplayName("calculateTotal: Testdaten aus CSV-Datei")
        void calculateTotal_fromCsvFile(int quantity, String price, String expected) {
            Product product = new Product("p1", "Produkt", Money.of(price.trim(), "EUR"));
            List<OrderItem> items = List.of(new OrderItem(product, quantity));

            Money result = service.calculateTotal(items);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal(expected.trim()));
        }
    }

    // =========================================================================
    // Variante 6: @Spy — Verifikation interner Delegation
    // =========================================================================

    @Nested
    @DisplayName("Variante 6: @Spy für Delegation")
    class SpyTests {

        /**
         * @Spy: Echte Methoden werden aufgerufen, Aufrufe werden verifiziert.
         * Hier: Sicherstellen, dass calculateTotalWithDiscount() intern calculateTotal() nutzt.
         */
        @ParameterizedTest(name = "Rabatt {1}%: Delegation an calculateTotal() verifiziert")
        @CsvSource({
            "10.0",
            "25.0",
            " 0.0"
        })
        @DisplayName("calculateTotalWithDiscount delegiert an calculateTotal()")
        void calculateTotalWithDiscount_spyVerifiesDelegation(double discount) {
            PriceCalculationService spy = Mockito.spy(service);
            Product product = new Product("p1", "Produkt", Money.of("100.00", "EUR"));
            List<OrderItem> items = List.of(new OrderItem(product, 1));

            spy.calculateTotalWithDiscount(items, discount);

            verify(spy).calculateTotal(items);
        }
    }

    // =========================================================================
    // Hilfsmethode
    // =========================================================================

    private Order orderWithQuantity(int quantity) {
        Order order = new Order("customer-1");
        Product product = new Product("p1", "Produkt", Money.of("10.00", "EUR"));
        order.addItem(new OrderItem(product, quantity));
        return order;
    }
}
