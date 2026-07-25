package com.lorafilm.movie.pricing.domain.entity;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.audit.BaseAuditableEntity;
import com.lorafilm.movie.pricing.domain.enums.PricePolicyStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "price_policies")
public class PricePolicy extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36,
            columnDefinition = "CHAR(36)")
    private String publicId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PricePolicyStatus status = PricePolicyStatus.DRAFT;

    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency = "VND";

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supersedes_policy_id")
    private PricePolicy supersedesPolicy;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "activated_by")
    private Long activatedBy;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivated_by")
    private Long deactivatedBy;

    @Column(name = "deactivation_reason", length = 500)
    private String deactivationReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PricePolicyRule> rules = new ArrayList<>();

    public void replaceRules(List<PricePolicyRule> replacement) {
        rules.clear();
        if (replacement != null) {
            replacement.forEach(this::addRule);
        }
    }

    public void addRule(PricePolicyRule rule) {
        rule.setPolicy(this);
        rules.add(rule);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Cinema getCinema() { return cinema; }
    public void setCinema(Cinema cinema) { this.cinema = cinema; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public PricePolicyStatus getStatus() { return status; }
    public void setStatus(PricePolicyStatus status) { this.status = status; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public PricePolicy getSupersedesPolicy() { return supersedesPolicy; }
    public void setSupersedesPolicy(PricePolicy supersedesPolicy) { this.supersedesPolicy = supersedesPolicy; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    public Long getActivatedBy() { return activatedBy; }
    public void setActivatedBy(Long activatedBy) { this.activatedBy = activatedBy; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(Instant deactivatedAt) { this.deactivatedAt = deactivatedAt; }
    public Long getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(Long deactivatedBy) { this.deactivatedBy = deactivatedBy; }
    public String getDeactivationReason() { return deactivationReason; }
    public void setDeactivationReason(String deactivationReason) { this.deactivationReason = deactivationReason; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<PricePolicyRule> getRules() { return rules; }
    public void setRules(List<PricePolicyRule> rules) { this.rules = rules; }
}
