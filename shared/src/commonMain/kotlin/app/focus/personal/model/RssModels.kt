package app.focus.personal.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("rss", "", "")
data class RssFeed(
    @XmlSerialName("version", "", "")
    val version: String = "2.0",
    val channel: RssChannel = RssChannel()
)

@Serializable
@XmlSerialName("channel", "", "")
data class RssChannel(
    @XmlElement(true)
    val title: String = "",
    @XmlElement(true)
    val link: String = "",
    @XmlElement(true)
    val description: String? = null,
    @XmlElement(true)
    val language: String? = null,
    @XmlElement(true)
    val pubDate: String? = null,
    @XmlSerialName("item", "", "")
    val items: List<RssItem> = emptyList()
)

@Serializable
@XmlSerialName("item", "", "")
data class RssItem(
    @XmlElement(true)
    val title: String = "",
    @XmlElement(true)
    val link: String = "",
    @XmlElement(true)
    val description: String? = null,
    @XmlElement(true)
    val pubDate: String? = null,
    @XmlElement(true)
    val guid: String? = null
)
