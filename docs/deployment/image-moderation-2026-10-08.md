# Modération des images — 8 octobre 2026

## Livraison

Itération **BEHAVIOUR**, routes external-adapter / command-feature, workflow
de commande Studio et réconciliation mobile. Implémentation locale dans les
trois dépôts. L’état de la livraison staging et TestFlight est consigné en fin
de document. Les tests automatisés simulent OpenAI.
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


## Livraison autorisée — staging et TestFlight

Itération **CHORE**, route technical/external-adapter. Nicolas a autorisé le
déploiement ; aucune publication App Store publique n’est effectuée.

Révisions de la livraison initiale :
- Backend : `1878e5760c722fad8bc7c082d1a5bd58224f6605`.
  Backend CI `37780599886` réussie : **891 tests, 0 échec, 0 erreur, 0 ignoré**.
  Déploiement staging `37780646673` réussi, y compris le contrôle des
  vulnérabilités de l’image, la sauvegarde préalable et la santé applicative.
- Studio : `8d2b39a017814830ce20a3da15eedecfc456db1b`.
  CI `37779637109` et déploiement `37779850467` réussis ; manifeste HTTPS
  sur https://studio-staging.anchor-event.fr/release-manifest.json conforme.
- Mobile : `57ab8e410b1a9ccb990d3c0e3fed467d6e2a8271`, CI `37779588171` réussie.
  Compilation EAS `9464553c-d087-4938-a86e-aa68ece18774` FINISHED,
  **1.0.0 (11)** ; soumission `e2d2a3d7-d817-4358-8643-52eedb2e9149`
  FINISHED sans erreur. Le traitement Apple et la disponibilité aux testeurs
  ne sont pas confirmés par ce seul statut EAS.

L’IPA téléchargé a été inspecté avec `scripts/inspect-ios-ipa.mjs` :
bundle `com.nico8156.fragments`, version/build conformes, connexion Apple
présente, attachement debugger désactivé, 13 manifests de confidentialité.
SHA-256 : `78ba4821b8fb31b4a4722daefcd852cc1df85186921612eb423e8d6b5544dacf`.
Signature : `integrity-checked-local-trust-unavailable` ; le contrôle local
ne constitue pas une validation Apple. Compilation réalisée depuis un clone
propre du commit, sans les captures ou archives locales non suivies.

Confidentialité publiée et contrôlée par HTTPS sur les deux racines :
- https://fragments.anchor-event.fr/legal/confidentialite.html :
  SHA-256 `ba806514aca1ec0c192b3583c3cf97323ea822f74e5698ac33acdbc4b0182c15`,
  release `moderation-a2d72b8`, précédent lien conservé.
  SSM `19ad377f-c65c-463d-aa55-fc4cd8b61ba1` Success.
- https://fragments-staging.anchor-event.fr/legal/confidentialite.html :
  SHA-256 `50e283124565faa51d3d9708706fc2816cd4fd830959d34f0292d3cac3ae112c`,
  sauvegarde de l’ancienne page hors racine publique.
  SSM `eacecb2a-ac76-42c7-a842-bd7134cf0920` Success.

La recette iPhone reste humaine : installer le build 11, vérifier annulation
et permission OpenAI, photo normale et avatar, puis revue Studio si une image
est signalée. Ne pas déduire de l’absence de signalement qu’une revue humaine
a été exercée. Les contrôles automatisés couvrent approbation/refus et reprise.
Mutation non applicable aux seules opérations de déploiement ; preuves des
mutations comportementales ci-dessus.


### Vérification serveur après bascule

SSM `b72efe54-e583-483b-a3d1-4d0770acae4e` : Success. Le conteneur actif utilise
exactement l’image `sha-1878e5760c722fad8bc7c082d1a5bd58224f6605`.
Le journal PostgreSQL confirme `image-moderation-2026-10`, source identique,
appliquée le 8 octobre à 13:11:21 UTC. Les quatre contraintes attendues
acceptent REVIEW_REQUIRED et REJECTED. La sauvegarde préalable est réussie
(dernier achèvement 13:11:21 UTC). Le contrôle HTTPS `verify-release-health.sh`
a confirmé tous les composants obligatoires UP après le déploiement.

### Ajustement visuel mobile demandé pendant la livraison

Nicolas a demandé d’harmoniser le bouton Google avec le bouton Apple avant
une nouvelle compilation TestFlight. CHORE visuel, route screen feature :
commit mobile `de2ffd1760b2f6331082806a04fc1e6be05d4547`, un composant seulement.
Le groupe logo/texte est centré ; texte système noir 21,5 points pour une
hauteur normale de 50 points, d’après les proportions de la documentation
[Apple](https://developer.apple.com/design/human-interface-guidelines/sign-in-with-apple/).
Le libellé reste sur une ligne et peut se réduire sur un écran étroit ; le
spinner occupe le même espace que le logo. Largeur et coins arrondis restent
alignés sur Apple. Le bouton Apple reste le composant natif existant.
Tests existants du composant : 3 réussis ; TypeScript et lint réussis.
Mutation non applicable à ce changement visuel. Aucun simulateur iOS installé :
comparaison visuelle native à confirmer sur iPhone, sans revendication d’une
égalité de rendu mesurée. Le prochain build remplace le 11 pour la recette.


### Build final pour la recette : 1.0.0 (12)

- CI mobile `37782445798` : réussite sur `de2ffd1`.
- Build EAS `6d8b5b2c-3b43-4f59-a8c7-ca1bc1c41b8d` : FINISHED.
- Soumission Apple `37e9e816-c862-493e-a31b-5ff0568f0339` : FINISHED, sans erreur.
- IPA inspecté : version **1.0.0**, build **12**, bundle attendu, entitlement
  Apple présent, debugger désactivé et 13 manifests de confidentialité.
- SHA-256 IPA : `eeaef54d223260359e38c2a60392991d390b99e1c8824275644640d0e32f918a`.
  Même limite de confiance locale de signature que le build 11, explicitée plus haut.

Installer **le build 12** pour la recette finale (modération et présentation des
boutons). L’envoi Apple est terminé ; l’achèvement du traitement App Store
Connect et la disponibilité aux groupes TestFlight ne sont pas vérifiés ici.
Aucune publication App Store publique effectuée. Aucun autre changement mobile
n’a été ajouté après la demande d’harmonisation des boutons.
