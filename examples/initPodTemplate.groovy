// Renders the pod template selected via config.ci.podyaml, substituting the
// ${MAVEN_IMAGE}/${KANIKO_IMAGE} placeholders with the images from ci-config.yaml.
// Templates without those placeholders are returned unchanged.
def call(Map config) {
    def podTemplateYaml = libraryResource("podtemplates/${config.ci.podyaml}")
    return renderTemplate(podTemplateYaml, [
        MAVEN_IMAGE : config.ci.maven?.image,
        KANIKO_IMAGE: config.ci.kaniko?.image
    ])
}
