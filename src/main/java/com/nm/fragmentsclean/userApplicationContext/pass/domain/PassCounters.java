package com.nm.fragmentsclean.userApplicationContext.pass.domain;

public record PassCounters(int publishedExperiences, int distinctExperiencedCoffees, int validatedTickets) {
    public PassCounters {
        if (publishedExperiences < 0 || distinctExperiencedCoffees < 0 || validatedTickets < 0) {
            throw new IllegalArgumentException("Pass counters cannot be negative");
        }
    }
}
