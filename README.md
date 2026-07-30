# Servlets

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

## 動作確認
ブラウザで下記URLが `OK` と表示されればOK

https://system.mitsuishifukai.co.jp:4343/FukaiSystem/hello

## デバッグ
アクティビティバーのデバッグアイコンをクリック
Attach to Docker Tomcat で右三角ボタンを押す