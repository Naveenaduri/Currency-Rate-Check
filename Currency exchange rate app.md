Currency exchange rate app

Feature Requirements:
when user submits from and to currency  find the exchange rate and show it to the user
stystem should be able to handle multiple currencies
able to cache and store the exchange rates
able to find path from to to currency


// implementation steps
define api contract for exchange rate api

back end 
1. create a new project
2. create services for exchange rate api use java  spring boot
3. use in memory data store for caching and storing the exchange rates
4. use graph traversal algorithm(union and disjoin set) to find the path from to to currency
5 add or remove currency from the system

front end 
react application to consume the api and display the exchange rates
auto render the exchange rates in a tableupon change detection from and to currency


have test with good code coverage of 85%



