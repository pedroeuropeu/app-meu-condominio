import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Assinatura de release: keystore.properties na raiz (fora do git). Sem ele o release sai sem assinatura.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun prop(name: String, default: String) = (project.findProperty(name) as String?) ?: default

// Produção: API e Keycloak (/auth) no mesmo domínio do gestao-condominio (deploy/server/provision.sh)
val prodUrl = prop("condovitta.prodUrl", "https://meucondominio.efinances.top/")

// Ambiente local (`make up` do gestao-condominio). "localhost" do aparelho aponta para o Mac via `adb reverse`
// (scripts/local-dev.sh), então os tokens têm o mesmo emissor da web (KEYCLOAK_PUBLIC_URL=http://localhost:8180).
val localApi = prop("condovitta.local.api", "http://localhost:8090/")
val localKeycloak = prop("condovitta.local.keycloak", "http://localhost:8180/")

android {
    namespace = "br.com.condovitta.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.condovitta.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "KEYCLOAK_CLIENT_ID", "\"condominio-client\"")
        buildConfigField("String", "API_BASE_URL", "\"$prodUrl\"")
        buildConfigField("String", "KEYCLOAK_TOKEN_URL", "\"${prodUrl}auth/realms/condominio/protocol/openid-connect/token\"")
        buildConfigField("String", "ENV_LABEL", "\"\"")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    flavorDimensions += "env"
    productFlavors {
        // demo: dados de exemplo no próprio aparelho, sem servidor (para ver o app e para testes)
        create("demo") {
            dimension = "env"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            buildConfigField("Boolean", "DEMO", "true")
            buildConfigField("String", "ENV_LABEL", "\"Demo\"")
            resValue("string", "app_name", "Condovitta Demo")
        }
        // local: o mesmo app do prod, ligado ao backend e ao Keycloak rodando nesta máquina
        create("local") {
            dimension = "env"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            buildConfigField("Boolean", "DEMO", "false")
            buildConfigField("String", "API_BASE_URL", "\"$localApi\"")
            buildConfigField("String", "KEYCLOAK_TOKEN_URL", "\"${localKeycloak}realms/condominio/protocol/openid-connect/token\"")
            buildConfigField("String", "ENV_LABEL", "\"Local\"")
            resValue("string", "app_name", "Condovitta Local")
        }
        // prod: API real do Condovitta
        create("prod") {
            dimension = "env"
            buildConfigField("Boolean", "DEMO", "false")
            resValue("string", "app_name", "Condovitta")
        }
    }

    sourceSets {
        // o flavor local usa as mesmas implementações remotas do prod (e libera HTTP para localhost)
        getByName("local").java.srcDir("src/prod/java")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        localeFilters += listOf("pt")
    }
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*")
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "ObsoleteLintCustomCheck")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        optIn.addAll("androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
