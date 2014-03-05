def static build(bookmarks) {
    def writer = new StringWriter()
    new groovy.xml.MarkupBuilder(writer).html {
        head {
            title "bookmarks"
        }
        body {
            ul {
                bookmarks.each { bookmark ->
                    li {
                        a (href:bookmark.url, bookmark.title)
                    }
                }
            }
        }
    }
    writer.toString()
}

