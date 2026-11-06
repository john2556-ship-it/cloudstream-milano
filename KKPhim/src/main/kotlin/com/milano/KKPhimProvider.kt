package com.milano

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * KKPhim (phimapi.com) for CloudStream
 */
class KKPhimProvider : MainAPI() {
    override var mainUrl = "https://phimapi.com"
    override var name = "KKPhim"
    override var lang = "vi"
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie, TvType.Cartoon, TvType.AsianDrama)

    private val imgCdn = "https://phimimg.com/"

    override val mainPage = mainPageOf(
        "/danh-sach/phim-moi-cap-nhat?" to "Mới cập nhật",
        "/v1/api/danh-sach/phim-bo?limit=24&" to "Phim bộ",
        "/v1/api/danh-sach/phim-bo?limit=24&sort_lang=thuyet-minh&" to "Phim bộ thuyết minh",
        "/v1/api/danh-sach/phim-bo?limit=24&sort_lang=long-tieng&" to "Phim bộ lồng tiếng",
        "/v1/api/danh-sach/phim-le?limit=24&" to "Phim lẻ",
        "/v1/api/danh-sach/phim-bo?limit=24&country=han-quoc&" to "Hàn Quốc",
        "/v1/api/danh-sach/phim-bo?limit=24&country=trung-quoc&" to "Trung Quốc",
        "/v1/api/danh-sach/hoat-hinh?limit=24&" to "Hoạt hình",
        "/v1/api/danh-sach/tv-shows?limit=24&" to "TV Shows",
    )

    /* ---------------- helpers ---------------- */

    private suspend fun getJson(url: String): JSONObject? =
        try { JSONObject(app.get(url, headers = mapOf("Accept" to "application/json")).text) } catch (e: Exception) { null }

    private fun img(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return if (path.startsWith("http")) path else imgCdn + path.trimStart('/')
    }

    private fun itemsOf(j: JSONObject?): JSONArray =
        j?.optJSONObject("data")?.optJSONArray("items") ?: j?.optJSONArray("items") ?: JSONArray()

    private fun totalPages(j: JSONObject?): Int {
        val p = j?.optJSONObject("data")?.optJSONObject("params")?.optJSONObject("pagination")
            ?: j?.optJSONObject("pagination") ?: return 1
        val tp = p.optInt("totalPages", 0)
        if (tp > 0) return tp
        val total = p.optInt("totalItems", 0); val per = p.optInt("totalItemsPerPage", 0)
        return if (total > 0 && per > 0) (total + per - 1) / per else 1
    }

    private fun typeOf(t: String?): TvType = when (t) {
        "single" -> TvType.Movie
        "hoathinh" -> TvType.Cartoon
        else -> TvType.TvSeries
    }

    private fun toSearch(m: JSONObject): SearchResponse? {
        val slug = m.optString("slug").ifBlank { return null }
        val title = m.optString("name").ifBlank { slug }
        val url = "$mainUrl/phim/$slug"
        val poster = img(m.optString("poster_url").ifBlank { m.optString("thumb_url") })
        val year = m.optInt("year", 0).takeIf { it > 0 }
        val type = typeOf(m.optString("type"))
        return if (type == TvType.Movie) {
            newMovieSearchResponse(title, url, TvType.Movie) { posterUrl = poster; this.year = year }
        } else {
            newTvSeriesSearchResponse(title, url, type) { posterUrl = poster; this.year = year }
        }
    }

    private fun list(j: JSONObject?): List<SearchResponse> {
        val arr = itemsOf(j)
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(::toSearch) }
    }

    /* ---------------- home / search ---------------- */

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val j = getJson("$mainUrl${request.data}page=$page")
        return newHomePageResponse(request.name, list(j), hasNext = page < totalPages(j))
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val q = URLEncoder.encode(query, "UTF-8")
        val j = getJson("$mainUrl/v1/api/tim-kiem?keyword=$q&page=$page&limit=24")
        return newSearchResponseList(list(j), page < totalPages(j))
    }

    /* ---------------- detail ---------------- */

    override suspend fun load(url: String): LoadResponse? {
        val j = getJson(url) ?: return null
        val m = j.optJSONObject("movie") ?: j.optJSONObject("data")?.optJSONObject("item") ?: return null
        val servers = j.optJSONArray("episodes") ?: m.optJSONArray("episodes") ?: JSONArray()

        val title = m.optString("name")
        val poster = img(m.optString("poster_url").ifBlank { m.optString("thumb_url") })
        val backdrop = img(m.optString("thumb_url").ifBlank { m.optString("poster_url") })
        val plot = m.optString("content").replace(Regex("<[^>]*>"), "").trim().ifBlank { null }
        val year = m.optInt("year", 0).takeIf { it > 0 }
        val tags = buildList {
            m.optJSONArray("category")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let { add(it) } }
            m.optJSONArray("country")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }?.let { add(it) } }
            m.optString("lang").takeIf { it.isNotBlank() }?.let { add(it) }
            m.optString("quality").takeIf { it.isNotBlank() }?.let { add(it) }
        }
        val actors = m.optJSONArray("actor")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() && it != "Đang cập nhật" } }
        val trailer = m.optString("trailer_url").ifBlank { null }
        val type = typeOf(m.optString("type"))

        val order = mutableListOf<String>()
        val names = mutableMapOf<String, String>()
        val links = mutableMapOf<String, JSONArray>()
        for (s in 0 until servers.length()) {
            val srv = servers.optJSONObject(s) ?: continue
            val srvName = srv.optString("server_name").ifBlank { "Server ${s + 1}" }
            val data = srv.optJSONArray("server_data") ?: continue
            for (e in 0 until data.length()) {
                val ep = data.optJSONObject(e) ?: continue
                val m3u8 = ep.optString("link_m3u8")
                if (m3u8.isBlank()) continue
                val key = ep.optString("slug").ifBlank { ep.optString("name") }.ifBlank { "tap-${e + 1}" }
                if (key !in links) { order += key; names[key] = ep.optString("name"); links[key] = JSONArray() }
                links[key]!!.put(JSONObject().put("server", srvName).put("url", m3u8))
            }
        }

        if (type == TvType.Movie) {
            val data = order.firstOrNull()?.let { links[it]?.toString() } ?: "[]"
            return newMovieLoadResponse(title, url, TvType.Movie, data) {
                posterUrl = poster; backgroundPosterUrl = backdrop; this.plot = plot; this.year = year; this.tags = tags
                addActors(actors); addTrailer(trailer)
            }
        }

        val episodes = order.mapIndexed { i, key ->
            val n = names[key].orEmpty()
            // fix = false: data is JSON, not a URL — with fix = true CloudStream would prepend mainUrl and break playback
            newEpisode(links[key]?.toString() ?: "[]", fix = false, initializer = {
                this.name = if (n.isBlank()) "Tập ${i + 1}" else if (n.all { it.isDigit() }) "Tập $n" else n
                this.episode = Regex("\\d+").find(n)?.value?.toIntOrNull() ?: (i + 1)
            })
        }
        return newTvSeriesLoadResponse(title, url, type, episodes) {
            posterUrl = poster; backgroundPosterUrl = backdrop; this.plot = plot; this.year = year; this.tags = tags
            addActors(actors); addTrailer(trailer)
            showStatus = if (m.optString("status") == "completed") ShowStatus.Completed else ShowStatus.Ongoing
        }
    }

    /* ---------------- play ---------------- */

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val arr = try { JSONArray(data) } catch (e: Exception) { return false }
        var found = false
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val url = o.optString("url")
            if (url.isBlank()) continue
            // newExtractorLink (not the ExtractorLink constructor): the constructor's parameters change
            // between CloudStream versions (pre-release added audioTracks), so a plugin built against the
            // pre-release stubs crashes on an older app with "No link found".
            callback(
                newExtractorLink(name, "$name · ${o.optString("server")}", url, ExtractorLinkType.M3U8) {
                    quality = Qualities.Unknown.value
                }
            )
            found = true
        }
        return found
    }
}
