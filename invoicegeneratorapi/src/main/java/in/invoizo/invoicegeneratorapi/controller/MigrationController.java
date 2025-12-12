package in.invoizo.invoicegeneratorapi.controller;

import in.invoizo.invoicegeneratorapi.migration.InvoiceMigrationService;
import in.invoizo.invoicegeneratorapi.migration.InvoiceMigrationService.MigrationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing database migrations.
 * 
 * This endpoint should be secured and only accessible to administrators.
 * In production, consider using a separate migration tool or script.
 */
@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
@Slf4j
public class MigrationController {

    private final InvoiceMigrationService migrationService;

    /**
     * Trigger invoice migration to add default values for new enhancement fields.
     * 
     * This endpoint should only be called once after deploying the enhancement features.
     * It is idempotent - running it multiple times will not cause issues.
     * 
     * @return MigrationResult with statistics about the migration
     */
    @PostMapping("/invoices")
    public ResponseEntity<MigrationResult> migrateInvoices() {
        log.info("Migration endpoint called");
        
        try {
            MigrationResult result = migrationService.migrateInvoices();
            
            if (result.success) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(207).body(result); // 207 Multi-Status
            }
        } catch (Exception e) {
            log.error("Migration failed with error", e);
            throw new RuntimeException("Migration failed: " + e.getMessage(), e);
        }
    }
}
