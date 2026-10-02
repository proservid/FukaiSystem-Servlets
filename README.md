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