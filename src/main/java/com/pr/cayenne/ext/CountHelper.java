package com.pr.cayenne.ext;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.SelectTranslator;
import org.apache.cayenne.query.SelectQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 用于统计总行数的帮助类
 */
public class CountHelper {
    public static long count(DataContext context, SelectQuery query) {
        return count(context, query, context.getParentDataDomain().getDataNodes().iterator().next());
    }

    public static long count(DataContext context, SelectQuery query,
                             DataNode node) {
        CountTranslator translator = new CountTranslator();

        translator.setQuery(query);
        translator.setAdapter(node.getAdapter());
        translator.setEntityResolver(context.getEntityResolver());

        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = node.getDataSource().getConnection();
            translator.setConnection(con);

            stmt = translator.createStatement();

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }

            throw new RuntimeException("Count query returned no result");
        } catch (Exception e) {
            throw new RuntimeException("Cannot count", e);
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (con != null) con.close();
            } catch (Exception ex) {
                throw new RuntimeException("Cannot close connection", ex);
            }
        }
    }

    private static class CountTranslator extends SelectTranslator {
        @Override
        public String createSqlString() throws Exception {
            String sql = super.createSqlString();
            return "SELECT COUNT(*) FROM (" + sql + ") t_cnt";
        }
    }
}
