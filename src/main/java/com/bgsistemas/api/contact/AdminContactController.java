package com.bgsistemas.api.contact;

import com.bgsistemas.api.contact.dtos.ContactResponseDTO;
import com.bgsistemas.api.contact.dtos.ContactUpdateRequestDTO;
import com.bgsistemas.api.contact.mappers.ContactMapper;
import com.bgsistemas.api.globals.PageResponseDTO;
import com.bgsistemas.api.security.RequiresConfirmation;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contacts")
public class AdminContactController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ContactService contactService;
    private final ContactMapper contactMapper;

    public AdminContactController(ContactService contactService, ContactMapper contactMapper) {
        this.contactService = contactService;
        this.contactMapper = contactMapper;
    }

    // GET /api/admin/contacts?status=NEW&page=0&size=10 -> newest first
    @GetMapping
    public PageResponseDTO<ContactResponseDTO> getContacts(@RequestParam(required = false) ContactStatus status,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponseDTO.from(
                contactService.getContacts(status, pageRequest).map(contactMapper::toResponse));
    }

    // PATCH /api/admin/contacts/{id}  { "status": "CONTACTED", "note": "..." }
    // Archiving = { "status": "ARCHIVED" }
    @PatchMapping("/{id}")
    public ContactResponseDTO updateContact(@PathVariable Long id,
                                            @Valid @RequestBody ContactUpdateRequestDTO request) {
        Contact updated = contactService.updateContact(id, request.status(), request.note());
        return contactMapper.toResponse(updated);
    }

    // DELETE /api/admin/contacts/{id} -> 204 No Content (404 if it does not exist). Needs the password (BGSI-275)
    @RequiresConfirmation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}
