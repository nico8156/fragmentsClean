# Fragments — candidate du prochain TestFlight, 13 septembre 2026

## Périmètre décidé

Préparer les essais iPhone, sans publier l'application sur l'App Store public.
Conservation et restauration complètes : **en pause à la demande de l'opérateur,
à reprendre après TestFlight, non corrigées**. Correctifs locaux `5fd0fc3` exclus.
Aucune migration backend, purge SQL/S3/DLQ ou restauration dans cette livraison.
Ne pas pousser cette branche vers un pipeline backend pour publier le seul HTML.

## Cible identifiée

- API : `https://fragments-staging.anchor-event.fr`.
- Backend : `c8dea6a977ee032cd638820538727a5b27800c0f`.
- Image ECR : `851725375299.dkr.ecr.eu-west-3.amazonaws.com/fragments/staging/backend`.
- Digest : `sha256:bf86ceb8c612a1457709ae7a148531216593efc3c65fb1a307eb020eae34a1df`.
- Conteneur `staging-fragments-backend-1`, démarré le 12 septembre à
  `13:39:02.396083141Z`, inchangé par publication du site.
- Mobile : `32f4413451df6accb985164e520f3c9721d0e6aa`, worktree propre à l'inspection.
- EAS : `@nico8156/fragmentsCleanFront`, projet
  `138089c2-0d66-415d-baa4-46495ea3a90b`, environnement `production`.
- Bundle : `com.nico8156.fragments`, App Store Connect : `6810558661`.

Gel opérationnel de référence : ne pas déployer un autre backend pendant la
recette sans consigner une nouvelle candidate. Aucun verrou technique du pipeline
n'a été installé ; l'identification du digest ne verrouille pas à elle seule CI.

## Pages et contact

- Présentation : https://fragments-staging.anchor-event.fr/
- Confidentialité : https://fragments-staging.anchor-event.fr/legal/confidentialite.html
- Conditions : https://fragments-staging.anchor-event.fr/legal/conditions.html
- Support/confidentialité : `studio@anchor-event.fr`, réception par redirection
  Gmail confirmée explicitement par l'opérateur le 13 septembre. Pas d'email de
  test envoyé par l'agent ni de modification MX/SES/OVH.

Site statique, sans JS, cookies ajoutés, analytics ou ressources distantes.
Les pages conservent les limites sur copies techniques, journaux, sauvegardes
et restauration ; pas de promesse d'effacement global à 30 jours. L'examen des
garanties contractuelles de transfert demeure incomplet et déclaré comme tel.
La transparence n'exonère pas des obligations de protection des données.

## Publication et rollback

Compte AWS `851725375299`, région `eu-west-3`, instance `i-004d3e9cbca327d01`.
Inspection SSM `d2d6affd-b58b-41ac-8845-041048768f48` : Caddyfile actif comparé,
aucun autre bloc de service à écraser. SHA avant publication :
`acd2f894f095344f82dd8205e14c2498462e638493ef91b3ed99e1b572211bf0`.

Publication SSM `174c82fb-cd68-498b-ae6f-6a15db04ba84` : **Success**.
Garde de SHA et absence de répertoire cible, staging des quatre fichiers avec
checksums, validation par vrai Caddy, sauvegarde puis copie en place du Caddyfile
(inode de bind mount conservé) et reload. Aucun restart plateforme/backend/DB.

- Fichiers servis : `/srv/platform/data/fragments-public` (uniquement HTML/CSS).
- Artefacts préparés : `/srv/platform/data/fragments-public-release-20260913`.
- Sauvegarde config : `/srv/platform/Caddyfile.before-public-20260913`.
- Caddyfile livré : `47b85e7b856f70c2cb2f838905572343be0a0d6a8c554f0eab4e4dfaae79d129`.

Précision support/Gmail publiée ensuite par SSM
`782f9bf5-ba80-4b31-b84f-6f2cabf9390d` (**Success**), avec garde du hash précédent
et copie récupérable hors racine servie. Hashes finaux vérifiés en HTTPS contre
les sources locales (égalité des octets) :

| Fichier | SHA-256 |
| --- | --- |
| index.html | `69c115317a406d5ada153c1093a957e2461ee655e7bda5eb53257a7e2204761e` |
| confidentialite.html | `18a26bcf2d48c18fe33cea3e231447548afa5cdd16b8a051586ada555e734717` |
| conditions.html | `5dfc3407ea2bea47b373b332dde3c89f1bcd069590767030d13b7e7a87325373` |
| site.css | `26899c02a55981523fae95dfc62dabacdd68a8e81174fc4ae086ee5c22dbf54e` |

Retour arrière opérateur après comparaison de la configuration (ne pas écraser
une évolution ultérieure) : recopier la sauvegarde dans `/srv/platform/Caddyfile`,
puis `docker exec platform-staging-caddy-1 caddy reload --config /etc/caddy/Caddyfile
--adapter caddyfile`. Les fichiers statiques peuvent rester hors routage ; aucune
suppression n'est nécessaire. Pas de rollback backend/DB pour retirer ces pages.

## Vérification

- Tests statiques : rouge attendu après changement des exigences, puis 5/5 verts.
- Intégration : 1/1 vert avec deux vrais Caddy ; HTML/CSS/CSP, racine limitée,
  404 et reverse proxy API/SSE. Conteneurs et réseau de test nettoyés.
- HTTPS réel : accueil, confidentialité, conditions et CSS 200 ; absent 404 ;
  contact et marqueur beta présents ; CSP restrictive effective.
- API cafés 200. SSE utilisateur/admin sans credentials : 401, pas de HTML.
  Cela ne prouve pas le streaming authentifié, à tester sur l'appareil.
- Santé globale `UP`, DB et vérification ticket `UP`. Indicateurs
  `articleAuthoringHealth` et `messagingRuntimeHealth` **DEGRADED** : ne pas
  présenter tous les sous-systèmes comme sains, ni les anciennes reprises comme
  corrigées. Aucun redrive/purge entrepris pour masquer ces réserves.
- Mobile : 8/8 tests de configuration verts ; gardes natifs et carte Redux verts.
- Suite mobile complète relancée : **77 suites, 289 tests verts**, 15,06 secondes.
  TypeScript vert ; lint : 0 erreur, 20 avertissements existants. Aucun code
  mobile ou contrat backend changé. Les suites Java/Studio ne sont pas relancées
  pour cette publication statique et ce paramétrage EAS ; pas de nouvelle preuve
  attribuée aux 543 tests du correctif backend suspendu.

## EAS et lancement

Trois variables publiques créées puis relues dans EAS `production`. La vraie
configuration `app.config.js` a été résolue avec ces valeurs distantes : projet
attendu et Apple Sign-In vérifiés, aucun garde-fou contourné.

```text
EXPO_PUBLIC_SUPPORT_EMAIL=studio@anchor-event.fr
EXPO_PUBLIC_PRIVACY_POLICY_URL=https://fragments-staging.anchor-event.fr/legal/confidentialite.html
EXPO_PUBLIC_TERMS_URL=https://fragments-staging.anchor-event.fr/legal/conditions.html
```

L'API et OAuth Google existaient déjà dans l'environnement EAS production.
Aucune clé privée Apple ni secret serveur dans ces variables publiques.
Le profil `production` utilise l'incrément de numéro distant ; `preview` est une
distribution ad hoc, pas le profil de cette livraison TestFlight.

**Aucun nouveau build ni upload lancé par l'agent.** L'opérateur avait demandé
à exécuter le build lui-même. Dernier build relu dans EAS :
`4d32e4ca-86de-48aa-98d7-f9ab3b2b4788`, version `1.0.0 (2)`, terminé le
10 septembre, ancien commit mobile `fb43508`. Il ne valide pas le chantier récent.

Commandes depuis le dépôt mobile, une fois les variables vérifiées :

```sh
eas build --platform ios --profile production
# Après réussite : choisir explicitement l'ID du nouveau build, pas un ancien.
eas submit --platform ios --profile production --id <ID_DU_NOUVEAU_BUILD>
```

Consigner ensuite ID EAS, numéro iOS, image/Xcode/SDK réels et groupe de testeurs.
Le build signé et la recette sur appareil restent des preuves distinctes des
tests sources. Tester Apple/Google, carte, expériences avec/sans ticket, photos,
Pass, offline/reconnexion, modération et suppression sur comptes de test.
Éviter données sensibles et documents de tiers ; ne pas restaurer staging pour
cette recette. Les réparations de conservation/reprise restent au lot suivant.

Sources consultées : [information CNIL](https://www.cnil.fr/fr/conformite-rgpd-information-des-personnes-et-transparence),
[TestFlight interne Apple](https://developer.apple.com/help/app-store-connect/test-a-beta-version/add-internal-testers/).
