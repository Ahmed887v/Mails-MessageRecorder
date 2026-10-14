// ملاحظة: تُقدَّم مستودعات جوجل/المركزية الرسمية أولاً.
// مهم: لا تضع مرآة aliyun/gradle-plugin في المقدمة لأنها تُعيد HTTP 502
// لبعض الإضافات (مثل KSP) فيتوقف Gradle عن الحل بدل الانتقال للمستودع التالي.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}

rootProject.name = "NewRecorder"
include(":app")
