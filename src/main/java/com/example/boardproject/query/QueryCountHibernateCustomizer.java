package com.example.boardproject.query;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

// QueryCountInspector를 Hibernate SessionFactory에 StatementInspector로 등록
@Component
@RequiredArgsConstructor
public class QueryCountHibernateCustomizer implements HibernatePropertiesCustomizer {

    private final QueryCountInspector queryCountInspector;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.session_factory.statement_inspector", queryCountInspector);
    }
}
