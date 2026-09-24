package edu.cit.antolijao.supplier;

import org.springframework.stereotype.Component;

@Component
class LegacySupplySession {
    private String token;
    private long issuedAtMillis;

    synchronized String getToken() {
        return token;
    }

    synchronized boolean hasToken() {
        return token != null;
    }

    synchronized void set(String token) {
        this.token = token;
        this.issuedAtMillis = System.currentTimeMillis();
    }

    synchronized void clear() {
        this.token = null;
    }

    synchronized long ageMillis() {
        return System.currentTimeMillis() - issuedAtMillis;
    }
}