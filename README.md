# Servlets
## FukaiSystem サーブレットアプリ

Swing クライアント群（`Applications/Clients/*`）の接続先となるサーバーサイドアプリ。
Eclipse + Sysdeo Tomcat プラグインのプロジェクトで、war にはパッケージせず **展開済みディレクトリのまま** Tomcat に配置する。

| 項目 | 値 |
|---|---|
| コンテキストパス | `/FukaiSystem` |
| 配置先 | Tomcat の `webapps/FukaiSystem`（このディレクトリをそのまま置く） |
| JNDI データソース | `jdbc/FukaiSystem` |
| ソース | `WEB-INF/src`（出力先は `WEB-INF/classes`） |

---

## 構成
```
project-root/
├── lib/                    # jar ファイル置き場
├── WEB-INF/
│   ├─ classes/
│   ├─ lib/                 # jar ファイル置き場
│   ├─ src/                 # ソースファイル置き場
│   │  ├─ fukaisystem/
│   │  │  ├─ application/   # アプリケーション
│   │  │  │  ├─ 
│   │  │  │
│   │  │  ├─ domain/        # ドメインクラス
│   │  │  ├─ dto/           # DTOクラス
│   │  │  ├─ foundation/    # サーブレット共通
│   │  │  ├─ print/         # 印刷関連
│   │  │  ├─ sql/           # DB接続定義
│   │  │  ├─ util/          # ユーティリティクラス
│   │  │  ├─ Hello.java     # 動作確認用
│   │  └─ log4j.properties  # ログ定義
│   └─ web.xml              # マッピング定義
├── .classpath              # クラスパスの指定
├── .gitignore
├── .project                # Eclipse用
├── .tomcatplugin           # ?
├── formatter.xml           # フォーマッタ定義
└── README.md               # このファイル
```
開発用は、 Tomcat を JPDA(Java Platform Debugger Architecture) モードで起動し、ホスト側の IDE からリモートデバッグできるようにする設定を追加している

---

## デプロイ要件

### JDBC ドライバは Tomcat の `$CATALINA_HOME/lib` に置く

**このアプリの `WEB-INF/lib` には JDBC ドライバを置かない。**
展開先の Tomcat の `$CATALINA_HOME/lib`（`<Tomcat>/lib/`）に `mssql-jdbc-12.10.0.jre8.jar` を配置すること。

コンテナ管理データソース（`<Resource type="javax.sql.DataSource">`）では **Tomcat 自身が接続プールを生成する** ため、ドライバが webapp の `WEB-INF/lib` にしか無いと解決できない。
Tomcat 公式も、再デプロイ時のクラスローダリークを避けるために JDBC ドライバを `$CATALINA_HOME/lib` へ置くことを推奨している。

`jre11` 版ではなく `jre8` 版を使う。JDK 8〜21 のいずれでも動作するため、展開先の JDK バージョンを問わず 1 ファイルで済む。

> **配置を忘れたときの症状**
>
> Tomcat 起動時、コンテキストの配備ログに次が出て DataSource が使えなくなる。
> アプリ自体は起動するため気づきにくい。
>
> ```
> WARNING ... Cannot load JDBC driver class 'com.microsoft.sqlserver.jdbc.SQLServerDriver'
> WARNING ... Failed to register in JMX: ... Unexpected exception resolving reference with name [FukaiSystem]
> ```

### コンテキスト定義が必要

`$CATALINA_HOME/conf/Catalina/localhost/FukaiSystem.xml` に、`jdbc/FukaiSystem` という名前の DataSource を定義すること。
`WEB-INF/web.xml` の `<resource-ref>` はこれを参照している。

同じファイルに、入荷通知の送信先も定義すること。

```xml
<Parameter name="tracker.arrivals.url"
           value="http://localhost:8080/FukaiTracker/api/arrivals" override="false" />
```

注文登録（`OrderRegister`）で入荷年月日が新たに入力された部品を、同じ Tomcat 上の部品入荷確認アプリ（`/FukaiTracker`）へ通知するために使う。
詳細は後述の「入荷通知」を参照。
未設定の場合は上記の値が既定値として使われるため、Tomcat の HTTP ポートが 8080 で FukaiTracker が同じ Tomcat に展開されていれば、記述が無くても動作する。

接続先は SQL Server の `FukaiSystem` データベース。
[DBConnection.java](WEB-INF/src/fukaisystem/sql/DBConnection.java) は JNDI ルックアップに失敗した場合、`DriverManager` による直接接続にフォールバックする。
どちらの経路でもドライバは `$CATALINA_HOME/lib` から解決される。

### ローカル Tomcat（Eclipse / Sysdeo）を使う場合

同じく、その Tomcat の `lib/` にドライバを配置すること。
`.classpath` は JDBC ドライバを参照していない（ソースがドライバのクラスを `import` しておらず `Class.forName` の文字列参照だけのため）ので、ビルドパスへの追加は不要。

---

## 開発用 Docker 環境

[Infrastructure](../../Infrastructure/) の Docker 環境は上記の要件を満たしている。

- ドライバ: `Infrastructure/tomcat/rootfs/usr/local/tomcat/lib/mssql-jdbc-12.10.0.jre8.jar`
- コンテキスト定義: `Infrastructure/tomcat/rootfs/.../conf/Catalina/localhost/FukaiSystem.xml`
- このディレクトリは `webapps/FukaiSystem` にバインドマウントされる

```bash
cd Infrastructure
docker compose up -d --build
# https://localhost:4343/FukaiSystem/hello -> OK
```

`reloadable="true"` のため、IDE で `WEB-INF/classes` へコンパイルすれば自動で再読み込みされる。
リモートデバッグは `localhost:5006`（設定は [.vscode/launch.json](.vscode/launch.json)）。

同じ Tomcat 上で部品入荷確認アプリ（`/FukaiTracker`）が併存している。
詳細は [Infrastructure/README.md](../../Infrastructure/README.md) を参照。

---

## ビルド設定（重要）

コンパイラ準拠レベルは `.settings/org.eclipse.jdt.core.prefs` で **1.8 に固定**している。

```
org.eclipse.jdt.core.compiler.compliance=1.8
org.eclipse.jdt.core.compiler.source=1.8
org.eclipse.jdt.core.compiler.codegen.targetPlatform=1.8
```

このファイルが無いと、IDE は自身が動いている JDK のバージョンでコンパイルしてしまう。
JDK 25 でビルドされ、クラスファイルのメジャーバージョンが 69 になると**JDK 21 の Tomcat が読み込めなくなる**障害が発生する（全サーブレットが 404 になる）。
`.vscode/settings.json` の `java.configuration.runtimes` だけでは固定されないので、このファイルを消さないこと。

生成されたクラスが Java 8 になっているかは次で確認できる（52 = Java 8）。

```bash
od -An -tu1 -j7 -N1 WEB-INF/classes/fukaisystem/Hello.class
```

IDE を使わずにコマンドラインでビルドし直す場合:

```bash
CP=$(ls lib/*.jar WEB-INF/lib/*.jar | tr '\n' ';')
find WEB-INF/src -name "*.java" > /tmp/srcs.txt
javac --release 8 -encoding UTF-8 -cp "$CP" -d WEB-INF/classes @/tmp/srcs.txt
cp WEB-INF/src/log4j.properties WEB-INF/classes/
```

---

## 入荷通知

注文画面（Swing クライアント `Clients/business`）で入庫年月日と入庫場所を入力して登録すると、[OrderRegister](WEB-INF/src/fukaisystem/application/business/OrderRegister.java) が部品入荷確認アプリ（`/FukaiTracker`）の入荷通知 API へ POST し、FukaiTracker が WebSocket で工場モニタ（入荷表示アプリ）と部品表へ配信する。
webapp はまたぐが同じ Tomcat・同じ JVM なので、通信はループバックで完結する（Apache は経由しない）。

**入荷そのものは `T_在庫_子` への登録で成立している。** 
FukaiTracker の入荷一覧（`GET /api/arrivals`）はこのテーブルを直接読むため、この POST は「今入荷した」ことを即座に伝える通知にすぎず、FukaiTracker 側は何も保存しない。

送るのは **在庫親ID・明細ID・製番・仕入先名・品名・図面番号・部品番号・入庫場所**。
在庫親IDと明細IDは `T_在庫_子` の主キーで、FukaiTracker はこの 2 つから入荷イベントIDを組み立てる。
入荷一覧も同じ主キーからIDを作るため、同じ入荷が WebSocket と一覧で別のIDになることがなく、工場モニタが重複表示しない。

製番は `注文期-注文番号 注文枝番`（集計処理の `製作期-製作番号 製作枝番` と同じ組み立て）、仕入先名は画面が保持している社名をそのまま DTO で受け取る。
FukaiTracker 側は図面番号を内部の図面IDに変換し、部品番号と合わせて対象の部品を入荷済にする。

> 明細行の列順は `T_在庫_子` の列順に合わせてある。
> 列を増やすときは DB・`OrderRegister`・`GetSelectedOrder`・`OrderSelectedInfoPanel` の 4 箇所を必ずそろえること。
> 各クラスの列位置は定数に切り出してある。


送信は [ArrivalNotifier](WEB-INF/src/fukaisystem/util/ArrivalNotifier.java) がデーモンスレッドで非同期に行う。
通信エラーと 5xx は指数バックオフ（500ms から倍にして上限 8 秒、フルジッター）で最大 5 回まで再送し、それでも届かなければ log4j に WARN を残して諦める。
送信内容の誤り（4xx）は何度送っても結果が変わらないので再送しない。
通知は業務処理の付随であり、FukaiTracker が停止していても注文登録そのものは成立させるため、失敗が業務処理に影響することはない。

> **同じ注文を登録し直しても、以前からの入荷済み分は再通知されない。**
> `T_在庫_子` は登録のたびに全行 DELETE → 再 INSERT されるため、入庫年月日が入っているというだけでは今回入荷したのか判別できない。
> DELETE の前に「図面番号＋部品番号」（部品に紐付かない材料の行は「品名＋数量」）ごとの入荷済み件数を控え、その差分だけを通知している。
> 画面側も、これらを変更した行では入荷チェックを付けられないようにしている。

## ライブラリ

`WEB-INF/lib` と `lib` は `.gitignore` の対象で、リポジトリには含まれない。

| ディレクトリ | 内容 |
|---|---|
| `WEB-INF/lib` | アプリが実行時に使うライブラリ（`core` / `log4j` / `print`）。**JDBC ドライバは含めない** |
| `lib` | コンパイル用に Tomcat から借りるライブラリ（`servlet-api` / `jsp-api` / `tomcat-dbcp` など） |

## 動作確認
ブラウザで下記URLが `OK` と表示されればOK

https://system.mitsuishifukai.co.jp:4343/FukaiSystem/hello

## デバッグ
アクティビティバーのデバッグアイコンをクリック
Attach to Docker Tomcat で右三角ボタンを押す