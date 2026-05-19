pluginManagement {
	repositories {
		google()
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

	repositories {
		google()
		mavenCentral()
	}
}

rootProject.name = "rsvpnano"

include(":shared")
include(":androidApp")

project(":shared").projectDir = file("RSVPNanoCompanion/shared")
project(":androidApp").projectDir = file("RSVPNanoCompanion/androidApp")