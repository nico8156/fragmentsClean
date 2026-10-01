# Iteration Workflow — Emergent Design

This workflow governs Fragments backend iterations and complements the existing
architecture routing and orchestrators under `.agents/`.

The architecture route answers **where the change belongs**. The iteration type
answers **how the change must be discovered and delivered**. Always classify
both axes before implementation.

## Iteration types

### `BEHAVIOUR`

Use when an observable product or business behavior changes.

Construct the change through emergent TDD. Start from one concrete example at
the closest stable hexagonal boundary. Do not design the complete solution and
then write tests that confirm it.

### `PIN`

Use when existing behavior needs characterization because it is insufficiently
protected, a regression exists, or a mutant survived.

Write the smallest test that exposes the missing protection. Production
behavior must not change unless the iteration is reclassified as `BEHAVIOUR`.

For a surviving mutant, the new test must pass on the original implementation
and fail on the mutated implementation for the expected behavioral reason.
This differential check is the `PIN` evidence; do not demand an artificial RED
on correct production code.

### `REFACTORING`

Use when structure changes while observable behavior stays unchanged.

Begin from a green, adequate suite. Do not manufacture a failing test. Add a
`PIN` first only when the behavior required to refactor safely is not protected.

### `CHORE`

Use for documentation, configuration, scripts, dependency maintenance, or
other work that does not introduce product behavior.

Choose proportionate static, build, or operational verification. Do not create
a ceremonial domain test.

## Behaviour trajectory

Before the first `BEHAVIOUR` test, expose a trajectory rather than a target
implementation:

```text
Observable outcome
Acceptance boundary
First concrete example
Likely next examples
Known product or design uncertainties
Final evidence expected
```

The trajectory is provisional. Revise it after each cycle when the code reveals
new information. Do not list classes, tables, interfaces, or patterns as decided
work unless they already exist or an architectural decision has been made.

## Emergent TDD cycle

```text
example
-> RED
-> RED Inspector
-> minimum GREEN
-> GREEN Inspector
-> local REFACTOR
-> Refactor Inspector
-> update trajectory
-> next example or completion
```

Before completing a meaningful behavioral slice, apply the targeted mutation
checkpoint below. It is not required after every micro-cycle.

An agent may anticipate a likely final design, but anticipation is not
permission to implement it. Production code and abstractions must be justified
by the current failing example or by an existing architecture invariant.

### RED Inspector

Return one verdict:

- `PASS`: the test is the smallest useful behavioral question and fails for the
  expected missing behavior; continue automatically.
- `REVIEW`: the test is valid but reveals a non-blocking implication; record it
  and continue.
- `ESCALATE`: the example contains product ambiguity, changes an invariant, or
  commits to one of several materially different domain models; stop for a
  human decision.

Reject a RED that:

- describes an anticipated implementation instead of behavior;
- bundles several independent examples;
- is already green;
- fails because of broken setup or unrelated code;
- invents a product rule not present in the request, accepted examples, or
  existing product doctrine.

### GREEN Inspector

Confirm that:

- the focused test and relevant existing tests pass;
- production code contains only what the current example requires;
- no future branch, abstraction, event, port, or configuration was added only
  because it may become useful later;
- architecture invariants remain respected.

If the implementation changes behavior beyond the current example, reduce it
or add the missing example before keeping that behavior.

### Refactor Inspector

Refactor only from green. Local naming, duplication removal, and responsibility
clarification may continue automatically. Escalate when a refactor would move
ownership across bounded contexts, introduce a new architectural abstraction,
or change a public contract.

## Targeted mutation checkpoint

Mutation testing deliberately changes production code temporarily to check
whether tests detect an observable fault. Input fuzzing, boundary examples,
code coverage, and merely proposing a mutation are not mutation execution.

### When and where

- For `BEHAVIOUR`, run targeted mutations once the meaningful slice is green,
  before declaring its changed business decisions protected.
- For `REFACTORING` that affects business decisions or algorithms, challenge
  the existing protection before restructuring; insert `PIN` when needed.
- For `PIN`, rerun the specific mutation that exposed the gap.
- For documentation, formatting, wiring-only chores, or purely visual changes,
  mark mutation testing not applicable with a short reason.

Keep the scope bounded to changed decisions and relevant tests. Prefer fast
domain/application tests and frontend reducers, selectors or use cases. Do not
run a whole-repository campaign or expensive infrastructure suite for every
cycle. Real database, transaction and transport guarantees still require their
own integration evidence.

Choose faults relevant to the contract: invert a condition, alter an exact
boundary, change an arithmetic operation or return value, or omit a required
state transition or event. Do not invent new product rules to kill mutants.

### Execution

1. Run the selected tests on the original code and establish a green baseline.
2. Prefer an existing configured mutation runner. If none exists, a bounded
   manual mutation is acceptable; report it explicitly as manual, without a
   campaign-wide score. Tool installation is a separate tooling change.
3. Execute each manual mutation independently in an isolated temporary copy
   containing the current relevant changes, or with an exact reversible patch.
   Preserve pre-existing edits and restore only the mutation, including after
   errors. Never commit, deploy or leave mutated production code behind.
4. Run the selected tests and inspect the actual failure. Compilation errors,
   setup failures, timeouts and tool errors do not prove a behavioral kill.
5. Classify the results, then restore the original code and verify the final
   relevant suite is green. Inspect the diff for leftover mutations.

### Mutation Inspector

- `KILLED`: a behavioral assertion detects the intended fault; record the test.
- `SURVIVED`: execution succeeded but no test detected the mutation; investigate.
- `NO_COVERAGE`: the target was not exercised; inspect the missing example or
  test selection rather than treating this as success.
- `INVALID/ERROR`: mutation could not be evaluated reliably; repair or report
  the verification limitation, never count it as a behavioral kill.

Resolve every survivor in the selected scope:

- missing protection for an agreed behavior: switch to `PIN`, add the smallest
  behavioral example, prove it passes on the original and fails on the mutant;
- equivalent mutation: explain why no observable contract behavior differs;
- outside the accepted contract: document the boundary and justification;
- uncertain expected behavior: `ESCALATE` for a product decision;
- real defect in the original: switch to `BEHAVIOUR`, reproduce the defect on
  the original code and fix it through TDD.

Continue known `PIN` corrections autonomously, then return to the original
iteration. Do not weaken assertions, exclude difficult mutants silently, or
change correct production behavior just to improve a mutation score. There is
no blanket 100% score target. An unresolved relevant survivor prevents claiming
that the selected behavior has passed the mutation checkpoint.

### Evidence

Record in the iteration's working document, or the handoff if none exists:
target and test scope, baseline command/result, runner or exact manual change,
mutation outcome, killing test or survivor disposition, `PIN` examples added,
and restored-code verification. Distinguish `EXECUTED`, `NOT APPLICABLE`, and
`NOT RUN` with reasons. If execution is unavailable, report the missing evidence
explicitly; a proposed mutation is not a passed checkpoint.

## Autonomy and human review

Do not stop after every RED. Continue mechanical cycles while inspectors return
`PASS` or `REVIEW` and the accepted trajectory remains valid.

Human review is required for:

- an ambiguous or missing product rule;
- a new or changed domain invariant;
- a bounded-context ownership decision;
- multiple materially different valid models;
- a public contract or migration choice with significant compatibility impact;
- a scope change beyond the requested outcome.

Tests provide feedback; they do not authorize the agent to invent requirements.

## Completion evidence

At completion report:

- iteration type and architecture route;
- examples that drove the design;
- design discoveries that changed the initial trajectory;
- focused and broader verification performed;
- mutation checkpoint status, scope, survivor dispositions and restoration proof;
- unresolved decisions or deliberately deferred examples.

## Repository application

Use fast aggregate/handler examples with named fakes. After a relevant green
slice, challenge replay guards, exact counters, command-status decisions or
lease/stale-completion checks. These are candidate faults, not instructions to
implement new behavior. SQS, inbox, transactions and persistence still need
integration tests. Root ownership and offline reconciliation rules remain binding.
