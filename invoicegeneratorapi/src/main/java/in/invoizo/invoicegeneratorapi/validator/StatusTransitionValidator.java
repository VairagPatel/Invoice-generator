package in.invoizo.invoicegeneratorapi.validator;

import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class StatusTransitionValidator {

    private static final Map<InvoiceStatus, Set<InvoiceStatus>> ALLOWED_TRANSITIONS = Map.of(
            InvoiceStatus.DRAFT, Set.of(InvoiceStatus.SENT, InvoiceStatus.CANCELLED),
            InvoiceStatus.SENT, Set.of(InvoiceStatus.VIEWED, InvoiceStatus.PAID, InvoiceStatus.OVERDUE, InvoiceStatus.CANCELLED),
            InvoiceStatus.VIEWED, Set.of(InvoiceStatus.PAID, InvoiceStatus.OVERDUE, InvoiceStatus.CANCELLED),
            InvoiceStatus.OVERDUE, Set.of(InvoiceStatus.PAID, InvoiceStatus.CANCELLED),
            InvoiceStatus.PAID, Set.of(),
            InvoiceStatus.CANCELLED, Set.of()
    );

    public boolean isValidTransition(InvoiceStatus from, InvoiceStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public void validateTransition(InvoiceStatus from, InvoiceStatus to) {
        if (!isValidTransition(from, to)) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot transition from %s to %s", from, to)
            );
        }
    }
}
