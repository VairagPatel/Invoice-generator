package in.invoizo.invoicegeneratorapi.controller;

import in.invoizo.invoicegeneratorapi.entity.Invoice;
import in.invoizo.invoicegeneratorapi.entity.Invoice.InvoiceStatus;
import in.invoizo.invoicegeneratorapi.exception.ExportGenerationException;
import in.invoizo.invoicegeneratorapi.service.EmailService;
import in.invoizo.invoicegeneratorapi.service.ExportService;
import in.invoizo.invoicegeneratorapi.service.InvoiceService;
import in.invoizo.invoicegeneratorapi.service.PaymentService;
import in.invoizo.invoicegeneratorapi.util.ValidationUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*") // for frontend access
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService service;
    private final EmailService emailService;
    private final ExportService exportService;
    private final ValidationUtil validationUtil;
    private final PaymentService paymentService;

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
    public ResponseEntity<?> sendInvoice(
            @RequestPart("file") MultipartFile file,
            @RequestPart("email") String customerEmail,
            @RequestPart(value = "invoiceId", required = false) String invoiceId) {
        try {
            // Validate email address
            validationUtil.validateEmail(customerEmail);
            
            // Send email (payment functionality is now handled separately via direct payment)
            emailService.sendInvoiceEmail(customerEmail, file);
            
            // If invoice ID is provided, update status to SENT
            if (invoiceId != null && !invoiceId.trim().isEmpty()) {
                try {
                    Invoice invoice = service.getInvoiceById(invoiceId);
                    if (invoice.getStatus() == InvoiceStatus.DRAFT) {
                        service.updateStatus(invoice.getClerkId(), invoiceId, InvoiceStatus.SENT);
                    }
                } catch (Exception statusError) {
                    log.warn("Failed to update invoice status to SENT", statusError);
                }
            }
            
            return ResponseEntity.ok().body("Invoice sent successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send invoice email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send invoice: " + e.getMessage());
        }
    }

    /**
     * Export invoices to Excel or CSV format
     * Supports filtering by invoice IDs and user authentication
     * 
     * @param format Export format: "excel" or "csv"
     * @param invoiceIds Optional list of specific invoice IDs to export
     * @param authentication User authentication for data isolation
     * @return Byte array of the exported file with appropriate content type
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportInvoices(
            @RequestParam String format,
            @RequestParam(required = false) List<String> invoiceIds,
            Authentication authentication) {
        
        // Validate format parameter
        try {
            validationUtil.validateExportFormat(format);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        
        // Get user's clerk ID for data isolation
        String clerkId = authentication.getName();
        if (clerkId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                    "Authentication required");
        }
        
        // Fetch invoices based on filters
        List<Invoice> invoices;
        if (invoiceIds != null && !invoiceIds.isEmpty()) {
            log.info("Received invoice IDs for export: {}", invoiceIds);
            
            // Filter out empty or null invoice IDs
            List<String> validInvoiceIds = invoiceIds.stream()
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .collect(Collectors.toList());
            
            log.info("Valid invoice IDs after filtering: {}", validInvoiceIds);
            
            if (!validInvoiceIds.isEmpty()) {
                // Get all user invoices first
                List<Invoice> allUserInvoices = service.fetchInvoices(clerkId);
                log.info("Total user invoices: {}", allUserInvoices.size());
                
                // Log all invoice IDs for debugging
                allUserInvoices.forEach(inv -> log.info("Available invoice ID: {}", inv.getId()));
                
                // Export specific invoices
                invoices = allUserInvoices.stream()
                        .filter(invoice -> validInvoiceIds.contains(invoice.getId()))
                        .collect(Collectors.toList());
                
                log.info("Matched invoices for export: {}", invoices.size());
            } else {
                // If no valid IDs, export all user's invoices
                invoices = service.fetchInvoices(clerkId);
                log.info("No valid IDs provided, exporting all {} invoices", invoices.size());
            }
        } else {
            // Export all user's invoices
            invoices = service.fetchInvoices(clerkId);
            log.info("No invoice IDs provided, exporting all {} invoices", invoices.size());
        }
        
        // Check if there are invoices to export
        if (invoices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "No invoices found to export");
        }
        
        try {
            // Generate export file
            byte[] exportData;
            String contentType;
            String fileExtension;
            
            if (format.equalsIgnoreCase("excel")) {
                log.info("Generating Excel export for {} invoices", invoices.size());
                exportData = exportService.exportToExcel(invoices);
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                fileExtension = "xlsx";
            } else {
                log.info("Generating CSV export for {} invoices", invoices.size());
                exportData = exportService.exportToCSV(invoices);
                contentType = "text/csv";
                fileExtension = "csv";
            }
            
            // Generate filename with date format
            String filename = generateExportFilename(fileExtension);
            
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(exportData.length);
            
            log.info("Export completed successfully: {}", filename);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(exportData);
                    
        } catch (ExportGenerationException e) {
            // Re-throw to be handled by global exception handler
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during export", e);
            throw new ExportGenerationException("Unexpected error during export: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate filename for export with date format: invoices_YYYY-MM-DD.{extension}
     * 
     * @param extension File extension (xlsx or csv)
     * @return Formatted filename
     */
    private String generateExportFilename(String extension) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return "invoices_" + today.format(formatter) + "." + extension;
    }

    /**
     * Update the status of an invoice
     * 
     * @param id Invoice ID
     * @param request Status update request containing the new status
     * @param authentication User authentication for authorization
     * @return Updated invoice
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Invoice> updateStatus(
            @PathVariable String id,
            @RequestBody StatusUpdateRequest request,
            Authentication authentication) {
        
        String clerkId = authentication.getName();
        if (clerkId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                    "Authentication required");
        }
        
        try {
            Invoice updatedInvoice = service.updateStatus(clerkId, id, request.getStatus());
            return ResponseEntity.ok(updatedInvoice);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    e.getMessage(), e);
        }
    }



    /**
     * Filter invoices by status
     * 
     * @param status Optional status filter
     * @param authentication User authentication for data isolation
     * @return List of filtered invoices
     */
    @GetMapping("/filter")
    public ResponseEntity<List<Invoice>> filterInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            Authentication authentication) {
        
        String clerkId = authentication.getName();
        if (clerkId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                    "Authentication required");
        }
        
        List<Invoice> invoices;
        if (status != null) {
            invoices = service.fetchInvoicesByStatus(clerkId, status);
        } else {
            invoices = service.fetchInvoices(clerkId);
        }
        
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get all overdue invoices for the authenticated user
     * 
     * @param authentication User authentication for data isolation
     * @return List of overdue invoices
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<Invoice>> getOverdueInvoices(Authentication authentication) {
        String clerkId = authentication.getName();
        if (clerkId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                    "Authentication required");
        }
        
        List<Invoice> overdueInvoices = service.fetchInvoicesByStatus(clerkId, InvoiceStatus.OVERDUE);
        return ResponseEntity.ok(overdueInvoices);
    }

    /**
     * Request DTO for status updates
     */
    @Data
    public static class StatusUpdateRequest {
        private InvoiceStatus status;
        private String notes;
    }
}
