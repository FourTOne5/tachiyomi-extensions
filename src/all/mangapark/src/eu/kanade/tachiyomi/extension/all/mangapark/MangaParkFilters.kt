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
            getStatusFilter(),
            getTypeFilter(),
            getDemographicFilter(),
            getContentFilter(),
            getGenreFilter()
        )
    }

    internal fun addFiltersToUrl(url: HttpUrl.Builder, page: Int, filters: FilterList): String {
        val includedGenre: MutableList<String> = mutableListOf()
        val excludedGenre: MutableList<String> = mutableListOf()

        return url.apply {
            addQueryParameter("page", page.toString())
            addQueryParameter(
                "genres",
                includedGenre.joinToString(",") + "|" + excludedGenre.joinToString(",")
            )
        }.toString()

    }

    // Sort Filter
    class SortItem(val name: String, val value: String, val sortable: Boolean = true)

    private val sortList: List<SortItem> = listOf(
        SortItem("Rating", "rating"),
        SortItem("Comments", "comments"),
        SortItem("Discuss", "discuss"),
        SortItem("Update", "update"),
        SortItem("Create", "create"),
        SortItem("Name", "name"),
        SortItem("Total Views", "d000", false),
        SortItem("Most Views 360 days", "d360", false),
        SortItem("Most Views 180 days", "d180", false),
        SortItem("Most Views 90 days", "d090", false),
        SortItem("Most Views 30 days", "d030", false),
        SortItem("Most Views 7 days", "d007", false),
        SortItem("Most Views 24 hours", "h024", false),
        SortItem("Most Views 12 hours", "h012", false),
        SortItem("Most Views 6 hours", "h006", false),
        SortItem("Most Views 60 minutes", "h001", false),
    )

    class SortDefault(val defaultSort: SortItem, val ascending: Boolean)

    private val defaultSort: SortDefault = SortDefault(SortItem("Rating", "rating"), false)

    class SortFilter(name: String, default: SortDefault, sorts: List<SortItem>) :
        Filter.Sort(name, sorts.map { it.name } .toTypedArray(), Selection(sorts.indexOf(default.defaultSort), default.ascending))

    private fun getSortFilter(): SortFilter = SortFilter("Sort By", defaultSort, sortList)

    // Min - Max Chapter Filter
    abstract class TextFilter(name: String) : Filter.Text(name)

    class MinChapterTextFilter : TextFilter("Min. Chapters")
    class MaxChapterTextFilter : TextFilter("Max. Chapters")

    private fun getMinChapterFilter(): TextFilter = MinChapterTextFilter()
    private fun getMaxChapterFilter(): TextFilter = MaxChapterTextFilter()


    // Status Filter
    class StatusItem(name: String, val value: String): Filter.CheckBox(name)

    private val statusList: List<StatusItem> = listOf(
            StatusItem("Pending", "pending"),
            StatusItem("Ongoing", "ongoing"),
            StatusItem("Completed", "completed"),
            StatusItem("Hiatus", "hiatus"),
            StatusItem("Cancelled", "cancelled"),
    )

    class StatusFilter(name: String, statusList: List<StatusItem>) :
        Filter.Group<StatusItem>(name, statusList)

    private fun getStatusFilter(): StatusFilter = StatusFilter("Status", statusList)

    //Type
    class TypeItem(name: String, val value: String): Filter.TriState(name)

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
    class DemographicItem(name: String, val value: String): Filter.TriState(name)

    private val demographicList: List<DemographicItem> = listOf(
        DemographicItem("Shounen", "shounen"),
        DemographicItem("Shoujo", "shoujo"),
        DemographicItem("Seinen", "seinen"),
        DemographicItem("Josei", "josei"),
    )

    class DemographicFilter(name: String, demographicList: List<DemographicItem>) :
        Filter.Group<DemographicItem>(name, demographicList)

    private fun getDemographicFilter(): DemographicFilter = DemographicFilter("Demographic", demographicList)

    // Content
    class ContentItem(name: String, val value: String): Filter.TriState(name)

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
    class GenreItem(name: String, val value: String): Filter.TriState(name)

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

    private fun getGenreFilter(): GenreFilter = GenreFilter("Demographic", genreList)












}
