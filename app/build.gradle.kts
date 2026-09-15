dependencies {
    // 1. AndroidX Media3 (ExoPlayer + Service chạy nền)
    implementation("androidx.media3:media3-exoplayer:1.3.0")
    implementation("androidx.media3:media3-session:1.3.0")
    implementation("androidx.media3:media3-ui:1.3.0")

    // 2. NewPipe Extractor (Bóc tách link audio YouTube không quảng cáo)
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.3")

    // 3. OkHttp & Coroutines
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
