plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.mycymapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mycymapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            // La forma correcta en Kotlin DSL para renombrar el APK de salida
            // Se hace en la configuración de la variante de salida
            // NO se usa archivesBaseName directamente aquí
        }

        release {
            isMinifyEnabled = false // Considera cambiar a 'true' para la versión de producción real
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Opcional: También puedes configurar el nombre para la versión de release
            // archivesBaseName = "MyCymApp_Release_v${defaultConfig.versionName}_${defaultConfig.versionCode}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // Asegúrate de que lifecycle-runtime-ktx esté aquí si usas viewModelScope
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") // Si no está ya con libs.androidx.lifecycle

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.recyclerview) // Asegúrate de que esta línea esté presente
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx) // Para soporte de corrutinas
    kapt(libs.androidx.room.compiler) // Para la generación de código
    implementation("com.google.code.gson:gson:2.10.1")

    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions) // Opcional, pero útil

    // ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode.scanning)


}