package com.example.boardproject.query;


import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

@Component
public class QueryCountInspector implements StatementInspector {
    // 쿼리를 hibernate에서 제공하는 statementinspector을 사용하기 위해

    // StatementInspector가 Serializable을 상속하므로 직렬화 대상에서 제외
    private final transient ThreadLocal<QueryCounter>queryCount =new ThreadLocal<>();


    public  void startCount() {
        queryCount.set(new QueryCounter(0L,System.currentTimeMillis()));
    }

    //쿼리 수 가져오기, 호출 쿼리수 뜸.
    public QueryCounter getQueryCount(){
        return queryCount.get();
    }

    public void clearCounter(){
        queryCount.remove();
    }

    @Override
    public String inspect(String sql){
        QueryCounter queryCounter  = queryCount.get();
        if(queryCounter != null){
            queryCounter.incressCount();
            queryCounter.addQuery(sql);
        }
        return sql;

    }
}
