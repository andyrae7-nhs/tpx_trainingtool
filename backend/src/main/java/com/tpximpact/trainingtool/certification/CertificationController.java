package com.tpximpact.trainingtool.certification;

import com.tpximpact.trainingtool.common.ApiException;
import com.tpximpact.trainingtool.progression.ProgressionService;
import com.tpximpact.trainingtool.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Your certifications, tagged to the framework items they support. */
@RestController
@RequestMapping("/api/certifications")
public class CertificationController {

    private final CertificationRepository certifications;
    private final CertificationSuggester suggester;
    private final ProgressionService progression;
    private final CurrentUser currentUser;

    public CertificationController(CertificationRepository certifications, CertificationSuggester suggester,
                                   ProgressionService progression, CurrentUser currentUser) {
        this.certifications = certifications;
        this.suggester = suggester;
        this.progression = progression;
        this.currentUser = currentUser;
    }

    public record RefView(String ref, String name) {}

    public record CertificationView(Long id, String name, String issuer, String status, String credentialId,
                                    String credentialUrl, LocalDate issuedOn, LocalDate expiresOn, String expiry,
                                    String notes, List<RefView> refs, Instant createdAt, Instant updatedAt) {}

    public record CertificationInput(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 120) String issuer,
            @Pattern(regexp = "EARNED|IN_PROGRESS", message = "must be EARNED or IN_PROGRESS") String status,
            @Size(max = 120) String credentialId,
            @Size(max = 500) @Pattern(regexp = "^$|^https?://.*", message = "must start with http:// or https://") String credentialUrl,
            LocalDate issuedOn,
            LocalDate expiresOn,
            @Size(max = 1000) String notes,
            List<String> refs) {}

    public record SuggestInput(@Size(max = 200) String name, @Size(max = 120) String issuer) {}

    /** Current and in-progress first, then most recently achieved. */
    private static final Comparator<Certification> ORDER = Comparator
            .comparing((Certification c) -> c.expiry(LocalDate.now()) == Certification.Expiry.EXPIRED)
            .thenComparing(c -> Certification.IN_PROGRESS.equals(c.getStatus()) ? 0 : 1)
            .thenComparing(Certification::getIssuedOn, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Certification::getId, Comparator.reverseOrder());

    private CertificationView view(Certification c) {
        return new CertificationView(c.getId(), c.getName(), c.getIssuer(), c.getStatus(), c.getCredentialId(),
                c.getCredentialUrl(), c.getIssuedOn(), c.getExpiresOn(), c.expiry(LocalDate.now()).name(), c.getNotes(),
                c.getRefs().stream().map(r -> new RefView(r, progression.refName(r).orElse(r))).toList(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    @GetMapping
    public List<CertificationView> list() {
        return certifications.findByUserId(currentUser.id()).stream().sorted(ORDER).map(this::view).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CertificationView create(@Valid @RequestBody CertificationInput in) {
        Certification c = new Certification();
        c.setUserId(currentUser.id());
        apply(c, in);
        return view(certifications.save(c));
    }

    @PutMapping("/{id}")
    @Transactional
    public CertificationView update(@PathVariable Long id, @Valid @RequestBody CertificationInput in) {
        Certification c = owned(id);
        apply(c, in);
        c.setUpdatedAt(Instant.now());
        return view(certifications.save(c));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        certifications.delete(owned(id));
    }

    /** Suggested framework items for a certification you're about to add. */
    @PostMapping("/suggest")
    public List<CertificationSuggester.Suggestion> suggest(@Valid @RequestBody SuggestInput in) {
        return suggester.suggest(currentUser.get(), in.name(), in.issuer());
    }

    /** Common certifications for type-ahead. You can still enter any certification. */
    @GetMapping("/known")
    public List<CertificationSuggester.KnownCertification> known() {
        return CertificationSuggester.KNOWN;
    }

    private Certification owned(Long id) {
        Certification c = certifications.findById(id).orElseThrow(() -> ApiException.notFound("Certification"));
        if (!c.getUserId().equals(currentUser.id())) throw ApiException.forbidden();
        return c;
    }

    private void apply(Certification c, CertificationInput in) {
        if (in.issuedOn() != null && in.expiresOn() != null && in.expiresOn().isBefore(in.issuedOn())) {
            throw ApiException.badRequest("The expiry date can't be before the date you achieved it");
        }
        c.setName(in.name().trim());
        c.setIssuer(blankToNull(in.issuer()));
        c.setStatus(Objects.requireNonNullElse(blankToNull(in.status()), Certification.EARNED));
        c.setCredentialId(blankToNull(in.credentialId()));
        c.setCredentialUrl(blankToNull(in.credentialUrl()));
        c.setIssuedOn(in.issuedOn());
        c.setExpiresOn(in.expiresOn());
        c.setNotes(blankToNull(in.notes()));
        c.setRefs(in.refs() == null ? List.of() : in.refs().stream()
                .filter(r -> progression.refName(r).isPresent()).distinct().toList());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
