# P1 FR-015 et FR-016 — santé de promotion et CI plateforme

Date : 2026-09-18
Branche : `fix/p1-release-health-ci`
Base : `0b2f508`

## Résultat local

- `DEGRADED` n'est plus masqué par l'agrégation globale Actuator.
- `/actuator/health/liveness` reste indépendant du backlog récupérable.
- `/actuator/health/release` regroupe DB, messaging, authoring Article,
  vérification Ticket et opérations éditoriales.
- `scripts/verify-release-health.sh` refuse un groupe dégradé, un composant
  dégradé ou un composant attendu absent, même si HTTP répond 200.
- une saga Article terminale `FAILED` dégrade désormais correctement
  `articleAuthoringHealth`.
- `.github/workflows/backend-ci.yml` exécute sur PR et `main` le même
  `scripts/verify-backend-ci.sh` que la promotion staging.
- le dépôt mobile exécute sur PR et `main` un gate unique couvrant lint,
  typecheck, tests Jest, carte Redux, configuration publique/native et garde-fous
  de release.
- le CI Studio couvre désormais `main` et publie un bundle identifié par le SHA ;
  le workflow de staging télécharge ce bundle exact sans reconstruire.
- le workflow Studio privilégié refuse tout artefact ne provenant pas d'un
  événement `push` sur `main` dans le dépôt courant ; une PR ou un fork ne peut
  pas déclencher cette promotion.
- le scanner de secrets distingue désormais un bloc PEM réel d'une simple
  chaîne de parsing/délimiteur ; les deux comportements sont testés sans
  imprimer la matière détectée.

## Frontières préservées

La liveness répond uniquement à la question « faut-il laisser tourner la JVM ? ».
Elle ne dépend pas d'une DLQ, d'une saga ou d'un provider récupérable. Le gate de
release est une décision d'exploitation distincte ; il lit des indicateurs mais
ne modifie aucune table et ne contourne aucun use case métier.

Le script CI orchestre les preuves existantes. Il ne recrée pas une deuxième
suite de tests et ne déploie rien. La promotion reste manuelle, sérialisée et
limitée à `main`.

## Limites encore ouvertes

- La legacy DLQ et les lignes/sagas staging observées lors de l'audit doivent
  encore être classifiées sans purge opportuniste.
- Aucune transaction synthétique authentifiée read-after-write n'est lancée :
  il manque un compte de recette isolé et un secret court terme adapté. Utiliser
  un compte utilisateur réel ou introduire un endpoint de contournement serait
  moins sûr que conserver cette preuve ouverte.
- Les changements mobile et Studio vivent dans des dépôts séparés et doivent
  être poussés puis observés dans GitHub Actions avant de constituer une preuve
  distante.
- Les règles de protection GitHub sont un état distant ; leur présence ne peut
  pas être déduite du YAML et doit être configurée/vérifiée séparément.
- Le gate `npm audit --audit-level=high` de Studio était rouge sur deux alertes
  transitives `js-yaml` via `@redocly/openapi-core`. FR-018 les a corrigées dans
  le lockfile le 2026-09-18 ; la recette complète est consignée dans
  `docs/audits/2026-09-18-p1-fr018-supply-chain.md`.
- Le `.env.local` Studio ignoré par Git contient encore des tokens navigateur
  historiques. Le build production les refuse correctement ; comme leur valeur
  a été exposée pendant l'inspection locale, ils doivent être révoqués/rotatés.

## Vérifications prévues

```text
./mvnw -q -Dtest=ArticleAuthoringHealthIndicatorTest,CommittedSecretGuardTest,PostgresRecoveryGuardrailTest,ReleaseHealthConfigurationTest,ReleaseHealthGateScriptTest,ReleaseWorkflowGuardrailTest test
bash -n scripts/verify-release-health.sh
bash -n scripts/verify-backend-ci.sh
bash scripts/verify-backend-ci.sh
git diff --check
```

## Vérifications exécutées

Le gate exact `bash scripts/verify-backend-ci.sh` est vert :

- scanner de secrets : vert ;
- syntaxe des scripts de déploiement et des nouveaux scripts : verte ;
- suite release avec Docker/Testcontainers : **617 tests**, 0 échec, 0 erreur,
  0 ignoré ;
- packaging Maven : vert.

Le gate exact `npm run verify:ci` du mobile est vert :

- lint Expo : vert ;
- typecheck TypeScript : vert ;
- **93 suites Jest / 356 tests**, tous verts ;
- carte Redux : à jour ;
- **9 tests** de configuration/release, tous verts ;
- configuration native source : alignée, sous réserve du build signé/appareil.

La recette Studio a produit les preuves suivantes :

- contrat OpenAPI généré : à jour ;
- **35 suites Vitest / 164 tests**, tous verts ;
- build Vite production avec l'environnement public CI : vert ;
- distribution : 3 fichiers vérifiés ;
- **2 tests** de workflow/promotion : verts ;
- `npm audit --audit-level=high` : **rouge au moment de FR-015/016**, avec 2
  vulnérabilités `high` transitives. FR-018 a ensuite ramené ce gate à zéro ;
  ce paragraphe conserve le résultat historique de la première recette.

La première recette Studio locale a aussi échoué avant ce résultat : Vitest
collectait le nouveau test Node de workflow. Le fichier a été séparé en
`*.check.mjs`, puis les 164 tests ont été relancés avec succès. Un second build
a refusé le `.env.local` historique contenant des secrets publics interdits ; le
build reproduisant l'environnement CI, sans ces tokens, est ensuite passé.

La première exécution complète a échoué avec 616 tests verts et un garde-fou
statique rouge : `PostgresRecoveryGuardrailTest` attendait littéralement
l'ancienne commande d'installation de `ripgrep`. Le workflow installe désormais
`jq` et `ripgrep`. Le test a été corrigé pour vérifier les deux outils et leur
ordre avant le gate partagé, puis les 617 tests ont été relancés avec succès.
Cette détection intermédiaire n'est pas masquée.

L'avertissement existant de fermeture tardive Surefire demeure : des threads
`projection-sync` tentent brièvement d'utiliser des pools PostgreSQL déjà
fermés, puis Surefire termine le fork après 30 secondes. Il n'a causé aucun
échec mais reste une dette distincte.

## État externe observé après la validation locale

Lecture staging non destructive du 2026-09-18, avant tout déploiement de cette
branche :

- `/actuator/health` répond encore globalement `UP` alors que
  `articleAuthoringHealth` et `messagingRuntimeHealth` sont `DEGRADED` ;
- `/actuator/health/liveness` répond `UP` ;
- `/actuator/health/release` répond HTTP 404, ce qui est attendu tant que cette
  branche n'est pas déployée ;
- la legacy shared DLQ contient toujours 3 messages visibles, aucun en vol ni
  retardé. Aucun message n'a été reçu, modifié, redrivé ou supprimé.

La protection de `main` n'a pas pu être vérifiée : `gh` n'est pas installé et
l'API GitHub publique retourne HTTP 401 pour ce réglage. Aucun réglage distant
n'a été supposé.

## FlowAtlas

FlowAtlas n'a pas été invoqué pour ce lot. Les objets modifiés sont des groupes
Actuator, scripts shell et workflows GitHub Actions, hors des projections Java
métier actuellement pertinentes. L'utiliser ici aurait constitué une validation
symbolique sans preuve supplémentaire ; les sources, tests exécutables et états
distants bornés sont les preuves appropriées.

Le gate distant ne sera considéré opérationnel qu'après push sur une branche,
exécution GitHub Actions, configuration du check requis et qualification des
composants staging. Aucun de ces états externes n'est affirmé par le présent
reçu local.

## Dépôts et branches de ce lot

- backend : `/Users/nicolasmaldiney/fragmentsClean`, branche
  `fix/p1-release-health-ci`, commit initial du lot `e074046` ;
- mobile : `/Users/nicolasmaldiney/fragmentsCleanFront`, branche
  `ci/mobile-release-gate`, commit `f2b05a3` ;
- Studio : `/Users/nicolasmaldiney/fragments-admin`, branche
  `ci/studio-release-gate`, commit `8e2ba19`.

Aucun merge, push ni déploiement n'est inclus dans ce reçu.
