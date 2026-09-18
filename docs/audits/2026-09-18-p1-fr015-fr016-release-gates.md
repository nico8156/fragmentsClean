# P1 FR-015 et FR-016 — santé de promotion et CI backend

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
- Les workflows mobile et Studio vivent dans des dépôts séparés et ne sont pas
  modifiés dans ce commit backend.
- Les règles de protection GitHub sont un état distant ; leur présence ne peut
  pas être déduite du YAML et doit être configurée/vérifiée séparément.
- Le triage supply-chain détaillé reste FR-018 ; ce lot n'autorise aucune
  exception CVE implicite.

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
