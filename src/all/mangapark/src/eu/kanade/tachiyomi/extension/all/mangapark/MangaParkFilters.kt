package eu.kanade.tachiyomi.extension.all.mangapark

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.HttpUrl

open class MangaParkFilters {

    internal fun getFilterList(): FilterList {
        return FilterList(
            Filter.Header("NOTE: Ignored if using text search!"),
            Filter.Separator(),
            getSortFilter(),
            getMinChapterFilter(),
            getMaxChapterFilter(),
            getPublicationFilter(),
            getTypeFilter(),
            getDemographicFilter(),
            getContentFilter(),
            getGenreFilter()
        )
    }

    internal fun addFiltersToUrl(url: HttpUrl.Builder, page: Int, filters: FilterList): String {
        val includedGenre: MutableList<String> = mutableListOf()
        val excludedGenre: MutableList<String> = mutableListOf()
        var sort: String? = null
        var release: String? = null
        val chapters: String?
        val genres: String = includedGenre.joinToString(",") + "|" + excludedGenre.joinToString(",")

        val minChapFilter = filters.findInstance<MinChapterTextFilter>()!!
        val maxChapFilter = filters.findInstance<MaxChapterTextFilter>()!!

        chapters = when {
            minChapFilter.state.isNotEmpty() and maxChapFilter.state.isEmpty() -> minChapFilter.state
            maxChapFilter.state.isNotEmpty() -> when {
                minChapFilter.state.isNotEmpty() -> minChapFilter.state + "-" + maxChapFilter.state
                else -> "0-" + maxChapFilter.state
            }
            else -> null
        }

        filters.forEach { filter ->
            when (filter) {
                is SortFilter -> {
                    if (filter.state != null) {
                        val sortValue = sortList[filter.state!!.index].value
                        sort = when (filter.state!!.ascending) {
                            true -> "$sortValue.az"
                            else -> "$sortValue.za"

                        }

                    }
                }
                is TypeFilter -> {
                    filter.state.forEach { tag ->
                        if (tag.isIncluded()) {
                            includedGenre.add(tag.value)
                        } else if (tag.isExcluded()) {
                            excludedGenre.add(tag.value)
                        }
                    }
                }
                is DemographicFilter -> {
                    filter.state.forEach { tag ->
                        if (tag.isIncluded()) {
                            includedGenre.add(tag.value)
                        } else if (tag.isExcluded()) {
                            excludedGenre.add(tag.value)
                        }
                    }
                }
                is ContentFilter -> {
                    filter.state.forEach { tag ->
                        if (tag.isIncluded()) {
                            includedGenre.add(tag.value)
                        } else if (tag.isExcluded()) {
                            excludedGenre.add(tag.value)
                        }
                    }
                }
                is GenreFilter -> {
                    filter.state.forEach { tag ->
                        if (tag.isIncluded()) {
                            includedGenre.add(tag.value)
                        } else if (tag.isExcluded()) {
                            excludedGenre.add(tag.value)
                        }
                    }
                }
                is PublicationFilter -> {
                    if (filter.state != 0) {
                        release = publicationList[filter.state].value
                    }
                }
            }
        }

        return url.apply {
            addQueryParameter("page", page.toString())
            addQueryParameter("sort", sort)
            addQueryParameter("genres", genres)
            addQueryParameter("release", release)
            addQueryParameter("chapters", chapters)
        }.toString()

    }

    // Sort Filter
    class SortItem(val name: String, val value: String)

    private val sortList: List<SortItem> = listOf(
        SortItem("Rating", "rating"),
        SortItem("Comments", "comments"),
        SortItem("Discuss", "discuss"),
        SortItem("Update", "update"),
        SortItem("Create", "create"),
        SortItem("Name", "name"),
        SortItem("Total Views", "d000"),
        SortItem("Most Views 360 days", "d360"),
        SortItem("Most Views 180 days", "d180"),
        SortItem("Most Views 90 days", "d090"),
        SortItem("Most Views 30 days", "d030"),
        SortItem("Most Views 7 days", "d007"),
        SortItem("Most Views 24 hours", "h024"),
        SortItem("Most Views 12 hours", "h012"),
        SortItem("Most Views 6 hours", "h006"),
        SortItem("Most Views 60 minutes", "h001"),
    )

    class SortDefault(val defaultSort: SortItem, val ascending: Boolean)

    private val defaultSort: SortDefault = SortDefault(SortItem("Rating", "rating"), false)

    class SortFilter(name: String, default: SortDefault, sorts: List<SortItem>) :
        Filter.Sort(
            name,
            sorts.map { it.name }.toTypedArray(),
            Selection(sorts.indexOf(default.defaultSort), default.ascending))

    private fun getSortFilter(): SortFilter = SortFilter("Sort By", defaultSort, sortList)

    // Min - Max Chapter Filter
    abstract class TextFilter(name: String) : Filter.Text(name)

    class MinChapterTextFilter : TextFilter("Min. Chapters")
    class MaxChapterTextFilter : TextFilter("Max. Chapters")

    private fun getMinChapterFilter(): TextFilter = MinChapterTextFilter()
    private fun getMaxChapterFilter(): TextFilter = MaxChapterTextFilter()


    // Publication Filter
    class PublicationItem(val name: String, val value: String)

    private val publicationList: List<PublicationItem> = listOf(
        PublicationItem("All", ""),
        PublicationItem("Pending", "pending"),
        PublicationItem("Ongoing", "ongoing"),
        PublicationItem("Completed", "completed"),
        PublicationItem("Hiatus", "hiatus"),
        PublicationItem("Cancelled", "cancelled"),
    )

    class PublicationFilter(
        name: String,
        statusList: List<PublicationItem>,
        defaultStatus: PublicationItem
    ) :
        Filter.Select<String>(
            name,
            statusList.map { it.name }.toTypedArray(),
            statusList.indexOf(defaultStatus))

    private fun getPublicationFilter(): PublicationFilter =
        PublicationFilter("Status", publicationList, PublicationItem("All", ""))

    //Type
    class TypeItem(name: String, val value: String) : Filter.TriState(name)

    private val typeList: List<TypeItem> = listOf(
        TypeItem("Cartoon", "cartoon"),
        TypeItem("Comic", "comic"),
        TypeItem("Doujinshi", "doujinshi"),
        TypeItem("Manga", "manga"),
        TypeItem("Manhua", "manhua"),
        TypeItem("Manhwa", "manhwa"),
        TypeItem("Webtoon", "webtoon"),
    )

    class TypeFilter(name: String, typeList: List<TypeItem>) :
        Filter.Group<TypeItem>(name, typeList)

    private fun getTypeFilter(): TypeFilter = TypeFilter("Type", typeList)

    //Demographic
    class DemographicItem(name: String, val value: String) : Filter.TriState(name)

    private val demographicList: List<DemographicItem> = listOf(
        DemographicItem("Shounen", "shounen"),
        DemographicItem("Shoujo", "shoujo"),
        DemographicItem("Seinen", "seinen"),
        DemographicItem("Josei", "josei"),
    )

    class DemographicFilter(name: String, demographicList: List<DemographicItem>) :
        Filter.Group<DemographicItem>(name, demographicList)

    private fun getDemographicFilter(): DemographicFilter =
        DemographicFilter("Demographic", demographicList)

    // Content
    class ContentItem(name: String, val value: String) : Filter.TriState(name)

    private val contentList: List<ContentItem> = listOf(
        ContentItem("Adult", "adult"),
        ContentItem("Ecchi", "ecchi"),
        ContentItem("Gore", "gore"),
        ContentItem("Hentai", "hentai"),
        ContentItem("Mature", "mature"),
        ContentItem("Smut", "smut"),
    )

    class ContentFilter(name: String, contentList: List<ContentItem>) :
        Filter.Group<ContentItem>(name, contentList)

    private fun getContentFilter(): ContentFilter = ContentFilter("Content", contentList)

    // Genre
    class GenreItem(name: String, val value: String) : Filter.TriState(name)

    private val genreList: List<GenreItem> = listOf(
        GenreItem("Action", "action"),
        GenreItem("Adaptation", "adaptation"),
        GenreItem("Adventure", "adventure"),
        GenreItem("Aliens", "aliens"),
        GenreItem("Animals", "animals"),
        GenreItem("Anthology", "anthology"),
        GenreItem("Award Winning", "award_winning"), // This Is Hidden In Web
        GenreItem("Cars", "cars"),
        GenreItem("Comedy", "comedy"),
        GenreItem("Cooking", "cooking"),
        GenreItem("Crime", "crime"),
        GenreItem("Crossdressing", "crossdressing"),
        GenreItem("Delinquents", "delinquents"),
        GenreItem("Dementia", "dementia"),
        GenreItem("Demons", "demons"),
        GenreItem("Drama", "drama"),
        GenreItem("Fantasy", "fantasy"),
        GenreItem("Full Color", "full_color"),
        GenreItem("Game", "game"),
        GenreItem("Gender Bender", "gender_bender"),
        GenreItem("Genderswap", "genderswap"),
        GenreItem("Gyaru", "gyaru"),
        GenreItem("Harem", "harem"),
        GenreItem("Historical", "historical"),
        GenreItem("Horror", "horror"),
        GenreItem("Incest", "incest"),
        GenreItem("Isekai", "isekai"),
        GenreItem("Kids", "kids"),
        GenreItem("Loli", "loli"),
        GenreItem("Lolicon", "lolicon"),
        GenreItem("Magic", "magic"),
        GenreItem("Magical Girls", "magical_girls"),
        GenreItem("Martial Arts", "martial_arts"),
        GenreItem("Mecha", "mecha"),
        GenreItem("Medical", "medical"),
        GenreItem("Military", "military"),
        GenreItem("Monster Girls", "monster_girls"),
        GenreItem("Monsters", "monsters"),
        GenreItem("Music", "music"),
        GenreItem("Mystery", "mystery"),
        GenreItem("Office Workers", "office_workers"),
        GenreItem("Oneshot", "oneshot"),
        GenreItem("Parody", "parody"),
        GenreItem("Philosophical", "philosophical"),
        GenreItem("Police", "police"),
        GenreItem("Post Apocalyptic", "post_apocalyptic"),
        GenreItem("Psychological", "psychological"),
        GenreItem("Reincarnation", "reincarnation"),
        GenreItem("Romance", "romance"),
        GenreItem("Samurai", "samurai"),
        GenreItem("School Life", "school_life"),
        GenreItem("Sci-fi", "sci_fi"),
        GenreItem("Shotacon", "shotacon"),
        GenreItem("Shounen Ai", "shounen_ai"),
        GenreItem("Shoujo Ai", "shoujo_ai"),
        GenreItem("Slice of Life", "slice_of_life"),
        GenreItem("Space", "space"),
        GenreItem("Sports", "sports"),
        GenreItem("Super Power", "super_power"),
        GenreItem("Superhero", "superhero"),
        GenreItem("Supernatural", "supernatural"),
        GenreItem("Survival", "survival"),
        GenreItem("Thriller", "thriller"),
        GenreItem("Traditional Games", "traditional_games"),
        GenreItem("Tragedy", "tragedy"),
        GenreItem("Vampires", "vampires"),
        GenreItem("Video Games", "video_games"),
        GenreItem("Virtual Reality", "virtual_reality"),
        GenreItem("Wuxia", "wuxia"),
        GenreItem("Yaoi", "yaoi"),
        GenreItem("Yuri", "yuri"),
        GenreItem("Zombies", "zombies"),
    )

    class GenreFilter(name: String, genreList: List<GenreItem>) :
        Filter.Group<GenreItem>(name, genreList)

    private fun getGenreFilter(): GenreFilter = GenreFilter("Genre", genreList)

    //Helper

    private inline fun <reified T> Iterable<*>.findInstance() = find { it is T } as? T

}
