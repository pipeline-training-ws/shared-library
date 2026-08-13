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
  - [pipelineTemplateMaven](#pipelinetemplatemaven)
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
ci-shared-library/
├── vars/                          # Global variables exposed as pipeline steps
│   ├── pipelineTemplateHelloWorld.groovy
│   ├── pipelineTemplateMaven.groovy
│   ├── buildMaven.groovy
│   ├── initPodTemplate.groovy
│   ├── newSemanticVersion.groovy
│   ├── gitShortCommit.groovy
│   ├── getGitBranches.groovy
│   ├── jfrogUploadArtifact.groovy
│   ├── jfrogDownloadArtifact.groovy
│   ├── jiraCreateIssue.groovy
│   └── helloWorld.groovy
├── resources/                     # Non-Groovy resources accessed via libraryResource()
│   ├── podtemplates/              # Kubernetes pod template YAML files
│   ├── jfrog/                     # JFrog Artifactory shell scripts
│   ├── jira/                      # Jira REST API helpers
│   └── newSemanticVersion/        # Semantic versioning shell script
└── samples/                       # Reference Jenkinsfiles and CI config examples
    ├── ci-config.yaml
    ├── JenkinsfileInitConfigSimple.groovy
    ├── ci-jfrog-integration/
    └── ci-jira-integration/
```

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
| Name | `ci-shared-library` |
| Default version | `main` |
| SCM | Git — `git@github.com:pipeline-training-ws/shared-library.git` |

Then reference it implicitly from any Jenkinsfile (auto-loaded), or explicitly:

```groovy
@Library('ci-shared-library') _
```

### Option 2 — Inline `library` step

```groovy
library identifier: 'ci-shared-library@main', retriever: modernSCM(
    [$class: 'GitSCMSource',
     remote: 'git@github.com:pipeline-training-ws/shared-library.git'])
```

---

## Pipeline Templates

### `pipelineTemplateHelloWorld`

A minimal template that runs a greeting stage on a Kubernetes agent. Designed to validate library loading and pod-template resolution.

**Parameters**

| Key | Type | Description |
|---|---|---|
| `hello` | String | Message printed in the "Hello World" stage |
| `firstName` | String | First name printed in the "Hi" stage (`main` branch only) |
| `lastName` | String | Last name printed in the "Hi" stage (`main` branch only) |

**Jenkinsfile**

```groovy
@Library('ci-shared-library') _

pipelineTemplateHelloWorld(
    hello    : 'CloudBees CI',
    firstName: 'Jane',
    lastName : 'Doe'
)
```

**Stages**

| Stage | Branch filter | What it does |
|---|---|---|
| CI / Hello World | all | `echo Hello <hello>` |
| CI / Hi | `main` only | `echo Hi <firstName> <lastName>` |

---

### `pipelineTemplateMaven`

An opinionated **CI/CD** template for Maven-based applications running on Kubernetes agents. Reads build configuration from a `ci-config.yaml` file committed alongside the application source.

**Stages**

| Stage | Branch filter | What it does |
|---|---|---|
| Init | all | Loads and merges `ci-config.yaml` defaults |
| CI / build | all | Executes Maven steps via `buildMaven` |
| CI / Image | `main` only | Builds and pushes container image |
| CI / test | all | Placeholder — add your test commands |
| CI / qa scans | all | Parallel Sonar + roxctl scans (stubs) |
| CD / deploy | all | Placeholder — add your deploy commands |
| CD / test | all | Placeholder — add post-deploy tests |

**`ci-config.yaml` schema (application side)**

```yaml
app: 'my-app'
ci:
  podyaml: podTemplate-maven.yaml      # Pod template from resources/podtemplates/
  maven:
    image: docker.io/library/eclipse-temurin:21-jdk-alpine
    steps:
      - "mvn clean verify"
  kaniko:
    image: gcr.io/kaniko-project/executor:latest
```

**Jenkinsfile**

```groovy
@Library('ci-shared-library') _

pipelineTemplateMaven([:])             # ci-config.yaml is resolved from the branch
```

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

Resolves a Kubernetes pod template YAML from the library's `resources/podtemplates/` directory and performs token substitution for `${MAVEN_IMAGE}` and `${KANIKO_IMAGE}` using values from the CI config.

```groovy
// Returns rendered YAML string — used inside an agent block
agent {
    kubernetes {
        yaml initPodTemplate(config)
        defaultContainer 'maven'
    }
}
```

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

Located in `resources/podtemplates/`. Loaded by `initPodTemplate` or directly via `libraryResource()`.

| File | Containers | Purpose |
|---|---|---|
| `podTemplate-agent.yaml` | `maven` (eclipse-temurin:21-jdk-alpine) | General-purpose Java/Maven build agent |

All pod templates follow Kubernetes security best practices:
- `nodeSelector` + `tolerations` to pin workloads to dedicated agent nodes
- `securityContext.runAsUser: 1000` / `fsGroup: 1000`
- Explicit `resources.requests` and `resources.limits`

### Integration Scripts

| Path | Description |
|---|---|
| `resources/jfrog/uploadArtifact.sh` | cURL-based Artifactory upload |
| `resources/jfrog/downloadArtifact.sh` | cURL-based Artifactory download |
| `resources/jira/createIssue.sh` | cURL-based Jira issue creation |
| `resources/newSemanticVersion/newSemanticVersion.sh` | SemVer bump logic |

---

## Sample Usage

The `samples/` directory contains ready-to-use reference implementations:

| File | Description |
|---|---|
| `samples/ci-config.yaml` | Minimal CI config for a Maven hello-world app |
| `samples/JenkinsfileInitConfigSimple.groovy` | Loads library inline, builds a config map, invokes a template |
| `samples/ci-jfrog-integration/` | End-to-end Artifactory upload/download example |
| `samples/ci-jira-integration/` | Automated Jira issue creation on build failure |

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
4. Add a sample Jenkinsfile under `samples/` demonstrating the change.
5. Open a Pull Request — pipeline linting runs automatically on PR creation.
