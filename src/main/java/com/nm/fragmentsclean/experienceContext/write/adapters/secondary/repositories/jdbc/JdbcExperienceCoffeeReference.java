package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jdbc;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceCoffeeReference;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcExperienceCoffeeReference implements ExperienceCoffeeReference{
    private final JdbcTemplate jdbc;public JdbcExperienceCoffeeReference(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override public boolean isAvailable(UUID coffeeId){return Boolean.TRUE.equals(jdbc.query("SELECT active FROM experience_coffee_references WHERE coffee_id=?",rs->rs.next()&&rs.getBoolean(1),coffeeId));}
}
