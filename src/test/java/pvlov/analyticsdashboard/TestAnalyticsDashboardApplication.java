package pvlov.analyticsdashboard;

import org.springframework.boot.SpringApplication;

public class TestAnalyticsDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.from(AnalyticsDashboardApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
