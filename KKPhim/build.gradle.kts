// Use an integer for version numbers
version = 3

cloudstream {
    name = "Milano"
    description = "Phim bộ, phim lẻ, hoạt hình, TV shows từ KKPhim (phimapi.com) — Vietsub, Thuyết minh, Lồng tiếng"
    authors = listOf("Milano")
    /** 0: Down, 1: Ok, 2: Slow, 3: Beta-only */
    status = 1
    tvTypes = listOf("TvSeries", "Movie", "Cartoon", "AsianDrama")
    language = "vi"
    iconUrl = "https://www.google.com/s2/favicons?domain=kkphim.com&sz=%size%"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xskip-metadata-version-check"
        )
    }
}
