package app.focus.personal.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

private const val NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
private const val NS_RSS = "http://purl.org/rss/1.0/"
private const val NS_DC = "http://purl.org/dc/elements/1.1/"
private const val NS_HATENA = "http://www.hatena.ne.jp/info/xmlns#"

@Serializable
@XmlSerialName("RDF", NS_RDF, "rdf")
data class HatenaRdf(
    @XmlSerialName("channel", NS_RSS, "")
    val channel: HatenaChannel,
    @XmlSerialName("item", NS_RSS, "")
    val items: List<HatenaItem>
)

@Serializable
@XmlSerialName("channel", NS_RSS, "")
data class HatenaChannel(
    @XmlElement(true)
    val title: String,
    @XmlElement(true)
    val link: String,
    @XmlElement(true)
    val description: String? = null
)

@Serializable
@XmlSerialName("item", NS_RSS, "")
data class HatenaItem(
    @XmlElement(true)
    val title: String,
    @XmlElement(true)
    val link: String,
    @XmlElement(true)
    val description: String? = null,
    @XmlSerialName("date", NS_DC, "dc")
    @XmlElement(true)
    val date: String? = null,
    @XmlSerialName("bookmarkcount", NS_HATENA, "hatena")
    @XmlElement(true)
    val bookmarkCount: Int? = null,
    @XmlSerialName("subject", "http://purl.org/rss/1.0/modules/taxonomy/", "taxo")
    @XmlElement(true)
    val subject: String? = null
)

fun HatenaItem.toRssItem(): RssItem {
    return RssItem(
        title = title,
        link = link,
        description = description,
        pubDate = date,
        guid = link,
        bookmarkCount = bookmarkCount,
        pubDateMillis = app.focus.personal.util.DateUtils.parseIso8601ToMillis(date)
    )
}
