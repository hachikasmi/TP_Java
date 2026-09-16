# TD2 – Magasin de Trombone : ce qui a été ajouté

Ce document explique ce qui a été implémenté pour terminer le **TD2** (réapprovisionnement,
livraison asynchrone, concurrence), la méthodologie suivie, et comment vérifier/tester le
résultat. Le **TD3 (géocodage)** n'a volontairement pas été commencé.

## 1. Périmètre traité

| Point du TD2 | Contenu | Statut |
|---|---|---|
| 1. Entité Magasin | déjà fait avant cette session | ✅ (inchangé) |
| 2. Réapprovisionnement | `GET /api/stores/{id}/factories`, `POST /api/stores/{id}/supply`, `GET /api/stores/{id}/shipments` | ✅ ajouté |
| 3. Livraison asynchrone | entité `Shipment` + scheduler toutes les 5s | ✅ ajouté |
| 4. Concurrence | retry optimiste (max 3 tentatives) sur les écritures concurrentes du stock usine | ✅ ajouté |
| TD3 (géocodage) | — | ❌ non commencé (hors périmètre demandé) |

## 2. Méthodologie suivie

Le travail a été fait **point par point, dans l'ordre du TD**, avec une règle simple :
*on ne passe au point suivant qu'après avoir vérifié que le point courant compile et se
comporte comme prévu.*

Pour chaque point :
1. Écriture du code (entité → repository → DTO → service → contrôleur / scheduler).
2. Compilation (`mvnw compile`) pour détecter immédiatement les erreurs de câblage
   (imports, injections Spring manquantes, mapping MapStruct, etc.).
3. Écriture d'un test unitaire ciblé (Mockito, sans contexte Spring complet) qui exerce
   la règle métier du point (ex : stock insuffisant → exception, distance générée dans la
   bonne plage, conflit optimiste → retry puis succès...).
4. Exécution du test. En cas d'échec, correction et re-test avant de continuer.

**Limitation de l'environnement de vérification** : Docker n'était pas disponible dans la
session utilisée pour développer, donc MySQL n'a pas pu être démarré et l'application n'a
pas pu être lancée en conditions réelles (`mvnw spring-boot:run`). La vérification "point
par point" a donc été faite par **compilation + tests unitaires avec repositories mockés**
(15 tests ajoutés, tous verts), ce qui couvre la logique métier et la logique de retry,
mais ne remplace pas un test end-to-end avec la vraie base. Voir section 6 pour la procédure
de test manuel à faire de ton côté avec Docker.

**Remarque annexe (sans lien avec le TD)** : pendant la vérification, la compilation a
échoué avec le JDK système par défaut (`JAVA_HOME` pointait vers un **JDK 21**, alors que
le `pom.xml` cible `java.version=25`, ce qui n'est pas supporté par javac 21). Il existe un
**JDK 26** installé par IntelliJ dans `C:\Users\<toi>\.jdks\openjdk-26.0.2.1`, avec lequel
la compilation passe. C'est probablement lié au souci "ça marche en cmd mais pas dans
IntelliJ" évoqué en début de session : si le SDK de projet configuré dans IntelliJ diffère
de celui utilisé par le terminal, l'un des deux échouera à supporter `--release 25`. À
vérifier dans IntelliJ : `File > Project Structure > Project SDK`.

## 3. Nouveaux fichiers

```
entity/Shipment.java              expédition : usine, magasin, quantité, distance, statut, départ/arrivée
entity/ShipmentStatus.java        enum IN_TRANSIT / DELIVERED
repository/ShipmentRepository.java

dto/LocationDto.java              nom + lat/lon (usine ou magasin)
dto/FactoryAvailabilityDto.java   usine + stock + distance aléatoire (pour GET /factories)
dto/SupplyRequestDto.java         { factoryId, quantity }
dto/ShipmentDto.java              représentation d'une expédition
dto/SupplyResponseDto.java        { shipment, factory (localisation), store (localisation) }

mapper/ShipmentMapper.java        MapStruct: Shipment -> ShipmentDto

exception/InsufficientStockException.java   -> 409
exception/ConcurrentUpdateException.java    -> 409

service/ShipmentService.java      logique métier réappro (point 2)
service/DeliveryScheduler.java    livraison asynchrone toutes les 5s (point 3)

config/TransactionConfig.java             bean TransactionTemplate
concurrency/OptimisticRetrySupport.java   retry optimiste générique (point 4)
```

## 4. Fichiers modifiés

- `controller/StoreController.java` : 3 nouveaux endpoints.
- `exception/GlobalExceptionHandler.java` : mapping des 2 nouvelles exceptions vers 409.
- `service/FactoryService.java` : `produce()` passe par le retry optimiste.
- `service/ProductionScheduler.java` : ne fait plus un `saveAll` global mais une
  transaction + retry **par usine**, pour qu'un conflit sur une usine ne bloque pas les
  autres.

## 5. Détail des règles métier implémentées

### `GET /api/stores/{id}/factories`
Retourne toutes les usines avec leur stock et une **distance aléatoire (10–1000 km)**,
générée à chaque appel (la vraie distance viendra du TD3). 404 si le magasin n'existe pas.

### `POST /api/stores/{id}/supply`
Body : `{ "factoryId": 1, "quantity": 50 }`

- Décrémente **immédiatement** le stock de l'usine.
- Si le stock de l'usine est insuffisant → `409 Conflict` (`InsufficientStockException`),
  aucune expédition n'est créée.
- Calcule une distance aléatoire (10–1000 km) et le temps de trajet :
  `temps (s) = (distance / 1235) * 3600`.
- Crée une `Shipment` en statut `IN_TRANSIT` avec heure de départ = maintenant, heure
  d'arrivée = départ + temps de trajet. **Le stock du magasin n'est pas encore modifié.**
- Répond `201 Created` avec l'expédition + la localisation (nom, lat, lon) de l'usine et
  du magasin.

### `GET /api/stores/{id}/shipments?status=delivered`
Sans paramètre → expéditions `IN_TRANSIT` (en cours). `?status=delivered` → expéditions
`DELIVERED`.

### `DeliveryScheduler` (toutes les 5 secondes)
Cherche les expéditions `IN_TRANSIT` dont `arrivalTime <= now`, crédite le stock du
magasin destinataire de la quantité transportée, puis passe l'expédition en `DELIVERED`.

## 6. Concurrence (point 4)

Le stock d'une usine peut être écrit en parallèle par :
- `ProductionScheduler` (production automatique, toutes les 5s),
- `FactoryService.produce()` (production manuelle),
- `ShipmentService.supply()` (décrément lors d'un réappro).

Chaque `Factory`/`Store`/`Shipment` porte un champ `@Version` (verrouillage optimiste
JPA). `OptimisticRetrySupport.executeWithRetry(...)` encapsule une action dans sa **propre
transaction** (via `TransactionTemplate`) et, si un `OptimisticLockingFailureException`
survient (la version a changé entre la lecture et l'écriture), **relit l'entité et
réessaie, jusqu'à 3 tentatives**. Au-delà, une `ConcurrentUpdateException` est levée
(→ `409 Conflict`).

Point important : dans `ProductionScheduler`, chaque usine est mise à jour dans sa propre
transaction retryable — un conflit sur l'usine A n'empêche pas la mise à jour de l'usine B.

Testé par :
- `OptimisticRetrySupportTest` : succès direct, récupération après conflits transitoires,
  échec après 3 tentatives.
- `FactoryServiceProduceTest` : `produce()` récupère après un conflit simulé.
- `ProductionSchedulerTest` : un conflit sur une usine n'empêche pas les autres.

## 7. Comment tester manuellement (avec Docker disponible)

```bash
docker-compose up -d
./mvnw spring-boot:run
```

```bash
# créer une usine et un magasin
curl -X POST localhost:8080/api/factories -H "Content-Type: application/json" \
  -d '{"name":"Usine Nord","production":10,"latitude":49.9,"longitude":2.3}'
curl -X POST localhost:8080/api/stores -H "Content-Type: application/json" \
  -d '{"name":"Magasin Centre","latitude":49.0,"longitude":2.0}'

# voir les usines disponibles pour le magasin 1
curl localhost:8080/api/stores/1/factories

# commander un réappro (ajuster factoryId/quantity selon le stock déjà produit)
curl -X POST localhost:8080/api/stores/1/supply -H "Content-Type: application/json" \
  -d '{"factoryId":1,"quantity":5}'

# expéditions en cours, puis (après le délai indiqué par arrivalTime) livrées
curl localhost:8080/api/stores/1/shipments
curl localhost:8080/api/stores/1/shipments?status=delivered
```

La distance étant aléatoire entre 10 et 1000 km, le temps de trajet varie entre ~29
secondes (10 km) et ~48 minutes (1000 km) — relance `POST /supply` plusieurs fois si tu
veux voir une livraison rapide sans attendre.

## 8. Lancer les tests unitaires

```bash
./mvnw test -Dtest=OptimisticRetrySupportTest,ShipmentServiceTest,DeliverySchedulerTest,FactoryServiceProduceTest,ProductionSchedulerTest
```

(Le test `TdJavaUsineTromboneApplicationTests` charge tout le contexte Spring et a besoin
d'une vraie connexion MySQL — il ne passera qu'avec `docker-compose up` lancé au préalable.)
