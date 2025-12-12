package in.invoizo.invoicegeneratorapi.validator;

import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for StatusTransitionValidator
 */
class StatusTransitionValidatorPropertyTest {

    private final StatusTransitionValidator validator = new StatusTransitionValidator();

    /**
     * Feature: invoice-enhancements, Property 5: Invalid transitions are rejected
     * Validates: Requirements 1.8
     * 
     * For any invoice and any status transition not in the allowed transitions map,
     * the system should reject the transition with an error
     */
    @Property(tries = 100)
    void invalidTransitionsAreRejected(
            @ForAll @From("invalidTransitions") StatusTransition transition) {
        
        // When attempting an invalid transition
        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> validator.validateTransition(transition.from, transition.to)
        );

        // Then the exception message should describe the invalid transition
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains(transition.from.toString()));
        assertTrue(exception.getMessage().contains(transition.to.toString()));
    }

    /**
     * Verify that valid transitions do not throw exceptions
     */
    @Property(tries = 100)
    void validTransitionsAreAccepted(
            @ForAll @From("validTransitions") StatusTransition transition) {
        
        // When attempting a valid transition
        // Then no exception should be thrown
        assertDoesNotThrow(() -> validator.validateTransition(transition.from, transition.to));
        
        // And isValidTransition should return true
        assertTrue(validator.isValidTransition(transition.from, transition.to));
    }

    /**
     * Verify that isValidTransition is consistent with validateTransition
     */
    @Property(tries = 100)
    void isValidTransitionConsistentWithValidateTransition(
            @ForAll InvoiceStatus from,
            @ForAll InvoiceStatus to) {
        
        boolean isValid = validator.isValidTransition(from, to);
        
        if (isValid) {
            // If isValidTransition returns true, validateTransition should not throw
            assertDoesNotThrow(() -> validator.validateTransition(from, to));
        } else {
            // If isValidTransition returns false, validateTransition should throw
            assertThrows(InvalidStatusTransitionException.class,
                    () -> validator.validateTransition(from, to));
        }
    }

    // Generator for invalid transitions
    @Provide
    Arbitrary<StatusTransition> invalidTransitions() {
        return Combinators.combine(
                Arbitraries.of(InvoiceStatus.class),
                Arbitraries.of(InvoiceStatus.class)
        ).as(StatusTransition::new)
         .filter(t -> !validator.isValidTransition(t.from, t.to));
    }

    // Generator for valid transitions
    @Provide
    Arbitrary<StatusTransition> validTransitions() {
        return Arbitraries.of(
                // DRAFT transitions
                new StatusTransition(InvoiceStatus.DRAFT, InvoiceStatus.SENT),
                new StatusTransition(InvoiceStatus.DRAFT, InvoiceStatus.CANCELLED),
                
                // SENT transitions
                new StatusTransition(InvoiceStatus.SENT, InvoiceStatus.VIEWED),
                new StatusTransition(InvoiceStatus.SENT, InvoiceStatus.PAID),
                new StatusTransition(InvoiceStatus.SENT, InvoiceStatus.OVERDUE),
                new StatusTransition(InvoiceStatus.SENT, InvoiceStatus.CANCELLED),
                
                // VIEWED transitions
                new StatusTransition(InvoiceStatus.VIEWED, InvoiceStatus.PAID),
                new StatusTransition(InvoiceStatus.VIEWED, InvoiceStatus.OVERDUE),
                new StatusTransition(InvoiceStatus.VIEWED, InvoiceStatus.CANCELLED),
                
                // OVERDUE transitions
                new StatusTransition(InvoiceStatus.OVERDUE, InvoiceStatus.PAID),
                new StatusTransition(InvoiceStatus.OVERDUE, InvoiceStatus.CANCELLED)
        );
    }

    // Helper class to represent a status transition
    static class StatusTransition {
        final InvoiceStatus from;
        final InvoiceStatus to;

        StatusTransition(InvoiceStatus from, InvoiceStatus to) {
            this.from = from;
            this.to = to;
        }
    }
}
