# Carrousel éditorial — diagnostic du 16 septembre 2026

## Symptôme et preuve de cause

TestFlight ne montre qu'un article à la une, bien que Studio indique cinq places
occupées. Diagnostic staging en lecture seule, sans modification des contenus :

- `/api/articles?locale=fr-FR&limit=100` : six publications, un seul rang (3).
- `/api/articles?locale=fr&limit=50` : quatre publications, rangs 1, 2, 4 et 5.
- SQL `BEGIN READ ONLY` : les cinq rangs sont identiques côté agrégat et projection,
  avec mêmes versions. Pas de retard de projection expliquant cette disparition.
- Image backend observée : `sha-e5c587a4cc2ba0d87b15aa5207309d620856345f`.
  La correction de concurrence précédente n'est donc pas encore déployée.
- Diagnostic SSM principal : `7cf6f091-4914-4e34-80e5-ec604ad30e3e`.

| Rang | Article publié | Locale stockée |
| --- | --- | --- |
| 1 | La Soupe et l'Aeropress : Similitudes et Différences | fr |
| 2 | Comment éviter un mauvais café filtre | fr |
| 3 | L'histoire fascinante du café : légendes et vérités | fr-FR |
| 4 | Pourquoi le café vous pousse à aller aux toilettes ? | fr |
| 5 | La Fermentation du Café : Un Voyage au Cœur du Goût | fr |

La lecture SQL filtrait par égalité exacte de locale. Les écritures acceptaient
la forme courte sans la normaliser ; le client mobile utilise le contrat fr-FR.
Le problème n'est ni un contenu mobile fictif ni une sauvegarde des rangs perdue.
Le rang 3 apparaît seul car c'est le seul publié sous la locale demandée.

## Autre dette mise au jour : seeds publics sans propriétaire éditorial

Cinq articles sont présents uniquement dans `articles_projection`, sans ligne
`articles` correspondante (SSM `897ccaba-081b-44a6-b009-48f67290baf6`) :

- Découvrir et choisir son café ;
- Comment préparer un bon café à la maison ;
- Cultiver le café ? ;
- Le café est-il bon pour la santé ? ;
- Qu'est-ce que le café de spécialité ?

Ce sont les anciens seeds du serveur (`ArticleReadSeedRunner`), pas des defaults
du mobile. Studio lit les articles/révisions métier et ne peut donc pas les gérer.
Décision utilisateur demandée : import éditorial conforme ou retrait public.
Aucune suppression ni migration de ces cinq contenus n'est exécutée ici.
Le bootstrap de seeds devra être traité avec cette décision pour éviter leur
réapparition en cas de catalogue vide. Ne pas considérer cette dette résolue.

## Correctifs locaux

### Backend

`ArticleLocale`, valeur du domaine article, normalise `fr` → `fr-FR` et `en` →
`en-US`. Les créations manuelles/legacy et les shells de génération utilisent la
valeur canonique ; l'événement de génération utilise la locale de l'agrégat.
L'édition considère ces alias comme la même identité, sans autoriser de changement
de langue vers une autre locale.

Les lectures liste/détail recherchent explicitement la locale canonique et son
alias historique, conservent filtre de publication/tri/pagination, et renvoient
la locale canonique attendue par le mapper mobile. Studio renvoie également la
locale canonique. Aucune réécriture SQL de contenu historique ni migration nécessaire.
Le détail privilégie la forme canonique si un même slug historique est ambigu.
Les autres locales ne sont pas fusionnées (notamment pas tout préfixe `fr-*`).

Le read side ne charge ni agrégat ni command handler : seul le value object pur
du même bounded context est partagé. Aucun nouveau flux inter-BC ni parsing de
transport dans l'UI. Une ancienne ligne SQL `fr` peut subsister ; la compatibilité
de lecture est volontaire et couverte en test, pas une migration silencieuse.

### Mobile

Le carrousel reçoit bien la sélection `featuredRank` fournie par Redux. FlowAtlas
a confirmé `articlesListRetrieval → articleListReceived → articleWlReducer` ; le
contrôle visuel et la règle de sélection ont été lus directement dans le code.
La cause de contenu est serveur, pas un filtrage arbitraire mobile.

`MasterHeader` : largeur réactive, pagination à largeur de page explicite, index
lié à la fin du défilement, remise à zéro lors d'une réorganisation/suppression ou
rotation. Points centrés par layout (`left/right: 0`, centrage), pas un décalage
constant prévu pour cinq points. Aucun point pour une carte unique. Indicateur non
interceptant et libellé accessible « Article N sur M ». Grand visuel et bandeau
au scroll inchangés ; aucun module natif ajouté.

### Studio

Changer la position lance immédiatement la commande existante. L'UI dit désormais
« Enregistrement automatique », distingue « Enregistrement de la position… » du
rang confirmé et explique la présence dans le carrousel ou dans Tous les articles.
Les erreurs restent visibles ; seul APPLIED permet d'annoncer un rang enregistré.

## Hiérarchie fonctionnelle conservée

- Brouillon : Studio seulement.
- Publié : catalogue mobile, indépendamment du rang.
- À la une : publié + rang 1..5, dans le carrousel par ordre croissant, et toujours
  dans le catalogue. Aucun emplacement vide visuel lorsque des rangs manquent.
- Supprimé : archive métier, absent des lectures publiques.
- À lire ensuite : trois publications récentes hors hero/à la une.

## Validation et livraison

Le test PostgreSQL a d'abord échoué avec un seul article, puis passe avec les cinq
rangs et un article ordinaire, pagination sur plusieurs pages, séparation anglais,
exclusion brouillons/archives, lecture de détail et contrat HTTP MockMvc.
Un autre test couvre persistance canonique, ancien brouillon `fr`, lecture Studio,
édition via commande avec les deux alias et publication relue par le mobile.
Tests mobile : cinq cartes, passage à la seconde, centrage, remplacement de liste,
retour à un seul élément, snapshot Redux passant d'un à cinq rangs.
Test Studio : aucun message de succès pendant la commande, feedback après APPLIED.

Validation locale : **93 suites / 353 tests mobiles**, TypeScript vert ;
**34 fichiers / 160 tests Studio**, build OAuth et vérification du bundle verts.
La première passe Studio en parallèle des suites backend/mobile a eu quatre
timeouts UI (écrans non modifiés). La suite complète relancée seule avec
`--maxWorkers=1` passe sans changement d'assertions ni augmentation de timeout.
Backend : tests ciblés domaine, génération, repository JDBC, projection structurée,
compatibilité de locale avec HTTP MockMvc et frontières d'architecture verts.
La suite backend globale et la recette physique iPhone ne sont pas attestées ici.
Pas de merge, push, déploiement ou mutation métier distante exécuté par cette correction.
Après déploiement backend, le TestFlight actuel pourra recevoir les articles via
refresh ; la correction visuelle nécessite une nouvelle distribution mobile.
Un nouveau déploiement Studio sera nécessaire pour le texte de confirmation.
Recette iPhone réelle restante : glisser sur les cinq cartes, ouvrir chacune,
rafraîchir après curation, vérifier centrage et retour du bandeau vertical.
Conservation/restauration reste en pause et hors périmètre.
