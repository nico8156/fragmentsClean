package com.nm.fragmentsclean.coffeeContext.read;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.QueryHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GetCoffeesQueryHandler implements QueryHandler<GetCoffeesQuery, CoffeeListView> {
    private final JdbcTemplate jdbcTemplate;

    public GetCoffeesQueryHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CoffeeListView handle(GetCoffeesQuery query) {

        var coffeeViews = jdbcTemplate.query(
                "SELECT * FROM coffees",
                (rs, rowNum) -> new CoffeeView(
                        rs.getObject("id", UUID.class),
                        rs.getString("google_id"),
                        rs.getString("display_name"),
                        rs.getString("formatted_address"),
                        rs.getString("national_phone_number"),
                        rs.getString("website_uri"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                )
        );
        return new CoffeeListView(coffeeViews, coffeeViews.size());
    }
}
