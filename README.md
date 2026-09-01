# Live Football World Cup Scoreboard

A small Java library for tracking football matches that are currently in progress: start a match,
update its score, finish it, and get a live-ordered summary. Built as a Sportradar recruitment
coding exercise.

## Requirements

- Java 21
- Maven 3.6+

## Build & test

```bash
mvn test      # run the test suite
mvn package   # build the jar
```

## Public API

```java
Scoreboard scoreboard = new Scoreboard();

MatchId id = scoreboard.startMatch("Mexico", "Canada");
scoreboard.updateScore(id, 0, 5);
scoreboard.finishMatch(id);

List<MatchSummary> inProgress = scoreboard.getSummary();          // required op #4
List<MatchSummary> history    = scoreboard.getFinishedMatches();  // chosen extra op #5
```

`Scoreboard` is the only entry point. It is backed by an in-memory `MatchRepository`, but that
dependency is injectable (`new Scoreboard(MatchRepository, Clock)`) for testing and future
extension.

## Assumptions

The exercise intentionally leaves several questions open. These are the assumptions made, in the
order they came up:

1. **A match is identified by an opaque `MatchId`, not by team names.** `startMatch` returns it;
   `updateScore` and `finishMatch` require it. Two team names alone are not a safe identifier
   (nothing rules out the same pairing occurring again later), and a generated id is the standard
   way to hand back a reference to a created resource.
2. **A team cannot be in two matches in progress at the same time.** `startMatch` rejects a
   pairing if either team already has a live match (`DuplicateMatchException`). This mirrors how a
   real tournament works and prevents an easy way to corrupt the "in progress" data.
3. **`updateScore` takes the new absolute score, not a delta** (`updateScore(id, homeGoals,
   awayGoals)`), matching how the scores are written in the exercise's example ("Mexico 0 –
   Canada 5"). A goal-scored event API (`scoreHomeGoal(id)`) was considered but rejected: the
   exercise's own example is expressed as absolute scores, and an event-based API would need to
   reconstruct the running total anyway.
4. **A score update can never lower either team's goal count.** A live match's score is monotonic;
   a decrease is a data-entry contract violation (`InvalidScoreException`), not a legitimate
   correction. There's no "correct a mistake" operation — deliberately, see Out of scope.
5. **Operations on a finished match are rejected**, not silently ignored: `updateScore` /
   `finishMatch` on an already-finished match throw `MatchAlreadyFinishedException`. Silent no-ops
   hide bugs in the caller.
6. **Summary ordering** is exactly as specified: total score descending, ties broken by most
   recently started first. Implemented as a single `Comparator` composed via
   `comparingInt(...).reversed().thenComparing(startTime, reverseOrder())` — no custom tie-break
   logic to get subtly wrong.
7. **Storage is in-memory only — no database, no SQLite.** The exercise repeatedly calls this a
   "simple library"; there is no requirement for state to survive a JVM restart. `MatchRepository`
   is still an interface (`InMemoryMatchRepository` the only implementation) so a persistent
   adapter could be added later without touching `Scoreboard` — the port exists for the
   Dependency Inversion it buys, not because SQLite was deemed necessary here.
8. **The library is not thread-safe.** `InMemoryMatchRepository` uses a plain `LinkedHashMap`, and
   there is no synchronization anywhere. This is an explicit scope decision: making a small
   in-memory domain library thread-safe (locking or concurrent collections, plus deciding what
   "atomic" means across a start/update/finish sequence) is a meaningfully bigger design problem
   than this exercise calls for, and it isn't hinted at in the requirements. It's called out here
   so it isn't mistaken for an oversight.
9. **Time is an injected `Clock`, not `Instant.now()` calls scattered through the code.**
   `Scoreboard(MatchRepository, Clock)` takes a `java.time.Clock`; a no-arg constructor defaults to
   `Clock.systemUTC()`. This is what makes the tie-break ordering test (and the exact example
   scenario from the spec) deterministic without `Thread.sleep`.
10. **Team names must be non-blank and a team cannot play itself** (`InvalidTeamException`). Basic
    input validation the exercise doesn't mention but that any real caller would hit immediately.

## Design reasoning

- **Domain-first, deliberately small.** `Match` is the aggregate root and owns every invariant
  about its own lifecycle (no score decrease, no mutation after finishing, valid team names). It
  never trusts a caller to have checked first — `Scoreboard` calls straight into `Match`, and
  `Match` throws if the call is invalid. `Score` and `MatchId` are immutable value objects.
  `MatchSummary` is a read-only projection (`record`) returned to callers — internal `Match`
  objects are never handed out, so nothing outside the package can mutate scoreboard state except
  through `Scoreboard`'s methods.
- **One package, not a `domain`/`application`/`infrastructure` split.** The library is ~13 small
  classes. A layered package structure is the right call once a codebase has real breadth; here it
  would just be ceremony — ADRs worth of ceremony to navigate for a class count you can read in one
  screen. `MatchRepository` still gives the one seam (storage) that plausibly changes.
- **Repository pattern for storage, without a real datastore.** `MatchRepository` is a narrow
  interface (`save` / `findById` / `findAll`) so `Scoreboard` depends on an abstraction, not on
  `LinkedHashMap` directly — see assumption 7.
- **Exceptions are specific, not `IllegalArgumentException` / `IllegalStateException`.** Six small
  domain exception types (`InvalidTeamException`, `InvalidScoreException`,
  `DuplicateMatchException`, `MatchNotFoundException`, `MatchAlreadyFinishedException`) make the
  contract of each method legible from its `catch` clauses and make blackbox tests read as
  specification rather than implementation detail.

## Testing approach

- **TDD throughout**: every class was written test-first (red → green), one class/feature per
  commit — see `git log`.
- **Mostly blackbox, through `Scoreboard`.** Most tests exercise the public API end-to-end
  (`ScoreboardTest`, `ScoreboardEdgeCasesTest`, `ScoreboardFinishedMatchesTest`) rather than
  reaching into internals — this is what the library is actually supposed to do, and it's what
  survives a refactor. A handful of focused unit tests cover the components where getting the
  logic subtly wrong is easy in isolation: `Score` validation, `Match`'s own lifecycle invariants,
  and `InMemoryMatchRepository`.
- **No mocks.** `InMemoryMatchRepository` is used directly in every test — it's simple, real,
  in-process code, so mocking it would only replace working code with a hand-rolled stand-in and
  make the tests describe implementation calls instead of behaviour.
- **`given` / `when` / `then` in every test**, named `should_...`, one behaviour per test.
- One test (`ScoreboardTest.should_order_matches_exactly_as_in_the_specification_example`)
  reproduces the exact scenario from the exercise text end-to-end.

## The chosen extra feature: finished match history

`getFinishedMatches()` returns matches that have been finished, most recently finished first.

**Why this one:** the exercise already gives a match a two-state lifecycle (in progress →
finished); once "finished" exists as a real state transition, being unable to see what got
finished is the obvious gap. It's the kind of feature that falls out of the domain model almost
for free — `Match` already tracks `finishTime` — rather than being bolted on, and it's something a
real scoreboard consumer (a results ticker, a "today's scores" screen) would need immediately.

Alternatives considered:
- Score-update validation (no negative / no decrease) — implemented anyway as part of the core
  invariants (assumption 4), so it didn't stand on its own as *the* extra feature.
- Elapsed match time in the summary — cheap, but it doesn't introduce a new capability, only a
  derived field.

## Trade-offs

| Decision | Trade-off accepted |
|---|---|
| In-memory storage only | No persistence across restarts; simplicity and zero extra dependencies over durability |
| Not thread-safe | Simpler code and tests; unsafe for genuinely concurrent callers without an external lock |
| Single flat package | Easy to navigate at this size; would need restructuring if the library grew significantly |
| `MatchId` instead of team-name lookups | One extra step for callers (hold onto the id); avoids ambiguity when the same fixture repeats |
| Absolute score updates, not deltas | Matches the spec's example directly; caller is responsible for computing the new total |
| Domain exceptions instead of generic ones | More types to know about; much clearer contract per method |

## Out of scope

- Persistence (see assumption 7).
- Concurrency safety (see assumption 8).
- Correcting a previously entered wrong score (would need an explicit "correction" operation with
  its own semantics — deliberately not designed here to avoid guessing requirements).
- Any transport/serialization layer (REST, JSON) — this is a library, not a service.
