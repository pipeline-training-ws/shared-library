# CI Shared Library

A **Jenkins Shared Library** providing reusable pipeline steps, opinionated pipeline templates, and Kubernetes pod templates for CloudBees CI / Jenkins on Kubernetes.

> **Training repository** — part of the `pipeline-training-2026` workshop. Consume it in your own Jenkinsfiles or use the bundled pipeline templates as a starting point.

---

## Table of Contents

- [Repository Layout](#repository-layout)
- [Prerequisites](#prerequisites)
- [Loading the Library](#loading-the-library)
- [Pipeline Templates](#pipeline-templates)
  - [pipelineTemplateHelloWorld](#pipelinetemplatehelloworld)
- [Global Variables (Steps)](#global-variables-steps)
- [Resources](#resources)
  - [Pod Templates](#pod-templates)
  - [Integration Scripts](#integration-scripts)
- [Sample Usage](#sample-usage)
- [Credential Requirements](#credential-requirements)
- [Contributing](#contributing)

---

## Repository Layout

```
shared-library/
├── vars/                          # Global variables exposed as pipeline steps
│   ├── pipelineTemplateHelloWorld.groovy
│   ├── buildMaven.groovy
│   ├── initPodTemplate.groovy         # unused — see note below
│   ├── newSemanticVersion.groovy
│   ├── gitShortCommit.groovy
│   ├── getGitBranches.groovy          # experimental, not used by any template
│   ├── jfrogUploadArtifact.groovy
│   ├── jfrogDownloadArtifact.groovy
│   ├── jiraCreateIssue.groovy
│   └── helloWorld.groovy
└── resources/                     # Non-Groovy resources accessed via libraryResource()
    ├── podtemplates/
    │   └── agent.yaml             # single-container (maven) pod template
    ├── jfrog/                     # reference cURL scripts (not wired into the vars/ steps)
    ├── jira/                      # reference cURL script + payload template (not wired into jiraCreateIssue)
    └── newSemanticVersion/        # semantic versioning shell script, used by newSemanticVersion.groovy
```

There is no `pipelineTemplateMaven` step or `templates/` directory in this repository (both were
removed). `pipelineTemplateHelloWorld` is the only pipeline template shipped here.

End-to-end usage examples for the JFrog and Jira steps live in the sibling
[`pipeline-samples/ci-jfrog-integration/`](../pipeline-samples/ci-jfrog-integration) and
[`pipeline-samples/ci-jira-integration/`](../pipeline-samples/ci-jira-integration) directories, not under `shared-library/`.

---

## Prerequisites

| Requirement | Notes |
|---|---|
| Jenkins ≥ 2.387 / CloudBees CI | Declarative Pipeline support required |
| Kubernetes plugin | Pod-based agents (`kubernetes { }` agent block) |
| Pipeline Maven plugin | Required by `buildMaven` step |
| Credentials Plugin | For JFrog and Jira credential bindings |
| `jq` in agent image | Required by `getGitBranches` step |

---

## Loading the Library

### Option 1 — Global / Folder configuration (recommended)

Configure the library once in **Manage Jenkins → System → Global Pipeline Libraries** (or at folder level in CloudBees CI):

| Field | Value |
|---|---|
| Name | `shared-library` |
| Default version | `main` |
| SCM | Git — `https://github.com/pipeline-training-ws/shared-library.git` |

Then reference it implicitly from any Jenkinsfile (auto-loaded), or explicitly:

```groovy
@Library('shared-library') _
```

### Option 2 — Inline `library` step

Every Jenkinsfile in this repo actually resolves the library's coordinates from `SHAREDLIB_GIT_*`
environment variables (defaulted in-line, overridable at folder/controller level) rather than a
hardcoded identifier — see [`sample-app-helloWorld/Jenkinsfile`](../sample-app-helloWorld/Jenkinsfile)
for the full pattern:

```groovy
env.SHAREDLIB_GIT_SERVER = env.SHAREDLIB_GIT_SERVER ?: "https://github.com"
env.SHAREDLIB_GIT_ORG = env.SHAREDLIB_GIT_ORG ?: "pipeline-training-ws"
env.SHAREDLIB_GIT_REPO = env.SHAREDLIB_GIT_REPO ?: "shared-library"
env.SHAREDLIB_GIT_TAG_ = env.SHAREDLIB_GIT_TAG ?: "main"
env.SHAREDLIB_GIT_CREDENTIALS = env.SHAREDLIB_GIT_CREDENTIALS ?: "gh-pat"

library identifier: "${env.SHAREDLIB_GIT_REPO}@${env.SHAREDLIB_GIT_TAG_}", retriever: modernSCM(
    [$class: 'GitSCMSource',
     remote: "${env.SHAREDLIB_GIT_SERVER}/${env.SHAREDLIB_GIT_ORG}/${env.SHAREDLIB_GIT_REPO}.git",
     credentialsId: "${env.SHAREDLIB_GIT_CREDENTIALS}"
    ])
```

---

## Pipeline Templates

### `pipelineTemplateHelloWorld`

A minimal template that runs a greeting stage on a Kubernetes agent. Designed to validate library loading and pod-template resolution. Takes the **path to a YAML config file** (not a map) and reads its `ci:` block itself.

**Parameter**

| Key | Type | Description |
|---|---|---|
| `configFile` | String | Path to a YAML file (e.g. `ci-config.yaml`) with a `ci:` block containing `hello`, `firstName`, `lastName` |

**Jenkinsfile**

```groovy
@Library('shared-library') _

pipelineTemplateHelloWorld('ci-config.yaml')
```

```yaml
# ci-config.yaml
ci:
  hello: CloudBees CI
  firstName: Jane
  lastName: Doe
```

**Stages**

| Stage | Branch filter | What it does |
|---|---|---|
| CI / Load Config | all | Reads `configFile` via `readYaml(file: configFile).ci` |
| CI / Hello World | all | `echo Hello <hello>` |
| CI / Hi | `main` only | `echo Hi <firstName> <lastName>` |

---

## Global Variables (Steps)

### `buildMaven`

Wraps the **Pipeline Maven** plugin's `withMaven` block and executes the steps listed in `config.ci.maven.steps`. After the build, JUnit results and WAR artifacts are automatically archived.

```groovy
buildMaven(config)
```

> **Tip:** Configure Maven settings globally at the folder level in CloudBees CI to keep credentials out of the Jenkinsfile.

---

### `initPodTemplate`

Resolves a Kubernetes pod template YAML named by `config.ci.podyaml` from the library's `resources/podtemplates/` directory and performs token substitution for `${MAVEN_IMAGE}` and `${KANIKO_IMAGE}` using values from the CI config.

```groovy
// Returns rendered YAML string — used inside an agent block
agent {
    kubernetes {
        yaml initPodTemplate(config)
        defaultContainer 'maven'
    }
}
```

> ⚠️ **Not currently used by anything in this library.** `initPodTemplate` delegates to a
> `renderTemplate` helper that is not defined anywhere in `vars/`, and no other step calls
> `initPodTemplate` (the template that used to call it, `pipelineTemplateMaven`, has been removed).
> `pipelineTemplateHelloWorld` loads its pod template directly via
> `libraryResource("podtemplates/agent.yaml")` instead of going through this step.

---

### `newSemanticVersion`

Calculates a new semantic version by delegating to the bundled `newSemanticVersion.sh` shell script. Sets `env.NEW_SEMANTIC_VERSION` and returns the new version string.

```groovy
def version = newSemanticVersion(arg: 'patch', version: '1.2.3')
// env.NEW_SEMANTIC_VERSION == '1.2.4'
```

| Parameter | Description |
|---|---|
| `arg` | Bump type: `major`, `minor`, or `patch` |
| `version` | Current SemVer string (e.g. `1.2.3`) |

---

### `gitShortCommit`

Populates `env.SHORT_COMMIT` and `env.COMMIT_AUTHOR` from the current workspace Git history.

```groovy
gitShortCommit(7)   // optional length, defaults to 7
echo env.SHORT_COMMIT
echo env.COMMIT_AUTHOR
```

---

### `jfrogUploadArtifact`

Uploads a file to JFrog Artifactory using a Bearer token stored in the `jfrog-user-token` credential.

```groovy
jfrogUploadArtifact(
    FILE_PATH      : 'target/app.war',
    ARTIFACTORY_URL: 'https://acme.jfrog.io',
    REPO_NAME      : 'libs-release-local',
    ARTIFACT_PATH  : 'com/acme/app/1.0.0/app-1.0.0.war'
)
```

---

### `jfrogDownloadArtifact`

Downloads an artifact from JFrog Artifactory using the `jfrog-user-token` credential.

```groovy
jfrogDownloadArtifact(
    ARTIFACTORY_URL: 'https://acme.jfrog.io',
    REPO_NAME      : 'libs-release-local',
    ARTIFACT_PATH  : 'com/acme/app/1.0.0/app-1.0.0.war',
    FILE_PATH      : 'app.war'
)
```

---

### `jiraCreateIssue`

Creates a Jira issue via the REST API v2. Credentials are read from the `jira-user-token` username/password credential. The raw API response is archived as a build artifact.

```groovy
jiraCreateIssue(
    JIRA_URL        : 'https://acme.atlassian.net',
    JIRA_KEY        : 'OPS',
    JIRA_SUMMARY    : "Deployment failed — build ${env.BUILD_NUMBER}",
    JIRA_DESCRIPTION: 'Automated issue created by Jenkins.',
    JIRA_ISSUE_TYPE : 'Bug'
)
```

---

### `helloWorld`

Minimal smoke-test step used to verify library loading.

```groovy
helloWorld()
```

---

### `getGitBranches` *(experimental)*

Fetches branch names for a GitHub repository via the GitHub REST API and sets `env.GIT_REPO_BRANCHES` as a comma-separated list. Requires `jq` in the agent container.

```groovy
getGitBranches(env.GITHUB_TOKEN, 'https://api.github.com/repos/acme/my-app/branches')
```

---

## Resources

### Pod Templates

Located in `resources/podtemplates/`. Loaded directly via `libraryResource()` by `pipelineTemplateHelloWorld` and the `0-helloWorld` template catalog entry. `initPodTemplate` looks for a different, configurable filename (`config.ci.podyaml`) but is currently unused — see the note above.

| File | Containers | Purpose |
|---|---|---|
| `agent.yaml` | `maven` (eclipse-temurin:21-jdk-alpine) | General-purpose Java/Maven build agent |

The pod template follows Kubernetes security best practices:
- `nodeSelector` + `tolerations` to pin workloads to dedicated agent nodes
- `securityContext.runAsUser: 1000` / `fsGroup: 1000`
- Explicit `resources.requests` and `resources.limits`

### Integration Scripts

Reference shell scripts kept alongside the library for documentation/testing purposes. They are **not**
invoked by the corresponding `vars/*.groovy` steps — `jfrogUploadArtifact`, `jfrogDownloadArtifact`, and
`jiraCreateIssue` build and run their own inline `sh` blocks rather than calling these files via
`libraryResource()`. Only `newSemanticVersion.sh` is actually loaded (by `newSemanticVersion.groovy`).

| Path | Description | Used by a `vars/` step? |
|---|---|---|
| `resources/jfrog/uploadArtifact.sh` | cURL-based Artifactory upload | No — standalone reference |
| `resources/jfrog/downloadArtifact.sh` | cURL-based Artifactory download | No — standalone reference |
| `resources/jira/createIssue.sh` + `createIssue.json` | cURL-based Jira issue creation | No — standalone reference |
| `resources/newSemanticVersion/newSemanticVersion.sh` | SemVer bump logic | Yes — `newSemanticVersion.groovy` |

---

## Sample Usage

| Path | Description |
|---|---|
| [`../sample-app-helloWorld/`](../sample-app-helloWorld) | Resolves the library from `SHAREDLIB_GIT_*` env vars and calls `pipelineTemplateHelloWorld('ci-config.yaml')`, plus its own `ci-config.yaml` |
| [`../pipeline-samples/ci-jfrog-integration/`](../pipeline-samples/ci-jfrog-integration) | End-to-end Artifactory upload/download example using `jfrogUploadArtifact`/`jfrogDownloadArtifact` |
| [`../pipeline-samples/ci-jira-integration/`](../pipeline-samples/ci-jira-integration) | Automated Jira issue creation via `jiraCreateIssue` |
| [`../template-catalog/templates/`](../template-catalog/templates) | Pipeline Template Catalog entries (`0-helloWorld`, `1-helloWorld-MB`) built on top of this library |

---

## Credential Requirements

Configure the following credentials in Jenkins / CloudBees CI before using the integration steps:

| Credential ID | Type | Used by |
|---|---|---|
| `jfrog-user-token` | Secret text | `jfrogUploadArtifact`, `jfrogDownloadArtifact` |
| `jira-user-token` | Username / Password | `jiraCreateIssue` |

---

## Contributing

1. Branch from `main` — use `feature/<short-description>` naming.
2. Add or update the relevant `vars/*.groovy` step.
3. Place any new shell scripts or YAML resources under `resources/`.
4. Add a sample Jenkinsfile under `../pipeline-samples/` demonstrating the change.
5. Open a Pull Request — pipeline linting runs automatically on PR creation.
