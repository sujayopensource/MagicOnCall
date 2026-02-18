rootProject.name = "magic-on-call"

include(
    "modules:domain",
    "modules:application",
    "modules:api",
    "modules:infrastructure:persistence",
    "modules:infrastructure:messaging",
    "modules:infrastructure:connectors",
    "modules:infrastructure:observability",
    "modules:workers",
    "modules:eval",
    "modules:ui-vaadin",
    "app"
)
