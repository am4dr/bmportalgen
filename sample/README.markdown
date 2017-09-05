links:
- title: bmportalgen/README.markdown at master · am4dr/bmportalgen
  url: https://github.com/am4dr/bmportalgen/blob/master/README.markdown

# bmportalgen

version: 0.0.1-prototype

メモ制作が進行中のメモを入れたディレクトリからポータルサイトというか、
なんのメモを書き進めていたかをざっくり見れる静的なHTMLを生成する。


## 使い方
対象ディレクトリには対応した形式のテキストファイルが入っているものとして

    groovy generate.groovy 対象ディレクトリ > 出力ファイル.html


## メモの仕様
ひとつのメモはひとつのテキストファイル。(BOM無し UTF-8)

メモはメタデータを含めることができるフォーマットが望ましかったが、
現状ではひとまず実装できた改変したmarkdownに対応している。

仮に対応形式を増やすとして、それぞれごとにメタデータの取り出し方は実装する方針。

### Markdown
最初の行から`^#.*`にマッチする最初の行のひとつ前の行までをメタデータ行とする。

メタデータ行について

- `^#.*`にマッチする行を持たない、有効なYAMLとする


本文について

- 一行目の一文字目は`#`で始める
- 見出し行は`#`を行頭に書くタイプのみを認める


## そのた

    sample/             # メモを入れているディレクトリの霊
    sample/index.html   # 生成したhtml
    script.js           # ページのタイトルとURLを
                        # YAML形式でクリップボードにコピーするブックマークレット



