package com.nuvik.litebansreborn.antivpn;

/**
 * Represents the result of a VPN/Proxy check
 */
public class VPNResult {

    private final String ip;
    private final boolean isVPN;
    private final boolean isProxy;
    private final boolean isHosting;
    private final boolean isTor;
    private final String vpnProvider;
    private final String isp;
    private final String country;
    private final String countryCode;
    private final String city;
    private final String realIP;  // Detected real IP behind VPN (if available)
    private final String asn;
    private final String org;
    private final double riskScore;
    private final long checkTime;
    private final String apiProvider;

    public VPNResult(Builder builder) {
        this.ip = builder.ip;
        this.isVPN = builder.isVPN;
        this.isProxy = builder.isProxy;
        this.isHosting = builder.isHosting;
        this.isTor = builder.isTor;
        this.vpnProvider = builder.vpnProvider;
        this.isp = builder.isp;
        this.country = builder.country;
        this.countryCode = builder.countryCode;
        this.city = builder.city;
        this.realIP = builder.realIP;
        this.asn = builder.asn;
        this.org = builder.org;
        this.riskScore = builder.riskScore;
        this.checkTime = System.currentTimeMillis();
        this.apiProvider = builder.apiProvider;
    }

    // Getters
    public String getIp() { return ip; }
    public boolean isVPN() { return isVPN; }
    public boolean isProxy() { return isProxy; }
    public boolean isHosting() { return isHosting; }
    public boolean isTor() { return isTor; }
    public String getVpnProvider() { return vpnProvider; }
    public String getIsp() { return isp; }
    public String getCountry() { return country; }
    public String getCountryCode() { return countryCode; }
    public String getCity() { return city; }
    public String getRealIP() { return realIP; }
    public String getAsn() { return asn; }
    public String getOrg() { return org; }
    public double getRiskScore() { return riskScore; }
    public long getCheckTime() { return checkTime; }
    public String getApiProvider() { return apiProvider; }

    /**
     * Check if this is a potentially dangerous connection
     * (VPN, Proxy, Hosting, or Tor)
     */
    public boolean isDangerous() {
        return isVPN || isProxy || isHosting || isTor;
    }

    /**
     * Get a friendly type description
     */
    public String getType() {
        if (isTor) return "Tor Exit Node";
        if (isVPN) return "VPN";
        if (isProxy) return "Proxy";
        if (isHosting) return "Hosting/Datacenter";
        return "Clean";
    }

    /**
     * Get the service provider name (VPN provider or ISP)
     */
    public String getServiceName() {
        if (vpnProvider != null && !vpnProvider.isEmpty()) {
            return vpnProvider;
        }
        if (org != null && !org.isEmpty()) {
            return org;
        }
        return isp != null ? isp : "Unknown";
    }

    @Override
    public String toString() {
        return String.format(
                "VPNResult{ip='%s', isVPN=%s, isProxy=%s, type='%s', provider='%s', country='%s', riskScore=%.2f}",
                ip, isVPN, isProxy, getType(), getServiceName(), country, riskScore
        );
    }

    /**
     * Builder pattern for VPNResult
     */
    public static class Builder {
        private String ip;
        private boolean isVPN = false;
        private boolean isProxy = false;
        private boolean isHosting = false;
        private boolean isTor = false;
        private String vpnProvider = "";
        private String isp = "";
        private String country = "";
        private String countryCode = "";
        private String city = "";
        private String realIP = "";
        private String asn = "";
        private String org = "";
        private double riskScore = 0.0;
        private String apiProvider = "";

        public Builder(String ip) {
            this.ip = ip;
        }

        public Builder isVPN(boolean isVPN) { this.isVPN = isVPN; return this; }
        public Builder isProxy(boolean isProxy) { this.isProxy = isProxy; return this; }
        public Builder isHosting(boolean isHosting) { this.isHosting = isHosting; return this; }
        public Builder isTor(boolean isTor) { this.isTor = isTor; return this; }
        public Builder vpnProvider(String vpnProvider) { this.vpnProvider = vpnProvider != null ? vpnProvider : ""; return this; }
        public Builder isp(String isp) { this.isp = isp != null ? isp : ""; return this; }
        public Builder country(String country) { this.country = country != null ? country : ""; return this; }
        public Builder countryCode(String countryCode) { this.countryCode = countryCode != null ? countryCode : ""; return this; }
        public Builder city(String city) { this.city = city != null ? city : ""; return this; }
        public Builder realIP(String realIP) { this.realIP = realIP != null ? realIP : ""; return this; }
        public Builder asn(String asn) { this.asn = asn != null ? asn : ""; return this; }
        public Builder org(String org) { this.org = org != null ? org : ""; return this; }
        public Builder riskScore(double riskScore) { this.riskScore = riskScore; return this; }
        public Builder apiProvider(String apiProvider) { this.apiProvider = apiProvider != null ? apiProvider : ""; return this; }

        public VPNResult build() {
            return new VPNResult(this);
        }
    }

    /**
     * Create a result for when the check couldn't be performed
     */
    public static VPNResult unknown(String ip) {
        return new Builder(ip)
                .riskScore(-1)
                .apiProvider("none")
                .build();
    }

    /**
     * Create a clean result (no VPN detected)
     */
    public static VPNResult clean(String ip, String apiProvider) {
        return new Builder(ip)
                .apiProvider(apiProvider)
                .build();
    }
}
