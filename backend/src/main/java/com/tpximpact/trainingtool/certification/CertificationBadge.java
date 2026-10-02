package com.tpximpact.trainingtool.certification;

import java.time.LocalDate;

/** The short form of a certification shown next to framework items in gap analysis and the journal. */
public record CertificationBadge(Long id, String name, String issuer, String status, String expiry, LocalDate expiresOn) {

    public static CertificationBadge of(Certification c, LocalDate today) {
        return new CertificationBadge(c.getId(), c.getName(), c.getIssuer(), c.getStatus(), c.expiry(today).name(),
                c.getExpiresOn());
    }
}
