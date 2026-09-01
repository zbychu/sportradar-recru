# AI usage

## Summary

This library was built with **Claude Code** (Anthropic, Sonnet 5 model), used as an interactive
pair-programmer for the entire exercise: design discussion, planning, and implementation. AI did
the typing; every design decision below was made deliberately and reviewed before being accepted,
not auto-generated and left unchecked. All code was produced test-first (TDD, red → green, one
commit per class/feature) with the AI running `mvn test` after each step and only proceeding once
the test suite confirmed the intended behaviour.

## Workflow

1. **Planning.** Used Claude Code's plan mode (not a spec-tracking tool like OpenSpec) to turn the
   exercise text into a concrete design before writing any code. Plan mode was chosen deliberately
   over OpenSpec: this is a one-shot, single-PR exercise, not a codebase that evolves over many
   sessions, so OpenSpec's change-proposal/spec-file machinery would have added overhead without a
   payoff. The full plan (assumptions, package layout, public API, ordered TDD steps) is preserved
   verbatim below as the artifact that guided implementation.
2. **Clarification.** Before finalizing the plan, the AI asked targeted questions on the points
   that were genuinely open calls rather than deriving them silently: which extra operation to add,
   the Java/test stack, and whether the library should be thread-safe.
3. **Implementation.** Each class was added as its own red → green → commit cycle, in the order
   fixed by the plan (`Score` → `Match` → `MatchRepository` → `Scoreboard` core flow → edge cases →
   the extra feature → docs). For every step: write a failing test, run `mvn test` and read the
   compiler/test failure to confirm it fails for the right reason, implement the minimum to make it
   pass, re-run to confirm green, commit.
4. **Documentation.** README.md and this file were written first-pass once the initial
   implementation and its trade-offs were settled.
5. **Human code review.** After opening the repo in IntelliJ, the user flagged a real
   encapsulation problem: `Match` exposed getters for every field, and `MatchSummary.from(Match)`
   (plus parts of `Scoreboard`) reached in and picked them apart — feature envy rather than "tell,
   don't ask". The user also asked, before agreeing to the fix, whether the same problem existed on
   `Score` (it did — `Match.updateScore` was manually comparing `homeGoals()`/`awayGoals()`) and
   whether a `ScoreSummary` type was warranted for it (AI's answer: no, that would duplicate `Score`
   itself — the fix was a `Score.hasDecreasedFrom(previous)` method, not a new type). AI then
   implemented the full closure test-first: `Score.hasDecreasedFrom`, `Match.toSummary()` /
   `isInProgress()` / `isFinished()` / `involves(team)`, the ordering comparators moved onto `Match`
   as static fields, and `MatchSummary` changed to carry a `Score` instead of flattened ints — each
   as its own red→green→commit cycle, with the full suite re-run green after every step. This is
   real, valuable review feedback that changed the shipped design, which is why it's recorded here
   rather than silently folded into the "first pass" story above.

## Prompt history (condensed)

The exercise brief (verbatim, in English) was pasted first. What follows is the condensed sequence
of decisions made in conversation, in order:

1. *"Should this be done with plan mode or OpenSpec, given OpenSpec seems more token-efficient?
   You decide and advise me."* → AI recommended native plan mode, reasoning that OpenSpec's
   spec/proposal overhead doesn't pay off for a single bounded exercise; the plan file doubles as
   the AI.md artifact.
2. AI asked three clarifying questions (via a structured question tool):
   - Which extra operation (spec point 5) to add → **finished match history**.
   - Java version / test stack → **Java 21, JUnit 5, AssertJ**, plus explicit process instructions:
     work iteratively in small chunks, follow TDD, name tests `should_...` with explicit
     given/when/then, favour blackbox tests over the full library with a few targeted unit tests on
     the more important components, use as much DDD as is proportionate, **do not use Mockito** —
     if storage is needed use either a hand-rolled in-memory repository or SQLite, and ask further
     questions if anything else was unclear.
   - Should the library be thread-safe → **no**, documented as an explicit scope decision.
3. AI decided, and flagged for review, that storage should be **in-memory only, not SQLite** — the
   exercise calls for a "simple library" with no stated persistence requirement, so a JDBC
   dependency would be disproportionate; a `MatchRepository` interface keeps the door open. This
   was written into the plan for the user to accept or override.
4. AI wrote the full plan (assumptions/decisions, package structure, public API sketch, the ordered
   TDD steps, verification approach) to the plan file.
5. User asked, on reviewing the plan: *"będziesz robił to iteracyjnie?"* ("will you do this
   iteratively?") → AI confirmed each plan step is its own red-green-refactor cycle with its own
   commit, not a single bulk write followed by tests. Plan approved as-is.
6. Implementation proceeded exactly per plan, step by step, with no further mid-flight direction
   from the user — each TDD cycle's failing test, minimal implementation, and passing run are
   visible in the corresponding commit.

## Artifacts

- The approved implementation plan (assumptions and reasoning, package layout, public API,
  step-by-step TDD sequence, verification approach) is reproduced in full below — it is the primary
  artifact that guided the implementation and matches what was actually built.
- `git log` is itself an artifact of the process: one commit per red-green TDD cycle, in the exact
  order the plan specified, ending with a distinct commit for the chosen extra feature
  (`Add finished matches history feature`).

<details>
<summary>Full approved plan (as written before implementation started)</summary>

```markdown
# Live Football World Cup Scoreboard — Sportradar recruitment exercise

## Context

Zadanie rekrutacyjne od Sportradar: biblioteka Java (Maven) zarządzająca tablicą wyników na żywo
meczów piłkarskich (start meczu, aktualizacja wyniku, zakończenie meczu, podsumowanie meczów
w toku posortowane wg łącznego wyniku malejąco / przy remisie wg czasu startu malejąco, plus jedna
dodatkowa operacja własnego wyboru). Zadanie celowo zostawia otwarte pytania projektowe — część
oceny polega na podjęciu i udokumentowaniu decyzji (README.md), a AI.md ma dokumentować sposób
użycia AI w procesie.

Ustalenia z użytkownikiem:
- Metodyka: natywny plan mode, nie OpenSpec.
- Dodatkowa operacja: historia zakończonych meczów (getFinishedMatches).
- Stack: Java 21, JUnit 5 + AssertJ, TDD iteracyjnie, testy should_... z given/when/then,
  blackbox + wybrane testy jednostkowe, bez Mockito, DDD proporcjonalnie do skali, brak
  thread-safety (świadome ograniczenie).
- Storage: in-memory, bez SQLite — uzasadnienie w README.

## Decyzje projektowe (skrót — pełne uzasadnienia w README.md)

1. MatchId zwracany ze startMatch, kolejne wywołania po id.
2. Drużyna nie może grać w dwóch meczach w toku jednocześnie.
3. updateScore przyjmuje wartości bezwzględne, nie delty.
4. Wynik nie może się zmniejszyć podczas aktualizacji.
5. Operacje na zakończonym meczu są odrzucane, nie ignorowane.
6. Kolejność podsumowania: suma goli malejąco, remis → czas startu malejąco.
7. Storage in-memory (LinkedHashMap) za interfejsem MatchRepository, bez SQLite.
8. Brak thread-safety — świadoma decyzja zakresu.
9. Zegar wstrzykiwany (java.time.Clock) zamiast Instant.now() rozsianego w kodzie.
10. Walidacja nazw drużyn (niepuste, drużyna nie gra sama ze sobą).

## Struktura projektu

Maven, groupId com.sportradar, artifactId football-scoreboard, Java 21, jeden pakiet
com.sportradar.scoreboard: Scoreboard (fasada), Match (encja/aggregate root), MatchId i Score
(value objects), MatchStatus (enum), MatchSummary (read-model), MatchRepository + 
InMemoryMatchRepository, dedykowane wyjątki domenowe.

Publiczne API:
  MatchId startMatch(String homeTeam, String awayTeam)
  void updateScore(MatchId id, int homeGoals, int awayGoals)
  void finishMatch(MatchId id)
  List<MatchSummary> getSummary()
  List<MatchSummary> getFinishedMatches()

## Plan iteracji TDD (małe kroki, osobne commity)

1. Setup: pom.xml (Java 21, JUnit5, AssertJ), .gitignore, git init, szkielet pakietów.
2. Score (VO): walidacja nieujemnych wartości, total().
3. Match (encja): cykl życia start/update/finish, invarianty.
4. MatchRepository + InMemoryMatchRepository.
5. Scoreboard — core flow: start/update/finish/getSummary z sortowaniem, blackbox test
   odtwarzający dokładnie przykład z treści zadania.
6. Edge cases blackbox: duplikat drużyny, update po finish, ujemny/malejący wynik, nieznany
   MatchId.
7. Dodatkowa funkcja — historia zakończonych meczów: osobny, wyraźny commit.
8. Dokumentacja: README.md i AI.md.

## Weryfikacja

- mvn test po każdym kroku (czerwony→zielony cykl dokumentowany w commitach).
- Dedykowany test blackbox odtwarzający dokładnie przykładową kolejność wynikową z treści
  zadania.
- mvn package na końcu.
- Przegląd git log — commity czytelne, atomowe, w tym wyraźny commit dla dodatkowej funkcji.
```

</details>
