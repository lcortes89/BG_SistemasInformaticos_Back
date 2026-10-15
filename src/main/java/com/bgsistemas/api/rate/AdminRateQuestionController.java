package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.AdminRateOptionDTO;
import com.bgsistemas.api.rate.dtos.AdminRateQuestionDTO;
import com.bgsistemas.api.rate.dtos.RateOptionRequestDTO;
import com.bgsistemas.api.rate.dtos.RateQuestionOrderDTO;
import com.bgsistemas.api.rate.dtos.RateQuestionRequestDTO;
import com.bgsistemas.api.rate.mappers.RateQuestionMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Only ADMIN: SecurityConfig protects every /api/admin/** route (BGSI-233)
@RestController
@RequestMapping("/api/admin/rate-questions")
public class AdminRateQuestionController {

    private final RateQuestionService rateQuestionService;
    private final RateQuestionMapper rateQuestionMapper;

    public AdminRateQuestionController(RateQuestionService rateQuestionService, RateQuestionMapper rateQuestionMapper) {
        this.rateQuestionService = rateQuestionService;
        this.rateQuestionMapper = rateQuestionMapper;
    }

    // GET /api/admin/rate-questions -> every question (also the hidden ones) with its options and prices
    @GetMapping
    public List<AdminRateQuestionDTO> getQuestions() {
        return toDTOs(rateQuestionService.getAllQuestions());
    }

    @PostMapping
    public ResponseEntity<AdminRateQuestionDTO> createQuestion(@Valid @RequestBody RateQuestionRequestDTO request) {
        RateQuestion saved = rateQuestionService.createQuestion(rateQuestionMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(rateQuestionMapper.toAdminDTO(saved));
    }

    // PUT /api/admin/rate-questions/order  { "ids": [3, 1, 2] }
    @PutMapping("/order")
    public List<AdminRateQuestionDTO> reorderQuestions(@Valid @RequestBody RateQuestionOrderDTO request) {
        return toDTOs(rateQuestionService.reorderQuestions(request.ids()));
    }

    @PutMapping("/{id}")
    public AdminRateQuestionDTO updateQuestion(@PathVariable Long id,
                                               @Valid @RequestBody RateQuestionRequestDTO request) {
        return rateQuestionMapper.toAdminDTO(rateQuestionService.updateQuestion(id, rateQuestionMapper.toEntity(request)));
    }

    // Deletes the question and its options
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        rateQuestionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/admin/rate-questions/{id}/options -> new option at the end of the question
    @PostMapping("/{id}/options")
    public ResponseEntity<AdminRateOptionDTO> addOption(@PathVariable Long id,
                                                        @Valid @RequestBody RateOptionRequestDTO request) {
        RateOption saved = rateQuestionService.addOption(id, rateQuestionMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(rateQuestionMapper.toAdminOptionDTO(saved));
    }

    @PutMapping("/options/{optionId}")
    public AdminRateOptionDTO updateOption(@PathVariable Long optionId,
                                           @Valid @RequestBody RateOptionRequestDTO request) {
        return rateQuestionMapper.toAdminOptionDTO(
                rateQuestionService.updateOption(optionId, rateQuestionMapper.toEntity(request)));
    }

    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long optionId) {
        rateQuestionService.deleteOption(optionId);
        return ResponseEntity.noContent().build();
    }

    private List<AdminRateQuestionDTO> toDTOs(List<RateQuestion> questions) {
        return questions.stream().map(rateQuestionMapper::toAdminDTO).toList();
    }
}
