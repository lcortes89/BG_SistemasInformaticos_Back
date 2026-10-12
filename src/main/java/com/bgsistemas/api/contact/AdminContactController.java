package com.bgsistemas.api.contact;

import com.bgsistemas.api.contact.dtos.ContactResponseDTO;
import com.bgsistemas.api.contact.mappers.ContactMapper;
import com.bgsistemas.api.globals.PageResponseDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
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
}
