// settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // RepositoriesMode.FAIL_ON_PROJECT_REPOS, projenin build.gradle dosyalarında
    // tanımlanmış repolar yerine sadece buradakilerin kullanılmasını zorlar.
    // Bu, derleme tutarlılığı için iyi bir pratiktir.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // TensorFlow Lite ve diğer birçok kütüphane için gerekli
        // Eğer JitPack gibi başka özel repolar kullanıyorsanız, buraya ekleyebilirsiniz:
        // maven("https://jitpack.io")
    }
}

rootProject.name = "AIGarage"
include(":app") // :app modülünü projeye dahil et
 