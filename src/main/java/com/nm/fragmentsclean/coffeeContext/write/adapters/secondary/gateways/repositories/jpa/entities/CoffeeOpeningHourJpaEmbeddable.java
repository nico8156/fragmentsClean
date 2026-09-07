package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CoffeeOpeningHourJpaEmbeddable {
    @Column(name = "day_code", nullable = false)
    private int dayCode;
    @Column(name = "start_minute", nullable = false)
    private int startMinute;
    @Column(name = "end_minute", nullable = false)
    private int endMinute;

    protected CoffeeOpeningHourJpaEmbeddable() { }

    public CoffeeOpeningHourJpaEmbeddable(int dayCode, int startMinute, int endMinute) {
        this.dayCode = dayCode;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    public int dayCode() { return dayCode; }
    public int startMinute() { return startMinute; }
    public int endMinute() { return endMinute; }
}
