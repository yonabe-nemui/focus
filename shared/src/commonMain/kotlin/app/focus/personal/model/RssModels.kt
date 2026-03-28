package app.focus.personal.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("rss", "", "")
data class RssFeed(
    val channel: RssChannel = RssChannel()
)

@Serializable
@XmlSerialName("channel", "", "")
data class RssChannel(
    val title: String = "",
    val link: String = "",
    val description: String? = null,
    @XmlSerialName("item", "", "")
    val items: List<RssItem> = emptyList()
)

@Serializable
@XmlSerialName("item", "", "")
data class RssItem(
    val title: String = "",
    val link: String = "",
    val description: String? = null,
    val pubDate: String? = null,
    val guid: String? = null
)
