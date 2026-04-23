package com.example.junitdemo.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Best Practice: Mockito — Mocks, Stubbing, Verifikation, ArgumentCaptor
 *
 * Demonstriert:
 * - @ExtendWith(MockitoExtension.class) statt MockitoAnnotations.openMocks()
 * - @Mock, @InjectMocks, @Captor
 * - BDD-Stil: given/when/then mit BDDMockito
 * - ArgumentCaptor für Verifikation komplexer Argumente
 * - verify() mit times(), never()
 * - verifyNoMoreInteractions() für strikte Verifikation
 * - Stubbing von void-Methoden mit willThrow()
 * - willAnswer() für dynamische Rückgabewerte
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)           // Best Practice: Extension statt Rule/Runner
@DisplayName("OrderService")
class OrderServiceTest {

    // Best Practice: @Mock erzeugt ein frisches Mock-Objekt für jeden Test
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    // Best Practice: @InjectMocks erstellt die Klasse unter Test und injiziert alle @Mock-Felder
    @InjectMocks
    private OrderService orderService;

    // Best Practice: @Captor als Klassenfeld — klarer als inline ArgumentCaptor.forClass()
    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private Product laptop;

    @BeforeEach
    void setUp() {
        laptop = new Product("p-laptop", "Laptop", Money.of("999.00", "EUR"));
    }

    // =========================================================================

    @Nested
    @DisplayName("createOrder()")
    class CreateOrder {

        @Test
        @DisplayName("speichert neue Bestellung im Repository und gibt sie zurück")
        void createOrder_validCustomer_savesAndReturnsOrder() {
            // Best Practice: BDD-Stil mit given/when/then verbessert Lesbarkeit
            // given
            given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Order result = orderService.createOrder("customer-1");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getCustomerId()).isEqualTo("customer-1");
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("ArgumentCaptor — verifiziert die Eigenschaften des gespeicherten Objekts")
        void createOrder_capturesOrderPassedToRepository() {
            // Best Practice: ArgumentCaptor wenn man nicht nur prüft OB eine Methode
            // aufgerufen wurde, sondern WIE (mit welchen Argumenten)
            given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

            orderService.createOrder("customer-99");

            // Captor fängt das Argument auf, das an save() übergeben wurde
            then(orderRepository).should().save(orderCaptor.capture());
            Order capturedOrder = orderCaptor.getValue();

            assertThat(capturedOrder.getCustomerId()).isEqualTo("customer-99");
            assertThat(capturedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(capturedOrder.getItems()).isEmpty();
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("addItemToOrder()")
    class AddItemToOrder {

        // Best Practice: @BeforeEach NUR für Testdaten, NICHT für Stubs.
        // Mockito's UnnecessaryStubbingException erzwingt dieses Prinzip:
        // Ein Stub, der in manchen Tests nie aufgerufen wird, ist totes Gewicht
        // und verschleiert die Testintention. Jeder Test stubbt nur, was er braucht.
        private Order existingOrder;

        @BeforeEach
        void setUp() {
            existingOrder = new Order("customer-1");
        }

        @Test
        @DisplayName("Happypath — fügt Artikel hinzu, reserviert Bestand, speichert Bestellung")
        void addItemToOrder_allDependenciesAvailable_addsItemAndReservesInventory() {
            // given — jeder Test stubbt nur was er tatsächlich durchläuft
            given(orderRepository.findById("order-1")).willReturn(Optional.of(existingOrder));
            given(productRepository.findById("p-laptop")).willReturn(Optional.of(laptop));
            given(inventoryService.isAvailable("p-laptop", 2)).willReturn(true);
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            Order result = orderService.addItemToOrder("order-1", "p-laptop", 2);

            // then — Zustand des Ergebnisses prüfen
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);

            // then — Interaktionen mit Abhängigkeiten verifizieren
            verify(inventoryService).reserve("p-laptop", 2);
            verify(orderRepository, times(1)).save(existingOrder);
        }

        @Test
        @DisplayName("ArgumentCaptor — verifiziert Artikel-Eigenschaften im gespeicherten Objekt")
        void addItemToOrder_capturesSavedOrderWithCorrectItem() {
            given(orderRepository.findById("order-1")).willReturn(Optional.of(existingOrder));
            given(productRepository.findById("p-laptop")).willReturn(Optional.of(laptop));
            given(inventoryService.isAvailable("p-laptop", 2)).willReturn(true);
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

            orderService.addItemToOrder("order-1", "p-laptop", 2);

            verify(orderRepository).save(orderCaptor.capture());
            Order saved = orderCaptor.getValue();

            // Best Practice: satisfies() für mehrere Assertions auf demselben Objekt
            assertThat(saved.getItems())
                .hasSize(1)
                .first()
                .satisfies(item -> {
                    assertThat(item.getProduct().getId()).isEqualTo("p-laptop");
                    assertThat(item.getQuantity()).isEqualTo(2);
                });
        }

        @Test
        @DisplayName("wirft OrderNotFoundException wenn Bestellung nicht existiert")
        void addItemToOrder_orderNotFound_throwsOrderNotFoundException() {
            given(orderRepository.findById("unknown-id")).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.addItemToOrder("unknown-id", "p-laptop", 1))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("unknown-id");

            // Best Practice: verify(mock, never()) — nachfolgende Schritte bei Fehler nicht ausgeführt
            verify(inventoryService, never()).reserve(anyString(), anyInt());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("wirft InsufficientInventoryException wenn Bestand fehlt")
        void addItemToOrder_insufficientInventory_throwsException() {
            given(orderRepository.findById("order-1")).willReturn(Optional.of(existingOrder));
            given(productRepository.findById("p-laptop")).willReturn(Optional.of(laptop));
            given(inventoryService.isAvailable("p-laptop", 2)).willReturn(false);

            assertThatThrownBy(() -> orderService.addItemToOrder("order-1", "p-laptop", 2))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("p-laptop");

            verify(inventoryService, never()).reserve(anyString(), anyInt());
        }

        @Test
        @DisplayName("wirft Exception wenn Produkt nicht gefunden")
        void addItemToOrder_productNotFound_throwsIllegalArgumentException() {
            given(orderRepository.findById("order-1")).willReturn(Optional.of(existingOrder));
            given(productRepository.findById("unknown-product")).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.addItemToOrder("order-1", "unknown-product", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");

            // verifyNoMoreInteractions — inventoryService wurde überhaupt nicht berührt
            verifyNoMoreInteractions(inventoryService);
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("confirmOrder()")
    class ConfirmOrder {

        @Test
        @DisplayName("bestätigt Bestellung und speichert aktualisierten Zustand")
        void confirmOrder_pendingOrderWithItems_savesConfirmedOrder() {
            Order order = new Order("customer-1");
            order.addItem(new OrderItem(laptop, 1));
            given(orderRepository.findById("order-1")).willReturn(Optional.of(order));
            given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            Order result = orderService.confirmOrder("order-1");

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            verify(orderRepository).save(eq(order));
        }

        @Test
        @DisplayName("wirft OrderNotFoundException wenn Bestellung nicht existiert")
        void confirmOrder_orderNotFound_throwsException() {
            given(orderRepository.findById("missing")).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.confirmOrder("missing"))
                .isInstanceOf(OrderNotFoundException.class);
        }
    }

    // =========================================================================

    @Nested
    @DisplayName("cancelOrder()")
    class CancelOrder {

        @Test
        @DisplayName("storniert Bestellung und persistiert den neuen Status")
        void cancelOrder_existingOrder_savesCancelledOrder() {
            Order order = new Order("customer-1");
            given(orderRepository.findById("order-1")).willReturn(Optional.of(order));
            given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            orderService.cancelOrder("order-1");

            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("willThrow() — simuliert Fehler in void-ähnlichen Methoden")
        void cancelOrder_repositoryFails_propagatesException() {
            Order order = new Order("customer-1");
            given(orderRepository.findById("order-1")).willReturn(Optional.of(order));

            // Best Practice: willThrow().given() für non-void Methoden die Exception werfen sollen
            willThrow(new RuntimeException("DB nicht erreichbar"))
                .given(orderRepository).save(any());

            assertThatThrownBy(() -> orderService.cancelOrder("order-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB nicht erreichbar");
        }
    }
}
