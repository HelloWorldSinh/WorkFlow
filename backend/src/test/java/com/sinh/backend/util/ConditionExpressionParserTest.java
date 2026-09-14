package com.sinh.backend.util;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConditionExpressionParserTest {

    @Test
    void testParseNestedExpression() {
        String expression = "(department == \"IT\" && level > 3) || role == \"ADMIN\"";
        
        Map<String, Object> tree = ConditionExpressionParser.parseToTree(expression);
        assertNotNull(tree);
        assertEquals("OR", tree.get("logicalOperator"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) tree.get("children");
        assertNotNull(children);
        assertEquals(2, children.size());

        // First child should be AND group
        Map<String, Object> andGroup = children.get(0);
        assertEquals("AND", andGroup.get("logicalOperator"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> andChildren = (List<Map<String, Object>>) andGroup.get("children");
        assertEquals(2, andChildren.size());

        // Rule 1: department == "IT" (STRING)
        Map<String, Object> rule1 = andChildren.get(0);
        assertEquals("department", rule1.get("field"));
        assertEquals("EQUALS", rule1.get("operator"));
        assertEquals("IT", rule1.get("value"));
        assertEquals("STRING", rule1.get("dataType"));

        // Rule 2: level > 3 (NUMBER)
        Map<String, Object> rule2 = andChildren.get(1);
        assertEquals("level", rule2.get("field"));
        assertEquals("GREATER_THAN", rule2.get("operator"));
        assertEquals(3L, rule2.get("value"));
        assertEquals("NUMBER", rule2.get("dataType"));

        // Second child: role == "ADMIN" (STRING)
        Map<String, Object> rule3 = children.get(1);
        assertEquals("role", rule3.get("field"));
        assertEquals("EQUALS", rule3.get("operator"));
        assertEquals("ADMIN", rule3.get("value"));
        assertEquals("STRING", rule3.get("dataType"));
    }

    @Test
    void testParseSimpleCondition() {
        String expression = "amount > 5000000";
        Map<String, Object> tree = ConditionExpressionParser.parseToTree(expression);
        assertNotNull(tree);
        assertEquals("amount", tree.get("field"));
        assertEquals("GREATER_THAN", tree.get("operator"));
        assertEquals(5000000L, tree.get("value"));
        assertEquals("NUMBER", tree.get("dataType"));
    }

    @Test
    void testParseBooleanAndDateTypes() {
        String expression = "is_active == true && start_date >= \"2026-01-01\"";
        Map<String, Object> tree = ConditionExpressionParser.parseToTree(expression);
        assertNotNull(tree);
        assertEquals("AND", tree.get("logicalOperator"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) tree.get("children");
        assertEquals(2, children.size());

        assertEquals("BOOLEAN", children.get(0).get("dataType"));
        assertEquals(true, children.get(0).get("value"));

        assertEquals("DATE", children.get(1).get("dataType"));
        assertEquals("2026-01-01", children.get(1).get("value"));
    }

    @Test
    void testInvalidParenthesesThrowsException() {
        String invalidExpr = "(department == \"IT\" && level > 3";
        assertThrows(IllegalArgumentException.class, () -> ConditionExpressionParser.parseToTree(invalidExpr));
    }

    @Test
    void testParseVariableToVariableComparison() {
        String expression = "actual_expense > budget_limit";
        Map<String, Object> tree = ConditionExpressionParser.parseToTree(expression);
        assertNotNull(tree);
        assertEquals("actual_expense", tree.get("field"));
        assertEquals("GREATER_THAN", tree.get("operator"));
        assertEquals("budget_limit", tree.get("value"));
        assertEquals("FIELD_REF", tree.get("valueType"));
        assertEquals("VARIABLE", tree.get("dataType"));
    }
}
