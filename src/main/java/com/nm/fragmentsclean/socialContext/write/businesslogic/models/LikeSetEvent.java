package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record LikeSetEvent(
        UUID eventId,        // identifiant unique de l’événement
        String commandId,      // pour corréler avec la commande envoyée depuis l’Outbox front
        UUID likeId,         // agrégat (généré côté front)
        UUID userId,
        UUID targetId,
        boolean active,      // état serveur après traitement : true = LIKE, false = UNLIKE
        long count,          // total des likes serveur pour ce target
        long version,        // version serveur du like (si tu l'ajoutes dans l’agrégat plus tard)
        Instant occurredAt,  // horodatage serveur (source de vérité temporelle)
        Instant clientAt     // horodatage client envoyé dans la commande (optionnel mais utile pour debug / résolution)
) implements DomainEvent {}
