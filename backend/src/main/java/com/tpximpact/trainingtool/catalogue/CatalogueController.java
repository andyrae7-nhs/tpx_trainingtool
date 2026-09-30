package com.tpximpact.trainingtool.catalogue;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogue")
public class CatalogueController {

    private final CatalogueService catalogue;

    public CatalogueController(CatalogueService catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping
    public List<Resource> search(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) String tag) {
        return catalogue.search(q, type, tag);
    }

    @GetMapping("/tags")
    public List<String> tags() {
        return catalogue.tags();
    }

    @GetMapping("/{id}")
    public Resource get(@PathVariable String id) {
        return catalogue.get(id);
    }
}
