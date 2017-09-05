@Grab('ch.qos.logback:logback-classic:1.1.7')
@Grab('org.yaml:snakeyaml:1.8')
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

import groovy.transform.Field

@Field
static final def cliParser = new CliBuilder(usage: 'groovy <this script file> [options] <target directories>').with {
    it._(longOpt: 'trace', 'log level: TRACE')
    it._(longOpt: 'debug', 'log level: DEBUG')
    it._(longOpt: 'info',  'log level: INFO')
    it._(longOpt: 'warn',  'log level: WARN')
    it._(longOpt: 'error', 'log level: ERROR')
    it._(longOpt: 'nolog', 'log level: OFF')
    it
}
@Field
final String loggerName = "${this.class.name}@${String.format('%x', System.identityHashCode(this))}"
@Field
final def log = LoggerFactory.getLogger(loggerName)

def configLogger(logger, logOpts) {
    def level
    if (logOpts.nolog) { level = "off" }
    else if (logOpts.trace) { level = "trace" }
    else if (logOpts.debug) { level = "debug" }
    else if (logOpts.info) { level = "info" }
    else if (logOpts.warn) { level = "warn" }
    else if (logOpts.error) { level = "error" }
    else { level = "warn" }
    ((ch.qos.logback.classic.Logger)logger).level = Level.valueOf(level)
}

def toRealPath(args) {
    log.trace("toRealPath: $args")
    // TODO パス変換のエラー処理
    args.collect { Paths.get(it).toAbsolutePath().toRealPath() }
}

def checkArgs(paths) {
    log.trace("checkArgs: $paths")
    if (paths.size() <= 0) {
        log.info("target directories are not specified")
        cliParser.usage()
        return false
    }
    log.debug("target paths ${paths}")

    def notDirctories = paths.findAll { !Files.isDirectory(it) }
    if (notDirctories.size() != 0) {
        notDirctories.each {
            log.error("$it is not a directory")
        }
        return false
    }
    return true
}

def processDirectory(dir) {
    log.trace("processDirectory: $dir")
    Files.list(dir)
        .findAll { it.fileName.toString().endsWith('.markdown') }
        .collect { parseTextFile(it) }
}
def parseTextFile(path) {
    log.trace("parseTextFile: $path")
    def lines = path.readLines("utf-8")
    def meta = extractMetaData(path, lines)
    [file: path, meta: meta, text: lines[meta.start-1..lines.size()-1]]
}
@Field
static final lastModifiedDateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
static String instantToString(instant) {
    instant.atZone(java.time.ZoneId.systemDefault()).format(lastModifiedDateTimeFormatter)
}
def extractMetaData(path, lines) {
    def frontMatter = lines.takeWhile { !(it ==~ /^#.*/) }
    def frontMatterYaml = parseYaml(frontMatter)
    if (frontMatterYaml == null) frontMatter = null
    def meta = frontMatterYaml ?: [:]
    def textFirstLineNum = 1 + (frontMatter ? frontMatter.size() : 0)
    def textFirstLine = lines.size() >= textFirstLineNum ? lines[textFirstLineNum-1] : ""
    def title = { -> textFirstLine ==~ /^#+.*/ ? textFirstLine.replaceAll('^#+ ', '') : path.fileName.toString() }
    [
        start: textFirstLineNum,
        title: meta.title ?: title(),
        links: meta.links ?: [],
        summary: meta.summary ?: "",
        lastModified: instantToString(Files.getLastModifiedTime(path).toInstant())
    ]
}
@Field
static final yamlParser = new org.yaml.snakeyaml.Yaml()
def parseYaml(text) {
    yamlParser.load(text.join('\n'))
}


def main() {
    def parsedArgs = cliParser.parse(args)
    configLogger(log, parsedArgs)
    def targetPaths = toRealPath(parsedArgs.arguments())
    if (!checkArgs(targetPaths)) {
        System.exit(1)
    }
    def processedDirs = targetPaths.collectEntries { dir ->
        [(dir): processDirectory(dir)]
    }
    def entriesList = []
    processedDirs.each { dir, targets ->
        log.debug("process template with $dir")
        def entryList = []
        targets
            .sort(false) { it.meta.lastModified }.reverse()
            .each {
                log.debug("process template with $it.file")
                def links = it.meta.links?.collect { linkTemplate.make(it).toString() }?.join('\n') ?: ""
                def entry = entryTemplate.make(it.meta + [links: links, text: it.text.join('\n')]).toString()
                entryList += entry
            }
        def entries = entriesTemplate.make([directory: dir.fileName, entries: entryList.join('\n')]).toString()
        entriesList += entries
    }
    println htmlTemplate.make([entries: entriesList.join('\n')]).toString()
}
main()

@Field
static final def templateEngine = new groovy.text.GStringTemplateEngine()

@Field
static final def htmlTemplate = generate.templateEngine.createTemplate('''\
<html>
<head>
    <meta charset="UTF-8">
    <title>メモとぶっくまーく</title>
    <style>
body {
    font-family: "メイリオ";
}
pre {
    font-family: "consolas";
}
.directory-name {
    font-size: 2em;
}
.directory-name::-webkit-details-marker {
    display: none;
}
.entries {
    padding-left: 2em;
}
.entry pre {
    padding-left: 1.75em;
}
.entry summary {
    padding-left: 0.75em;
    padding-top: 0.25em;
    padding-bottom: 0.25em;
    background-color: rgb(30,30,30);
    color: snow;
}
.entry summary .lastModified {
    color: gray;
    font-size: 0.75em;
    margin-left: 1rem;
}
.entry summary .summary {
    margin: 0;
    padding-left: 1rem;
    color: lightgray;
    font-size: 0.8em;
}
.entry summary .link a {
    color: #12dc9c;
}
li.entry {
    list-style-type: none;
}
    </style>
</head>
<body>
${entries}
</body>
</html>
''')

@Field
static final def entriesTemplate = generate.templateEngine.createTemplate('''\
<details class="directory" open>
    <summary class="directory-name">$directory</summary>
    <ul class="entries">
${entries}
    </ul>
</details>
''')
@Field
static final def entryTemplate = generate.templateEngine.createTemplate('''\
<li class="entry">
    <details>
        <summary>${title}<span class="lastModified">$lastModified</span>
            <p class="summary">$summary</p>
            <ul class="links">
$links
            </ul>
        </summary>
        <pre>
${text.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')}
        </pre>
    </details>
</li>
''')

@Field
static final def linkTemplate = generate.templateEngine.createTemplate('''\
<li class="link"><a href="${url}">${title}</a></li>''')

