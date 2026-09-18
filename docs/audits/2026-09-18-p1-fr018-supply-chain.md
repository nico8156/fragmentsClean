# P1 FR-018 — durcissement de la supply chain

Date : 2026-09-18  
Backend : branche `fix/p1-supply-chain`, base `c821105`  
Mobile : branche `fix/p1-supply-chain`, base `f2b05a3`  
Studio : branche `fix/p1-supply-chain`, base `8e2ba19`

Les trois arbres étaient propres au départ. Les modifications décrites ci-dessous
étaient locales et non poussées au moment de cette recette.

## Résultat

FR-018 est **traité localement pour les sévérités HIGH et CRITICAL** :

- backend : SBOM CycloneDX généré avec le packaging et scan Trivy du SBOM
  applicatif à zéro HIGH/CRITICAL ;
- image backend complète : construction réussie et scan Trivy de l'OS, du JAR
  et du binaire embarqué à zéro HIGH/CRITICAL ;
- Studio : passage de 2 HIGH à zéro vulnérabilité npm ;
- mobile : passage de 43 alertes (`20 high`, `21 moderate`, `2 low`) à
  20 alertes modérées, sans HIGH ni CRITICAL ;
- CI mobile et Studio : `npm audit --audit-level=high` bloque désormais une
  régression ;
- CI backend : Trivy bloque une vulnérabilité HIGH/CRITICAL du SBOM ;
- promotion staging backend : l'image est chargée localement, scannée, puis
  seulement poussée vers ECR ;
- actions GitHub tierces épinglées par SHA complet ;
- trois images de base du Dockerfile épinglées par digest SHA-256.

Ce résultat ne transforme pas les 20 alertes modérées mobiles en faux négatifs.
Elles restent documentées et visibles. Elles proviennent de deux chaînes
d'outillage Expo : `decode-uri-component` via `query-string`/`expo-router`, et
une ancienne version de `uuid` embarquée par `xcode`/les plugins de
configuration Expo. `npm audit fix --force` propose des changements de versions
Expo incompatibles entre eux et potentiellement régressifs. Ils seront traités
dans une migration Expo qualifiée, avec build natif signé et recette appareil,
plutôt que forcés dans ce lot.

## Corrections appliquées

### Backend et image

- Spring Boot `3.3.0` -> `3.5.16` ;
- Tomcat embarqué `10.1.60`, Netty `4.1.138.Final`, HttpCore5 `5.4.3` ;
- PostgreSQL JDBC `42.7.13`, OpenAI Java `4.0.1`, Testcontainers `1.21.4`
  géré de façon cohérente par Spring Boot ;
- annotation processor Lombok `1.18.48` ;
- suppression du BOM Spring AI inutilisé ;
- génération CycloneDX JSON par `cyclonedx-maven-plugin:2.9.1` durant
  `package` ;
- scan SBOM bloquant dans `backend-ci.yml` ;
- scan d'image bloquant avant `docker push` dans la promotion staging ;
- images Debian, Maven/Temurin et Temurin JRE verrouillées par digest ;
- tests d'architecture empêchant le retour d'actions `@vN` mutables ou de
  bases Docker non épinglées.

La version Tomcat `10.1.58`, annoncée par une première entrée CVE comme version
corrective, n'existait pas dans Maven Central au moment de la recette (les
versions `10.1.59` et `10.1.60` existaient). Elle n'a donc pas été simulée : le
lot utilise `10.1.60`, réellement résolue et testée.

### Mobile

- correctifs transitifs compatibles appliqués au lockfile ;
- famille Metro alignée atomiquement de `0.83.3` vers `0.83.8` ; cette version
  retire la dépendance vulnérable `image-size` sans imposer son API majeure
  incompatible à Metro ;
- overrides bornés pour `brace-expansion 1.1.18` et `postcss 8.5.28` ;
- audit HIGH/CRITICAL intégré à `verify:ci` ;
- actions Checkout et Setup Node épinglées par SHA ;
- garde-fou de workflow vérifiant les références immuables.

### Studio

- lockfile corrigé (`@redocly/openapi-core`/`js-yaml`) : zéro vulnérabilité
  npm ;
- Checkout, Setup Node, upload/download artifact et AWS credentials épinglés
  par SHA ;
- AWS credentials du rollback aligné sur l'action v6 épinglée ;
- tests de livraison étendus aux trois workflows et aux références immuables.

## Vérifications exécutées

### Backend

```text
bash scripts/verify-backend-ci.sh
```

Résultat : **618 tests**, 0 échec, 0 erreur, 0 ignoré ; contrôles secrets,
scripts, architecture, PostgreSQL/Testcontainers, SQS/LocalStack, migrations et
packaging verts. Le passage de 617 à 618 correspond au nouveau garde-fou Docker.

```text
docker run --rm -v /Users/nicolasmaldiney/fragmentsClean:/workspace \
  aquasec/trivy:0.68.2 sbom --quiet --severity HIGH,CRITICAL \
  --exit-code 1 /workspace/target/bom.json
```

Résultat : `Java / jar / 0 vulnerabilities`.

```text
docker build --tag fragments-backend:fr018 .
docker save --output /private/tmp/fragments-backend-fr018-final.tar \
  fragments-backend:fr018
docker run --rm -v /private/tmp:/scan aquasec/trivy:0.68.2 image --quiet \
  --input /scan/fragments-backend-fr018-final.tar --severity HIGH,CRITICAL \
  --exit-code 1
```

Résultat : image `sha256:f12cb8b0e7aceccae511fef26d38ad8e78f8ae6bc0b34df60fc741b570f7511f` ;
Ubuntu, `app.jar` et binaire détecté : **0 HIGH/CRITICAL**.

L'image de scanner utilisée était `aquasec/trivy:0.68.2`, digest localement
résolu `sha256:05d0126976bdedcd0782a0336f77832dbea1c81b9cc5e4b3a5ea5d2ec863aca7`.

### Mobile

```text
npm ci
npm run verify:ci
EAS_BUILD_PROFILE=development npx expo export --platform ios \
  --output-dir /private/tmp/fragments-mobile-fr018-export --clear
```

Résultat : installation reproductible ; audit sans HIGH/CRITICAL ; lint et
typecheck verts ; **93 suites / 356 tests** verts ; carte Redux et 9 tests de
release verts ; configuration native source valide ; bundle Hermes iOS produit
avec Metro 0.83.8 (2 094 modules, bundle 5,88 MB).

L'export local valide le bundling JavaScript. Il ne remplace pas un build EAS
signé ni une recette TestFlight sur appareil.

### Studio

```text
npm ci
npm run contract:check -- ../../fragmentsClean/contracts/studio-api/v1/openapi.json
npm test
npm run build
npm run verify:dist
npm run test:delivery
npm audit --audit-level=high
```

Résultat : zéro vulnérabilité ; contrat courant ; **35 fichiers / 164 tests**
verts ; build Vite vert ; 3 fichiers de distribution vérifiés ; 3 tests de
livraison verts.

## Échecs intermédiaires conservés

- le premier scan Trivy relu pendant ce lot ciblait encore un ancien SBOM
  Spring Boot 3.5.14 ; son résultat rouge n'a pas été présenté comme final ;
- la résolution de Tomcat `10.1.58` a échoué car cette version n'est pas
  publiée ; passage à `10.1.60` après lecture des métadonnées Maven Central ;
- le premier export Expo a refusé l'absence de variables de production ; la
  recette de bundling a été relancée en profil development sans secret ;
- le premier build Studio local a correctement refusé des tokens historiques
  chargés depuis `.env.local` ; la recette CI les a explicitement neutralisés ;
- le premier montage du socket Docker dans le conteneur Trivy a échoué sous
  Docker Desktop macOS ; l'image a été exportée dans `/private/tmp` puis scannée
  en lecture seule ;
- Docker Scout exigeait une connexion Docker Hub : aucune identité personnelle
  n'a été demandée, Trivy a fourni la preuve locale ;
- une tentative supplémentaire de sérialiser le rapport npm mobile dans
  `/private/tmp` a été refusée par l'environnement à cause de l'envoi de
  métadonnées au registre. Les résultats déjà obtenus par le gate autorisé ont
  été conservés ; aucun contournement n'a été tenté.

## Limites et suites

- observer les trois CI distantes après push ; les YAML locaux ne prouvent pas
  l'état GitHub Actions ni les règles de protection de branche ;
- produire un build EAS signé et faire une recette appareil avant App Store ;
- maintenir une exception explicite pour les 20 MODERATE mobiles jusqu'à la
  migration Expo qualifiée ; aucune exception HIGH/CRITICAL n'est admise ;
- surveiller le prochain correctif Spring Boot afin de réduire les overrides
  Tomcat/Netty/HttpCore, sans les retirer avant preuve SCA ;
- l'avertissement Vite sur l'import sans extension et les dépréciations Spring
  de tests appartiennent à FR-020, pas à FR-018 ;
- les tokens historiques locaux Studio signalés dans FR-015/016 doivent rester
  révoqués/rotatés ; aucune valeur n'est reproduite ici.

## FlowAtlas

FlowAtlas n'a pas été utilisé pour ce lot. FR-018 porte sur les graphes de
dépendances, lockfiles, images OCI et workflows GitHub ; les projections métier
Redux/Java n'auraient fourni aucune preuve de reachability supply-chain. Les
preuves adaptées étaient les résolveurs Maven/npm, les builds réels, les suites
complètes, CycloneDX et le scan de l'image finale. Ce choix évite une utilisation
symbolique de FlowAtlas et ne remet pas en cause son intérêt pour les flux
métier des lots suivants.
