package in.invoizo.invoicegeneratorapi.repository;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {

    List<Invoice> findByClerkId(String id);

    Optional<Invoice> findByClerkIdAndId(String clerkId, String id);

    List<Invoice> findByStatusIn(List<InvoiceStatus> statuses);

    List<Invoice> findByClerkIdAndStatus(String clerkId, InvoiceStatus status);

    List<Invoice> findByStatus(InvoiceStatus status);
}
