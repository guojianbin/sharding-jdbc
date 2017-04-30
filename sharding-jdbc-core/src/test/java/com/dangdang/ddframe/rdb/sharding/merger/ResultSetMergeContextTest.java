/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.sharding.merger;

import com.dangdang.ddframe.rdb.sharding.merger.fixture.MergerTestUtil;
import com.dangdang.ddframe.rdb.sharding.parser.contstant.AggregationType;
import com.dangdang.ddframe.rdb.sharding.parser.contstant.SQLType;
import com.dangdang.ddframe.rdb.sharding.parser.result.SQLParsedResult;
import com.dangdang.ddframe.rdb.sharding.parser.result.router.ConditionContext;
import com.dangdang.ddframe.rdb.sharding.parser.sql.context.GroupByContext;
import com.dangdang.ddframe.rdb.sharding.parser.sql.context.OrderByContext;
import com.dangdang.ddframe.rdb.sharding.parser.contstant.OrderType;
import com.google.common.base.Optional;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class ResultSetMergeContextTest {
    
    @Test
    public void assertNewResultSetMergeContext() throws SQLException {
        ResultSetMergeContext actual = new ResultSetMergeContext(
                new ShardingResultSets(Collections.singletonList(MergerTestUtil.mockResult(Arrays.asList("order_col", "group_col", "count_col", "avg_col", "sharding_gen_1", "sharding_gen_2")))), 
                createSQLParsedResult());
        assertThat(actual.getSqlParsedResult().getOrderByContexts().get(0).getColumnIndex(), is(1));
        assertThat(actual.getSqlParsedResult().getGroupByContexts().get(0).getColumnIndex(), is(2));
        assertThat(actual.getSqlParsedResult().getAggregationColumns().get(0).getColumnIndex(), is(3));
        assertThat(actual.getSqlParsedResult().getAggregationColumns().get(1).getColumnIndex(), is(4));
        assertThat(actual.getSqlParsedResult().getAggregationColumns().get(1).getDerivedAggregationSelectItemContexts().get(0).getColumnIndex(), is(5));
        assertThat(actual.getSqlParsedResult().getAggregationColumns().get(1).getDerivedAggregationSelectItemContexts().get(1).getColumnIndex(), is(6));
        assertThat(actual.getCurrentOrderByKeys(), is(actual.getSqlParsedResult().getOrderByContexts()));
    }
    
    private SQLParsedResult createSQLParsedResult() {
        SQLParsedResult result = new SQLParsedResult(SQLType.SELECT, new ConditionContext());
        result.getOrderByContexts().add(new OrderByContext("order_col", OrderType.ASC, Optional.<String>absent()));
        result.getGroupByContexts().add(new GroupByContext(Optional.<String>absent(), "group_col", OrderType.ASC, Optional.<String>absent()));
        result.getAggregationColumns().add(MergerTestUtil.createAggregationColumn(AggregationType.COUNT, "count_col", "count_col", -1));
        result.getAggregationColumns().add(MergerTestUtil.createAggregationColumn(AggregationType.AVG, "avg_col", "avg_col", -1));
        return result;
    }
    
    @Test
    public void assertIsNotNeedMemorySortForGroupByWithoutGroupBy() throws SQLException {
        ResultSetMergeContext actual = new ResultSetMergeContext(
                new ShardingResultSets(Collections.singletonList(MergerTestUtil.mockResult(Collections.<String>emptyList()))), new SQLParsedResult(SQLType.SELECT, new ConditionContext()));
        assertFalse(actual.isNeedMemorySortForGroupBy());
    }
    
    @Test
    public void assertIsNeedMemorySortForGroupByWithGroupByAndOrderBySame() throws SQLException {
        SQLParsedResult sqlParsedResult = new SQLParsedResult(SQLType.SELECT, new ConditionContext());
        sqlParsedResult.getOrderByContexts().add(new OrderByContext("col", OrderType.ASC, Optional.<String>absent()));
        sqlParsedResult.getGroupByContexts().add(new GroupByContext(Optional.<String>absent(), "col", OrderType.ASC, Optional.<String>absent()));
        ResultSetMergeContext actual = new ResultSetMergeContext(new ShardingResultSets(Collections.singletonList(MergerTestUtil.mockResult(Collections.singletonList("col")))), sqlParsedResult);
        assertFalse(actual.isNeedMemorySortForGroupBy());
    }
    
    @Test
    public void assertIsNeedMemorySortForGroupByWithGroupByAndOrderByDifferent() throws SQLException {
        SQLParsedResult sqlParsedResult = new SQLParsedResult(SQLType.SELECT, new ConditionContext());
        sqlParsedResult.getOrderByContexts().add(new OrderByContext("order_col", OrderType.ASC, Optional.<String>absent()));
        sqlParsedResult.getGroupByContexts().add(new GroupByContext(Optional.<String>absent(), "group_col", OrderType.ASC, Optional.<String>absent()));
        ResultSetMergeContext actual = new ResultSetMergeContext(
                new ShardingResultSets(Collections.singletonList(MergerTestUtil.mockResult(Arrays.asList("order_col", "group_col")))), sqlParsedResult);
        assertTrue(actual.isNeedMemorySortForGroupBy());
    }
    
    @Test
    public void assertSetGroupByKeysToCurrentOrderByKeys() throws SQLException {
        SQLParsedResult sqlParsedResult = new SQLParsedResult(SQLType.SELECT, new ConditionContext());
        sqlParsedResult.getOrderByContexts().add(new OrderByContext("order_col", OrderType.ASC, Optional.<String>absent()));
        sqlParsedResult.getGroupByContexts().add(new GroupByContext(Optional.<String>absent(), "group_col", OrderType.ASC, Optional.<String>absent()));
        ResultSetMergeContext actual = new ResultSetMergeContext(
                new ShardingResultSets(Collections.singletonList(MergerTestUtil.mockResult(Arrays.asList("order_col", "group_col")))), sqlParsedResult);
        actual.setGroupByKeysToCurrentOrderByKeys();
        assertThat(actual.getCurrentOrderByKeys().size(), is(1));
        assertThat(actual.getCurrentOrderByKeys().get(0).getColumnName().get(), is("group_col"));
    }
    
    @Test
    public void assertIsNotNeedMemorySortForOrderByWithoutOrderBy() throws SQLException {
        ResultSetMergeContext actual = new ResultSetMergeContext(
                new ShardingResultSets(Collections.singletonList(MergerTestUtil.mockResult(Collections.<String>emptyList()))), new SQLParsedResult(SQLType.SELECT, new ConditionContext()));
        assertFalse(actual.isNeedMemorySortForOrderBy());
    }
    
    @Test
    public void assertIsNeedMemorySortForOrderByWithGroupByAndOrderBySame() throws SQLException {
        SQLParsedResult sqlParsedResult = new SQLParsedResult(SQLType.SELECT, new ConditionContext());
        sqlParsedResult.getOrderByContexts().add(new OrderByContext("col", OrderType.ASC, Optional.<String>absent()));
        sqlParsedResult.getGroupByContexts().add(new GroupByContext(Optional.<String>absent(), "col", OrderType.ASC, Optional.<String>absent()));
        ResultSetMergeContext actual = new ResultSetMergeContext(new ShardingResultSets(Collections.singletonList(MergerTestUtil.mockResult(Collections.singletonList("col")))), sqlParsedResult);
        assertFalse(actual.isNeedMemorySortForOrderBy());
    }
    
    @Test
    public void assertIsNeedMemorySortForOrderByWithGroupByAndOrderByDifferent() throws SQLException {
        SQLParsedResult sqlParsedResult = new SQLParsedResult(SQLType.SELECT, new ConditionContext());
        sqlParsedResult.getOrderByContexts().add(new OrderByContext("order_col", OrderType.ASC, Optional.<String>absent()));
        sqlParsedResult.getGroupByContexts().add(new GroupByContext(Optional.<String>absent(), "group_col", OrderType.ASC, Optional.<String>absent()));
        ResultSetMergeContext actual = new ResultSetMergeContext(
                new ShardingResultSets(Collections.singletonList(MergerTestUtil.mockResult(Arrays.asList("order_col", "group_col")))), sqlParsedResult);
        actual.setGroupByKeysToCurrentOrderByKeys();
        assertTrue(actual.isNeedMemorySortForOrderBy());
    }
    
    @Test
    public void assertSetOrderByKeysToCurrentOrderByKeys() throws SQLException {
        SQLParsedResult sqlParsedResult = new SQLParsedResult(SQLType.SELECT, new ConditionContext());
        sqlParsedResult.getOrderByContexts().add(new OrderByContext("order_col", OrderType.ASC, Optional.<String>absent()));
        sqlParsedResult.getGroupByContexts().add(new GroupByContext(Optional.<String>absent(), "group_col", OrderType.ASC, Optional.<String>absent()));
        ResultSetMergeContext actual = new ResultSetMergeContext(
                new ShardingResultSets(Collections.singletonList(MergerTestUtil.mockResult(Arrays.asList("order_col", "group_col")))), sqlParsedResult);
        actual.setGroupByKeysToCurrentOrderByKeys();
        actual.setOrderByKeysToCurrentOrderByKeys();
        assertThat(actual.getCurrentOrderByKeys().size(), is(1));
        assertThat(actual.getCurrentOrderByKeys().get(0).getColumnName().get(), is("order_col"));
    }
}