package com.narayan.staffing.cms;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CmsController {

    private final CmsService cmsService;
    private final com.narayan.staffing.document.DocumentService documentService;

    @PostMapping(value = "/api/cms/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public Map<String, String> media(@RequestPart("file") MultipartFile file) {
        var document = documentService.store("CMS", UUID.randomUUID(), "IMAGE", file, true, true);
        return Map.of("id", document.getId().toString(), "url", "/api/public/media/" + document.getId());
    }

    @GetMapping("/api/public/home")
    public Map<String, Object> home() {
        return cmsService.home();
    }

    @GetMapping("/api/public/about")
    public Map<String, Object> about() {
        return cmsService.about();
    }

    @GetMapping("/api/public/layout")
    public Map<String, Object> layout() {
        return cmsService.layout();
    }

    @GetMapping("/api/public/services")
    public List<ServiceOffering> services() {
        return cmsService.services(true);
    }

    @GetMapping("/api/public/services/{slug}")
    public ServiceOffering service(@PathVariable String slug) {
        return cmsService.service(slug);
    }

    @GetMapping("/api/public/clients")
    public List<WebsiteClient> clients() {
        return cmsService.clients(true);
    }

    @GetMapping("/api/public/gallery")
    public List<GalleryItem> gallery() {
        return cmsService.gallery(true);
    }

    @GetMapping("/api/public/industries")
    public List<Industry> industries() {
        return cmsService.industries(true);
    }

    @GetMapping("/api/public/pages/{pageKey}/{sectionKey}")
    public WebsiteSection page(@PathVariable String pageKey, @PathVariable String sectionKey) {
        return cmsService.page(pageKey, sectionKey);
    }

    @PostMapping("/api/public/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactMessage contact(@Valid @RequestBody CmsService.ContactRequest request) {
        return cmsService.contact(request);
    }

    @GetMapping("/api/cms/sections")
    @PreAuthorize("hasAuthority('CMS_READ')")
    public List<WebsiteSection> sections(@RequestParam(required = false) String pageKey) {
        return cmsService.sections(pageKey);
    }

    @PutMapping("/api/cms/sections")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public WebsiteSection saveSection(@RequestBody WebsiteSection section) {
        return cmsService.saveSection(section);
    }

    @GetMapping("/api/cms/blocks")
    @PreAuthorize("hasAuthority('CMS_READ')")
    public List<ContentBlock> blocks(@RequestParam(required = false) String groupKey) {
        return cmsService.blocks(groupKey);
    }

    @PutMapping("/api/cms/blocks")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public ContentBlock saveBlock(@RequestBody ContentBlock block) {
        return cmsService.saveBlock(block);
    }

    @DeleteMapping("/api/cms/blocks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public void deleteBlock(@PathVariable UUID id) {
        cmsService.deleteBlock(id);
    }

    @GetMapping("/api/services")
    @PreAuthorize("hasAuthority('CMS_READ')")
    public List<ServiceOffering> adminServices() {
        return cmsService.services(false);
    }

    @PostMapping("/api/services")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public ServiceOffering createService(@RequestBody ServiceOffering service) {
        service.setId(null);
        return cmsService.saveService(service);
    }

    @PutMapping("/api/services/{id}")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public ServiceOffering updateService(@PathVariable UUID id, @RequestBody ServiceOffering service) {
        service.setId(id);
        return cmsService.saveService(service);
    }

    @DeleteMapping("/api/services/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public void deleteService(@PathVariable UUID id) {
        cmsService.deleteService(id);
    }

    @GetMapping("/api/clients")
    @PreAuthorize("hasAuthority('CMS_READ')")
    public List<WebsiteClient> adminClients() {
        return cmsService.clients(false);
    }

    @PostMapping("/api/clients")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public WebsiteClient createClient(@RequestBody WebsiteClient client) {
        client.setId(null);
        return cmsService.saveClient(client);
    }

    @PutMapping("/api/clients/{id}")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public WebsiteClient updateClient(@PathVariable UUID id, @RequestBody WebsiteClient client) {
        client.setId(id);
        return cmsService.saveClient(client);
    }

    @DeleteMapping("/api/clients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public void deleteClient(@PathVariable UUID id) {
        cmsService.deleteClient(id);
    }

    @GetMapping("/api/gallery")
    @PreAuthorize("hasAuthority('CMS_READ')")
    public List<GalleryItem> adminGallery() {
        return cmsService.gallery(false);
    }

    @PostMapping("/api/gallery")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public GalleryItem createGallery(@RequestBody GalleryItem item) {
        item.setId(null);
        return cmsService.saveGallery(item);
    }

    @PutMapping("/api/gallery/{id}")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public GalleryItem updateGallery(@PathVariable UUID id, @RequestBody GalleryItem item) {
        item.setId(id);
        return cmsService.saveGallery(item);
    }

    @DeleteMapping("/api/gallery/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public void deleteGallery(@PathVariable UUID id) {
        cmsService.deleteGallery(id);
    }

    @GetMapping("/api/testimonials")
    @PreAuthorize("hasAuthority('CMS_READ')")
    public List<Testimonial> testimonials() {
        return cmsService.testimonials(false);
    }

    @PostMapping("/api/testimonials")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public Testimonial createTestimonial(@RequestBody Testimonial item) {
        item.setId(null);
        return cmsService.saveTestimonial(item);
    }

    @PutMapping("/api/testimonials/{id}")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public Testimonial updateTestimonial(@PathVariable UUID id, @RequestBody Testimonial item) {
        item.setId(id);
        return cmsService.saveTestimonial(item);
    }

    @DeleteMapping("/api/testimonials/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public void deleteTestimonial(@PathVariable UUID id) {
        cmsService.deleteTestimonial(id);
    }

    @GetMapping("/api/industries")
    @PreAuthorize("hasAuthority('CMS_READ')")
    public List<Industry> adminIndustries() {
        return cmsService.industries(false);
    }

    @PostMapping("/api/industries")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public Industry createIndustry(@RequestBody Industry industry) {
        industry.setId(null);
        return cmsService.saveIndustry(industry);
    }

    @PutMapping("/api/industries/{id}")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public Industry updateIndustry(@PathVariable UUID id, @RequestBody Industry industry) {
        industry.setId(id);
        return cmsService.saveIndustry(industry);
    }

    @DeleteMapping("/api/industries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public void deleteIndustry(@PathVariable UUID id) {
        cmsService.deleteIndustry(id);
    }

    @GetMapping("/api/cms/settings")
    @PreAuthorize("hasAuthority('CMS_READ') or hasAuthority('SETTINGS_MANAGE')")
    public Map<String, String> settings() {
        return cmsService.site();
    }

    @PutMapping("/api/cms/settings")
    @PreAuthorize("hasAuthority('CMS_WRITE') or hasAuthority('SETTINGS_MANAGE')")
    public Map<String, String> saveSettings(@RequestBody Map<String, String> values) {
        return cmsService.saveSettings(values);
    }

    @GetMapping("/api/contact-messages")
    @PreAuthorize("hasAuthority('CMS_READ')")
    public com.narayan.staffing.common.PageResponse<ContactMessage> messages(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) com.narayan.staffing.domain.ContactStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return cmsService.messages(q, status, page, size);
    }

    @PatchMapping("/api/contact-messages/{id}")
    @PreAuthorize("hasAuthority('CMS_WRITE')")
    public ContactMessage updateMessage(@PathVariable UUID id, @RequestParam com.narayan.staffing.domain.ContactStatus status) {
        return cmsService.updateMessage(id, status);
    }
}
