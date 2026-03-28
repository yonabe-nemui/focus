package app.focus.personal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform