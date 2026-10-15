package com.bgsistemas.api.rate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.util.ArrayList;
import java.util.List;

// One question of the rate wizard, e.g. "¿Qué tipo de negocio tienes?" (BGSI-233).
// The admin creates, edits, hides and reorders them from the dashboard.
@Entity
public class RateQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Short name of the step, e.g. "Negocio"
    @Column(nullable = false, length = 40)
    private String title;

    @Column(nullable = false, length = 150)
    private String question;

    @Column(length = 255)
    private String hint;

    // Order of the step in the wizard (0, 1, 2...)
    @Column(nullable = false)
    private int position;

    // A hidden question is not shown in the wizard and not used in the price
    @Column(nullable = false)
    private boolean active = true;

    // Deleting a question also deletes its options
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<RateOption> options = new ArrayList<>();

    public RateQuestion() {
    }

    public RateQuestion(String title, String question, String hint, int position) {
        this.title = title;
        this.question = question;
        this.hint = hint;
        this.position = position;
    }

    // Keeps both sides of the relation in sync
    public void addOption(RateOption option) {
        option.setQuestion(this);
        option.setPosition(options.size());
        options.add(option);
    }

    public void removeOption(RateOption option) {
        options.remove(option);
        option.setQuestion(null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<RateOption> getOptions() { return options; }
}
