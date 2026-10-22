# jbetting

Mutual (pari-mutuel) bets created by members among themselves, staked with **virtual tokens** (whole
numbers, no real money). The domain was discovered in an Event Storming workshop:
`workspace/event-storming.md` (events, policies, read models, open hot spots). Read it when a task needs
business detail; business rules themselves live in the tests, not here.

## Bounded contexts

| Context | Owns (aggregates) | « utilisateur » means | Role |
|---|---|---|---|
| `members` | `Member` (registration, confirmed email, login) | an application account | **upstream** |
| `betting` | `Player` (spendable token balance, token history, refills), `Bet` (outcomes, invitations, settlement), `Stake` (made by `Bet`) | a player: creator, invitee, bettor | **downstream** |

- Contexts never import each other: no Maven dependency, no shared class, no shared table.
- **Reads may be synchronous**: a client composes its screens by querying each context's read side
  directly (e.g. the members to invite come from `members`). How a context would read another one for
  its own decisions is decided at the first test that needs it, then written here.
- **Changes go through published events**: a change in one context that must change another one is
  never a synchronous call.
- Cross-context events: `members` → `betting`: **email confirmed** only. `betting` reacts by creating the
  `Player` and crediting the welcome tokens. Nothing flows from `betting` to `members`.
- No published event may be lost (outbox on the publishing side); consumers are idempotent — a member's
  `Player` is created once.
- Tokens live in `betting` only: staking, refills and settlement are local to the context (decided after
  the workshop, see the revision in `workspace/event-storming.md`).
- Possible later split of `betting` (bet/outcomes vs stakes) is noted in the workshop summary, not decided;
  the player's balance would go with the stakes.

## Ubiquitous language (code names)

Code (production and tests) is in English; French is the business vocabulary of the workshops and of
the specifications in `workspace/`. This table is the translation between the two.

| Français | Code | Français | Code |
|---|---|---|---|
| membre / compte | `Member` | pari | `Bet` |
| joueur (créateur, invité, parieur) | `Player` | cagnotte | `Pot` |
| solde, jetons | `Balance`, `Tokens` | issue | `Outcome` |
| recharge (plancher mensuel) | `Refill` | mise | `Stake` |
| versement (gains, commission, restitution) | `Payout`, `PayoutReason` | invitation | `Invitation` |
| pari réglé | `BetSettled` | date de clôture | `closesAt` |
| jetons détruits | `burnt` | commission | `Commission` |

## Module layout

```
pom.xml                            parent (Spring Boot 4.1.1, Java 25, groupId com.jbetting)
sharedkernel/                      pure Java, what every context may use (empty until a test needs it)
boundedcontexts/<context>/
├── write/   (<context>-write)     com.jbetting.<context>.write — commands
├── read/    (<context>-read)      com.jbetting.<context>.read  — queries, own read models
└── src/test/                      every test of the context, both sides
jbetting-app/                      @SpringBootApplication(scanBasePackages = "com.jbetting"), nothing else
```

Inside a side:

```
businesslogic/models/<aggregate>/          aggregate, its VOs and exceptions — pure Java
businesslogic/models/<aggregate>/ports/    repository port (interface only)
businesslogic/usecases/<theme>/            …Command + …CommandHandler / …Query + …QueryHandler
adapters/primary/                          controllers
adapters/secondary/persistence/fakes/      Fake… (in memory)
adapters/secondary/persistence/jpa/        Sql…Repository, …JpaEntity, …JpaRepository
adapters/secondary/time/                   SystemDateProvider, DeterministicDateProvider
configuration/                             composition root of the side
```

`write` and `read` of one context do not depend on each other (Maven). Imports point inward only.

## Design doctrine

- **The architecture is tested.** `…/unit/<Context>ArchitectureTest` (e.g. `BettingArchitectureTest`)
  forbids in `..businesslogic..`: Spring, Jakarta, Hibernate, Jackson, Lombok, `..adapters..`,
  `..configuration..`, and any `java.time` `now()`. `archunit.properties` sets
  `archRule.failOnEmptyShould=false` only because `businesslogic` is still empty: **remove that line**
  when the context gets its first business class.
- **Time is a port.** Domain and use cases get the time from a clock port with a deterministic fake.
- **Aggregates are small and reference each other by id.** One aggregate written per transaction;
  anything spanning two aggregates is a policy reacting to an event. **One assumed exception: placing a
  stake** debits the `Player` and inserts the `Stake` in one local transaction, so the bettor gets an
  immediate answer; only the player's balance is locked, the `Bet` is not modified. Crediting the players
  at settlement (one transaction, or one per player) is decided with the settlement feature.
- **`Bet` is the factory of `Stake`**: `Stake`'s constructor is package-private; placing a stake reads
  the bet and inserts the stake, the bet is not modified.
- **Snapshots.** An aggregate exposes no getters. It exports its state with `toSnapshot()` (records of
  raw values) and is rebuilt with `fromSnapshot(...)`. Adapters, fakes and tests use snapshots only.
  Aggregate equality (id-only vs full state) is chosen at the first test that needs it, then written here.
- **Fakes live in the production tree** (`adapters/secondary/persistence/fakes/`) so `configuration/`
  and every test level can reuse them; their port stays in `businesslogic/models/<aggregate>/ports/`.
- **One SQL schema per context**, named after it, with its own migrations (when persistence arrives).
- **The back never formats**: raw values in HTTP (`tokens: 20`, ISO instants, enum names).

## Tests

```
boundedcontexts/<context>/src/test/java/com/jbetting/<context>/
├── unit/          domain + use cases, no Spring   — Surefire: **/unit/**/*Test.java
├── integration/   adapters on real PostgreSQL     — Failsafe: **/*IT.java (Testcontainers)
└── e2e/           HTTP black box (MockMvc)        — Failsafe: **/e2e/**/*E2ETest.java
```

- Under each level, test classes mirror the production package they drive, side included
  (`unit/write/businesslogic/usecases/staking/PlaceStakeCommandHandlerTest`).
- Any other file name **never runs**. `./mvnw test` skips integration and e2e: use `./mvnw verify`.
- Test methods in short English snake_case, `should_[outcome]`, `_when_[condition]` only when the
  outcome alone does not tell two tests apart (e.g. `should_create_the_bet`,
  `should_refuse_the_creation_when_the_creator_is_not_a_player`); helpers in English camelCase.
- Specifications (`workspace/*-spec-by-example.md`) stay in French; tests translate them with the
  ubiquitous-language table.
- Docker is required for integration and e2e tests (Testcontainers, `postgres:17-alpine`).

## Commands

- Build and every test: `./mvnw verify` (there is no global `mvn` on this machine). One context:
  `./mvnw -pl boundedcontexts/betting -am verify`.
- Run locally against a throwaway PostgreSQL: `TestJbettingApplication` (jbetting-app test sources).
