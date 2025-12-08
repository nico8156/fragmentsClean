    package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.controllers;

    import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;

    import com.nm.fragmentsclean.socialContext.read.projections.GetLikeStatusQuery;
    import com.nm.fragmentsclean.socialContext.read.projections.LikeStatusView;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    import java.util.UUID;

    @RestController
    @RequestMapping("/api/social/targets")
    public class ReadLikeController {

        private final QueryBus querryBus;

        public ReadLikeController(QueryBus querryBus) {
            this.querryBus = querryBus;
        }

        @GetMapping("/{targetId}/likes")
        public ResponseEntity<LikeStatusView> getLikeStatus(@PathVariable String targetId) {
            var query = new GetLikeStatusQuery(UUID.fromString(targetId));
            LikeStatusView view = querryBus.dispatch(query);
            return ResponseEntity.ok(view);
        }
    }
