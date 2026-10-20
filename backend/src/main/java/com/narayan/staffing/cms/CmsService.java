package com.narayan.staffing.cms;

import com.narayan.staffing.common.ConflictException;
import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.common.PageRequests;
import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.common.SlugUtils;
import com.narayan.staffing.domain.ContactStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CmsService {

    private final WebsiteSectionRepository sections;
    private final ContentBlockRepository blocks;
    private final ServiceOfferingRepository services;
    private final WebsiteClientRepository clients;
    private final GalleryItemRepository gallery;
    private final TestimonialRepository testimonials;
    private final IndustryRepository industries;
    private final ContactMessageRepository messages;
    private final SiteSettingRepository settings;

    @Transactional(readOnly = true)
    public Map<String, Object> home() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("site", site());
        body.put("sections", sections.findByPageKeyOrderByDisplayOrderAsc("HOME").stream().filter(WebsiteSection::isActive).toList());
        body.put("services", services.findByActiveTrueOrderByDisplayOrderAsc());
        body.put("why", blocks.findByGroupKeyAndActiveTrueOrderByDisplayOrderAsc("WHY_CHOOSE_US"));
        body.put("stats", blocks.findByGroupKeyAndActiveTrueOrderByDisplayOrderAsc("STATS"));
        body.put("industries", industries.findByActiveTrueOrderByDisplayOrderAsc());
        body.put("clients", clients.findByActiveTrueOrderByDisplayOrderAsc());
        body.put("testimonials", testimonials.findByActiveTrueOrderByDisplayOrderAsc());
        body.put("cta", section("HOME", "CTA"));
        return body;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> about() {
        return Map.of(
                "site", site(),
                "sections", sections.findByPageKeyOrderByDisplayOrderAsc("ABOUT"),
                "values", blocks.findByGroupKeyAndActiveTrueOrderByDisplayOrderAsc("VALUES"),
                "process", blocks.findByGroupKeyAndActiveTrueOrderByDisplayOrderAsc("PROCESS"),
                "team", blocks.findByGroupKeyAndActiveTrueOrderByDisplayOrderAsc("TEAM"),
                "trust", blocks.findByGroupKeyAndActiveTrueOrderByDisplayOrderAsc("TRUST"),
                "stats", blocks.findByGroupKeyAndActiveTrueOrderByDisplayOrderAsc("STATS")
        );
    }

    @Transactional(readOnly = true)
    public Map<String, String> site() {
        Map<String, String> map = new LinkedHashMap<>();
        settings.findAllByOrderBySettingKeyAsc().forEach(setting -> map.put(setting.getSettingKey(), setting.getSettingValue()));
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> layout() {
        return Map.of(
                "site", site(),
                "nav", blocks.findByGroupKeyAndActiveTrueOrderByDisplayOrderAsc("NAV"),
                "footer", section("FOOTER", "BLURB"),
                "services", services.findByActiveTrueOrderByDisplayOrderAsc()
        );
    }

    @Transactional(readOnly = true)
    public WebsiteSection page(String pageKey, String sectionKey) {
        return section(pageKey, sectionKey);
    }

    @Transactional
    public ContactMessage contact(ContactRequest request) {
        ContactMessage message = new ContactMessage();
        message.setName(request.name().trim());
        message.setEmail(request.email().trim().toLowerCase());
        message.setPhone(request.phone());
        message.setCompany(request.company());
        message.setSubject(request.subject().trim());
        message.setMessage(request.message().trim());
        message.setStatus(ContactStatus.NEW);
        return messages.save(message);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactMessage> messages(String q, ContactStatus status, int page, int size) {
        var pageable = PageRequests.of(page, size, "createdAt", "desc", Set.of("createdAt", "status"), "createdAt");
        return PageResponse.from(messages.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("email")), like), cb.like(cb.lower(root.get("subject")), like)));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable));
    }

    @Transactional
    public ContactMessage updateMessage(java.util.UUID id, ContactStatus status) {
        ContactMessage message = messages.findById(id).orElseThrow(() -> new NotFoundException("Message not found"));
        message.setStatus(status);
        return message;
    }

    @Transactional
    public WebsiteSection saveSection(WebsiteSection incoming) {
        WebsiteSection section = incoming.getId() == null
                ? sections.findByPageKeyAndSectionKey(incoming.getPageKey(), incoming.getSectionKey()).orElseGet(WebsiteSection::new)
                : sections.findById(incoming.getId()).orElseThrow(() -> new NotFoundException("Section not found"));
        section.setPageKey(incoming.getPageKey());
        section.setSectionKey(incoming.getSectionKey());
        section.setTitle(incoming.getTitle());
        section.setSubtitle(incoming.getSubtitle());
        section.setContent(incoming.getContent());
        section.setImagePath(incoming.getImagePath());
        section.setCtaLabel(incoming.getCtaLabel());
        section.setCtaUrl(incoming.getCtaUrl());
        section.setSecondaryCtaLabel(incoming.getSecondaryCtaLabel());
        section.setSecondaryCtaUrl(incoming.getSecondaryCtaUrl());
        section.setDisplayOrder(incoming.getDisplayOrder());
        section.setActive(incoming.isActive());
        return sections.save(section);
    }

    @Transactional(readOnly = true)
    public List<WebsiteSection> sections(String pageKey) {
        return pageKey == null ? sections.findAll() : sections.findByPageKeyOrderByDisplayOrderAsc(pageKey);
    }

    @Transactional
    public ContentBlock saveBlock(ContentBlock incoming) {
        ContentBlock block = incoming.getId() == null ? new ContentBlock() : blocks.findById(incoming.getId()).orElseThrow(() -> new NotFoundException("Block not found"));
        block.setGroupKey(incoming.getGroupKey());
        block.setTitle(incoming.getTitle());
        block.setSubtitle(incoming.getSubtitle());
        block.setBody(incoming.getBody());
        block.setIcon(incoming.getIcon());
        block.setImagePath(incoming.getImagePath());
        block.setLinkUrl(incoming.getLinkUrl());
        block.setMetricValue(incoming.getMetricValue());
        block.setDisplayOrder(incoming.getDisplayOrder());
        block.setActive(incoming.isActive());
        return blocks.save(block);
    }

    @Transactional(readOnly = true)
    public List<ContentBlock> blocks(String groupKey) {
        return groupKey == null ? blocks.findAll() : blocks.findByGroupKeyOrderByDisplayOrderAsc(groupKey);
    }

    @Transactional
    public void deleteBlock(java.util.UUID id) {
        blocks.delete(blocks.findById(id).orElseThrow(() -> new NotFoundException("Block not found")));
    }

    @Transactional
    public ServiceOffering saveService(ServiceOffering incoming) {
        ServiceOffering service = incoming.getId() == null ? new ServiceOffering() : services.findById(incoming.getId()).orElseThrow(() -> new NotFoundException("Service not found"));
        String slug = incoming.getSlug() == null || incoming.getSlug().isBlank() ? SlugUtils.slugify(incoming.getTitle()) : SlugUtils.slugify(incoming.getSlug());
        services.findBySlug(slug).filter(existing -> service.getId() == null || !existing.getId().equals(service.getId())).ifPresent(existing -> {
            throw new ConflictException("That service slug is already used.");
        });
        service.setTitle(incoming.getTitle());
        service.setSlug(slug);
        service.setShortDescription(incoming.getShortDescription());
        service.setDetailedDescription(incoming.getDetailedDescription());
        service.setIcon(incoming.getIcon());
        service.setBannerImage(incoming.getBannerImage());
        service.setActive(incoming.isActive());
        service.setDisplayOrder(incoming.getDisplayOrder());
        service.setSeoTitle(incoming.getSeoTitle());
        service.setSeoDescription(incoming.getSeoDescription());
        return services.save(service);
    }

    @Transactional
    public void deleteService(java.util.UUID id) {
        services.delete(services.findById(id).orElseThrow(() -> new NotFoundException("Service not found")));
    }

    @Transactional(readOnly = true)
    public List<ServiceOffering> services(boolean publicOnly) {
        return publicOnly ? services.findByActiveTrueOrderByDisplayOrderAsc() : services.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public ServiceOffering service(String slug) {
        return services.findBySlug(slug).filter(ServiceOffering::isActive).orElseThrow(() -> new NotFoundException("Service not found"));
    }

    @Transactional
    public WebsiteClient saveClient(WebsiteClient incoming) {
        WebsiteClient client = incoming.getId() == null ? new WebsiteClient() : clients.findById(incoming.getId()).orElseThrow(() -> new NotFoundException("Client not found"));
        client.setName(incoming.getName());
        client.setLogoPath(incoming.getLogoPath());
        client.setWebsite(incoming.getWebsite());
        client.setDisplayOrder(incoming.getDisplayOrder());
        client.setActive(incoming.isActive());
        return clients.save(client);
    }

    @Transactional
    public void deleteClient(java.util.UUID id) {
        clients.delete(clients.findById(id).orElseThrow(() -> new NotFoundException("Client not found")));
    }

    @Transactional(readOnly = true)
    public List<WebsiteClient> clients(boolean publicOnly) {
        return publicOnly ? clients.findByActiveTrueOrderByDisplayOrderAsc() : clients.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public GalleryItem saveGallery(GalleryItem incoming) {
        GalleryItem item = incoming.getId() == null ? new GalleryItem() : gallery.findById(incoming.getId()).orElseThrow(() -> new NotFoundException("Gallery item not found"));
        item.setTitle(incoming.getTitle());
        item.setDescription(incoming.getDescription());
        item.setCategory(incoming.getCategory());
        item.setImagePath(incoming.getImagePath());
        item.setActive(incoming.isActive());
        item.setDisplayOrder(incoming.getDisplayOrder());
        return gallery.save(item);
    }

    @Transactional
    public void deleteGallery(java.util.UUID id) {
        gallery.delete(gallery.findById(id).orElseThrow(() -> new NotFoundException("Gallery item not found")));
    }

    @Transactional(readOnly = true)
    public List<GalleryItem> gallery(boolean publicOnly) {
        return publicOnly ? gallery.findByActiveTrueOrderByDisplayOrderAsc() : gallery.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public Testimonial saveTestimonial(Testimonial incoming) {
        Testimonial item = incoming.getId() == null ? new Testimonial() : testimonials.findById(incoming.getId()).orElseThrow(() -> new NotFoundException("Testimonial not found"));
        item.setAuthorName(incoming.getAuthorName());
        item.setAuthorRole(incoming.getAuthorRole());
        item.setCompanyName(incoming.getCompanyName());
        item.setQuote(incoming.getQuote());
        item.setPhotoPath(incoming.getPhotoPath());
        item.setDisplayOrder(incoming.getDisplayOrder());
        item.setActive(incoming.isActive());
        return testimonials.save(item);
    }

    @Transactional
    public void deleteTestimonial(java.util.UUID id) {
        testimonials.delete(testimonials.findById(id).orElseThrow(() -> new NotFoundException("Testimonial not found")));
    }

    @Transactional(readOnly = true)
    public List<Testimonial> testimonials(boolean publicOnly) {
        return publicOnly ? testimonials.findByActiveTrueOrderByDisplayOrderAsc() : testimonials.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public Industry saveIndustry(Industry incoming) {
        Industry industry = incoming.getId() == null ? new Industry() : industries.findById(incoming.getId()).orElseThrow(() -> new NotFoundException("Industry not found"));
        String slug = SlugUtils.slugify(incoming.getName());
        industries.findBySlug(slug).filter(existing -> industry.getId() == null || !existing.getId().equals(industry.getId())).ifPresent(existing -> {
            throw new ConflictException("That industry already exists.");
        });
        industry.setName(incoming.getName());
        industry.setSlug(slug);
        industry.setDescription(incoming.getDescription());
        industry.setIcon(incoming.getIcon());
        industry.setDisplayOrder(incoming.getDisplayOrder());
        industry.setActive(incoming.isActive());
        return industries.save(industry);
    }

    @Transactional
    public void deleteIndustry(java.util.UUID id) {
        industries.delete(industries.findById(id).orElseThrow(() -> new NotFoundException("Industry not found")));
    }

    @Transactional(readOnly = true)
    public List<Industry> industries(boolean publicOnly) {
        return publicOnly ? industries.findByActiveTrueOrderByDisplayOrderAsc() : industries.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public Map<String, String> saveSettings(Map<String, String> values) {
        values.forEach((key, value) -> {
            SiteSetting setting = settings.findBySettingKey(key).orElseGet(SiteSetting::new);
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            settings.save(setting);
        });
        return site();
    }

    private WebsiteSection section(String page, String key) {
        return sections.findByPageKeyAndSectionKey(page, key).orElse(null);
    }

    public record ContactRequest(@NotBlank String name, @NotBlank @Email String email, String phone, String company, @NotBlank String subject, @NotBlank String message) {
    }
}
