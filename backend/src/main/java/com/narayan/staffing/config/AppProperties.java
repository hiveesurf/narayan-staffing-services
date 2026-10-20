package com.narayan.staffing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String publicBaseUrl = "http://localhost:5173";
    private Jwt jwt = new Jwt();
    private Storage storage = new Storage();
    private Cors cors = new Cors();
    private Mail mail = new Mail();
    private Security security = new Security();
    private Bootstrap bootstrap = new Bootstrap();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessExpiration = 900_000L;
        private long refreshExpiration = 1_209_600_000L;
    }

    @Getter
    @Setter
    public static class Storage {
        private String path = "./storage";
    }

    @Getter
    @Setter
    public static class Cors {
        private String origins = "http://localhost:5173";
    }

    @Getter
    @Setter
    public static class Mail {
        private boolean enabled;
        private String from = "hr@narayanstaffingservices.com";
    }

    @Getter
    @Setter
    public static class Security {
        private String sensitiveDataKey;
    }

    @Getter
    @Setter
    public static class Bootstrap {
        private String adminEmail;
        private String adminPassword;
        private String adminName = "Narayan Admin";
        private boolean seedSampleData;
        private List<SampleUser> sampleUsers = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class SampleUser {
        private String email;
        private String password;
        private String name;
        private String role;
    }
}
