# Open Questions for Product and Architecture Review

This document separates decisions that can be made from engineering judgment from decisions that require product-owner input or real deployment constraints.

## Product
1. Is engineering knowledge the intended long-term wedge, or should the ontology be deliberately domain-neutral from the start? Current recommendation: keep the core model generic but make the first product workflows engineering-specific.
2. Who is the first actual user: individual developer, team lead, platform engineering team, or engineering organization? Current MVP personas cover all four roles but evaluation should prioritize engineers and maintainers.
3. Should manual knowledge publication require review before becoming visible? Current recommendation: contributor-authored items are visible within their scope; extracted relationships remain proposals unless deterministic policy permits acceptance.
4. What is the desired product boundary around source content storage versus pointers? Current recommendation: store bounded normalized snapshots and provenance; define retention before real customer data.

## Identity and security
5. Which identity provider is required for production? Local development identity is only a development convenience.
6. Which source ACL semantics must be supported first? This is connector-specific and cannot be safely guessed.
7. What are the retention, deletion, backup, and data residency requirements?

## Technical
8. Which embedding provider/model provides the best quality/cost/privacy trade-off on the benchmark?
9. Which LLM provider/model is acceptable for extraction, considering source data sensitivity?
10. Does the selected MCP client support delegated user identity in the desired hosting mode?
11. What deployment target and budget should constrain the operational design?

## Evaluation
12. What benchmark size is large enough to be useful for the intended review? Start with a manually labeled synthetic set and expand through failure analysis.
13. Which tasks should define success for an AI coding agent: answer correctness, time-to-context, code-change quality, or reduced hallucination? Current MVP measures retrieval first; downstream coding quality requires a separate controlled experiment.
