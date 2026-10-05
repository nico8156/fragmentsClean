# Studio release — isoler la recette de replay avatar

05/10/2026. CHORE / fixture d’intégration, sans modification produit.
La PR backend #3 a passé le scan de secrets mais échoué sur le gate de release :
849 tests, 1 échec, 0 erreur, 0 skip (run 37312208684).
AvatarMediaCatalogIT.source_replay_resumes_batches_and_uses_stable_avatar_envelopes
observait un deuxième lot de 15 lignes, alors que son assertion arbitraire
acceptait 1 à 10. Le scan est global au producteur ; PostgreSQL est partagé
entre classes d’intégration. Des avatars de fixtures précédentes entraient donc
dans le scan, indépendamment de l’utilisateur synthétique courant.

La recette supprime les anciens avatars exclusivement dans la base Testcontainers
avant de créer son inventaire de 101 lignes. Le premier lot reste exactement 100,
le curseur doit exister, le second lot est désormais exactement 1, puis 0.
Les contrôles du snapshot/enveloppe stable, destination, propriétaire et preview
restent présents. Aucune borne de production n’a été élargie ; aucun inventaire
réel supprimé ni S3 appelé par cette correction.

Les six tests AvatarMediaCatalogIT passent sur JDK 21. La nouvelle CI complète
est requise ; les 849 tests locaux verts du candidat précédent ne sont pas
présentés comme une preuve du nouveau candidat. Mutation métier non applicable
à l’isolation de fixture : code produit inchangé, RED CI réel et assertions
exactes conservées. Ce commit est distinct de la qualification des drivers :
il corrige un défaut de fixture révélé ensuite par le run CI, avec sa propre preuve.
