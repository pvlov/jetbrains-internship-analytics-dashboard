INSERT INTO passenger (survived, pclass, name, sex, age, sibsp, parch, ticket, fare, cabin, embarked) SELECT * FROM CSVREAD('classpath:initial-dataset.csv');
INSERT INTO passenger (survived, pclass, name, sex, age, sibsp, parch, ticket, fare, cabin, embarked) SELECT * FROM CSVREAD('classpath:fake-dataset.csv');
