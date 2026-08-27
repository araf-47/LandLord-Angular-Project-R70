package com.landlord.backend.maintenance;

import com.idb.auth.model.User;
import com.landlord.backend.tenant.TenantRepository;
import com.landlord.backend.unit.Unit;
import com.landlord.backend.unit.UnitRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MaintenanceController {

    private final MaintenanceTicketRepository tickets;
    private final ExpenseRepository expenses;
    private final UnitRepository units;
    private final TenantRepository tenants;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    public MaintenanceController(MaintenanceTicketRepository tickets, ExpenseRepository expenses, UnitRepository units,
            TenantRepository tenants) {
        this.tickets = tickets;
        this.expenses = expenses;
        this.units = units;
        this.tenants = tenants;
    }

    /** See BillingController.effectiveTenantId - same reasoning, same fix. */
    private Long effectiveTenantId(User principal, Long requestedTenantId) {
        boolean isLandlord = principal.getRoles().stream().anyMatch(r -> "LANDLORD".equals(r.getName()));
        if (isLandlord) {
            return requestedTenantId;
        }
        return tenants.findFirstByAuthUserId(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tenant record linked to this account"))
            .getId();
    }

    @GetMapping("/api/maintenance-tickets")
    public List<MaintenanceTicket> list(@AuthenticationPrincipal User principal,
            @RequestParam(required = false) Long tenantId, @RequestParam(required = false) Long unitId) {
        Long effective = effectiveTenantId(principal, tenantId);
        if (effective != null) return tickets.findByTenantId(effective);
        if (unitId != null) return tickets.findByUnitId(unitId);
        return tickets.findAll();
    }

    @GetMapping("/api/maintenance-tickets/{id}")
    public MaintenanceTicket get(@PathVariable Long id) {
        return tickets.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    public record NewTicketRequest(Long unitId, Long tenantId, String description) {}

    @PostMapping("/api/maintenance-tickets")
    public ResponseEntity<MaintenanceTicket> create(@AuthenticationPrincipal User principal,
            @RequestBody NewTicketRequest request) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setUnitId(request.unitId());
        ticket.setTenantId(effectiveTenantId(principal, request.tenantId()));
        ticket.setDescription(request.description());
        ticket.setStatus("pending");
        return ResponseEntity.status(HttpStatus.CREATED).body(tickets.save(ticket));
    }

    @PostMapping("/api/maintenance-tickets/{id}/photo")
    public ResponseEntity<MaintenanceTicket> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        MaintenanceTicket ticket = tickets.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file uploaded");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + extension;

        try {
            Path targetDir = Path.of(uploadsDir, "maintenance", String.valueOf(id));
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store photo", e);
        }

        ticket.setPhotoUrl("/uploads/maintenance/" + id + "/" + filename);
        return ResponseEntity.ok(tickets.save(ticket));
    }

    public record UpdateStatusRequest(String status, Double cost, String bearer) {}

    @PutMapping("/api/maintenance-tickets/{id}/status")
    public ResponseEntity<MaintenanceTicket> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        MaintenanceTicket ticket = tickets.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        ticket.setStatus(request.status());
        if (request.cost() != null) {
            ticket.setCost(request.cost());
        }
        tickets.save(ticket);

        if ("resolved".equals(request.status()) && request.cost() != null) {
            Long propertyId = null;
            if (ticket.getUnitId() != null) {
                Unit unit = units.findById(ticket.getUnitId()).orElse(null);
                if (unit != null) propertyId = unit.getPropertyId();
            }

            Expense expense = new Expense();
            expense.setPropertyId(propertyId);
            expense.setTicketId(ticket.getId());
            expense.setCategory("Maintenance");
            expense.setDescription(ticket.getDescription());
            expense.setAmount(request.cost());
            expense.setBearer(request.bearer() == null ? "landlord" : request.bearer());
            expense.setTenantId(ticket.getTenantId());
            expenses.save(expense);
        }

        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/api/expenses")
    public List<Expense> expenses(@RequestParam(required = false) Long propertyId, @RequestParam(required = false) Long tenantId) {
        if (propertyId != null) return expenses.findByPropertyId(propertyId);
        if (tenantId != null) return expenses.findByTenantId(tenantId);
        return expenses.findAll();
    }

    public record NewExpenseRequest(Long propertyId, String category, String description, Double amount, String bearer, Long tenantId) {}

    @PostMapping("/api/expenses")
    public ResponseEntity<Expense> createExpense(@RequestBody NewExpenseRequest request) {
        Expense expense = new Expense();
        expense.setPropertyId(request.propertyId());
        expense.setCategory(request.category());
        expense.setDescription(request.description());
        expense.setAmount(request.amount());
        expense.setBearer(request.bearer() == null ? "landlord" : request.bearer());
        expense.setTenantId(request.tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(expenses.save(expense));
    }

    @DeleteMapping("/api/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenses.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
