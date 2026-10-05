package com.nm.fragmentsclean.adminImportContextTest.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.authenticationContextTest.e2e.AbstractBaseE2E;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.security.JdbcAdminAuditLogRepository;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchAdminAuditQuery;

class StudioAuditVolumeIT extends AbstractBaseE2E {
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Test void selective_history_reads_do_not_scan_the_entire_journal() throws Exception {
        UUID common=UUID.fromString("11111111-1111-4111-8111-111111111111");
        UUID rare=UUID.fromString("22222222-2222-4222-8222-222222222222");
        UUID command=UUID.fromString("33333333-3333-4333-8333-333333333333");
        for(var actor:java.util.List.of(common,rare)) jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now())",actor,actor.toString());
        jdbc.update("""
            INSERT INTO admin_audit_log(id,actor_user_id,action,target_type,target_id,command_id,outcome,occurred_at)
            SELECT md5(n::text)::uuid, CASE WHEN n<=50 THEN ?::uuid ELSE ?::uuid END,
                   'COFFEE_IMPORTED','COFFEE',md5((n%100)::text)::uuid,
                   CASE WHEN n<=50 THEN ?::uuid ELSE NULL END,'APPLIED',
                   timestamptz '2026-10-05 00:00:00+00' + n * interval '1 microsecond'
            FROM generate_series(1,100000) n
            """,rare,common,command);
        jdbc.execute("ANALYZE admin_audit_log");
        var plans=new LinkedHashMap<String,JsonNode>();
        var observing=Mockito.spy(jdbc);
        Mockito.doAnswer(call -> {
            String sql=call.getArgument(0);Object[] args=(Object[])call.getRawArguments()[2];
            String plan=jdbc.queryForObject("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + sql,String.class,args);
            plans.put("query-"+plans.size(),json.readTree(plan).get(0));
            return call.callRealMethod();
        }).when(observing).query(anyString(),any(RowMapper.class),any(Object[].class));
        var repository=new JdbcAdminAuditLogRepository(observing);
        var actorPage=repository.search(new SearchAdminAuditQuery(null,null,rare,null,null,null,null,30));
        var commandPage=repository.search(new SearchAdminAuditQuery(null,null,null,command,null,null,null,30));
        assertThat(actorPage.items()).hasSize(30);assertThat(commandPage.items()).hasSize(30);
        assertThat(actorPage.nextCursor()).isNotNull();assertThat(commandPage.nextCursor()).isNotNull();
        assertThat(plans).hasSize(2);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/studio-audit-volume.json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(plans));
        // Work measured in rows, not a brittle wall-clock threshold.
        for(var plan:plans.values()) assertThat(removedRows(plan.path("Plan"))).as(plan.toString()).isLessThan(500);
    }
    private static long removedRows(JsonNode node) {
        long removed=node.path("Rows Removed by Filter").asLong()*node.path("Actual Loops").asLong(1);
        for(var child:node.path("Plans")) removed+=removedRows(child);
        return removed;
    }
}
