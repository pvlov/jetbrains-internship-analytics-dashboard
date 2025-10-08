Develop a simple Spring Application with REST API for storing and executing analytical SQL queries over a dataset.

For this assignment, you can use an in-memory database like H2. Load some small dataset into the database as a separate table.

You can use this classic Titanic passengers dataset https://github.com/datasciencedojo/datasets/blob/master/titanic.csv as an example.

The service must have the following API endpoints:

    Adding a query for later execution:

        The body should contain the query text

        For example, POST /queries with the body “SELECT * FROM passengers;”

        The result should contain the ID of the created query, like { “id”: 1 }

    Listing all stored queries:

        The result should return the IDs and texts of all saved queries

        For example, GET /queries should return [{ “id”: 1, “query”: “SELECT * FROM passengers;”}]

    Executing a stored query:

        The request should contain the ID of the query to execute

        The result should be a two-dimensional array with the data

        For example, GET /execute?query=1 should return [[1, 0, 3, “Braund, Mr Owen Harris”, …], …] with all the data for this query

        Don’t worry too much about result formatting

Bonus assignments (not required, but will be a big plus):

    The queries should not modify the data inside the database. How can you guarantee that?

    For larger datasets, queries might take some time to execute. Modify the API to account for this, and implement the solution that can handle long-running queries.

    The analytic data usually isn’t updated often, in this assignment it’s not updated at all. You can use this fact to improve the performance of your service.

    Along with unit tests, you can also add integration tests that run a full flow of adding, listing and running a query.

A complete solution should include:

    A repository with code and instructions on how to get it running

    Unit tests that cover all implemented features

    A document describing the design of your solution, listing your assumptions, design decisions, limitations of your approach and what might be improved further. This is the most important part of the assignment! 

Keep your solution simple enough to do this in a couple evenings. The best solution for this assignment would be simple, non-production-ready, but documented to highlight what its limitations are. It would be great if you can list the ways you can break the implemented system and the ways to prevent it.

Best way to send your assignment would be as a private GitHub repo and adding me (@dkaznacheev) as a collaborator.

Small descriptive commits are preferred to one large commit with the entire solution.

Be ready to answer questions on your design and implementation during the interview.