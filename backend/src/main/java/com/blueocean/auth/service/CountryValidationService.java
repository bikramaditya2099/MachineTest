package com.blueocean.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Service to validate whether a user's country is allowed to register.
 * Uses the free ip-api.com service to resolve IP addresses to countries.
 *
 * Restricted countries include those under US/EU sanctions:
 * Syria (SY), Afghanistan (AF), Iran (IR), North Korea (KP),
 * Cuba (CU), Sudan (SD), Myanmar (MM), Libya (LY), Somalia (SO), Yemen (YE).
 */
@Service
public class CountryValidationService {

    /** Comma-separated ISO country codes from application.yml */
    @Value("${app.restricted-countries}")
    private String restrictedCountriesConfig;

    /** Dev mode flag: if true, IP-based check is skipped */
    @Value("${app.dev-mode:false}")
    private boolean devMode;

    private final RestTemplate restTemplate;

    public CountryValidationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Returns the display names of all restricted countries, sorted alphabetically.
     */
    public List<String> getRestrictedCountryNames() {
        List<String> names = new ArrayList<>();
        for (String code : getRestrictedCountries()) {
            names.add(getCountryName(code));
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Returns the set of restricted country ISO codes.
     */
    private Set<String> getRestrictedCountries() {
        Set<String> codes = new HashSet<>();
        for (String code : restrictedCountriesConfig.split(",")) {
            if (!code.isBlank()) {
                codes.add(code.trim().toUpperCase());
            }
        }
        return codes;
    }

    /**
     * Checks whether the country explicitly selected by the user is restricted.
     *
     * @param countryCode ISO 3166-1 alpha-2 code chosen on the sign-up form
     * @return true if the country is on the restricted list
     */
    public boolean isCountryCodeRestricted(String countryCode) {
        return countryCode != null && getRestrictedCountries().contains(countryCode.trim().toUpperCase());
    }

    /**
     * Returns the English display name for an ISO country code.
     */
    public String getCountryName(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return "Unknown";
        }
        String name = new Locale("", countryCode.trim().toUpperCase()).getDisplayCountry(Locale.ENGLISH);
        return name.isBlank() ? countryCode : name;
    }

    /**
     * Returns true if the code is a valid ISO 3166-1 alpha-2 country code.
     */
    public boolean isValidCountryCode(String countryCode) {
        return countryCode != null
                && Arrays.asList(Locale.getISOCountries()).contains(countryCode.trim().toUpperCase());
    }

    /**
     * Returns all countries (code, name, restricted flag) sorted by name,
     * for populating the sign-up country dropdown.
     */
    public List<Map<String, Object>> getAllCountries() {
        Set<String> restricted = getRestrictedCountries();
        List<Map<String, Object>> countries = new ArrayList<>();
        for (String code : Locale.getISOCountries()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("code", code);
            entry.put("name", getCountryName(code));
            entry.put("restricted", restricted.contains(code));
            countries.add(entry);
        }
        countries.sort(Comparator.comparing(c -> (String) c.get("name")));
        return countries;
    }

    /**
     * Checks whether registration is allowed from the given IP address.
     * Returns true if allowed, false if restricted.
     *
     * @param ipAddress the client's IP address (from request)
     * @return true if country is allowed, false if blocked
     */
    public boolean isCountryAllowed(String ipAddress) {
        // Skip check in dev mode (for localhost testing)
        if (devMode || ipAddress == null || ipAddress.isBlank()) {
            return true;
        }

        // Treat localhost addresses as allowed
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1") ||
            ipAddress.equals("::1") || ipAddress.startsWith("192.168.") || ipAddress.startsWith("10.")) {
            return true;
        }

        try {
            // Call free IP geolocation API
            String url = "http://ip-api.com/json/" + ipAddress + "?fields=status,countryCode";
            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.getForObject(url, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                String countryCode = response.get("countryCode");
                return !getRestrictedCountries().contains(countryCode);
            }
        } catch (Exception e) {
            // If geolocation fails, allow by default (fail-open for availability)
            // In production, consider fail-closed
            return true;
        }

        return true;
    }

    /**
     * Returns the country name for a given IP address.
     * Returns "Unknown" if lookup fails.
     *
     * @param ipAddress the client's IP address
     * @return ISO country code or "Unknown"
     */
    public String getCountryCode(String ipAddress) {
        if (devMode || ipAddress == null || ipAddress.isBlank()) {
            return "LOCAL";
        }

        try {
            String url = "http://ip-api.com/json/" + ipAddress + "?fields=status,country,countryCode";
            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.getForObject(url, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                return response.get("countryCode");
            }
        } catch (Exception ignored) {
        }

        return "Unknown";
    }
}
