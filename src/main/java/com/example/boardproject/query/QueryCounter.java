package com.example.boardproject.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryCounter {

    private long count;
    private final long startTimeMillis;
    private final List<String> queries = new ArrayList<>();


    public QueryCounter(long count, long startTimeMillis) {
        this.count = count;
        this.startTimeMillis = startTimeMillis;
    }

    public void incressCount()
    {
         count++;

    }

    public void addQuery(String sql) {
        queries.add(sql);
    }

    public long getQueryCount(){
        return count;
    }

    public List<String> getQueries() {
        return Collections.unmodifiableList(queries);
    }

    public long getElapsedMillis() {
        return System.currentTimeMillis() - startTimeMillis;
    }


    public long startCount() {
        return startTimeMillis;
    }




}
