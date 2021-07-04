package eu.kanade.tachiyomi.extension.all.mangapark

import com.squareup.duktape.Duktape
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.util.Calendar
import java.util.concurrent.TimeUnit
import eu.kanade.tachiyomi.source.model.SManga.Companion
import okhttp3.HttpUrl.Companion.toHttpUrl

open class MangaPark(
    override val lang: String,
    private val siteLang: String
) : ParsedHttpSource() {

    override val name: String = "MangaPark v3"

    override val baseUrl: String = "https://mangapark.net"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    private val mpFilters = MangaParkFilters()

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Latest
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/browse?sort=update&page=$page")

    override fun latestUpdatesSelector(): String = "div#subject-list div.col"

    override fun latestUpdatesNextPageSelector() = "div#mainer nav.d-none .pagination .page-item:last-of-type:not(.disabled)"

    override fun latestUpdatesFromElement(element: Element): SManga {
        return SManga.create().apply {
            setUrlWithoutDomain(element.select("a.fw-bold").attr("href"))
            title = element.select("a.fw-bold").text()
            thumbnail_url = element.select("a.position-relative img").attr("abs:src")
            author = element.select("div.autarts").text().split("/")
                .joinToString(", ") { it.trim() }
            genre = element.select("div.genres").select("span, u")
                .joinToString(", ") { it.text().trim() }
        }
    }


    // Popular
    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/browse?sort=d007&page=$page")

    override fun popularMangaSelector(): String = latestUpdatesSelector()

    override fun popularMangaNextPageSelector(): String = latestUpdatesNextPageSelector()

    override fun popularMangaFromElement(element: Element): SManga = latestUpdatesFromElement(element)


    // Search
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return when {
            query.startsWith(PREFIX_ID_SEARCH) -> fetchSearchIdManga(query.removePrefix(PREFIX_ID_SEARCH))
            query.isNotBlank() -> fetchSearchManga(page, query)
            else -> fetchGenreSearchManga(page, filters)
        }
    }

    // Search With Manga ID
    private fun fetchSearchIdManga(id: String): Observable<MangasPage> {
        return client.newCall(GET("$baseUrl/comic/$id", headers))
            .asObservableSuccess()
            .map { response ->
                MangasPage(listOf(mangaDetailsParse(response.asJsoup())), false)
            }
    }

    // Search WIth Query
    private fun fetchSearchManga(page: Int, query: String): Observable<MangasPage> {
        return client.newCall(GET("$baseUrl/search?word=$query&page=$page", headers))
            .asObservableSuccess()
            .map { response ->
                searchMangaParse(response, false)
            }
    }

    // Search With filter
    private fun fetchGenreSearchManga(page: Int, filters: FilterList): Observable<MangasPage> {
        var url = "$baseUrl/browse".toHttpUrl().newBuilder()

        return client.newCall(GET(mpFilters.addFiltersToUrl(url, page, filters), headers))
            .asObservableSuccess()
            .map { response ->
                searchMangaParse(response, true)
            }
    }

    private fun searchMangaSelector(genreSearch: Boolean): String {
        return when(genreSearch) {
            false -> "div#search-list div.col"
            true -> latestUpdatesSelector()
        }
    }

    private fun searchMangaNextPageSelector(genreSearch: Boolean) = latestUpdatesNextPageSelector()

    private fun searchMangaParse(response: Response, genreSearch: Boolean): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(searchMangaSelector(genreSearch)).map { element ->
            searchMangaFromElement(element)
        }

        val hasNextPage = searchMangaNextPageSelector(genreSearch)?.let { selector ->
            document.select(selector).first()
        } != null

        return MangasPage(mangas, hasNextPage)
    }


    fun ggfetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return when {
            query.startsWith(PREFIX_ID_SEARCH) -> {
                val id = query.removePrefix(PREFIX_ID_SEARCH)

            }

            query.isNotBlank() -> {
                val url = ""
                client.newCall(GET(url, headers)).asObservableSuccess()
                    .map { response ->
                        searchMangaParse(response)
                    }
            }

            else -> {
                val sortFilter = filters.findInstance<SortFilter>()!!
                val reverseSortFilter = filters.findInstance<ReverseSortFilter>()!!
                val statusFilter = filters.findInstance<StatusFilter>()!!
                val genreFilter = filters.findInstance<GenreGroupFilter>()!!
                val minChapterFilter = filters.findInstance<MinChapterTextFilter>()!!
                val maxChapterFilter = filters.findInstance<MaxChapterTextFilter>()!!
                val url = "$baseUrl/browse".toHttpUrlOrNull()!!.newBuilder()
                url.addQueryParameter("page", page.toString())

                with(sortFilter) {
                    if (reverseSortFilter.state) {
                        url.addQueryParameter("sort", "${this.selected}.az")
                    } else {
                        url.addQueryParameter("sort", "${this.selected}.za")
                    }
                }

                with(genreFilter) {
                    url.addQueryParameter(
                        "genres", included.joinToString(",") + "|" + excluded.joinToString(",")
                    )
                }

                with(statusFilter) {
                    url.addQueryParameter("release", this.selected)
                }

                if (maxChapterFilter.state.isNotEmpty() or minChapterFilter.state.isNotEmpty()) {
                    url.addQueryParameter("chapters", minChapterFilter.state + "-" + maxChapterFilter.state)
                }

                client.newCall(GET(url.build().toString(), headers)).asObservableSuccess()
                    .map { response ->
                        genreSearchMangaParse(response)
                    }
            }
        }
    }

    private fun genreSearchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select("div#subject-list div.col").map { element ->
            searchMangaFromElement(element)
        }

        val hasNextPage = document.select(latestUpdatesNextPageSelector()).first() != null

        return MangasPage(mangas, hasNextPage)
    }

    private fun mangaFromID(response: Response, id: String): MangasPage {
        val infoElement = response.asJsoup().select("div#mainer div.container-fluid")
        val manga = SManga.create().apply {
            url = "/comic/$id"
            title = infoElement.select("h3.item-title").text()
            thumbnail_url = infoElement.select("div.detail-set div.attr-cover img").attr("abs:src")
        }

        return MangasPage(listOf(manga), false)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException("Not used")



    override fun searchMangaFromElement(element: Element) = latestUpdatesFromElement(element)




    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select("div#mainer div.container-fluid")

        return SManga.create().apply {
            setUrlWithoutDomain(infoElement.select("h3.item-title a").attr("href"))
            title = infoElement.select("h3.item-title").text()
            description = infoElement.select("div.limit-height-body")
                .select("h5.text-muted, div.limit-html")
                .joinToString("\n\n") { it.text() }
            author = infoElement.select("div.attr-item:contains(author) a")
                .joinToString { it.text().trim() }
            status = infoElement.select("div.attr-item:contains(status) span")
                .text().parseStatus()
            thumbnail_url = infoElement.select("div.detail-set div.attr-cover img").attr("abs:src")
            genre = infoElement.select("div.attr-item:contains(genres) span span")
                .joinToString { it.text().trim() }
        }
    }

    private fun String?.parseStatus() = when {
        this == null -> SManga.UNKNOWN
        this.contains("Ongoing") -> SManga.ONGOING
        this.contains("Hiatus") -> SManga.ONGOING
        this.contains("Completed") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListRequest(manga: SManga): Request {

        val url = manga.url
        val sid = url.split("/")[2]

        val jsonPayload = buildJsonObject {
            put("lang", siteLang)
            put("sid", sid)
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json;charset=UTF-8".toMediaType())

        val refererUrl = "$baseUrl/$url".toHttpUrlOrNull()!!.newBuilder()
            .toString()
        val newHeaders = headersBuilder()
            .add("Content-Length", requestBody.contentLength().toString())
            .add("Content-Type", requestBody.contentType().toString())
            .set("Referer", refererUrl)
            .build()

        return POST(
            "$baseUrl/ajax.reader.subject.episodes.lang",
            headers = newHeaders,
            body = requestBody
        )
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val resToJson = json.parseToJsonElement(response.body!!.string()).jsonObject
        val document = Jsoup.parse(resToJson["html"]!!.jsonPrimitive.content)
        return document.select(chapterListSelector()).map { chapterFromElement(it) }
    }

    override fun chapterListSelector() = "div.episode-item"

    override fun chapterFromElement(element: Element): SChapter {

        val urlElement = element.select("a.chapt")
        val time = element.select("div.extra > i.ps-2").text()

        return SChapter.create().apply {
            name = urlElement.text()
            chapter_number = urlElement.attr("href").substringAfterLast("/").toFloat()
            if (time != "") { date_upload = time.parseChapterDate() }
            setUrlWithoutDomain(urlElement.attr("href"))
        }
    }

    private fun String.parseChapterDate(): Long {
        val value = this.split(' ')[0].toInt()

        return when (this.split(' ')[1].removeSuffix("s")) {
            "sec" -> Calendar.getInstance().apply {
                add(Calendar.SECOND, value * -1)
            }.timeInMillis
            "min" -> Calendar.getInstance().apply {
                add(Calendar.MINUTE, value * -1)
            }.timeInMillis
            "hour" -> Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, value * -1)
            }.timeInMillis
            "day" -> Calendar.getInstance().apply {
                add(Calendar.DATE, value * -1)
            }.timeInMillis
            "week" -> Calendar.getInstance().apply {
                add(Calendar.DATE, value * 7 * -1)
            }.timeInMillis
            "month" -> Calendar.getInstance().apply {
                add(Calendar.MONTH, value * -1)
            }.timeInMillis
            "year" -> Calendar.getInstance().apply {
                add(Calendar.YEAR, value * -1)
            }.timeInMillis
            else -> {
                return 0
            }
        }
    }

    override fun pageListParse(document: Document): List<Page> {
        val duktape = Duktape.create()
        val script = document.select("script").html()
        val imgCdnHost = script.substringAfter("const imgCdnHost = \"").substringBefore("\";")
        val imgPathLisRaw = script.substringAfter("const imgPathLis = ").substringBefore(";")
        val imgPathLis = json.parseToJsonElement(imgPathLisRaw).jsonArray
        val amPass = script.substringAfter("const amPass = ").substringBefore(";")
        val amWord = script.substringAfter("const amWord = ").substringBefore(";")

        val decryptScript = cryptoJS + "CryptoJS.AES.decrypt($amWord, $amPass).toString(CryptoJS.enc.Utf8);"

        val imgWordLisRaw = duktape.evaluate(decryptScript).toString()
        val imgWordLis = json.parseToJsonElement(imgWordLisRaw).jsonArray

        return imgWordLis.mapIndexed { i, imgWordE ->
            val imgPath = imgPathLis[i].jsonPrimitive.content
            val imgWord = imgWordE.jsonPrimitive.content

            Page(i, "", "$imgCdnHost$imgPath?$imgWord")
        }
    }

    private val cryptoJS by lazy {
        client.newCall(
            GET(
                CryptoJSUrl,
                headers
            )
        ).execute().body!!.string()
    }

    override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    override fun getFilterList() = mpFilters.getFilterList()

    /*
    class SelectFilterOption(val name: String, val value: String)
    class CheckboxFilterOption(val value: String, name: String, default: Boolean = false) : Filter.CheckBox(name, default)
    class TriStateFilterOption(val value: String, name: String, default: Int = 0) : Filter.TriState(name, default)

    abstract class SelectFilter(name: String, private val options: List<SelectFilterOption>, default: Int = 0) : Filter.Select<String>(name, options.map { it.name }.toTypedArray(), default) {
        val selected: String
            get() = options[state].value
    }
    abstract class CheckboxGroupFilter(name: String, options: List<CheckboxFilterOption>) : Filter.Group<CheckboxFilterOption>(name, options) {
        val selected: List<String>
            get() = state.filter { it.state }.map { it.value }
    }
    abstract class TriStateGroupFilter(name: String, options: List<TriStateFilterOption>) : Filter.Group<TriStateFilterOption>(name, options) {
        val included: List<String>
            get() = state.filter { it.isIncluded() }.map { it.value }

        val excluded: List<String>
            get() = state.filter { it.isExcluded() }.map { it.value }
    }
    */
    abstract class TextFilter(name: String) : Filter.Text(name)

    class SortFilter(options: List<SelectFilterOption>, default: Int) : SelectFilter("Sort By", options, default)
    class ReverseSortFilter(default: Boolean = false) : Filter.CheckBox("Revers Sort", default)
    class StatusFilter(options: List<SelectFilterOption>, default: Int) : SelectFilter("Status", options, default)
    class GenreGroupFilter(options: List<TriStateFilterOption>) : TriStateGroupFilter("Genre", options)
    class MinChapterTextFilter : TextFilter("Min. Chapters")
    class MaxChapterTextFilter : TextFilter("Max. Chapters")

    private fun getSortFilter() = listOf(
        SelectFilterOption("Rating", "rating"),
        SelectFilterOption("Comments", "comments"),
        SelectFilterOption("Discuss", "discuss"),
        SelectFilterOption("Update", "update"),
        SelectFilterOption("Create", "create"),
        SelectFilterOption("Name", "name"),
        SelectFilterOption("Total Views", "d000"),
        SelectFilterOption("Most Views 360 days", "d360"),
        SelectFilterOption("Most Views 180 days", "d180"),
        SelectFilterOption("Most Views 90 days", "d090"),
        SelectFilterOption("Most Views 30 days", "d030"),
        SelectFilterOption("Most Views 7 days", "d007"),
        SelectFilterOption("Most Views 24 hours", "h024"),
        SelectFilterOption("Most Views 12 hours", "h012"),
        SelectFilterOption("Most Views 6 hours", "h006"),
        SelectFilterOption("Most Views 60 minutes", "h001"),
    )

    private fun getStatusFilter() = listOf(
        SelectFilterOption("All", ""),
        SelectFilterOption("Pending", "pending"),
        SelectFilterOption("Ongoing", "ongoing"),
        SelectFilterOption("Completed", "completed"),
        SelectFilterOption("Hiatus", "hiatus"),
        SelectFilterOption("Cancelled", "cancelled"),
    )

    private fun getGenreFilter() = listOf(
        // Hidden Genres
        TriStateFilterOption("award_winning", "Award Winning"),
    )
    */
    private inline fun <reified T> Iterable<*>.findInstance() = find { it is T } as? T

    companion object {

        const val PREFIX_ID_SEARCH = "id:"

        const val CryptoJSUrl = "https://cdnjs.cloudflare.com/ajax/libs/crypto-js/4.0.0/crypto-js.min.js"
    }
}
