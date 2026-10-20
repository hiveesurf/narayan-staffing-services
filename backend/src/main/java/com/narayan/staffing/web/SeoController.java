package com.narayan.staffing.web;

import com.narayan.staffing.config.AppProperties;
import com.narayan.staffing.domain.JobStatus;
import com.narayan.staffing.job.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeoController {

    private final JobRepository jobs;
    private final com.narayan.staffing.cms.ServiceOfferingRepository services;
    private final AppProperties properties;

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return "User-agent: *\nAllow: /\nDisallow: /admin\nSitemap: " + properties.getPublicBaseUrl() + "/sitemap.xml\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        String base = properties.getPublicBaseUrl();
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : java.util.List.of("/", "/about", "/services", "/careers", "/clients", "/gallery", "/contact", "/privacy-policy", "/terms")) {
            xml.append("<url><loc>").append(base).append(path).append("</loc></url>\n");
        }
        services.findByActiveTrueOrderByDisplayOrderAsc().forEach(service ->
                xml.append("<url><loc>").append(base).append("/services/").append(service.getSlug()).append("</loc></url>\n"));
        jobs.findAll((root, query, cb) -> cb.equal(root.get("status"), JobStatus.PUBLISHED))
                .forEach(job -> xml.append("<url><loc>").append(base).append("/careers/").append(job.getSlug()).append("</loc></url>\n"));
        xml.append("</urlset>");
        return xml.toString();
    }
}
