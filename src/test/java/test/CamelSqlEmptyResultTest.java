package test;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

public class CamelSqlEmptyResultTest extends CamelTestSupport {

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("seda:in")
                    .to("sql:select * from Persons where PersonID=1?outputType=selectOne")
                    .process(exchange -> {
                        Object body = exchange.getMessage().getBody();
                        System.err.println("Body: " + body);
                        if (body != null) {
                            System.err.println("Body class: " + body.getClass());
                        }
                    })
                    .setHeader("PersonID", simple("${body[PersonID]}"))
                    .to("mock:out");
            }
        };
    }

    @Override
    protected void bindToRegistry(Registry registry) throws Exception {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true)
                .build();
        new JdbcTemplate(db).execute("CREATE TABLE Persons (PersonID int, LastName varchar(255))");
        registry.bind("dataSource", db);
    }

    @Test
    public void test() throws Exception {
        MockEndpoint out = getMockEndpoint("mock:out");
        out.expectedMessageCount(1);

        template.sendBody("seda:in", "");

        out.assertIsSatisfied(3_000);
    }
}
