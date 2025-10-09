package pvlov.analyticsdashboard;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

@Configuration
public class DatabaseConnectionConfiguration {

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Bean("readOnlyConnectionFactory")
    @Primary
    public ConnectionFactory readOnlyConnectionFactory(@Value("${app.r2dbc.read-url}") final String url) {
        final ConnectionFactoryOptions options = ConnectionFactoryOptions.parse(url)
                .mutate()
                .option(ConnectionFactoryOptions.USER, this.username)
                .option(ConnectionFactoryOptions.PASSWORD, this.password)
                .build();
        return ConnectionFactories.get(options);
    }

    @Bean("writeConnectionFactory")
    public ConnectionFactory writeConnectionFactory(@Value("${app.r2dbc.write-url}") final String url) {
        final ConnectionFactoryOptions options = ConnectionFactoryOptions.parse(url)
                .mutate()
                .option(ConnectionFactoryOptions.USER, this.username)
                .option(ConnectionFactoryOptions.PASSWORD, this.password)
                .build();
        return ConnectionFactories.get(options);
    }

    @Bean
    public ConnectionFactoryInitializer initializer(
            @Qualifier("writeConnectionFactory") final ConnectionFactory connectionFactory
    ) {
        final ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        final ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"),
                new ClassPathResource("data.sql")
        );
        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}
