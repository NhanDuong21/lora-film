# OJT GitLab Workflow & Branching Guidelines

This document serves as the single source of truth for the Git and GitLab workflow rules for our 5-member On-the-Job Training (OJT) team. Adhering to these standards ensures code quality, traceability, and seamless integration across our microservices architecture (`client`, `server`, `api-gateway`).

---

## 1. Introduction

In a collaborative microservices environment, workflow discipline is paramount. With multiple developers concurrently changing the frontend, backend, and API gateway components, ad-hoc branching or direct commits will inevitably lead to broken builds, lost code, and integration blockages. 

By enforcing structured branching, conventional commits, systematic issue board transitions, and rigorous merge request reviews, we:
*   Ensure that every change is associated with an approved **GitLab Issue**.
*   Maintain a clean, readable, and searchable project history.
*   Prevent regression bugs from entering our integration (`develop`) and production (`main`) branches.
*   Foster a culture of continuous peer review and mentorship under our Team Leader.

---

## 2. Branching Strategy

Our repository uses a Git Flow-inspired branching strategy designed to keep the stable production code separated from ongoing development.

```mermaid
gitGraph
    commit id: "Initial Commit"
    branch develop
    checkout develop
    commit id: "Setup develop"
    branch docs/issue-#2-gitlab-workflow-guidelines
    checkout docs/issue-#2-gitlab-workflow-guidelines
    commit id: "docs(workflow): add team gitlab workflow"
    checkout develop
    merge docs/issue-#2-gitlab-workflow-guidelines id: "Merge MR #2"
    checkout main
    merge develop tag: "v1.0.0"
```

### Core Branches

| Branch | Protection Status | Purpose | Rules |
| :--- | :--- | :--- | :--- |
| `main` | **Strictly Protected** | Production-ready state. Represents the latest stable deployment. | Direct pushes are forbidden. Code only arrives via Merge Requests (MRs) merged from `develop` by the Team Leader. |
| `develop` | **Strictly Protected** | Main integration branch. All features, bug fixes, and documentation are merged here first. | Direct pushes are forbidden. All modifications must be made via local task branches and merged through MRs. |

### Feature-Specific Prefixes

All development tasks must be executed on a dedicated branch cut from the latest `develop` branch. Branches must use one of the following prefixes based on the nature of the task:

| Prefix | Category | Usage Description |
| :--- | :--- | :--- |
| `feature/` | Features | Developing a new user feature or functional component. |
| `fix/` | Bug Fixes | Fixing a bug, resolving a runtime error, or correcting broken logic. |
| `docs/` | Documentation | Creating, editing, or updating Markdown files, API specs, or guides. |
| `setup/` | Setup & Environment | Configuring project setup, CI/CD pipelines, Docker configurations, or directory structures. |
| `test/` | Testing | Adding or updating unit tests, integration tests, or end-to-end testing configurations. |

> [!IMPORTANT]
> **Strict Workflow Rule:** Every development task must map to a GitLab Issue. Direct pushes to `main` or `develop` are strictly prohibited. Always cut your branch from the latest `develop` branch.

### Branch Naming Conventions

To maintain uniformity and trace code back to project tasks, all branch names must follow this exact format:

$$\text{<prefix>/issue-\#<id>-<short-description>}$$

*   `<prefix>`: One of the prefixes listed in the table above (e.g., `feature`, `fix`, `docs`).
*   `<id>`: The GitLab Issue ID number.
*   `<short-description>`: A concise, lowercase, hyphen-separated (kebab-case) description of the task.

#### Examples:
*   `docs/issue-#2-gitlab-workflow-guidelines`
*   `feature/issue-#14-client-login-ui`
*   `fix/issue-#29-server-jwt-expiration`
*   `setup/issue-#5-docker-compose-environment`
*   `test/issue-#42-api-gateway-routing-tests`

---

## 3. Commit Message Guidelines

We enforce the **Conventional Commits** specification to keep our commit history clear, organized, and machine-readable.

### Commit Format

$$\text{<type>(<scope>): <description>}$$

*   **`<type>`**: Describes the intent of the commit (e.g., `feat`, `fix`, `docs`).
*   **`<scope>`** (Optional but highly recommended): The specific module or package affected, enclosed in parentheses. Common scopes for our project include: `client`, `server`, `api-gateway`, `docker`, `workflow`, `deps`, `config`.
*   **`<description>`**: A brief summary of the changes in the imperative, present tense (e.g., "add login form", NOT "added login form" or "adds login form"). The description should start with a lowercase letter and must not end with a period.

### Commit Types

| Type | Description |
| :--- | :--- |
| `feat` | A new feature or major addition to the codebase. |
| `fix` | A bug fix or correction to existing code. |
| `docs` | Documentation-only changes. |
| `style` | Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc.). |
| `refactor` | A code change that neither fixes a bug nor adds a feature (e.g., rewriting for performance or cleanliness). |
| `test` | Adding missing tests or correcting existing tests. |
| `chore` | Changes to the build process, auxiliary tools, libraries, or dependencies (e.g., updating package versions). |

### Concrete Examples

Here are exact commit messages formatted according to our rules:

```bash
# Feature addition in the client
feat(client): implement interactive navigation sidebar

# Bug fix in the server
fix(server): resolve null pointer exception in auth interceptor

# Documentation update
docs(workflow): add team gitlab workflow and branching guidelines

# Build tool/Dependency chore
chore(deps): upgrade axios to version 1.6.0 in client

# Test addition
test(api-gateway): add request rate limiting tests
```

---

## 4. GitLab Issue Board Workflow

Our GitLab Issue Board is the control center for our sprint progress. We track the status of all work items using scoped labels that form a clear pipeline from planning to completion.

```mermaid
stateDiagram-v2
    [*] --> status::ready : Issue Created
    status::ready --> status::in-progress : Assignee starts work / Cuts branch
    status::in-progress --> status::review : Push to GitLab & Open Merge Request
    status::in-progress --> status::blocked : Blocked by external task/issue
    status::blocked --> status::in-progress : Blocker resolved
    status::review --> status::done : MR Approved & Merged by Leader
    status::done --> [*]
```

### Scoped Labels & States

1.  **`status::ready`**
    *   **Meaning:** The issue has been groomed, estimated, and is ready for development. It is placed in the backlog or active sprint backlog.
    *   **Action:** When a developer is ready to start a new task, they select an issue from this column, assign it to themselves, and move it to the next column.
2.  **`status::in-progress`**
    *   **Meaning:** The developer is actively working on the task. A corresponding local branch has been created.
    *   **Action:** The developer writes code, commits changes locally using Conventional Commits, and pushes the branch to GitLab.
3.  **`status::review`**
    *   **Meaning:** The code is complete, tests are passing, and a Merge Request (MR) has been opened targeting the `develop` branch.
    *   **Action:** The developer sets the MR reviewers, links the issue, and moves the issue to this status on the board.
4.  **`status::blocked`**
    *   **Meaning:** Development cannot proceed due to external dependencies, unresolved architectural decisions, or bugs in other modules.
    *   **Action:** The developer moves the issue to this state, adds a comment explaining the blocker, and tags the Team Leader or relevant team members. Once resolved, it moves back to `status::in-progress`.
5.  **`status::done`**
    *   **Meaning:** The MR has been approved, conflicts resolved, pipeline passed, and the branch is successfully merged into `develop`.
    *   **Action:** The issue is automatically or manually closed and marked as Done.

> [!WARNING]
> **Issue Movement Policy:** Do not skip stages. Never move an issue to `status::done` yourself. The transition to `status::done` is strictly coupled with the merging of the MR or confirmation by the Team Leader.

---

## 5. Merge Request (MR) Process

Once you finish development and verify your changes locally, you must submit your code for review. The MR process acts as our gatekeeper for quality.

### Merge Request Pipeline Steps

1.  **Prepare your Branch:**
    Ensure your local branch is updated with the latest changes from remote `develop`.
    ```bash
    git checkout develop
    git pull origin develop
    git checkout docs/issue-#2-gitlab-workflow-guidelines
    git merge develop
    ```
2.  **Push and Create MR:**
    Push your branch to the GitLab repository and click the link in your terminal or navigate to GitLab to open a new Merge Request.
3.  **Configure the MR Details:**
    *   **Title:** Follow the commit convention (e.g., `docs(workflow): add team gitlab workflow and branching guidelines`).
    *   **Source Branch:** `<your-prefix>/issue-#<id>-<short-description>`
    *   **Target Branch:** `develop` (Never target `main` directly for regular tasks).
    *   **Description:** Use the Standard MR Template below to outline your changes.
    *   **Reviewer Assignment:** Mandatorily assign **Thành (Team Leader)** as the primary Reviewer. You may also tag other team members for peer review.
4.  **Review and Resolve Discussions:**
    *   Reviewer Thành will inspect the code, leave comments/feedback, and approve or request changes.
    *   Address all feedback by pushing additional commits to the same branch.
    *   Once all discussions are marked resolved and the Team Leader approves, the MR is ready to be merged.

### Standard MR Template

Copy and paste the template below into the GitLab MR description box:

```markdown
## Summary
This MR introduces the definitive GitLab Workflow Guideline for the OJT team. It details the rules for branching, commit formats, issue board transitions, and the merge request review cycle to maintain repository health and team alignment.

## Related Issue
Closes #2

## Changes
* Created `docs/gitlab-workflow.md` containing full development lifecycle documentation.
* Added branch naming rules and examples (`feature/*`, `fix/*`, `docs/*`, etc.).
* Standardized commit messages based on Conventional Commits.
* Outlined the `status::` label pipeline for the issue board.

## How to Test
1. Checkout to this branch: `git checkout docs/issue-#2-gitlab-workflow-guidelines`
2. Open and review `docs/gitlab-workflow.md` using a Markdown viewer or IDE preview.
3. Verify that all rules match Sprint 0 requirements exactly.

## Checklist
- [x] Documentation follows the repository structure rules.
- [x] No direct pushes to main/develop are implied.
- [x] Scoped status labels match the board configuration.
- [x] Formatting and syntax are clean and readable.
```

---

## 6. Definition of Done (DoD)

A task is never complete just because the code is written. To ensure that our codebase remains stable and deployable, every issue must meet the following **Definition of Done (DoD)**:

*   [ ] **Code Quality:** Code complies with clean code principles, has no unused imports, commented-out blocks, or temporary debugger statements.
*   [ ] **Build & Tests:** The application builds locally without errors. All unit and integration tests run successfully.
*   [ ] **Git Conformity:** Branch naming and commit messages follow the guidelines in this document.
*   [ ] **Review & Approvals:** The MR has been reviewed and approved by the Team Leader (**Thành**).
*   [ ] **Integration:** The MR is successfully merged into the `develop` branch.
*   [ ] **Closing Issues:** The associated GitLab Issue is closed.

> [!CAUTION]
> **Important Rule on Done Status:** An issue is only considered DONE when the Merge Request has been officially merged into `develop` or confirmed/closed by the Team Leader. **Members must not close issues prematurely.**
