# Prompt history

This is the raw session transcript backing `AI.md`'s condensed summary: the actual prompts
exchanged with Claude Code (Sonnet 5) while building this library, kept in the language the
session happened in (Polish), with top-level responses condensed to their substance (tool calls,
file diffs, and command output are omitted — those are visible in `git log` and the working tree
itself). Nothing here is reconstructed after the fact; it reflects what was actually asked and
decided, in order.

---

## 1. Kickoff — methodology and task brief

**User** pasted the full exercise brief (English) and asked, in Polish: whether to run this with
Claude Code's plan mode or with OpenSpec, noting OpenSpec seemed more token-efficient; asked Claude
to decide and advise; asked the session to be nameable as "sportradar recruitment" for later
reference.

**Claude** recommended native plan mode over OpenSpec: this is a one-shot, single-PR exercise, not
a codebase evolving over many sessions, so OpenSpec's change-proposal/spec-file machinery adds
overhead without payoff; the plan file itself can double as an AI.md artifact. Noted there is no
tool available to rename the session from within the conversation — suggested doing it via the
Claude Code UI instead.

## 2. Clarifying questions before planning

**Claude** asked three questions before writing the plan:
1. Which extra operation (spec point 5) to add.
2. Java version / test stack.
3. Whether the library should be thread-safe.

**User** answered:
- Extra operation: finished match history (the recommended option).
- Stack: Java 21, AssertJ for assertions — plus explicit process instructions: work iteratively in
  small chunks, use TDD to avoid going off track, name tests `should_...` with an explicit
  given/when/then split, keep tests reasonably scoped (mostly blackbox on the whole library's
  behaviour, plus unit tests on the more important components, not a test per component), use as
  much Domain-Driven Design as fits, **do not use Mockito** — if storage is needed use either a
  hand-rolled in-memory repository or SQLite, and ask further questions if anything else about the
  approach was unclear.
- Thread-safety: no — document it as an explicit assumption/limitation.

## 3. Plan

**Claude** decided storage should be **in-memory only, not SQLite**: the brief repeatedly calls
this a "simple library" with no stated persistence requirement, so a JDBC dependency would be
disproportionate; kept `MatchRepository` as an interface so a persistent adapter could be added
later without touching `Scoreboard`. Wrote the full plan (assumptions and reasoning, package
layout, public API sketch, an ordered list of TDD steps, one commit per step, verification
approach) to the plan file for review.

**User**, on reviewing the plan: *"będziesz robił to iteracyjnie?"*

**Claude** confirmed: each plan step is its own red-green-refactor cycle with its own commit, not a
bulk write followed by tests at the end. Plan approved as-is.

## 4. Implementation (autonomous)

Implemented exactly per plan, step by step, no further mid-flight direction: `Score` → `Match` →
`MatchRepository`/`InMemoryMatchRepository` → `Scoreboard` core flow (including a test reproducing
the exact ordering example from the brief) → edge cases → the finished-matches-history feature (its
own distinct commit, as the brief requires for the chosen extra operation) → `README.md` / `AI.md`.
Each step: write a failing test, run `mvn test` and confirm it fails for the right reason,
implement the minimum to pass, re-run to confirm green, commit. Result reported back: 8 commits, 35
tests, `mvn package` clean.

## 5. First review round — encapsulation

**User**, after opening the project in IntelliJ: *"co myślisz o tym że Match łamie zasady
enkapsulacji - niby pola są private ale później rozbebeszamy je używając getterów - może Match
mógłby dostarczać metodę która by zwracała MatchSummary - wdyt?"*

**Claude** agreed: `MatchSummary.from(Match)` (and `Scoreboard`) reaching into `Match`'s getters to
assemble a DTO is feature envy. Recommended `Match.toSummary()`, and flagged the same pattern
elsewhere in `Scoreboard` (`status() == MatchStatus.IN_PROGRESS`, manual
`homeTeam().equals(x) || awayTeam().equals(x)`) — offered to close those too with
`isInProgress()`/`isFinished()`/`involves(team)`, and to move the sort comparators onto `Match`
itself as static fields (since they'd then need access to fields that no longer have public
getters). Asked whether the user wanted just `toSummary()` or the full closure.

**User**: *"zdecydowanie pełne domknięcie, toSummary to za mało. Zanim zaczniesz to robić to co
myślisz o klasie Score - tam dokładnie to samo jest łamane poprzez getter do homeGoals i awayGoals
- może tutaj jakiś scoreSummary czy to już wg ciebie overengineering?"*

**Claude**: a `ScoreSummary` type would be overengineering — `Score` already is that value object,
duplicating it as a second type doesn't buy anything. The real instance of the same smell is
narrower: `Match.updateScore()` manually comparing `newScore.homeGoals() < score.homeGoals()`
instead of asking `Score` to make that comparison itself. Proposed `Score.hasDecreasedFrom(Score
previous)`, and — since `toSummary()` was already agreed — proposed `MatchSummary` hold a `Score`
object rather than flattened `homeGoals`/`awayGoals` ints, so there's one representation of "a
score" in the codebase. Implemented both, test-first, in a few commits: `Score.hasDecreasedFrom`
first (isolated, its own commit), then the full `Match`/`MatchSummary`/`Scoreboard` closure
together (coupled changes, one commit), then updated `README.md`/`AI.md` to explain the change.

## 6. Second review round — test organization

**User**: *"jeszcze nie podoba mi się to że testy do scoreboarda są w 3 plikach - przerzuć testy
specyficzne edgecases i finishedmatches jak nested do scoreboardtest i nazwij je jakoś sensownie (o
ile finishedmatches to spoko podgrupa, to edgecases nic nie mówi - może można je złapać w jakieś
fajne podgrupy te edge case'y?)"*

**Claude** merged `ScoreboardTest`, `ScoreboardEdgeCasesTest`, and `ScoreboardFinishedMatchesTest`
into one file using JUnit 5 `@Nested` classes sharing one `setUp()`. Agreed "edge cases" wasn't a
real category and split it by what the tests actually verified:
`StartingAMatch` (the duplicate-team-in-progress rule), `UnknownMatch`
(`MatchNotFoundException` on an unrecognized id), and `InvalidOperations` (`Match`'s own
lifecycle invariants — decreasing score, updates after finish — surfacing correctly through
`Scoreboard`). Kept `Summary` and `FinishedMatches` as the other two groups. Re-ran the full suite
(38 tests, same count as before, all green) to confirm the reorganization was behaviour-neutral.

## 7. This file

**User** asked for this transcript to be written to `prompts.md` for the recruiter to read, and
asked whether Claude was happy with the result.

**Claude**: yes — the domain model is small but has real invariants enforced in the right place,
every step was driven by a failing test first, and the two review rounds materially improved the
design rather than being rubber-stamped. If anything, `README.md` is long for a "simple library" —
kept that way deliberately, since the brief explicitly rewards documented reasoning over brevity.
