# Modération des images — 8 octobre 2026

## Livraison

Itération **BEHAVIOUR**, routes external-adapter / command-feature, workflow
de commande Studio et réconciliation mobile. Implémentation locale dans les
trois dépôts ; aucun déploiement effectué. Les tests automatisés simulent OpenAI.
Un contrôle réel ultérieur depuis le serveur staging a réussi avec une image
unie synthétique, sans envoi de photo utilisateur (voir ci-dessous).

Périmètre : nouvelles photos d’expériences et nouveaux avatars téléversés.
Les images éditoriales, les tickets, les avatars OAuth et le stock historique
ne sont pas reclassifiés. Le texte conserve sa modération existante.
Un résultat favorable ne garantit pas l’absence de contenu inadapté :
les signalements et la modération humaine restent utiles.

## Parcours livré

1. Après sélection, l’utilisateur autorise explicitement l’envoi de cette photo
   à **OpenAI** pour l’analyse de sécurité. Annuler ne prépare aucune copie
   durable et n’ajoute aucune commande d’upload.
2. Le consentement accompagne cette image dans la commande mobile hors ligne.
   Les confirmations HTTP exigent **moderationConsent: true**. Une valeur absente
   ou fausse produit **422 / IMAGE_MODERATION_CONSENT_REQUIRED** avant tout appel
   fournisseur. Une ancienne commande sans ce champ doit être recréée en
   choisissant de nouveau la photo ; aucun accord n’est inventé à la reprise.
   Ce contrôle ne concerne pas les suppressions de photo.
3. Le serveur normalise l’image (JPEG sans EXIF), transmet ses octets à
   POST https://api.openai.com/v1/moderations, modèle omni-moderation-latest.
   Aucun identifiant utilisateur, nom ou localisation n’est ajouté à la requête.
   L’image peut elle-même contenir des données personnelles.
4. Image non signalée : AVAILABLE. Image signalée : REVIEW_REQUIRED,
   sans URL dans les lectures publiques. Les champs requis de la réponse sont
   validés, y compris la couverture image des catégories sexual, violence et
   violence/graphic. Une valeur manquante ne devient pas implicitement « sûre ».
   Le signal flagged ou une catégorie vraie suffit à demander une revue ;
   aucun seuil de score maison n’a été ajouté.
5. Le Studio propose un filtre de statut, un aperçu réservé aux opérateurs,
   puis autoriser/refuser avec motif obligatoire. La décision passe par une
   commande du contexte propriétaire, avec reçu canonique et audit.
   Le catalogue est une projection, pas une source de mutation métier.
6. Une approbation rend l’image disponible. Un refus conserve l’image privée
   (REJECTED). L’ancien avatar reste actif pendant la revue ou après refus ;
   l’approbation le remplace. La revue d’un ancien avatar ne peut pas écraser
   un avatar plus récent. Supprimer son avatar annule aussi les candidats en
   attente, même si aucun avatar n’était encore actif.

Le mobile explique « en cours de validation » / « refusée » et permet la
suppression. APPLIED sur la confirmation d’upload signifie que le dépôt est
traité ; cela ne signifie pas qu’un humain a autorisé la publication.
Les reprises de la confirmation originale après approbation ou refus ne
relancent pas l’analyse. Les identités de commandes restent stables.

Le Studio n’annonce pas un succès sur le seul HTTP 202. Il interroge
/api/admin/commands/{commandId}, permet de revérifier et de renvoyer exactement
la même décision après une réponse perdue. La décision reste visible lorsqu’on
ouvre un autre média. Le suivi est en mémoire du Studio, comme les opérations
existantes ; après rechargement, consulter l’état source et l’audit.

## Fournisseur indisponible

Clé absente, 401/429/5xx, timeout, JSON invalide ou couverture image incomplète :
échec technique, photo maintenue PENDING, aucune publication ni rejet métier.
La reprise utilise l’outbox mobile existante. Connexion bornée à 5 secondes,
lecture à 15 secondes ; aucun appel fournisseur dans une transaction SQL.

L’adaptateur de stockage privé analyse les octets normalisés avant leur écriture
S3 et retourne un signal technique au contexte propriétaire. Celui-ci décide
de la transition métier. Le nom de l’objet conservé contient son SHA-256 :
les octets analysés et publiés sont les mêmes, même après modification du
fichier d’upload initial. Une panne du fournisseur ne crée aucun nouvel objet
normalisé. Le risque préexistant d’un objet orphelin si S3 réussit puis que la
transaction SQL échoue n’est pas résolu globalement ici.

Les erreurs de l’adaptateur ne journalisent ni clé, ni image, ni réponse brute.
Le profil fake dispose d’un analyseur local ; ne pas l’employer en production.

## Configuration et déploiement

- Fournir **OPENAI_API_KEY** uniquement au serveur via les secrets/runtime existants.
  Le bootstrap SSM sait déjà la charger. Sa présence dans le conteneur staging
  et son accès effectif à Moderation ont été vérifiés avec succès le 8 octobre. Ne pas placer
  la clé dans Expo, Vite, un fichier suivi ou une conversation.
  La propriété fragments.media.moderation.api-key peut surcharger cette valeur.
- Appliquer le manifeste journalisé **image-moderation-2026-10.psql**, qui nécessite
  le baseline avatar-media-lifecycle-2026-10. Le script de déploiement existant
  télécharge et rend désormais ce manifeste après les précédents. Aucun ancien
  manifeste ni checksum n’a été réécrit.
- La migration élargit les états autorisés des deux sources et des projections ;
  elle ne supprime ni ne reclassifie les lignes historiques.
- Déployer backend + Studio et distribuer le mobile mis à jour. Les anciennes
  versions ne peuvent plus confirmer de nouveaux uploads sans permission :
  leur rejet est intentionnel. Coordonner cette transition avec TestFlight.
- Publier aussi la page de confidentialité mise à jour, qui nomme OpenAI et
  décrit le partage. Vérifier les modalités effectives du compte OpenAI
  (traitement/rétention/transferts) avant activation publique ; aucune localisation
  européenne ni durée de conservation fournisseur n’est promise ici.
- Recette iPhone : autoriser/annuler, upload normal, revue/refus Studio et
  interruption réseau. L’appel OpenAI réel a été validé depuis staging avec une
  image synthétique ; le parcours complet sur iPhone reste à tester après déploiement.

Les images refusées suivent la suppression demandée par leur auteur et la
suppression de compte existantes. Cette livraison n’invente pas de durée
automatique de conservation pour les dossiers de revue.

## Preuves

Les premiers RED ont montré qu’une photo signalée devenait disponible.
Le minimum GREEN a introduit l’état privé, puis les décisions, la reprise et les
contrats de lecture. Un second RED a montré que l’absence de permission
n’empêchait pas l’analyse ; les gardes HTTP et la confirmation mobile ferment
ce parcours. Le passage de l’URL présignée aux octets normalisés découle du
contrôle de l’identité du contenu analysé.

Vérifications locales :
- Backend : 84 tests ciblés réussis (12 classes), domaine/fournisseur, correspondance octets analysés/stockés,
  panne sans écriture S3, parcours MockMvc/PostgreSQL photos et avatars,
  refus/approbation/admin-only/audit/idempotence, catalogue, cycle de vie,
  stockage LocalStack et frontières de contextes.
- Migration : exécution PostgreSQL du manifeste rendu, reprise sans double
  journal, conservation des lignes AVAILABLE/RETIRED et nouveaux états ;
  tests de sécurité du script de déploiement.
- Mobile : 105 suites / 458 tests, TypeScript et lint.
- Studio : 74 fichiers / 578 tests, build de production OAuth avec jetons publics
  vides, conformité du contrat généré et vérification des fichiers distribués.
  Le build refuse correctement les jetons de développement de la configuration
  locale ; les valeurs de production ont été fournies au processus, sans
  modifier les secrets locaux.

Checkpoint de mutations **EXÉCUTÉ**, manuel et borné, sans score global :
- Serveur : remplacer REVIEW_REQUIRED par AVAILABLE.
  ImageModerationTest.flagged_experience_photo_is_stored_but_not_available
  échoue sur le statut attendu ; deux assertions détectent la mutation.
- Mobile : supprimer le filtrage des URL privées dans le mapper.
  Les cas REVIEW_REQUIRED et REJECTED détectent l’URL indûment exposée.
- Studio : annoncer APPLIED dès HTTP 202. Les tests de statut canonique
  détectent le faux succès et le rejet masqué.
- Mobile : ignorer le refus de permission. Le test du picker détecte la copie
  durable créée malgré l’annulation.
Chaque mutation a été restaurée à l’octet puis les tests ciblés ont repassé.
Aucun mutant sélectionné ne subsiste. Aucune campagne exhaustive n’est revendiquée.

Sources : [OpenAI Moderation](https://developers.openai.com/api/docs/guides/moderation),
[Apple 5.1.2 — Data Use and Sharing](https://developer.apple.com/app-store/review/guidelines/#data-use-and-sharing).


## Vérification du secret existant et accès réel — 8 octobre 2026

À la demande de Nicolas, réutilisation autorisée de la clé serveur déjà employée
par le Studio. Contrôle opérationnel CHORE, sans modification de configuration.

- Paramètre SSM /fragments/staging/OPENAI_API_KEY présent, SecureString, version 1.
- Un seul conteneur actif de service fragments-backend sur l’hôte staging ;
  variable OPENAI_API_KEY présente et non factice dans son environnement.
- Depuis cet hôte, appel HTTPS à /v1/moderations avec cette clé et
  omni-moderation-latest : HTTP 200, verdict booléen, image synthétique non
  signalée, couverture image attendue présente.
- Image de test : carré uni 256 × 256 généré en mémoire ; aucune photo utilisateur.
  Aucune valeur de clé, empreinte de clé ou réponse fournisseur brute affichée.
- Référence du diagnostic SSM : 83203caf-0906-4a98-887f-e0bbbaa7dacc (Success).

**Aucune nouvelle clé nécessaire pour ce serveur.** Ce contrôle ne déploie pas
l’intégration : migration, livraison serveur/Studio/mobile et recette iPhone
restent à effectuer. Mutation non applicable à ce contrôle opérationnel.

## Qualification globale pendant le déploiement

La première CI globale a exécuté 891 tests et détecté deux échecs dans
StagingReleaseUpgradeIT : son manifeste de qualification explicite n’incluait
pas encore image-moderation-2026-10. Son garde d’égalité avec le script de
déploiement et la comparaison au schéma neuf ont tous deux détecté cet oubli.
Le déploiement s’est arrêté avant construction/push d’image et intervention SSM.

Correction CHORE de qualification : ajouter le nouveau driver à la liste
ordonnée du test, sans supprimer ni assouplir ses assertions. La qualification
locale StagingReleaseUpgradeIT repasse, y compris application depuis le schéma
historique, équivalence au schéma neuf et reprise idempotente. Le workflow
complet est ensuite relancé sur le commit corrigé.
