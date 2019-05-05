package org.evomaster.client.java.controller.internal.db;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.evomaster.client.java.controller.db.DataRow;
import org.evomaster.client.java.controller.db.QueryResult;

import java.util.List;

import static org.evomaster.client.java.controller.internal.db.ParserUtils.getWhere;

public class SelectHeuristics {



    /**
     * The constraints in the WHERE clause might reference
     * fields that are not retrieved in the SELECT.
     * Therefore, we need to add them, otherwise it
     * would not be possible to calculate any heuristics
     *
     * @param select the string containing the SQL SELECT command
     * @return  the modified SQL SELECT
     */
    public static String addFieldsToSelect(String select) {

        Select stmt = asSelectStatement(select);

        SelectBody selectBody = stmt.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;

            Expression where = plainSelect.getWhere();
            if (where == null) {
                //nothing to do
                return select;
            }

            List<SelectItem> fields = plainSelect.getSelectItems();

            where.accept(new ExpressionVisitorAdapter() {
                @Override
                public void visit(Column column) {

                    String target = column.toString();

                    boolean found = false;
                    for (SelectItem si : fields) {
                        SelectExpressionItem field = (SelectExpressionItem) si;
                        String exp = field.getExpression().toString();
                        if (target.equals(exp)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        SelectExpressionItem item = new SelectExpressionItem();
                        item.setExpression(column);
                        fields.add(item);
                    }
                }
            });
        }

        return stmt.toString();
    }

    /**
     * For example, when we have "select count(*)" we are not interested
     * in the count, but the actual involved fields, so we want to
     * transform it into "select *" by removing the count() operation.
     *
     * @param select
     * @return
     */
    public static String removeOperations(String select){

        Select stmt = asSelectStatement(select);
        SelectBody selectBody = stmt.getSelectBody();

        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;

            plainSelect.getSelectItems()
                    .removeIf(item ->
                            ((SelectExpressionItem)item).getExpression() instanceof Function);
        }

        return stmt.toString();
    }


    public static String removeConstraints(String select) {

        Select stmt = asSelectStatement(select);

        SelectBody selectBody = stmt.getSelectBody();
        handleSelectBody(selectBody);

        return stmt.toString();
    }

    private static Select asSelectStatement(String select) {
        Statement stmt = ParserUtils.asStatement(select);
        if(! (stmt instanceof Select)){
            throw new IllegalArgumentException("SQL statement is not a SELECT: " + select);
        }
        return (Select) stmt;
    }

    private static void handleSelectBody(SelectBody selectBody) {
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            plainSelect.setWhere(null);
            plainSelect.setLimit(null);
        } else if (selectBody instanceof SetOperationList) {
            for(SelectBody select : ((SetOperationList) selectBody).getSelects()){
                handleSelectBody(select);
            }
        } else {
            throw new RuntimeException("Cannot handle " + selectBody.getClass());
        }

    }


    public static double computeDistance(String select, QueryResult data) {

        Select stmt = asSelectStatement(select);

        if (data.isEmpty()) {
            //if no data, we have no info whatsoever
            return Double.MAX_VALUE;
        }

        Expression where = getWhere(stmt);
        if (where == null) {
            //no constraint, and at least one data point
            return 0;
        }


        SqlNameContext context = new SqlNameContext(stmt);
        HeuristicsCalculator calculator = new HeuristicsCalculator(context);

        double min = Double.MAX_VALUE;
        for (DataRow row : data.seeRows()) {
            double dist = calculator.computeExpression(where, row);
            if (dist == 0) {
                return 0;
            }
            if (dist < min) {
                min = dist;
            }
        }

        return min;
    }




}
