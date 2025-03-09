plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-parcelize")
}

android {
    namespace = "com.rescue.flutter_720yun"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rescue.flutter_720yun"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }


}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.ui.android)
//    implementation(libs.androidx.ui.desktop)
//    implementation(libs.androidx.ui.jvmstubs)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.glide)
    implementation(libs.roundedimageview)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.viewpager2.viewpager2)
    implementation("com.webtoonscorp.android:readmore-view:1.3.3")
    implementation(libs.google.flexbox)
    implementation("io.github.lucksiege:pictureselector:v3.11.2")
    implementation("io.github.lucksiege:compress:v3.11.2")
    implementation("io.github.lucksiege:ucrop:v3.11.2")
    implementation("com.qiniu:qiniu-android-sdk:7.6.4")
    implementation("com.github.Wei-1021:WImagePreview:1.4.3")
    implementation("com.github.getActivity:XXPermissions:20.0")
    implementation("io.github.scwang90:refresh-layout-kernel:3.0.0-alpha") //核心必须依赖
    implementation("io.github.scwang90:refresh-header-material:3.0.0-alpha") //谷歌刷新头
    implementation("io.github.scwang90:refresh-footer-classics:3.0.0-alpha")    //经典加载
    implementation("com.github.rbro112:Android-Indefinite-Pager-Indicator:1.5")
    implementation("androidx.core:core-splashscreen:1.0.1")

}

