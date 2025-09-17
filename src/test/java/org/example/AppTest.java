package org.example;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class AppTest {

    @BeforeAll
    public static void setup() {
        RestAssured.defaultParser = Parser.JSON;
    }

    @Test
    public void checkStatusCode() {
        when().
                get("https://openlibrary.org/people/anasjarrar/lists.json").
                then().
                statusCode(200).
                body("size", equalTo(99));

        Response response = RestAssured.get("https://openlibrary.org/people/anasjarrar/lists.json");
        assertTrue(response.statusCode() == 200);
        assertTrue(response.time() < 3000, "Response took longer than 10 seconds");
    }
    @Test
    public void validateNameFirstEntries() {
        given().get("https://openlibrary.org/people/anasjarrar/lists.json").
                then().assertThat().body("entries[0].'name'", equalTo("List Name 1")).
                and().assertThat().body("entries[0].'seed_count'", equalTo(2));
    }
    @Test
    public void validateEntriesSize() {
        Response response = given().
                get("https://openlibrary.org/people/anasjarrar/lists.json?limit=100&offset=0");

        int sizeFromField = response.jsonPath().getInt("size");
        int actualEntries = response.jsonPath().getList("entries").size();
        assertEquals(sizeFromField, actualEntries);

    }
    @Test
    public void noNullUrl(){
        List<String> urls = given().
                get("https://openlibrary.org/people/anasjarrar/lists.json").
                then().
                extract().
                jsonPath().getList("entries.url");

        assertTrue(urls.stream().allMatch(u -> u != null && !u.isEmpty()));

    }

}
