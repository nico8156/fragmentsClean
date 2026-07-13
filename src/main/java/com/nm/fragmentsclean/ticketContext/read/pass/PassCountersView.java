package com.nm.fragmentsclean.ticketContext.read.pass;

public record PassCountersView(
        int validatedTickets,
        int publishedComments,
        int confirmedLikes) {
}
