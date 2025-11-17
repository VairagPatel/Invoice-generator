package in.bushansirgur.invoicegeneratorapi.controller;

import in.bushansirgur.invoicegeneratorapi.entity.Invoice;
import in.bushansirgur.invoicegeneratorapi.service.EmailService;
import in.bushansirgur.invoicegeneratorapi.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@CrossOrigin(origins = "*") // for frontend access
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;
    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<Invoice> saveInvoice(@RequestBody Invoice invoice) {
        return ResponseEntity.ok(service.saveInvoice(invoice));
    }

    @GetMapping
    public ResponseEntity<List<Invoice>> fetchInvoices(Authentication authentication) {
        System.out.println(authentication.getName());
        return ResponseEntity.ok(service.fetchInvoices(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeInvoice(@PathVariable String id, Authentication authentication) {
        if (authentication.getName() != null) {
            service.removeInvoice(authentication.getName(), id);
            return ResponseEntity.noContent().build();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "User does not have permission to access this resource");
    }

    @PostMapping("/sendinvoice")
    public ResponseEntity<?> sendInvoice(@RequestPart("file") MultipartFile file,
            @RequestPart("email") String customerEmail) {
        try {
            emailService.sendInvoiceEmail(customerEmail, file);
            return ResponseEntity.ok().body("Invoice sent successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send invoice.");
        }
    }
}
