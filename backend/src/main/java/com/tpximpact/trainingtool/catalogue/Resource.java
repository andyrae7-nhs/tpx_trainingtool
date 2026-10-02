package com.tpximpact.trainingtool.catalogue;

import java.util.List;

/** A training resource in the catalogue (course, book, event, programme or article). */
public record Resource(String id, String title, String type, String provider, String url, String description,
                       List<String> tags, String level, String duration, String cost, String format) {}
