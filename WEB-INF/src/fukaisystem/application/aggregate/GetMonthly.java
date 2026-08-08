package fukaisystem.application.aggregate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

import javax.servlet.ServletResponse;

import fukaisystem.dto.AggregateDTO;
import fukaisystem.foundation.ServiceFoundation;
import fukaisystem.sql.ResultSetConverter;

/**
 * 月次集計を取得するためのクラス
 * 複雑な集計は一度のクエリで全データを取得できないので対象外（専用サーブレットを配備）
 *
 * @author kameura
 *
 */
public class GetMonthly extends ServiceFoundation {

	private static final int PURCHASE_1 = 0;
	private static final int PURCHASE_2 = 1;
	private static final int PURCHASE_3 = 2;
	private static final int PURCHASE_4 = 3;
	private static final int PURCHASE_5 = 4;
	private static final int PURCHASE_6 = 5;
	private static final int PURCHASE_TAX = 6;
	private static final int DISPATCH = 7;
	private static final int SALES_1 = 8;
	private static final int SALES_2 = 9;
	private static final int SALES_TAX = 12;
	private static final int QUOTATION = 14;
	private static final int RECEIPT = 15;
	private static final int CHECK_DELIVERY = 16;
	private static final int CREATE_DELIVERY = 17;
	private static final int AGGREGATE_ACCOUNT = 18;
	private static final int CHECK_ACCOUNT = 19;
	private static final int MH = 20;
	private static final int SALES_MANAGEMENT = 21;

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		AggregateDTO condition = cast(response, o, AggregateDTO.class);
		if (condition == null) {
			return null;
		}

		String sql = getQuery(
			condition.getOrder(), condition.getYear(), condition.getMonth(), condition.getSupplier()
		);
		if (sql.isEmpty()) {
			reportException(response, new Exception("月次集計では対応していない集計種別です"));
			return null;
		}

		try (Statement st = c.createStatement();) {
			try (ResultSet rs = st.executeQuery(sql);) {
				return ResultSetConverter.toTable(rs);
			}
		}
	}

	/**
	 * 集計用クエリを取得する
	 *
	 * @param order    集計種別（メニューのインデックス、0 はじまり）
	 * @param y        年
	 * @param m        月
	 * @param supplier 仕入先CD（未指定は 0）
	 *
	 * @return 集計用クエリ、対応していない集計種別の場合は空文字列
	 */
	private String getQuery(int order, int y, int m, int supplier) {
		Calendar target = Calendar.getInstance();
		target.set(Calendar.YEAR, y);
		target.set(Calendar.MONTH, m - 1);
		target.set(Calendar.DATE, 1);
		target.add(Calendar.MONTH, 1);
		int ny = target.get(Calendar.YEAR);// 翌月の年
		int nm = target.get(Calendar.MONTH) + 1;
		target.set(Calendar.YEAR, y);
		target.set(Calendar.MONTH, m - 1);
		target.set(Calendar.DATE, 1);
		target.add(Calendar.MONTH, -1);
		int by = target.get(Calendar.YEAR);// 前月の年
		int bm = target.get(Calendar.MONTH) + 1;

		String o_period = "納品書日>='";// 注文書の集計期間
		if (y > 2013) {// 61期より、月末〆
			o_period += y + "/" + m + "/1' and 納品書日<'" + ny + "/" + nm + "/1'";
		} else {// 60期以前は25日〆
			if (m == 1) {// 1月のみ1日から25日まで
				o_period += y + "/1/1' and 納品書日<'" + y + "/1/26'";
			} else if (m == 12) {// 12月のみ11月26日から12月末まで
				o_period += by + "/11/26' and 納品書日<'" + ny + "/1/1'";
			} else {// それ以外の月は、前月26日から当月25日まで
				o_period += by + "/" + bm + "/26' and 納品書日<'" + y + "/" + m + "/26'";
			}
		}

		String query = "";
		switch (order) {
			case PURCHASE_1:
				query = "select" +
						" case when 注文先名 is null then '' else convert(varchar,仕入先CD) end as 仕入先CD," +
						" 注文先名," +
						" case when 納品書番号=99999 then '' else convert(varchar, 納品書番号) end as 伝票番号," +
						" case when 納品書日 is null then '' else convert(varchar, 納品書日) end as 日付," +
						" 注文番号,金額,消費税," +
						" 大分類名 as 科目名" +
						" from (" +
						"  select" +
						"  case when 会社名 is null then 99999 else p.仕入先CD end as 仕入先CD," +
						"  種別CD," +
						"  case when 納品書番号 is null then '' else " +
						"   CASE WHEN 会社名 IS NULL THEN ''" +
						"        WHEN 種別CD = 1 THEN '㈱' + 会社名" +
						"        WHEN 種別CD = 2 THEN 会社名 + '㈱'" +
						"        WHEN 種別CD = 3 THEN '㈲' + 会社名" +
						"        WHEN 種別CD = 4 THEN 会社名 + '㈲'" +
						"        ELSE 会社名 END" +
						"  end as 注文先名," +
						"  case when 納品書番号 is null then 99999 else 納品書番号 end as 納品書番号," +
						"  納品書日," +
						"  case when 納品書日 is null then '' else convert(varchar, 注文期)+'-'+convert(varchar,注文番号)+convert(varchar,注文枝番) end as 注文番号," +
						"  合計 as 金額," +
						"  税合計 as 消費税," +
						"  case when 会社名 is null then '総合計'" +
						"       when 大分類名 is null then '合計'" +
						"       when 納品書番号 is null then '小計'" +
						"       else 大分類名" +
						"       end as 大分類名," +
						"  case when 大分類名 is null then 999 else 大分類CD end as 大分類CD" +
						"  from (" +
						"   select" +
						"    納品書番号," +
						"    sum(伝票計) as 合計," +
						"    sum(消費税) as 税合計," +
						"    仕入先CD," +
						"    min(p.在庫親ID) as 在庫親ID," +
						"    大分類CD" +
						"   from (" +
						"    select sum(金額) as 伝票計," +
						"     min(在庫親ID) as 在庫親ID," +
						"     min(大分類CD) as 大分類CD," +
						"     納品書番号" +
						"    from T_在庫_子 c" +
						"    group by (納品書番号)" +
						"   ) x" +
						"   left outer join T_在庫_親 p on x.在庫親ID=p.在庫親ID" +
						"   left outer join T_指定納品書 d on x.納品書番号=d.ID" +
						"   where " + o_period +
						"   group by rollup(p.仕入先CD,大分類CD,納品書番号)" +
						"  ) a" +
						"  left outer join T_指定納品書 d on a.納品書番号=d.ID" +
						"  left outer join T_在庫_親 p on a.在庫親ID=p.在庫親ID" +
						"  left outer join M_法人 corp on a.仕入先CD=corp.仕入先CD" +
						"  left outer join M_原価 cost on a.大分類CD=cost.CD" +
						" ) z" +
						" order by 仕入先CD,大分類CD,納品書番号";
				break;
			case PURCHASE_2:
				query = "select * from (" +
						"select " +
						" case when c.仕入先CD is null then '99999' else convert(varchar,c.仕入先CD) end as 仕入先CD," +
						" case when c.仕入先CD is null then '合計' else" +
						"  CASE when min(会社名) is null then ''" +
						"       WHEN min(種別CD) = 1 THEN '㈱' + min(会社名)" +
						"       WHEN min(種別CD) = 2 THEN min(会社名) + '㈱'" +
						"       WHEN min(種別CD) = 3 THEN '㈲' + min(会社名)" +
						"       WHEN min(種別CD) = 4 THEN min(会社名) + '㈲'" +
						"       ELSE min(会社名) END" +
						" end as 注文先名," +
						" SUM(消費税+合計額) as 税込金額" +
						"  from T_指定納品書 d" +
						"  left outer join (" +
						"   select" +
						"    sum(金額) as 合計額," +
						"    納品書番号," +
						"    min(仕入先CD) as 仕入先CD" +
						"   from T_在庫_子 cc" +
						"   left outer join T_在庫_親 pp on cc.在庫親ID=pp.在庫親ID" +
						"   group by 納品書番号,仕入先CD" +
						"  ) c" +
						"  on d.ID=c.納品書番号" +
						"  left outer join M_法人 corp on c.仕入先CD=corp.仕入先CD" +
						"  where " + o_period +
						"  group by rollup(c.仕入先CD)" +
						" ) a" +
						" order by a.仕入先CD";
				break;
			case PURCHASE_3:
				query = "select " +
						" case when 科目コード is null then 9999 else 科目コード end as 科目コード," +
						" case when 科目コード is null then '合計' else min(大分類名) end as 科目名," +
						" SUM(消費税+合計額) as 税込金額" +
						" from T_指定納品書 d" +
						" left outer join (" +
						"  select MIN(大分類CD) AS 大分類CD,sum(金額) as 合計額,納品書番号 from T_在庫_子 group by 納品書番号" +
						" ) c" +
						" on d.ID=c.納品書番号" +
						" left outer join M_原価 cost on c.大分類CD=cost.CD" +
						" left outer join M_科目コード code on cost.CD=code.原価CD" +
						" where " + o_period +
						" group by rollup(科目コード)" +
						" order by 科目コード";
				break;
			case PURCHASE_4:
				query = "select 科目コード,科目名," +
						" CASE when 会社名 is null then ''" +
						" WHEN 種別CD = 1 THEN '㈱' + 会社名" +
						" WHEN 種別CD = 2 THEN 会社名 + '㈱'" +
						" WHEN 種別CD = 3 THEN '㈲' + 会社名" +
						" WHEN 種別CD = 4 THEN 会社名 + '㈲'" +
						" ELSE 会社名 END as 注文先名," +
						" 税込金額 from (" +
						"  select" +
						"   case when 科目コード is null then 9999 else 科目コード end as 科目コード," +
						"   case when 科目コード is null then '合計' else min(大分類名) end as 科目名," +
						"   min(種別CD) as 種別CD," +
						"   会社名," +
						"   case when 会社名 is null then 99999 else min(c.仕入先CD) end as 仕入先CD," +
						"   SUM(消費税+合計額) as 税込金額" +
						"   from T_指定納品書 d" +
						"   left outer join (" +
						"    select" +
						"     MIN(大分類CD) AS 大分類CD," +
						"     sum(金額) as 合計額," +
						"     納品書番号," +
						"     min(仕入先CD) as 仕入先CD" +
						"    from T_在庫_子 cc" +
						"    left outer join T_在庫_親 pp on cc.在庫親ID=pp.在庫親ID" +
						"    group by 納品書番号,仕入先CD" +
						"   ) c" +
						"   on d.ID=c.納品書番号" +
						"   left outer join M_原価 cost on c.大分類CD=cost.CD" +
						"   left outer join M_科目コード code on cost.CD=code.原価CD" +
						"   left outer join M_法人 corp on c.仕入先CD=corp.仕入先CD" +
						"   where " + o_period +
						"   group by rollup(科目コード,会社名)" +
						" ) a" +
						" order by 科目コード, a.仕入先CD";
				break;
			case PURCHASE_5:
				query = "select " +
						" case when 注文番号 like '999-9999' then '9999'" +
						"      when 注文番号 is null then '合計'" +
						"      else 注文番号 end as 注文番号," +
						" sum(金額) as 金額 from (" +
						"	select " +
						"	 convert(varchar,case when 注文期>1000 then 999 else 注文期 end) +" +
						"	 '-' +" +
						"	 convert(varchar,case when 注文期>1000 then 9999 else 注文番号 end) +" +
						"	 case when 注文期>1000 then '' else convert(varchar,注文枝番) end as 注文番号," +
						"	 sum(金額) as 金額 from T_指定納品書 d" +
						"	left outer join  T_在庫_子 c on c.納品書番号=d.ID" +
						"	left outer join  T_在庫_親 p on c.在庫親ID=p.在庫親ID" +
						"	where " + o_period +
						"	group by 注文期,注文番号,注文枝番" +
						" ) a group by rollup(注文番号)" +
						" order by 注文番号";

				break;
			case PURCHASE_6: // 買掛帳票
				query = "select" +
					" convert(varchar,仕入先CD) as 仕入先コード," +
					" convert(varchar, 注文期) + '-' + convert(varchar,注文番号) + convert(varchar,注文枝番) as 注文番号," +
					" 伝票番号 as 注文コード," +
					" 入庫年月日 as 入荷日," +
					" 科目コード," +
					" 大分類名 as 科目名," +
					" sum(金額) as 金額," +
					" 消費税" +
					" from T_指定納品書 d" +
					" left outer join T_在庫_子 c on c.納品書番号=d.ID" +
					" left outer join T_在庫_親 p on c.在庫親ID=p.在庫親ID" +
					" left outer join M_科目コード a on a.原価CD=c.大分類CD" +
					" left outer join M_原価 b on b.CD=c.大分類CD" +
					" where 仕入先CD=" + supplier + " and " + o_period +
					" group by 仕入先CD, 科目コード, 大分類名, 注文年月日, 注文期, 注文番号, 注文枝番, 伝票番号, 入庫年月日, 消費税" +
					" order by 科目コード, 入荷日, 注文年月日, 注文番号";
				break;
			case PURCHASE_TAX:
				query = "select" +
						" sum(消費税) as 仕入消費税" +
						" from T_指定納品書 d" +
						" where " + o_period;
				break;
			case DISPATCH:
				query = "select 出庫年月日, convert(varchar, 製作期) + '-' + convert(varchar,製作番号) + convert(varchar,製作枝番) as 出庫製番, 品名, convert(varchar, convert(float, 数量)) as 数量, 金額" +
						" from T_出庫_親 p" +
						" left outer join T_出庫_子 c on c.出庫親ID=p.出庫親ID" +
						" where 出庫年月日>='" + y + "/" + m + "/1' and 出庫年月日<'" + ny + "/" + nm + "/1'";
				break;
			case SALES_1:
				query = "select case when 得意先名 is null then '合計' else 得意先名 end as 得意先名," +
						" convert(int,SUM(税込金額)) AS 金額" +
						" from (select 得意先CD,得意先名,税込金額" +
						"  from (" +
						"   select s.売上親ID,s.得意先CD," +
						"    CASE WHEN 種別CD=1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         WHEN 種別CD=2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         WHEN 種別CD=3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         WHEN 種別CD=4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         ELSE 会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END END AS 得意先名," +
						"    min(売上年月日) AS 売上年月日," +
						"    SUM(金額) AS 税込金額,売上FLG,pp.製作番号,納品区分CD" +
						"   from (select 製作親ID,金額,sp.売上親ID,得意先CD,売上年月日,売上FLG,納品区分CD" +
						"     from T_売上_子 sc left outer join T_売上_親 sp on sp.売上親ID=sc.売上親ID" +
						"     where 納品区分CD<5 and 売上年月日>='" + y + "/" + m + "/1' and 売上年月日<'" + ny + "/" + nm + "/1' and 売上FLG='true') s" +
						"   left outer join T_製作_親 pp on pp.製作親ID=s.製作親ID" +
						"   left outer join M_法人 c on s.得意先CD=c.得意先CD" +
						"   group by s.売上親ID,s.得意先CD,会社名,支店名,種別CD,売上FLG,pp.製作番号,納品区分CD) a" +
						"  where 製作番号<9000" +
						"  and not exists(select 1 from V_売上集計ヘッダ WHERE 売上年月日>='" + y + "/" + m + "/1' AND 売上年月日<'" + ny + "/" + nm + "/1'" +
						"  and 売上親ID=a.売上親ID group by 得意先CD)" +
						"  union all" +
						"  (SELECT 得意先CD,得意先名,税込金額 from (" +
						"   select h.得意先CD," +
						"    CASE WHEN 種別CD=1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         WHEN 種別CD=2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         WHEN 種別CD=3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         WHEN 種別CD=4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         ELSE 会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END END AS 得意先名," +
						"          SUM(金額) + ROUND(SUM(金額) * (SELECT 税率 FROM M_消費税 t WHERE 適用開始日<'" + ny + "/" + nm + "/1'" +
						"          AND NOT EXISTS (" +
						"           SELECT 1 FROM M_消費税 t2 WHERE t.適用開始日<t2.適用開始日 AND 適用開始日<'" + ny + "/" + nm + "/1')" +
						"			),0) AS 税込金額" +
						"   FROM V_売上集計ヘッダ h" +
						"   left outer join M_法人 c on h.得意先CD=c.得意先CD" +
						"   WHERE 売上年月日>='" + y + "/" + m + "/1' AND 売上年月日<'" + ny + "/" + nm + "/1'" +
						"   group by h.得意先CD,会社名,支店名,種別CD) b)) af" +
						" group by rollup(得意先名)" +
						" order by case when 得意先名 is null then '9999' else max(得意先CD) end";
				break;
			case SALES_2:
				query = "select case when 台 is null then '合計' else 台+'0' end AS 製番," +
						" SUM(金額) as 合計金額 from (" +
						"  select LEFT(製作番号,1) as 台,金額 from T_売上_子 c" +
						"  left outer join T_売上_親 p" +
						"  on c.売上親ID=p.売上親ID" +
						"  left outer join T_製作_親 pp" +
						"  on c.製作親ID=pp.製作親ID" +
						"  where 表示CD<>5" +
						"   and 売上年月日>='" + y + "/" + m + "/1'" +
						"   AND 売上年月日<'" + ny + "/" + nm + "/1'" +
						"   and 売上FLG='true'" +
						"   and 製作番号<9000" +
						"   and 納品区分CD<5) a" +
						" group by rollup(台)";
				break;
			// 売③は getSalesData、売④は getLedger、棚は getShelf
			case SALES_TAX:
				query = "select convert(integer,SUM(税合計)) as 売上消費税 from (" +
						" SELECT " +
						"  得意先CD," +
						"  SUM(金額) AS 納入合計," +
						"  round(SUM(金額) * " +
						"   (SELECT 税率 FROM M_消費税 t WHERE 適用開始日<'" + ny + "/" + nm + "/1'" +
						"    AND NOT EXISTS (" +
						"     SELECT 1 FROM M_消費税 t2 WHERE t.適用開始日<t2.適用開始日 AND 適用開始日<'" + ny + "/" + nm + "/1'" +
						"    )),0) AS 税合計" +
						" FROM V_売上集計ヘッダ" +
						" WHERE 売上年月日>='" + y + "/" + m + "/1'" +
						" AND 売上年月日<'" + ny + "/" + nm + "/1'" +
						" GROUP BY 得意先CD" +
						" union all" +
						" select 0,0,金額 from T_売上_子 c" +
						" left outer join T_売上_親 p" +
						" on c.売上親ID=p.売上親ID" +
						" WHERE 表示CD=5" +
						" AND 売上年月日>='" + y + "/" + m + "/1'" +
						" AND 売上年月日<'" + ny + "/" + nm + "/1'" +
						" and 売上FLG='true'" +
						") a";
				break;
			case QUOTATION:
				query = "select convert(varchar,見積期)+'-'+right('000' + convert(varchar, 見積番号), 3)+見積枝番 as 見積番号," +
						"CASE WHEN convert(varchar,製作期)+'-'+convert(varchar,製作番号)+製作枝番 IS NULL THEN '' else convert(varchar,製作期)+'-'+convert(varchar,製作番号)+製作枝番 end as 製作番号," +
						"    CASE WHEN 種別CD=1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         WHEN 種別CD=2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         WHEN 種別CD=3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         WHEN 種別CD=4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"         ELSE 会社名 END AS 得意先名," +
						"ep.案件名," +
						"見積金額" +
						" from T_見積_親 ep" +
						" left outer join T_見積製作 map on ep.見積親ID=map.見積親ID" +
						" left outer join T_製作_親 pp on map.製作親ID=pp.製作親ID" +
						" left outer join M_法人 c on ep.得意先CD=c.得意先CD" +
						" where 見積期 >=0 and 見積年月日>='" + y + "/" + m + "/1' and 見積年月日<'" + ny + "/" + nm + "/1'" +
						" order by 見積番号";
				break;
			case RECEIPT: // 入荷実績
				query = "select" +
					" convert(varchar,仕入先CD) as 仕入先コード," +
					" 注文年月日," +
					" convert(varchar, 注文期) + '-' + convert(varchar,注文番号) + convert(varchar,注文枝番) as 注文番号," +
					" 伝票番号 as 注文コード," +
					" 入庫年月日 as 入荷日," +
					" sum(金額) as 金額" +
					" from T_指定納品書 d" +
					" left outer join T_在庫_子 c on c.納品書番号=d.ID" +
					" left outer join T_在庫_親 p on c.在庫親ID=p.在庫親ID" +
					" where 仕入先CD=" + supplier + " and " + o_period +
					" group by 仕入先CD, 注文年月日, 注文期, 注文番号, 注文枝番, 伝票番号, 入庫年月日" +
					" order by 入荷日, 注文年月日, 注文番号";
				break;
			case CHECK_DELIVERY: // 納品書入力チェック
				query = "select 納品書番号,合計額,convert(int,round(合計額*税率,0)) as 計算消費税,消費税,消費税-convert(int,round(合計額*税率,0)) as 差額" +
						" from (select min(p.在庫親ID) as 在庫親ID,sum(金額) as 合計額,税率,納品書番号 from T_在庫_子 c" +
						" left outer join T_在庫_親 p on c.在庫親ID=p.在庫親ID" +
						" left outer join (select 税率 from M_消費税 t where 適用開始日<'" + ny + "/" + nm + "/1'" +
						" AND NOT EXISTS (" +
						" SELECT 1 FROM M_消費税 t2 WHERE t.適用開始日<t2.適用開始日 AND 適用開始日<'" + ny + "/" + nm + "/1'" +
						" )) tax on tax.税率 is not null" +
						"  group by 納品書番号,税率) o" +
						" left outer join T_指定納品書 d on o.納品書番号=d.ID" +
						" where " + o_period +
						" order by 納品書番号";
				break;
			case CREATE_DELIVERY: // 注文書から納品書を生成
				query = "select 納品書番号,convert(varchar, 注文期)+'-'+convert(varchar, 注文番号)+convert(varchar, 注文枝番)as 注文番号,p.仕入先CD,会社名," +
						" 材料品名,case when 各FLG='true' then '各' else '' end as 各,数量,単価,金額,消費税" +
						" from T_在庫_親 p" +
						" left outer join T_在庫_子 c on p.在庫親ID=c.在庫親ID" +
						" left outer join M_法人 co on p.仕入先CD=co.仕入先CD" +
						" left outer join T_指定納品書 d on c.納品書番号=d.ID" +
						" where 納品書番号>0 and " + o_period +
						" order by 納品書番号,c.ID";
				break;
			case AGGREGATE_ACCOUNT: // 科目集計表
				query = "select 仕入先CD," +
						"  CASE when 会社名 is null then ''" +
						"  WHEN 種別CD = 1 THEN '㈱' + 会社名" +
						"  WHEN 種別CD = 2 THEN 会社名 + '㈱'" +
						"  WHEN 種別CD = 3 THEN '㈲' + 会社名" +
						"  WHEN 種別CD = 4 THEN 会社名 + '㈲'" +
						"  ELSE 会社名 END as 注文先名," +
						"  科目コード,科目名,合計金額," +
						"  convert(int, 合計金額 * " +
						"  (SELECT 税率 FROM M_消費税 t WHERE 適用開始日<'" + ny + "/" + nm + "/1'" +
						"    AND NOT EXISTS (" +
						"     SELECT 1 FROM M_消費税 t2 WHERE t.適用開始日<t2.適用開始日 AND 適用開始日<'" + ny + "/" + nm + "/1'" +
						"    )" +
						"  )) as 消費税" +
						"  from (" +
						"   select" +
						"    case when 科目コード is null then 9999 else 科目コード end as 科目コード," +
						"    case when 科目コード is null then '合計' else min(大分類名) end as 科目名," +
						"    min(種別CD) as 種別CD," +
						"    会社名," +
						"    case when 会社名 is null then 99999 else min(c.仕入先CD) end as 仕入先CD," +
						"    SUM(合計額) as 合計金額" +
						"    from (" +
						"     select" +
						"      大分類CD," +
						"      sum(金額) as 合計額," +
						"      入庫年月日," +
						"      min(仕入先CD) as 仕入先CD" +
						"     from T_在庫_子 cc" +
						"     left outer join T_在庫_親 pp on cc.在庫親ID=pp.在庫親ID" +
						"     group by 大分類CD, 入庫年月日, 仕入先CD" +
						"    ) c" +
						"    left outer join M_原価 cost on c.大分類CD=cost.CD" +
						"    left outer join M_科目コード code on cost.CD=code.原価CD" +
						"    left outer join M_法人 corp on c.仕入先CD=corp.仕入先CD" +
						"    where 入庫年月日>='" + y + "/" + m + "/1' and 入庫年月日<'" + ny + "/" + nm + "/1'" +
						"    group by rollup(会社名, 科目コード)" +
						"  ) a" +
						"  order by a.仕入先CD, 科目コード";
				break;
			case CHECK_ACCOUNT: // 科目確認表
				query = "select 科目コード,科目名,仕入先CD," +
						"  CASE when 会社名 is null then ''" +
						"  WHEN 種別CD = 1 THEN '㈱' + 会社名" +
						"  WHEN 種別CD = 2 THEN 会社名 + '㈱'" +
						"  WHEN 種別CD = 3 THEN '㈲' + 会社名" +
						"  WHEN 種別CD = 4 THEN 会社名 + '㈲'" +
						"  ELSE 会社名 END as 注文先名," +
						"  合計金額,消費税,合計金額+消費税 as 税込金額 from (" +
						"   select" +
						"    case when 科目コード is null then 9999 else 科目コード end as 科目コード," +
						"    case when 科目コード is null then '合計' else min(大分類名) end as 科目名," +
						"    min(種別CD) as 種別CD," +
						"    会社名," +
						"    case when 会社名 is null then 99999 else min(c.仕入先CD) end as 仕入先CD," +
						"    SUM(消費税) as 消費税," +
						"    SUM(合計額) as 合計金額" +
						"    from T_指定納品書 d" +
						"    left outer join (" +
						"     select" +
						"      MIN(大分類CD) AS 大分類CD," +
						"      sum(金額) as 合計額," +
						"      納品書番号," +
						"      min(仕入先CD) as 仕入先CD" +
						"     from T_在庫_子 cc" +
						"     left outer join T_在庫_親 pp on cc.在庫親ID=pp.在庫親ID" +
						"     group by 納品書番号,仕入先CD" +
						"    ) c" +
						"    on d.ID=c.納品書番号" +
						"    left outer join M_原価 cost on c.大分類CD=cost.CD" +
						"    left outer join M_科目コード code on cost.CD=code.原価CD" +
						"    left outer join M_法人 corp on c.仕入先CD=corp.仕入先CD" +
						"    where " + o_period +
						"    group by rollup(科目コード,会社名)" +
						"  ) a" +
						"  order by 科目コード, a.仕入先CD";
				break;
			case MH: // 月次工数サマリー
				query = "select case when 台 is null then '計' else 台+'0' end as 台, convert(float, sum(時間))/100 AS 工数 from " +
						"(select 製作期,left(製作番号, 1) as 台, 時間" +
						" from T_加工実績 where 着手日時>='" + y + "/" + m + "/1'" +
						" and 着手日時<'" + ny + "/" + nm + "/1') w group by rollup(台)";
				break;
			case SALES_MANAGEMENT: // 売上管理
				query = "SELECT cast(p.製作期 as varchar)+'-'+cast(p.製作番号 as varchar)+p.製作枝番 AS 製番,会社名,支店名,案件名,売上年月日,売上,CASE WHEN 購入額 IS NULL THEN 0 ELSE 購入額 END+CASE WHEN 出庫額 IS NULL THEN 0 ELSE 出庫額 END+CASE WHEN 加工費 IS NULL THEN 0 ELSE 加工費 END AS 原価,購入額,出庫額,加工費" +
						" FROM (SELECT 製作親ID,MAX(売上年月日) AS 売上年月日,SUM(金額) AS 売上 FROM (SELECT * FROM T_売上_親 WHERE 売上年月日>='" + y + "/" + m + "/1' AND 売上年月日<'" + ny + "/" + nm + "/1') sp " +
						" LEFT OUTER JOIN T_売上_子 sc ON sp.売上親ID=sc.売上親ID" +
						" WHERE sc.表示CD=2 OR sc.表示CD=6 GROUP BY 製作親ID) s" +
						" LEFT OUTER JOIN T_製作_親 p ON s.製作親ID=p.製作親ID" +
						" LEFT OUTER JOIN (SELECT 製作期,製作番号,製作枝番,SUM(時間*w2.単価/100) AS 加工費 FROM T_加工実績 w2 LEFT OUTER JOIN M_加工_子 wc ON w2.加工CD=wc.CD GROUP BY 製作期,製作番号,製作枝番) w ON p.製作期=w.製作期 AND p.製作番号=w.製作番号 AND p.製作枝番=w.製作枝番" +
						" LEFT OUTER JOIN (SELECT 注文期,注文番号,注文枝番,SUM(金額) AS 購入額 FROM T_在庫_親 bp LEFT OUTER JOIN T_在庫_子 bc ON bp.在庫親ID=bc.在庫親ID GROUP BY 注文期,注文番号,注文枝番) b ON p.製作期=b.注文期 AND p.製作番号=b.注文番号 AND p.製作枝番=b.注文枝番" +
						" LEFT OUTER JOIN (SELECT 製作期,製作番号,製作枝番,SUM(金額) AS 出庫額 FROM T_出庫_親 dp LEFT OUTER JOIN T_出庫_子 dc ON dp.出庫親ID=dc.出庫親ID GROUP BY 製作期,製作番号,製作枝番) d ON p.製作期=d.製作期 AND p.製作番号=d.製作番号 AND p.製作枝番=d.製作枝番" +
						" LEFT OUTER JOIN M_法人 c ON p.得意先CD=c.得意先CD";
				break;
		}
		return query;
	}
}
