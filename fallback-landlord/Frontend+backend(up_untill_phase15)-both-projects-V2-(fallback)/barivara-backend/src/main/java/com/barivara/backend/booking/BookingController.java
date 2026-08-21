package com.barivara.backend.booking;

import com.barivara.backend.listing.Listing;
import com.barivara.backend.listing.ListingRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/booking-requests")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class BookingController {

    private final BookingRequestRepository requests;
    private final ListingRepository listings;

    public BookingController(BookingRequestRepository requests, ListingRepository listings) {
        this.requests = requests;
        this.listings = listings;
    }

    /** tenantId → that tenant's own requests. ownerId → every request against that owner's listings. */
    @GetMapping
    public List<BookingRequest> list(
            @RequestParam(required = false) Long listingId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long ownerId) {
        if (listingId != null) return requests.findByListingId(listingId);
        if (tenantId != null) return requests.findByTenantId(tenantId);
        if (ownerId != null) {
            List<Long> listingIds = listings.findByOwnerId(ownerId).stream().map(Listing::getId).toList();
            return requests.findByListingIdIn(listingIds);
        }
        return requests.findAll();
    }

    @GetMapping("/{id}")
    public BookingRequest one(@PathVariable Long id) {
        return requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record NewBookingRequest(Long listingId, Long tenantId, String applicantName, String message) {}

    @PostMapping
    public ResponseEntity<BookingRequest> create(@RequestBody NewBookingRequest body) {
        listings.findById(body.listingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        BookingRequest request = new BookingRequest();
        request.setListingId(body.listingId());
        request.setTenantId(body.tenantId());
        request.setApplicantName(body.applicantName());
        request.setMessage(body.message());
        request.setStatus("pending");
        return ResponseEntity.status(HttpStatus.CREATED).body(requests.save(request));
    }

    public record DecisionRequest(String status) {}

    @PutMapping("/{id}/status")
    public BookingRequest decide(@PathVariable Long id, @RequestBody DecisionRequest decision) {
        BookingRequest request = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        request.setStatus(decision.status());
        if ("approved".equals(decision.status())) {
            Listing listing = listings.findById(request.getListingId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
            listing.setStatus("taken");
            listings.save(listing);
        }
        return requests.save(request);
    }
}
