CREATE TABLE passenger (
    id          INT PRIMARY KEY,
    survived    BOOLEAN,
    pclass      TINYINT,
    name        VARCHAR(100),
    sex         VARCHAR(10),
    age         DECIMAL(4,1),
    sibsp       TINYINT,
    parch       TINYINT,
    ticket      VARCHAR(20),
    fare        DECIMAL(8,2),
    cabin       VARCHAR(20),
    embarked    CHAR(1)
);
