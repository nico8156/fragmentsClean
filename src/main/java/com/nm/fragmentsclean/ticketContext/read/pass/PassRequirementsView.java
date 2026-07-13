package com.nm.fragmentsclean.ticketContext.read.pass;

public record PassRequirementsView(
        Integer validatedTickets,
        Integer publishedComments,
        Integer confirmedLikes) {
}
