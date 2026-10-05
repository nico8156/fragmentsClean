# Studio — qualification de la chaîne de release

05/10/2026. CHORE / tests et livraison. Actualisation de deux recettes techniques,
sans modification du code produit ni des manifestes déjà appliqués.

Le premier gate complet a exécuté 848 tests : 2 échecs, 0 erreur, 0 skip.
StagingReleaseUpgradeIT assemblait neuf drivers au lieu des vingt-trois de la
procédure staging ; sa comparaison au bootstrap montrait les champs manquants.
VersionedArticleSeedImportIT attendait trois faits, sans les trois snapshots
catalogue média désormais publiés lors des transitions d’import. Les 46 tests
ciblés de la tranche précédente n’attestaient pas ce gate complet.

La recette d’upgrade utilise les 23 drivers dans l’ordre qualifié et vérifie
cet ordre contre le script de déploiement. Elle utilise le véritable renderer
et PostgreSQL 15, conserve la comparaison des colonnes au bootstrap et les
assertions de préservation des données historiques, états acquis et reçus.
Replay, concurrence, checksum/baseline et replay d’effacement sur restauration
restent testés. Les deux index audit sont inclus dans la parité d’index.
Aucune assertion globale n’a été écartée pour rendre la recette verte.

L’import vérifie les six types de faits, dont trois snapshots média, au lieu
de seulement augmenter un compteur. Les statuts canoniques et l’absence de
mutation synchrone de projection restent protégés. L’outbox stocke les classes
domaine avant traduction en types stables ; une première assertion utilisant
les types stables à cette frontière a été corrigée.

Preuves : 8 tests ciblés verts. Nouveau gate complet verify-backend-ci.sh sur
JDK 21, MAVEN_OPTS=-XX:TieredStopAtLevel=1 : 849 tests / 273 classes, 0 échec,
0 erreur, 0 skip, puis packaging réussi. Rapports frais :
target/release-verification.hCmvJJ. Scan des patterns de secrets committés vert.
Le run intermédiaire MTmSpk interrompu (565 tests rapportés) n’est pas compté
comme un gate réussi. Docker a été relancé avec l’accord de Nicolas.

Mutation métier non applicable à cette correction de recettes techniques :
aucune décision métier ni migration de production modifiée. Les RED du gate,
les assertions exactes de faits et les contrôles de parité/replay sont consignés ;
aucun score de mutation revendiqué. La suite release et le packaging locaux ne
remplacent pas les scans/gates CI de la prochaine release ni une recette serveur.

La livraison staging est autorisée par Nicolas. Aucun déploiement exécuté ici.
Le préflight observe encore l’ancienne image et trois reçus ; un propriétaire
bootstrap Google vérifié existe, mais son association au compte de Nicolas
reste à confirmer avant activation exclusive. Aucun dump réel téléchargé,
aucune donnée serveur modifiée. Le fichier personnel non suivi
docs/linkedin-editorial-backlog.md reste hors du commit.
